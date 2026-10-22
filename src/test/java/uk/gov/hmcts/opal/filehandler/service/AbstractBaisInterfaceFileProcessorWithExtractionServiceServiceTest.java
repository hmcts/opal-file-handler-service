package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.util.BinaryData;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.BusinessUnitBankAccountEntity;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.PaymentType;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.exception.BlobChecksumValidationException;
import uk.gov.hmcts.opal.filehandler.exception.UnexpectedDomainException;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.extraction.ExtractionService;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.BankDetails;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.DestinationDetails;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.InterfaceFileCommonDataExtract;
import uk.gov.hmcts.opal.filehandler.service.queue.InterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.filehandler.util.FeatureFlagUtil;

@ExtendWith(MockitoExtension.class)
class AbstractBaisInterfaceFileProcessorWithExtractionServiceServiceTest {

    private static final String CONTAINER = "natwest-report";
    private static final String SOURCE_JSON_NAME = "typical.dat";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-30T10:15:30Z"), ZoneOffset.UTC);
    private static final UUID FILE_UUID = UUID.randomUUID();

    @Mock
    private FeatureFlagUtil featureFlagUtil;

    @Mock
    private BaisSftpClient baisSftpClient;

    @Mock
    private InterfaceFileBlobStoreService blobStoreService;

    @Mock
    private InterfaceFilesRepository repository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private BaisFileProcessorConfiguration config;

    @Mock
    private ExtractionService<InterfaceFileCommonDataExtract> extractionService;

    @Mock
    private InterfaceFilePreprocessQueueService finesQueueService;

    @Mock
    private InterfaceFilePreprocessQueueService maintenanceQueueService;

    private ObjectMapper objectMapper;
    private TestProcessor service;
    private InterfaceFileEntity sourceFile;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();
        sourceFile = sourceFile();
        service = new TestProcessor(
            featureFlagUtil,
            baisSftpClient,
            blobStoreService,
            repository,
            transactionTemplate,
            objectMapper,
            extractionService,
            finesQueueService,
            maintenanceQueueService
        );

        lenient().when(config.getSource()).thenReturn(Interface.NATWEST);
        lenient().when(config.getTarget()).thenReturn(Interface.OPAL);
        lenient().when(config.getContainerName()).thenReturn(CONTAINER);
        lenient().when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    class ProcessFile {

        @Test
        void shouldSetSourceStatusToSuccessNoTransactionsWhenNoExtractsReturned() {
            TestProcessor processor = spy(service);
            InputStream inputStream = new ByteArrayInputStream("source".getBytes(StandardCharsets.UTF_8));
            when(extractionService.extractStandardData(sourceFile, inputStream)).thenReturn(List.of());

            processor.processFile(config, sourceFile, inputStream);

            assertThat(sourceFile.getStatus()).isEqualTo(Status.SUCCESS_NO_TRANSACTIONS);
            verify(repository).save(sourceFile);
            verify(extractionService).extractStandardData(sourceFile, inputStream);
            verify(extractionService, never()).getBusinessUnitBankAccount(any());
            verify(processor, never()).validateSupportedDomain(any(), any());
            verify(processor, never()).updateSourceBusinessUnitAndDomain(any(), any(), any());
            verify(processor, never()).createAndUploadSourceJson(any(), any(), any());
            verify(blobStoreService, never()).uploadBaisFile(any(), any(), any(), any());
            verify(finesQueueService, never()).send(any());
            verify(maintenanceQueueService, never()).send(any());
        }

        @Test
        void shouldPreProcessAndQueueNewExtract() {
            TestProcessor processor = spy(service);
            InputStream inputStream = new ByteArrayInputStream("source".getBytes(StandardCharsets.UTF_8));
            InterfaceFileCommonDataExtract extract = extract();
            BusinessUnitBankAccountEntity businessUnit = businessUnit("BC12", Domain.FINES);
            InterfaceFileEntity sourceJson = sourceJsonFile(200L, Status.SUCCESS, Domain.FINES);

            when(extractionService.extractStandardData(sourceFile, inputStream)).thenReturn(List.of(extract));
            when(extractionService.getBusinessUnitBankAccount(extract)).thenReturn(businessUnit);
            doReturn(sourceJson).when(processor).createAndUploadSourceJson(config, sourceFile, extract);

            processor.processFile(config, sourceFile, inputStream);

            assertThat(sourceFile.getBusinessUnitCode()).containsExactly("BC12");
            assertThat(sourceFile.getOpalDomain()).isEqualTo(Domain.FINES);
            verify(processor).createAndUploadSourceJson(config, sourceFile, extract);
            verify(finesQueueService).send(200L);
            verify(maintenanceQueueService, never()).send(any());
            verify(repository).save(sourceFile);
        }

        @Test
        void shouldSkipProcessingExtractWhenBusinessUnitIs065() {
            TestProcessor processor = spy(service);
            InputStream inputStream = new ByteArrayInputStream("source".getBytes(StandardCharsets.UTF_8));
            InterfaceFileCommonDataExtract extract = extract();
            BusinessUnitBankAccountEntity businessUnit = businessUnit("065", Domain.FINES);

            when(extractionService.extractStandardData(sourceFile, inputStream)).thenReturn(List.of(extract));
            when(extractionService.getBusinessUnitBankAccount(extract)).thenReturn(businessUnit);

            processor.processFile(config, sourceFile, inputStream);

            assertThat(sourceFile.getBusinessUnitCode()).containsExactly("065");
            assertThat(sourceFile.getOpalDomain()).isEqualTo(Domain.FINES);
            verify(processor, never()).createAndUploadSourceJson(any(), any(), any());
            verify(blobStoreService, never()).uploadBaisFile(any(), any(), any(), any());
            verify(finesQueueService, never()).send(any());
            verify(maintenanceQueueService, never()).send(any());
            verify(repository).save(sourceFile);
        }

        @Test
        void shouldProcessEachExtractAndSaveSourceFileOnce() {
            TestProcessor processor = spy(service);
            InputStream inputStream = new ByteArrayInputStream("source".getBytes(StandardCharsets.UTF_8));
            InterfaceFileCommonDataExtract first = extract();
            InterfaceFileCommonDataExtract second = InterfaceFileCommonDataExtract.builder()
                .fileName("second.dat")
                .paymentType(PaymentType.CHEQUE)
                .build();
            BusinessUnitBankAccountEntity firstBusinessUnit = businessUnit("BC12", Domain.FINES);
            BusinessUnitBankAccountEntity secondBusinessUnit = businessUnit("MN01", Domain.MAINTENANCE);
            InterfaceFileEntity firstSourceJson = sourceJsonFile(200L, Status.SUCCESS, Domain.FINES);
            InterfaceFileEntity secondSourceJson = sourceJsonFile(201L, Status.SUCCESS, Domain.MAINTENANCE);

            when(extractionService.extractStandardData(sourceFile, inputStream)).thenReturn(List.of(first, second));
            when(extractionService.getBusinessUnitBankAccount(first)).thenReturn(firstBusinessUnit);
            when(extractionService.getBusinessUnitBankAccount(second)).thenReturn(secondBusinessUnit);
            doReturn(firstSourceJson).when(processor).createAndUploadSourceJson(config, sourceFile, first);
            doReturn(secondSourceJson).when(processor).createAndUploadSourceJson(config, sourceFile, second);

            processor.processFile(config, sourceFile, inputStream);

            assertThat(sourceFile.getBusinessUnitCode()).containsExactly("BC12", "MN01");
            assertThat(sourceFile.getOpalDomain()).isEqualTo(Domain.MAINTENANCE);
            verify(processor).createAndUploadSourceJson(config, sourceFile, first);
            verify(processor).createAndUploadSourceJson(config, sourceFile, second);
            verify(finesQueueService).send(200L);
            verify(maintenanceQueueService).send(201L);
            verify(repository).save(sourceFile);
        }

        @Test
        void shouldPropagateUnsupportedDomainValidationFailure() {
            TestProcessor processor = spy(service);
            InputStream inputStream = new ByteArrayInputStream("source".getBytes(StandardCharsets.UTF_8));
            InterfaceFileCommonDataExtract extract = extract();
            BusinessUnitBankAccountEntity businessUnit = businessUnit("CF01", Domain.CONFISCATION);

            when(extractionService.extractStandardData(sourceFile, inputStream)).thenReturn(List.of(extract));
            when(extractionService.getBusinessUnitBankAccount(extract)).thenReturn(businessUnit);

            assertThatThrownBy(() -> processor.processFile(config, sourceFile, inputStream))
                .isInstanceOf(UnexpectedDomainException.class);

            verify(processor, never()).createAndUploadSourceJson(any(), any(), any());
            verify(blobStoreService, never()).uploadBaisFile(any(), any(), any(), any());
            verify(finesQueueService, never()).send(any());
            verify(maintenanceQueueService, never()).send(any());
            verify(repository, never()).save(any());
        }
    }

    @Nested
    class ValidateSupportedDomain {

        @ParameterizedTest
        @EnumSource(value = Domain.class, names = {"FINES", "MAINTENANCE"})
        void shouldAllowSupportedDomains(Domain supportedDomain) {
            assertThat(catchThrowable(() -> service.validateSupportedDomain(sourceFile, supportedDomain))).isNull();
        }

        @ParameterizedTest
        @EnumSource(value = Domain.class, names = {"FINES", "MAINTENANCE"}, mode = Mode.EXCLUDE)
        void shouldRejectUnsupportedDomain(Domain unSupportedDomain) {
            assertThatThrownBy(() -> service.validateSupportedDomain(sourceFile, unSupportedDomain))
                .isInstanceOf(UnexpectedDomainException.class)
                .hasMessageContaining("Domain '" + unSupportedDomain + "' found for source file '100'");
        }
    }

    @Nested
    class UpdateSourceBusinessUnitAndDomain {

        @Test
        void shouldMergeBusinessUnitCodesAndUpdateDomain() {
            sourceFile.setBusinessUnitCode(new String[] {"AA01"});

            service.updateSourceBusinessUnitAndDomain(sourceFile, businessUnit("BC12", Domain.FINES), Domain.FINES);

            assertThat(sourceFile.getBusinessUnitCode()).containsExactly("AA01", "BC12");
            assertThat(sourceFile.getOpalDomain()).isEqualTo(Domain.FINES);
        }

        @Test
        void shouldMergeBusinessUnitCodesAndUpdateDomainSourceHasNullBusinessUnitCode() {
            sourceFile.setBusinessUnitCode(null);

            service.updateSourceBusinessUnitAndDomain(sourceFile, businessUnit("BC12", Domain.FINES), Domain.FINES);

            assertThat(sourceFile.getBusinessUnitCode()).containsExactly("BC12");
            assertThat(sourceFile.getOpalDomain()).isEqualTo(Domain.FINES);
        }
    }

    @Nested
    class PopulateMissingDestinationBankDetails {

        @Test
        void shouldPopulateWhenAccountNumberAndSortCodeMissing() {
            InterfaceFileCommonDataExtract extract = extract();
            extract.setDestinationDetails(null);

            BusinessUnitBankAccountEntity businessUnitBankAccountEntity = businessUnit("BC12", Domain.FINES);
            businessUnitBankAccountEntity.setBankAccountNumber("some-account-number");
            businessUnitBankAccountEntity.setBankSortCode("some-sort-code");

            service.populateMissingDestinationBankDetails(extract, businessUnitBankAccountEntity);

            assertThat(extract.getDestinationDetails().getBankDetails().getAccountNumber()).isEqualTo(
                "some-account-number");
            assertThat(extract.getDestinationDetails().getBankDetails().getSortCode()).isEqualTo("some-sort-code");
        }

        @Test
        void shouldNotPopulateWhenOnlySortCodeFieldIsMissing() {
            InterfaceFileCommonDataExtract extract = extract();
            extract.setDestinationDetails(DestinationDetails.builder()
                .bankDetails(BankDetails.builder().accountNumber("11111111").build())
                .build());

            service.populateMissingDestinationBankDetails(extract, businessUnit("BC12", Domain.FINES));

            assertThat(extract.getDestinationDetails().getBankDetails().getAccountNumber()).isEqualTo("11111111");
            assertThat(extract.getDestinationDetails().getBankDetails().getSortCode()).isNull();
        }

        @Test
        void shouldNotPopulateWhenOnlyAccountNumberFieldIsMissing() {
            InterfaceFileCommonDataExtract extract = extract();
            extract.setDestinationDetails(DestinationDetails.builder()
                .bankDetails(BankDetails.builder().sortCode("11111111").build())
                .build());

            service.populateMissingDestinationBankDetails(extract, businessUnit("BC12", Domain.FINES));

            assertThat(extract.getDestinationDetails().getBankDetails().getAccountNumber()).isNull();
            assertThat(extract.getDestinationDetails().getBankDetails().getSortCode()).isEqualTo("11111111");
        }
    }

    @Nested
    class AlreadyProcessedSuccessfully {

        @Test
        void shouldReturnTrueWhenSuccessfulDuplicateExists() {
            InterfaceFileCommonDataExtract extract = extract();
            when(repository.findByRelatedInterfaceFileInterfaceFileIdAndTypeAndFileNameAndChecksumAndStatus(
                100L, Type.SOURCE_JSON, SOURCE_JSON_NAME, "checksum", Status.SUCCESS))
                .thenReturn(Optional.of(sourceJsonFile(200L, Status.SUCCESS)));

            assertThat(service.alreadyProcessedSuccessfully(sourceFile, extract, "checksum")).isTrue();
        }

        @Test
        void shouldReturnFalseWhenSuccessfulDuplicateDoesNotExist() {
            assertThat(service.alreadyProcessedSuccessfully(sourceFile, extract(), "checksum")).isFalse();
        }
    }

    @Nested
    class SupersedeFailedSourceJson {

        @Test
        void shouldMarkFailedDuplicatesAsSupersededAndSaveThem() {
            InterfaceFileEntity failed = sourceJsonFile(200L, Status.FAILED);
            when(repository.findAllByRelatedInterfaceFileInterfaceFileIdAndTypeAndFileNameAndChecksumAndStatus(
                100L, Type.SOURCE_JSON, SOURCE_JSON_NAME, "checksum", Status.FAILED))
                .thenReturn(List.of(failed));

            service.supersedeFailedSourceJson(sourceFile, extract(), "checksum");

            assertThat(failed.getStatus()).isEqualTo(Status.FAILED_SUPERSEDED);
            verify(repository).saveAll(List.of(failed));
        }

        @Test
        void shouldNotSaveWhenNoFailedDuplicatesExist() {
            service.supersedeFailedSourceJson(sourceFile, extract(), "checksum");

            verify(repository, never()).saveAll(any());
        }
    }

    @Nested
    class CalculateExtractChecksum {

        @Test
        void shouldCalculateChecksumFromBytes() {
            assertThat(service.calculateExtractChecksum("abc".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("900150983cd24fb0d6963f7d28e17f72");
        }

        @Test
        void shouldWrapChecksumReadFailure() {
            try (MockedStatic<AbstractInterfaceFileProcessorService> checksum =
                mockStatic(AbstractInterfaceFileProcessorService.class, CALLS_REAL_METHODS)) {

                checksum.when(() -> AbstractInterfaceFileProcessorService.calculateChecksum(any(InputStream.class)))
                    .thenThrow(new IOException("read failed"));
                Throwable thrown =
                    catchThrowable(() -> service.calculateExtractChecksum("abc".getBytes(StandardCharsets.UTF_8)));
                assertThat(thrown)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Unable to calculate SOURCE_JSON checksum")
                    .hasCauseInstanceOf(IOException.class);
            }
        }
    }

    @Nested
    class CreateSourceJson {

        @Test
        void shouldMapEntityFields() {
            InterfaceFileCommonDataExtract extract = extract();

            InterfaceFileEntity sourceJson = service.createSourceJson(
                config, sourceFile, extract, new String[] {"BC12"}, Domain.FINES, "checksum");

            assertThat(sourceJson.getSource()).isEqualTo(Interface.NATWEST);
            assertThat(sourceJson.getTarget()).isEqualTo(Interface.OPAL);
            assertThat(sourceJson.getType()).isEqualTo(Type.SOURCE_JSON);
            assertThat(sourceJson.getOpalDomain()).isEqualTo(Domain.FINES);
            assertThat(sourceJson.getFileName()).isEqualTo(SOURCE_JSON_NAME);
            assertThat(sourceJson.getChecksum()).isEqualTo("checksum");
            assertThat(sourceJson.getStatus()).isEqualTo(Status.SUCCESS);
            assertThat(sourceJson.getRelatedInterfaceFile()).isSameAs(sourceFile);
            assertThat(sourceJson.getBusinessUnitCode()).containsExactly("BC12");
            assertThat(sourceJson.getPaymentType()).isEqualTo(uk.gov.hmcts.opal.filehandler.entity.PaymentType.CASH);
        }
    }

    @Nested
    class UploadSourceJson {

        @Test
        void shouldPopulateFilestoreUuidAndUploadJsonBytes() throws IOException {
            InterfaceFileEntity sourceJson = sourceJsonFile(200L, Status.SUCCESS);
            byte[] jsonBytes = "{}".getBytes(StandardCharsets.UTF_8);
            ArgumentCaptor<InputStream> inputStreamCaptor = ArgumentCaptor.forClass(InputStream.class);

            service.uploadSourceJson(config, sourceJson, jsonBytes);

            assertThat(sourceJson.getFilestoreUuid()).isNotNull();
            verify(blobStoreService).uploadBaisFile(any(UUID.class), eq(CONTAINER), inputStreamCaptor.capture(),
                eq(sourceJson.getChecksum()));
            assertThat(inputStreamCaptor.getValue().readAllBytes()).isEqualTo(jsonBytes);
        }

        @Test
        void shouldMarkFailedWhenBlobChecksumValidationFails() {
            InterfaceFileEntity sourceJson = sourceJsonFile(200L, Status.SUCCESS);
            BlobChecksumValidationException exception = new BlobChecksumValidationException(
                UUID.fromString("11111111-1111-1111-1111-111111111111"), "expected", "actual");
            doThrow(exception).when(blobStoreService)
                .uploadBaisFile(any(UUID.class), eq(CONTAINER), any(InputStream.class), eq(sourceJson.getChecksum()));

            service.uploadSourceJson(config, sourceJson, "{}".getBytes(StandardCharsets.UTF_8));

            assertThat(sourceJson.getStatus()).isEqualTo(Status.FAILED);
            assertThat(sourceJson.getErrors())
                .isEqualTo(
                    "{\"message\":\"Blob checksum validation failed: Blob checksum validation failed for filestore "
                        + "UUID '11111111-1111-1111-1111-111111111111': expected 'expected' but was 'actual'\"}");
            assertThat(sourceJson.getFilestoreUuid()).isNull();
        }

        @Test
        void shouldMarkFailedWhenUploadThrowsGenericRuntimeException() {
            InterfaceFileEntity sourceJson = sourceJsonFile(200L, Status.SUCCESS);
            doThrow(new RuntimeException("blob unavailable")).when(blobStoreService)
                .uploadBaisFile(any(UUID.class), eq(CONTAINER), any(InputStream.class), eq(sourceJson.getChecksum()));

            service.uploadSourceJson(config, sourceJson, "{}".getBytes(StandardCharsets.UTF_8));

            assertThat(sourceJson.getStatus()).isEqualTo(Status.FAILED);
            assertThat(sourceJson.getErrors())
                .isEqualTo("{\"message\":\"Blob upload failed: blob unavailable\"}");
            assertThat(sourceJson.getFilestoreUuid()).isNull();
        }
    }

    @Nested
    class SendToQueue {

        @Test
        void shouldSendToSelectedQueue() {
            InterfaceFileEntity sourceJson = sourceJsonFile(200L, Status.SUCCESS);

            service.sendToQueue(sourceJson, Domain.FINES);

            verify(finesQueueService).send(200L);
            verify(maintenanceQueueService, never()).send(any());
        }

        @Test
        void shouldMarkFailedWhenQueueThrows() {
            InterfaceFileEntity sourceJson = sourceJsonFile(200L, Status.SUCCESS);
            doThrow(new RuntimeException("queue unavailable")).when(finesQueueService).send(200L);

            service.sendToQueue(sourceJson, Domain.FINES);

            assertThat(sourceJson.getStatus()).isEqualTo(Status.FAILED);
            assertThat(sourceJson.getErrors()).isEqualTo("{\"message\":\"Queue send failed: queue unavailable\"}");
        }
    }

    @Nested
    class QueueService {

        @Test
        void shouldReturnQueueServiceForDomain() {
            assertThat(service.queueService(Domain.FINES)).isSameAs(finesQueueService);
            assertThat(service.queueService(Domain.MAINTENANCE)).isSameAs(maintenanceQueueService);
        }

        @Test
        void shouldThrowWhenNoQueueConfiguredForDomain() {
            assertThatThrownBy(() -> service.queueService(Domain.FILE_HANDLER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unsupported queue domain 'FILE_HANDLER'");
        }
    }

    @Nested
    class MarkSourceJsonFailed {

        @Test
        void shouldSetFailureStatusAndErrors() {
            InterfaceFileEntity sourceJson = sourceJsonFile(200L, Status.SUCCESS);

            service.markSourceJsonFailed(sourceJson, "failed message", new RuntimeException("failure"));

            assertThat(sourceJson.getStatus()).isEqualTo(Status.FAILED);
            assertThat(sourceJson.getErrors()).contains("failed message");
        }
    }

    @Nested
    class SelectFilesToProcess {

        @Test
        void successfulSourceJsonShouldNotBeReprocessed() {
            sourceJsonFile(200L, Status.SUCCESS);
            service.selectFilesToProcess(config);

            verify(blobStoreService, never()).fetchInterfaceFile(
                sourceFile.getInterfaceFileId(), sourceFile.getFilestoreUuid(), CONTAINER);
        }

        @Test
        void failedSourceJsonShouldBeReprocessed() {
            sourceJsonFile(200L, Status.FAILED);

            when(repository.findAll(ArgumentMatchers.<Specification<InterfaceFileEntity>>any()))
                .thenReturn(List.of(sourceFile));

            when(blobStoreService.fetchInterfaceFile(anyLong(), any(UUID.class), anyString()))
                .thenReturn(BinaryData.fromBytes("hello world".getBytes()));

            service.selectFilesToProcess(config);

            verify(blobStoreService, times(1)).fetchInterfaceFile(
                sourceFile.getInterfaceFileId(), sourceFile.getFilestoreUuid(), CONTAINER);
        }

        @Test
        void failedSupersededSourceJsonWithNoFailuresShouldNotBeReprocessed() {
            sourceJsonFile(200L, Status.FAILED_SUPERSEDED);
            service.selectFilesToProcess(config);

            verify(blobStoreService, never()).fetchInterfaceFile(anyLong(), any(UUID.class), anyString());
        }

        @Test
        void tooManyFailuresShouldNotBeReprocessed() {
            LongStream.range(200, 205).forEach(
                id -> sourceJsonFile(id, Status.FAILED_SUPERSEDED));

            sourceJsonFile(205L, Status.FAILED);

            service.selectFilesToProcess(config);

            verify(blobStoreService, never()).fetchInterfaceFile(anyLong(), any(UUID.class), anyString());
        }
    }

    private InterfaceFileEntity sourceFile() {
        return InterfaceFileEntity.builder()
            .interfaceFileId(100L)
            .source(Interface.NATWEST)
            .target(Interface.OPAL)
            .type(Type.SOURCE)
            .opalDomain(Domain.FILE_HANDLER)
            .fileName("source.dat")
            .checksum("source-checksum")
            .status(Status.INGESTED)
            .filestoreUuid(FILE_UUID)
            .createdDatetime(LocalDateTime.now(CLOCK))
            .build();
    }

    private InterfaceFileEntity sourceJsonFile(long id, Status status) {
        return sourceJsonFile(id, status, Domain.FINES);
    }

    private InterfaceFileEntity sourceJsonFile(long id, Status status, Domain domain) {
        return InterfaceFileEntity.builder()
            .interfaceFileId(id)
            .source(Interface.NATWEST)
            .target(Interface.OPAL)
            .type(Type.SOURCE_JSON)
            .opalDomain(domain)
            .fileName(SOURCE_JSON_NAME)
            .checksum("json-checksum")
            .status(status)
            .createdDatetime(LocalDateTime.now(CLOCK))
            .relatedInterfaceFile(sourceFile)
            .build();
    }

    private BusinessUnitBankAccountEntity businessUnit(String code, Domain domain) {
        return BusinessUnitBankAccountEntity.builder()
            .id(1L)
            .businessUnitCode(code)
            .domain(domain)
            .bankAccountNumber("27048527")
            .bankSortCode("560033")
            .build();
    }

    private InterfaceFileCommonDataExtract extract() {
        return InterfaceFileCommonDataExtract.builder()
            .fileName(SOURCE_JSON_NAME)
            .paymentType(PaymentType.CASH)
            .build();
    }

    @Nested
    class PreProcessExtract {

        @Test
        void shouldUpdateSourceFilePopulateBankDetailsAndReturnTrue() {
            TestProcessor processor = spy(service);
            InterfaceFileCommonDataExtract extract = extract();
            BusinessUnitBankAccountEntity businessUnit = businessUnit("BC12", Domain.FINES);

            when(extractionService.getBusinessUnitBankAccount(extract)).thenReturn(businessUnit);
            doNothing().when(processor).validateSupportedDomain(sourceFile, Domain.FINES);
            doNothing().when(processor).updateSourceBusinessUnitAndDomain(sourceFile, businessUnit, Domain.FINES);
            doNothing().when(processor).populateMissingDestinationBankDetails(extract, businessUnit);

            boolean result = processor.preProcessExtract(config, sourceFile, extract);

            assertThat(result).isTrue();
            verify(extractionService).getBusinessUnitBankAccount(extract);
            verify(processor).validateSupportedDomain(sourceFile, Domain.FINES);
            verify(processor).updateSourceBusinessUnitAndDomain(sourceFile, businessUnit, Domain.FINES);
            verify(processor).populateMissingDestinationBankDetails(extract, businessUnit);
        }

        @Test
        void shouldReturnFalseForBusinessUnit065AfterUpdatingSourceFile() {
            TestProcessor processor = spy(service);
            InterfaceFileCommonDataExtract extract = extract();
            BusinessUnitBankAccountEntity businessUnit = businessUnit("065", Domain.FINES);

            when(extractionService.getBusinessUnitBankAccount(extract)).thenReturn(businessUnit);
            doNothing().when(processor).validateSupportedDomain(sourceFile, Domain.FINES);
            doNothing().when(processor).updateSourceBusinessUnitAndDomain(sourceFile, businessUnit, Domain.FINES);

            boolean result = processor.preProcessExtract(config, sourceFile, extract);

            assertThat(result).isFalse();
            verify(extractionService).getBusinessUnitBankAccount(extract);
            verify(processor).validateSupportedDomain(sourceFile, Domain.FINES);
            verify(processor).updateSourceBusinessUnitAndDomain(sourceFile, businessUnit, Domain.FINES);
            verify(processor, never()).populateMissingDestinationBankDetails(extract, businessUnit);
        }
    }

    @Nested
    class PostProcessExtract {

        @Test
        void shouldSendSuccessfulSourceJsonToDomainQueue() {
            TestProcessor processor = spy(service);
            InterfaceFileEntity sourceJson = sourceJsonFile(200L, Status.SUCCESS, Domain.FINES);
            InterfaceFileCommonDataExtract extract = extract();
            doNothing().when(processor).sendToQueue(sourceJson, Domain.FINES);

            processor.postProcessExtract(config, sourceJson, extract);

            verify(processor).sendToQueue(sourceJson, Domain.FINES);
            verify(repository, never()).save(sourceJson);
        }

        @Test
        void shouldIgnoreFailedSourceJson() {
            TestProcessor processor = spy(service);
            InterfaceFileEntity sourceJson = sourceJsonFile(200L, Status.FAILED, Domain.FINES);
            InterfaceFileCommonDataExtract extract = extract();

            processor.postProcessExtract(config, sourceJson, extract);

            verify(processor, never()).sendToQueue(sourceJson, Domain.FINES);
            verify(repository, never()).save(sourceJson);
        }

        @Test
        void shouldSaveSourceJsonWhenQueueSendMarksItFailed() {
            TestProcessor processor = spy(service);
            InterfaceFileEntity sourceJson = sourceJsonFile(200L, Status.SUCCESS, Domain.FINES);
            InterfaceFileCommonDataExtract extract = extract();
            doAnswer(invocation -> {
                sourceJson.setStatus(Status.FAILED);
                sourceJson.setErrors("{\"message\":\"Queue send failed: queue unavailable\"}");
                return null;
            }).when(processor).sendToQueue(sourceJson, Domain.FINES);

            processor.postProcessExtract(config, sourceJson, extract);

            assertThat(sourceJson.getStatus()).isEqualTo(Status.FAILED);
            assertThat(sourceJson.getErrors()).isEqualTo("{\"message\":\"Queue send failed: queue unavailable\"}");
            verify(processor).sendToQueue(sourceJson, Domain.FINES);
            verify(repository).save(sourceJson);
        }
    }

    @Nested
    class GetBusinessUnitsFromExtract {

        @Test
        void shouldReturnBusinessUnitCodeFromExtractionServiceBankAccount() {
            InterfaceFileCommonDataExtract extract = extract();

            when(extractionService.getBusinessUnitBankAccount(extract)).thenReturn(businessUnit("BC12", Domain.FINES));

            assertThat(service.getBusinessUnitsFromExtract(config, extract)).containsExactly("BC12");
        }
    }

    @Nested
    class GetDomainFromExtract {

        @Test
        void shouldReturnDomainFromExtractionServiceBankAccount() {
            InterfaceFileCommonDataExtract extract = extract();

            when(extractionService.getBusinessUnitBankAccount(extract)).thenReturn(businessUnit("BC12", Domain.FINES));

            assertThat(service.getDomainFromExtract(config, extract)).isEqualTo(Domain.FINES);
        }
    }



    @Service
    private static class TestProcessor
        extends AbstractBaisInterfaceFileProcessorWithExtractionService<InterfaceFileCommonDataExtract> {

        TestProcessor(
            FeatureFlagUtil featureFlagUtil,
            BaisSftpClient baisSftpClient,
            InterfaceFileBlobStoreService blobStoreService,
            InterfaceFilesRepository repository,
            TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper,
            ExtractionService<InterfaceFileCommonDataExtract> extractionService,
            InterfaceFilePreprocessQueueService finesQueueService,
            InterfaceFilePreprocessQueueService maintenanceQueueService
        ) {
            super(CLOCK, featureFlagUtil, baisSftpClient, blobStoreService, repository, transactionTemplate,
                objectMapper, extractionService, finesQueueService, maintenanceQueueService);

            maxRetries = 5;
        }
    }
}
