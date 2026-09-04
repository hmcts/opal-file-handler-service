package uk.gov.hmcts.opal.filehandler.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import java.io.IOException;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.DigestUtils;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.common.launchdarkly.config.LaunchDarklyProperties;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.PaymentType;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.service.AbstractInterfaceFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.InterfaceFileCommonDataExtract;
import uk.gov.hmcts.opal.filehandler.service.queue.InterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.testdata.BusinessUnitBankAccountEntityTestData;

public abstract class AbstractBacsStandard18BaisFileProcessorServiceIntegrationTest
    extends AbstractBaisFileProcessorServiceIntegrationTest {

    private static final long BUSINESS_UNIT_BANK_ACCOUNT_ID = 920003L;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BusinessUnitBankAccountEntityTestData businessUnitBankAccountTestData;

    @Autowired
    private LaunchDarklyProperties launchDarklyProperties;

    protected abstract AbstractInterfaceFileProcessorService processor();

    protected abstract BaisFileProcessorConfiguration processorConfiguration();

    protected abstract InterfaceFilePreprocessQueueService queueService();

    protected abstract BacsStandard18Fixture validFixture();

    protected abstract String unsupportedFileName();

    @BeforeEach
    void setUpBacsStandard18Contract() {
        repository.deleteAll();
        businessUnitBankAccountTestData.clear();

        BacsStandard18Fixture fixture = validFixture();
        businessUnitBankAccountTestData.saveBusinessUnitBankAccount(
            BUSINESS_UNIT_BANK_ACCOUNT_ID,
            fixture.businessUnitCode(),
            fixture.domain(),
            fixture.bankSortCode(),
            fixture.bankAccountNumber());

        BlobContainerClient container = blobContainer();
        container.createIfNotExists();
        container.listBlobs().forEach(blob -> container.getBlobClient(blob.getName()).deleteIfExists());

        deleteSftpFiles();
        clearInvocations(queueService());

        setFeatureFlag(FeatureFlags.RELEASE_1C_BANKING_INTERFACES, true);
        setFeatureFlag(processorConfiguration().getFeatureFlag(), true);
    }

    @AfterEach
    void tearDownBacsStandard18Contract() {
        deleteSftpFiles();
        businessUnitBankAccountTestData.clear();
    }

    @Test
    @DisplayName("BACS18 processor feature flag has an offline default")
    void shouldConfigureProcessorFeatureFlagDefault() {
        assertThat(launchDarklyProperties.getDefaultFlagValues())
            .containsKey(processorConfiguration().getFeatureFlag());
    }

    @ParameterizedTest(name = "banking interfaces enabled={0}, processor enabled={1}")
    @CsvSource({
        "false, true, release-1c-banking-interfaces",
        "true, false, processor",
        "false, false, release-1c-banking-interfaces"
    })
    @DisplayName("AC1: processing requires both feature flags")
    void shouldNotProcessWhenARequiredFeatureIsDisabled(
        boolean bankingInterfacesEnabled,
        boolean processorEnabled,
        String disabledFeature
    ) {
        BacsStandard18Fixture fixture = validFixture();
        // A real file proves disabled feature flags prevent ingestion and leave SFTP contents untouched.
        uploadFixture(fixture.fileName());

        setFeatureFlag(FeatureFlags.RELEASE_1C_BANKING_INTERFACES, bankingInterfacesEnabled);
        setFeatureFlag(processorConfiguration().getFeatureFlag(), processorEnabled);

        String expectedDisabledFeature = "processor".equals(disabledFeature)
            ? processorConfiguration().getFeatureFlag()
            : FeatureFlags.RELEASE_1C_BANKING_INTERFACES;

        assertThatThrownBy(() -> processor().run(processorConfiguration()))
            .isInstanceOf(FeatureDisabledException.class)
            .hasMessage(expectedDisabledFeature + " is not enabled");

        assertThat(repository.findAll()).isEmpty();
        assertThat(blobContainer().listBlobs()).isEmpty();
        assertThat(sftpClient.listRegularFiles(processorConfiguration().getSftpUsername()))
            .containsExactly(fixture.fileName());
        verify(queueService(), never()).send(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("AC2: a valid BACS18 file is ingested, extracted, stored and queued")
    void shouldIngestValidFile() throws IOException {
        BacsStandard18Fixture fixture = validFixture();
        final byte[] expectedSourceBytes =
            new ClassPathResource(fixture.classpathResource()).getContentAsByteArray();

        uploadFixture(fixture.fileName());
        processor().run(processorConfiguration());

        InterfaceFileEntity source = findOnly(Type.SOURCE, Status.SUCCESS);
        InterfaceFileEntity sourceJson = findOnly(Type.SOURCE_JSON, Status.SUCCESS);

        assertSource(source, fixture);
        assertSourceJson(sourceJson, source, fixture);

        byte[] sourceBytes = assertStoredBlob(source);
        byte[] sourceJsonBytes = assertStoredBlob(sourceJson);
        assertThat(sourceBytes).isEqualTo(expectedSourceBytes);

        InterfaceFileCommonDataExtract extract = objectMapper.readValue(
            sourceJsonBytes, InterfaceFileCommonDataExtract.class);
        assertThat(extract.getFileName()).isEqualTo(fixture.fileName());
        assertThat(extract.getPaymentType()).isEqualTo(fixture.paymentType());
        assertThat(extract.getDestinationDetails().getBankDetails().getSortCode()).isEqualTo(fixture.bankSortCode());
        assertThat(extract.getDestinationDetails().getBankDetails().getAccountNumber())
            .isEqualTo(fixture.bankAccountNumber());
        assertThat(extract.getTransactions()).hasSize(2);

        verify(queueService()).send(sourceJson.getInterfaceFileId());
        assertNumberOfSftpFiles(processorConfiguration().getSftpUsername(), 0);
    }

    @Test
    @DisplayName("Unsupported BACS18 filenames remain on SFTP without processing")
    void shouldIgnoreUnsupportedFileAndLeaveItOnSftp() {
        uploadFixture(unsupportedFileName());

        processor().run(processorConfiguration());

        assertThat(repository.findAll()).isEmpty();
        assertThat(blobContainer().listBlobs()).isEmpty();
        assertThat(sftpClient.listRegularFiles(processorConfiguration().getSftpUsername()))
            .containsExactly(unsupportedFileName());
        verify(queueService(), never()).send(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    @DisplayName("Duplicate BACS18 files are recorded without repeat extraction or queueing")
    void shouldRecordDuplicateWithoutCreatingAnotherSourceJson() {
        uploadFixture(validFixture().fileName());
        processor().run(processorConfiguration());

        uploadFixture(validFixture().fileName());
        processor().run(processorConfiguration());

        List<InterfaceFileEntity> entities = repository.findAll();
        assertThat(entities)
            .filteredOn(entity -> entity.getType() == Type.SOURCE && entity.getStatus() == Status.SUCCESS)
            .hasSize(1);
        assertThat(entities)
            .filteredOn(entity -> entity.getType() == Type.SOURCE && entity.getStatus() == Status.DUPLICATE)
            .hasSize(1);
        InterfaceFileEntity sourceJson = entities.stream()
            .filter(entity -> entity.getType() == Type.SOURCE_JSON)
            .findFirst()
            .orElseThrow();
        assertThat(entities).filteredOn(entity -> entity.getType() == Type.SOURCE_JSON).hasSize(1);
        verify(queueService(), times(1)).send(sourceJson.getInterfaceFileId());
        assertNumberOfSftpFiles(processorConfiguration().getSftpUsername(), 0);
    }

    @Test
    @DisplayName("An empty BACS18 SFTP directory completes without side effects")
    void shouldSucceedWhenSftpDirectoryIsEmpty() {
        assertThatCode(() -> processor().run(processorConfiguration())).doesNotThrowAnyException();

        assertThat(repository.findAll()).isEmpty();
        assertThat(blobContainer().listBlobs()).isEmpty();
        verify(queueService(), never()).send(org.mockito.ArgumentMatchers.anyLong());
    }

    private void assertSource(InterfaceFileEntity source, BacsStandard18Fixture fixture) {
        assertThat(source.getSource()).isEqualTo(fixture.source());
        assertThat(source.getTarget()).isEqualTo(fixture.target());
        assertThat(source.getType()).isEqualTo(Type.SOURCE);
        assertThat(source.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(source.getOpalDomain()).isEqualTo(fixture.domain());
        assertThat(source.getFileName()).isEqualTo(fixture.fileName());
        assertThat(source.getChecksum()).isEqualTo(fixture.checksum());
        assertThat(source.getBusinessUnitCode()).containsExactly(fixture.businessUnitCode());
        assertThat(source.getPaymentType()).isNull();
        assertThat(source.getRelatedInterfaceFile()).isNull();
        assertThat(source.getErrors()).isNull();
    }

    private void assertSourceJson(
        InterfaceFileEntity sourceJson,
        InterfaceFileEntity source,
        BacsStandard18Fixture fixture
    ) {
        assertThat(sourceJson.getSource()).isEqualTo(fixture.source());
        assertThat(sourceJson.getTarget()).isEqualTo(fixture.target());
        assertThat(sourceJson.getType()).isEqualTo(Type.SOURCE_JSON);
        assertThat(sourceJson.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(sourceJson.getOpalDomain()).isEqualTo(fixture.domain());
        assertThat(sourceJson.getFileName()).isEqualTo(fixture.fileName());
        assertThat(sourceJson.getBusinessUnitCode()).containsExactly(fixture.businessUnitCode());
        assertThat(sourceJson.getPaymentType()).isEqualTo(fixture.paymentType());
        assertThat(sourceJson.getRelatedInterfaceFile().getInterfaceFileId()).isEqualTo(source.getInterfaceFileId());
        assertThat(sourceJson.getErrors()).isNull();
    }

    private byte[] assertStoredBlob(InterfaceFileEntity entity) {
        BlobClient blob = blobContainer().getBlobClient(entity.getFilestoreUuid().toString());
        assertThat(blob.exists()).isTrue();

        byte[] content = blob.downloadContent().toBytes();
        assertThat(DigestUtils.md5DigestAsHex(content)).isEqualTo(entity.getChecksum());
        assertThat(HexFormat.of().formatHex(blob.getProperties().getContentMd5())).isEqualTo(entity.getChecksum());
        return content;
    }

    private InterfaceFileEntity findOnly(Type type, Status status) {
        return repository.findAll().stream()
            .filter(entity -> entity.getType() == type && entity.getStatus() == status)
            .reduce((first, second) -> {
                throw new AssertionError("Expected one " + type + " with status " + status);
            })
            .orElseThrow(() -> new AssertionError("Expected one " + type + " with status " + status));
    }

    protected final void uploadFixture(String destinationFileName) {
        uploadResourceToSftp(validFixture().classpathResource(), sftpPath(destinationFileName));
    }

    private String sftpPath(String fileName) {
        return "/home/%s/%s".formatted(processorConfiguration().getSftpUsername(), fileName);
    }

    private BlobContainerClient blobContainer() {
        return blobServiceClient.getBlobContainerClient(processorConfiguration().getContainerName());
    }

    private void deleteSftpFiles() {
        String username = processorConfiguration().getSftpUsername();
        sftpClient.listRegularFiles(username).forEach(file -> sftpClient.deleteFile(username, file));
    }

    private void setFeatureFlag(String featureFlag, boolean enabled) {
        assertThat(launchDarklyProperties.getDefaultFlagValues()).containsKey(featureFlag);
        launchDarklyProperties.getDefaultFlagValues().put(featureFlag, enabled);
    }

    public record BacsStandard18Fixture(
        String fileName,
        String classpathResource,
        String checksum,
        Interface source,
        Interface target,
        String businessUnitCode,
        Domain domain,
        PaymentType paymentType,
        String bankSortCode,
        String bankAccountNumber
    ) {
    }
}
