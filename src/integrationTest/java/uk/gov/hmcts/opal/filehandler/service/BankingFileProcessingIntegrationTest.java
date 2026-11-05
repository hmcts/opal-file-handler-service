package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.images.builder.Transferable;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.common.launchdarkly.service.FeatureToggleApi;
import uk.gov.hmcts.opal.filehandler.config.AbstractBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.BusinessUnitBankAccountEntity;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.repository.BusinessUnitBankAccountRepository;
import uk.gov.hmcts.opal.filehandler.service.queue.FinesInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.service.queue.InterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.support.AbstractInterfaceFileProcessorServiceIntegrationTest;
import uk.gov.hmcts.opal.filehandler.support.TestContainerConfig;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraEpic;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraStory;

/** PO-7244: real SFTP, blob and database processing with controlled queue availability. */
public class BankingFileProcessingIntegrationTest extends AbstractInterfaceFileProcessorServiceIntegrationTest {

    private static final short BUSINESS_UNIT_ID = 7244;
    private static final String BANK_SORT_CODE = "724401";
    private static final String BANK_ACCOUNT_NUMBER = "72440001";
    private static final String COURT_CODE = "7244000001";

    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private BusinessUnitBankAccountRepository bankAccounts;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private FeatureToggleApi featureToggleApi;
    @MockitoBean
    private FinesInterfaceFilePreprocessQueueService finesQueue;
    @MockitoBean
    private MaintenanceInterfaceFilePreprocessQueueService maintenanceQueue;

    private AbstractBaisFileProcessorConfiguration configuration;
    private BankingFile file;
    private boolean bankAccountCreated;
    private String originalSftpUsername;
    private boolean fixtureClaimed;

    static Stream<BankingFile> bankingFiles() {
        return Stream.of(
            new BankingFile("DWP", "dwpBaisFileProcessorConfig", "DWP", "0000015232_dat_0000000612_08011008_111355.txt",
                Domain.MAINTENANCE, "DWP1234567"),
            new BankingFile("jacobs", "JacobsBaisFileProcessorConfig", "Jacobs",
                "0000031712_dat_0000098475_20260408_103500.txt",
                Domain.FINES, "0000031714"),
            new BankingFile("cder", "cderBaisFileProcessorConfig", "CDER",
                "0000031712_dat_0000098475_20260408_103500.txt",
                Domain.FINES, "0000031714"),
            new BankingFile("marston", "marstonBaisFileBaisFileProcessorConfig", "MARSTON",
                "1234567890_dat_0987654321_20260910_143015.txt",
                Domain.FINES, "DWP1234567"),
            new BankingFile("natWest", "NatWestBaisFileBaisFileProcessorConfig", "NATWEST",
                "Y01A.CARS.#D.SBURZ38.D080426",
                Domain.FINES, null),
            new BankingFile("barclaycard", "BarclaycardBaisFileProcessorConfig", "BARCLAYCARD",
                "a121_00010065_317608.dat",
                Domain.FINES, null),
            new BankingFile("allpay", "AllpayBaisFileBaisFileProcessorConfig", "AllPay", "a121_00350005_300000.dat",
                Domain.MAINTENANCE, null),
            new BankingFile("BTEckoh", "BTEckohBaisFileProcessorConfig", "BTEckoh", "a121_00350005_300000.dat",
                Domain.MAINTENANCE, null)
        );
    }

    @ParameterizedTest(name = "{0}: stored file is not retried while its processing flag is disabled")
    @MethodSource("bankingFiles")
    @JiraStory("PO-7244")
    @JiraEpic("PO-3497")
    void disabledFlagLeavesStoredFileUnprocessed(BankingFile bankingFile) throws IOException {
        InterfaceFileProcessorService processor = prepare(bankingFile);
        final InterfaceFileEntity failedJson = ingestWithUnavailableQueue(processor);
        final var before = storedBlobs(configuration.getContainerName());
        clearInvocations(expectedQueue(), otherQueue());
        when(featureToggleApi.isFeatureEnabled(configuration.getFeatureFlag())).thenReturn(false);

        FeatureDisabledException exception = assertThrows(FeatureDisabledException.class,
            () -> processor.run(configuration));

        assertThat(exception).hasMessage(configuration.getFeatureFlag() + " is not enabled");
        assertThat(records()).hasSize(2);
        assertThat(repository.findById(failedJson.getInterfaceFileId()).orElseThrow().getStatus())
            .isEqualTo(Status.FAILED);
        assertThat(storedBlobs(configuration.getContainerName())).isEqualTo(before);
        verifyNoInteractions(finesQueue, maintenanceQueue);
    }

    @ParameterizedTest(name = "{0}: queue recovery processes the stored blob once without another BAIS upload")
    @MethodSource("bankingFiles")
    @JiraStory("PO-7244")
    @JiraEpic("PO-3497")
    void queueRecoveryProcessesStoredBlobOnce(BankingFile bankingFile) throws Exception {
        InterfaceFileProcessorService processor = prepare(bankingFile);
        final InterfaceFileEntity failedJson = ingestWithUnavailableQueue(processor);
        final long sourceId = failedJson.getRelatedInterfaceFile().getInterfaceFileId();
        clearInvocations(expectedQueue(), otherQueue());
        doNothing().when(expectedQueue()).send(anyLong());

        // No SFTP file remains: recovery must read the persisted SOURCE blob.
        processor.run(configuration);

        List<InterfaceFileEntity> successfulJson = records().stream()
            .filter(entity -> entity.getType() == Type.SOURCE_JSON && entity.getStatus() == Status.SUCCESS)
            .toList();
        assertThat(successfulJson).singleElement();
        InterfaceFileEntity recovered = successfulJson.getFirst();
        assertThat(recovered.getRelatedInterfaceFile().getInterfaceFileId()).isEqualTo(sourceId);
        assertThat(recovered.getOpalDomain()).isEqualTo(file.domain());
        assertThat(recovered.getBusinessUnitId()).containsExactly(BUSINESS_UNIT_ID);
        assertThat(recovered.getInterfaceFileId()).isNotEqualTo(failedJson.getInterfaceFileId());
        assertThat(recovered.getChecksum()).isEqualTo(failedJson.getChecksum());
        assertThat(recovered.getErrors()).isNull();
        assertThat(repository.findById(failedJson.getInterfaceFileId()).orElseThrow().getStatus())
            .isEqualTo(Status.FAILED_SUPERSEDED);
        byte[] jsonBytes = blobServiceClient.getBlobContainerClient(configuration.getContainerName())
            .getBlobClient(recovered.getFilestoreUuid().toString()).downloadContent().toBytes();
        var json = objectMapper.readTree(jsonBytes);
        assertThat(json.get("file_name").asString()).isEqualTo(file.fileName());
        assertThat(json.at("/destination_details/bank_details/sort_code").asString()).isEqualTo(BANK_SORT_CODE);
        assertThat(json.at("/destination_details/bank_details/account_number").asString())
            .isEqualTo(BANK_ACCOUNT_NUMBER);
        assertThat(json.get("transactions").isArray()).isTrue();
        assertThat(json.get("transactions").size()).isPositive();
        assertThat(org.springframework.util.DigestUtils.md5DigestAsHex(jsonBytes))
            .isEqualTo(recovered.getChecksum());
        verify(expectedQueue()).send(recovered.getInterfaceFileId());
        verifyNoInteractions(otherQueue());
        var beforeReplay = storedBlobs(configuration.getContainerName());

        processor.run(configuration);

        assertThat(records()).hasSize(3);
        assertThat(storedBlobs(configuration.getContainerName())).isEqualTo(beforeReplay);
        verifyNoMoreInteractions(finesQueue, maintenanceQueue);
        assertNumberOfSftpFiles(file.username(), 0);
    }

    static Stream<BankingFile> dwpAndBailiffFiles() {
        return bankingFiles().filter(bankingFile -> bankingFile.courtCode() != null);
    }

    @ParameterizedTest(name = "{0}: missing court mapping retains the source and does not queue payments")
    @MethodSource("dwpAndBailiffFiles")
    @JiraStory("PO-7244")
    @JiraEpic("PO-3497")
    void missingCourtMappingRetainsSource(BankingFile bankingFile) throws IOException {
        InterfaceFileProcessorService processor = prepare(bankingFile);
        // Remove only this test's mapping, leaving other business units untouched.
        bankAccounts.deleteById(BUSINESS_UNIT_ID);

        processor.run(configuration);

        assertThat(records()).singleElement().satisfies(source -> {
            assertThat(source.getType()).isEqualTo(Type.SOURCE);
            assertThat(source.getStatus()).isEqualTo(Status.FAILED);
            assertThat(source.getErrors()).contains(COURT_CODE, "could not be located");
            assertThat(source.getFilestoreUuid()).isNotNull();
            assertThat(blobServiceClient.getBlobContainerClient(configuration.getContainerName())
                .getBlobClient(source.getFilestoreUuid().toString()).exists()).isTrue();
        });
        assertThat(sftpClient.listRegularFiles(file.username())).contains(file.fileName());
        verifyNoInteractions(finesQueue, maintenanceQueue);
    }

    @ParameterizedTest(name = "{0}: a continuing queue outage keeps the retry failed without reporting success")
    @MethodSource("dwpAndBailiffFiles")
    @JiraStory("PO-7244")
    @JiraEpic("PO-3497")
    void continuingQueueOutageRemainsFailed(BankingFile bankingFile) throws IOException {
        InterfaceFileProcessorService processor = prepare(bankingFile);
        final InterfaceFileEntity firstFailure = ingestWithUnavailableQueue(processor);
        clearInvocations(expectedQueue(), otherQueue());

        // The publisher still fails: a retry must not be recorded as successful processing.
        processor.run(configuration);

        assertThat(records()).hasSize(3);
        assertThat(records()).filteredOn(entity -> entity.getType() == Type.SOURCE).hasSize(1);
        assertThat(repository.findById(firstFailure.getInterfaceFileId()).orElseThrow().getStatus())
            .isEqualTo(Status.FAILED_SUPERSEDED);
        assertThat(records()).filteredOn(entity -> entity.getType() == Type.SOURCE_JSON)
            .noneMatch(entity -> entity.getStatus() == Status.SUCCESS);
        assertThat(records()).filteredOn(entity -> entity.getStatus() == Status.FAILED)
            .singleElement().satisfies(retry -> {
                assertThat(retry.getErrors()).contains("Queue send failed", "PO-7244 test queue unavailable");
                assertThat(retry.getRelatedInterfaceFile().getInterfaceFileId())
                    .isEqualTo(firstFailure.getRelatedInterfaceFile().getInterfaceFileId());
                assertThat(retry.getChecksum()).isEqualTo(firstFailure.getChecksum());
                assertThat(retry.getFilestoreUuid()).isNotNull();
                verify(expectedQueue()).send(retry.getInterfaceFileId());
            });
        verifyNoMoreInteractions(finesQueue, maintenanceQueue);
        assertNumberOfSftpFiles(file.username(), 0);
    }

    private InterfaceFileProcessorService prepare(BankingFile bankingFile) throws IOException {
        file = bankingFile;
        configuration = applicationContext.getBean(file.configBean(),
            AbstractBaisFileProcessorConfiguration.class);
        // SFTP usernames are supplied only for this test context; no deployment config is changed.
        originalSftpUsername = configuration.getSftpUsername();
        configuration.setSftpUsername(file.username());
        when(featureToggleApi.isFeatureEnabled(FeatureFlags.RELEASE_1C_BANKING_INTERFACES)).thenReturn(true);
        when(featureToggleApi.isFeatureEnabled(configuration.getFeatureFlag())).thenReturn(true);
        assertThat(records()).as("The processing fixture filename must be unused").isEmpty();
        assertThat(sftpClient.listRegularFiles(file.username())).doesNotContain(file.fileName());
        fixtureClaimed = true;
        assertThat(bankAccounts.existsById(BUSINESS_UNIT_ID)).isFalse();
        bankAccounts.saveAndFlush(BusinessUnitBankAccountEntity.builder()
            .id(BUSINESS_UNIT_ID).businessUnitCode("T244").domain(file.domain())
            .bankSortCode(BANK_SORT_CODE).bankAccountNumber(BANK_ACCOUNT_NUMBER)
            .dwpCourtCode(file.courtCode() == null ? null : COURT_CODE).build());
        bankAccountCreated = true;
        blobServiceClient.createBlobContainerIfNotExists(configuration.getContainerName());
        // Reuse the established format/content, isolating only the destination routing data.
        String content = new ClassPathResource("bais-emulator/" + file.fileName())
            .getContentAsString(StandardCharsets.UTF_8);
        content = file.courtCode() == null
            ? content.replace("01010112341234", "72440172440001").replace("56003327048527", "72440172440001")
            : content.replace(file.courtCode(), COURT_CODE);
        TestContainerConfig.SFTP_CONTAINER.copyFileToContainer(
            Transferable.of(content.getBytes(StandardCharsets.UTF_8)),
            "/home/" + file.username() + "/" + file.fileName());
        return applicationContext.getBean(file.processorBean() + "InterfaceFileProcessorService",
            InterfaceFileProcessorService.class);
    }

    private InterfaceFileEntity ingestWithUnavailableQueue(InterfaceFileProcessorService processor) {
        doThrow(new IllegalStateException("PO-7244 test queue unavailable")).when(expectedQueue()).send(anyLong());
        processor.run(configuration);
        List<InterfaceFileEntity> failed = records().stream()
            .filter(entity -> entity.getType() == Type.SOURCE_JSON).toList();
        assertThat(failed).singleElement();
        InterfaceFileEntity json = failed.getFirst();
        assertThat(json.getStatus()).isEqualTo(Status.FAILED);
        assertThat(json.getErrors()).contains("Queue send failed", "PO-7244 test queue unavailable");
        assertThat(json.getFilestoreUuid()).isNotNull();
        assertThat(repository.findById(json.getRelatedInterfaceFile().getInterfaceFileId())
            .orElseThrow().getStatus()).isEqualTo(Status.SUCCESS);
        verify(expectedQueue()).send(json.getInterfaceFileId());
        verifyNoInteractions(otherQueue());
        assertNumberOfSftpFiles(file.username(), 0);
        return json;
    }

    private List<InterfaceFileEntity> records() {
        return repository.findAll().stream()
            .filter(entity -> entity.getSource() == configuration.getSource())
            .filter(entity -> file.fileName().equals(entity.getFileName())).toList();
    }

    private InterfaceFilePreprocessQueueService expectedQueue() {
        return file.domain() == Domain.FINES ? finesQueue : maintenanceQueue;
    }

    private InterfaceFilePreprocessQueueService otherQueue() {
        return file.domain() == Domain.FINES ? maintenanceQueue : finesQueue;
    }

    @AfterEach
    void cleanUp() {
        try {
            cleanOwnedFiles();
        } finally {
            if (configuration != null) {
                configuration.setSftpUsername(originalSftpUsername);
            }
            if (bankAccountCreated) {
                bankAccounts.deleteById(BUSINESS_UNIT_ID);
                assertThat(bankAccounts.existsById(BUSINESS_UNIT_ID)).isFalse();
            }
        }
    }

    private void cleanOwnedFiles() {
        if (configuration != null && fixtureClaimed) {
            sftpClient.deleteFile(file.username(), file.fileName());
            List<InterfaceFileEntity> owned = records();
            owned.stream().filter(entity -> entity.getFilestoreUuid() != null).forEach(entity ->
                blobServiceClient.getBlobContainerClient(configuration.getContainerName())
                    .getBlobClient(entity.getFilestoreUuid().toString()).deleteIfExists());
            repository.deleteAll(owned.stream().filter(entity -> entity.getType() == Type.SOURCE_JSON).toList());
            repository.deleteAll(owned.stream().filter(entity -> entity.getType() == Type.SOURCE).toList());
            assertThat(records()).isEmpty();
            assertThat(sftpClient.listRegularFiles(file.username())).doesNotContain(file.fileName());
            owned.stream().filter(entity -> entity.getFilestoreUuid() != null).forEach(entity ->
                assertThat(blobServiceClient.getBlobContainerClient(configuration.getContainerName())
                    .getBlobClient(entity.getFilestoreUuid().toString()).exists()).isFalse());
        }
    }

    record BankingFile(String processorBean, String configBean, String username, String fileName,
                       Domain domain, String courtCode) {
        @Override
        public String toString() {
            return processorBean;
        }
    }
}
