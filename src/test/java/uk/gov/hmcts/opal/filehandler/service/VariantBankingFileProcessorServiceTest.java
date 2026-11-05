package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.opal.filehandler.config.DWPBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.extraction.VariantBacsStandard18BaisExtractionService;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.InterfaceFileCommonDataExtract;
import uk.gov.hmcts.opal.filehandler.service.queue.FinesInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.filehandler.util.FeatureFlagUtil;

@ExtendWith(MockitoExtension.class)
class VariantBankingFileProcessorServiceTest {

    private static final String FILE_NAME = "a121_003500VB123_13.dat";
    private static final String CHECKSUM = "3685d7f2b30e9b34b8d3e5496fb45506";
    private static final String DIFFERENT_CHECKSUM = "00000000000000000000000000000000";

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
    private VariantBacsStandard18BaisExtractionService extractionService;

    @Mock
    private FinesInterfaceFilePreprocessQueueService finesQueueService;

    @Mock
    private MaintenanceInterfaceFilePreprocessQueueService maintenanceQueueService;

    private VariantBankingFileProcessorService service;

    @BeforeEach
    void setUp() {
        service = new VariantBankingFileProcessorService(
            Clock.systemUTC(), featureFlagUtil, baisSftpClient, blobStoreService, repository,
            transactionTemplate, JsonMapper.builder().build(), extractionService,
            finesQueueService, maintenanceQueueService, new DWPBaisFileProcessorConfiguration());
    }

    @Nested
    @DisplayName("selectFilesToProcess()")
    class SelectFilesToProcess {

        @Test
        @DisplayName("Returns no files without querying SFTP or retrying stored files")
        void skipsFileSelection() {
            assertThat(service.selectFilesToProcess()).isEmpty();

            verifyNoInteractions(baisSftpClient, repository, blobStoreService, extractionService,
                finesQueueService, maintenanceQueueService);
        }
    }

    @Nested
    @DisplayName("alreadyProcessedSuccessfully()")
    class AlreadyProcessedSuccessfully {

        @ParameterizedTest
        @EnumSource(value = Status.class, names = {"SUCCESS", "SUCCESS_NO_TRANSACTIONS"})
        @DisplayName("Recognises a successful extract for the same source and filename regardless of checksum")
        void ignoresChecksumForSuccessfulExtract(Status status) {
            InterfaceFileEntity source = InterfaceFileEntity.builder().interfaceFileId(100L).build();
            InterfaceFileCommonDataExtract extract = InterfaceFileCommonDataExtract.builder()
                .fileName("extract.json").build();
            InterfaceFileEntity existing = InterfaceFileEntity.builder()
                .status(status).checksum(CHECKSUM).build();
            when(repository.findByRelatedInterfaceFileInterfaceFileIdAndTypeAndFileNameAndStatusIn(
                100L, Type.SOURCE_JSON, "extract.json", List.of(Status.SUCCESS, Status.SUCCESS_NO_TRANSACTIONS)))
                .thenReturn(Optional.of(existing));

            assertThat(service.alreadyProcessedSuccessfully(source, extract, CHECKSUM)).isTrue();
            assertThat(service.alreadyProcessedSuccessfully(source, extract, DIFFERENT_CHECKSUM)).isTrue();

            verify(repository, times(2)).findByRelatedInterfaceFileInterfaceFileIdAndTypeAndFileNameAndStatusIn(
                100L, Type.SOURCE_JSON, "extract.json", List.of(Status.SUCCESS, Status.SUCCESS_NO_TRANSACTIONS));
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Returns false when no successful extract matches the source and filename")
        void returnsFalseWhenNoSuccessfulExtractExists() {
            InterfaceFileEntity source = InterfaceFileEntity.builder().interfaceFileId(200L).build();
            InterfaceFileCommonDataExtract extract = InterfaceFileCommonDataExtract.builder()
                .fileName("other-extract.json").build();
            when(repository.findByRelatedInterfaceFileInterfaceFileIdAndTypeAndFileNameAndStatusIn(
                200L, Type.SOURCE_JSON, "other-extract.json", List.of(Status.SUCCESS, Status.SUCCESS_NO_TRANSACTIONS)))
                .thenReturn(Optional.empty());

            assertThat(service.alreadyProcessedSuccessfully(source, extract, CHECKSUM)).isFalse();

            verify(repository).findByRelatedInterfaceFileInterfaceFileIdAndTypeAndFileNameAndStatusIn(
                200L, Type.SOURCE_JSON, "other-extract.json", List.of(Status.SUCCESS, Status.SUCCESS_NO_TRANSACTIONS));
            verifyNoMoreInteractions(repository);
        }
    }

    @Nested
    @DisplayName("getDuplicateFile()")
    class GetDuplicateFile {

        @ParameterizedTest
        @EnumSource(value = Status.class, names = {"SUCCESS", "SUCCESS_NO_TRANSACTIONS"})
        @DisplayName("Finds a successful file using type and filename regardless of checksum")
        void ignoresChecksumForSuccessfulFile(Status status) {
            InterfaceFileEntity existing = InterfaceFileEntity.builder()
                .interfaceFileId(123L).fileName(FILE_NAME).checksum(CHECKSUM).status(status).build();

            when(repository.findByTypeAndFileNameAndStatusIn(
                Type.SOURCE, FILE_NAME, List.of(Status.SUCCESS, Status.SUCCESS_NO_TRANSACTIONS)))
                .thenReturn(Optional.of(existing));

            for (String checksum : List.of(CHECKSUM, DIFFERENT_CHECKSUM)) {
                assertThat(service.getDuplicateFile(Type.SOURCE, FILE_NAME, checksum))
                    .hasValueSatisfying(duplicate -> {
                        assertThat(duplicate.getInterfaceFileId()).isEqualTo(123L);
                        assertThat(duplicate.getChecksum()).isEqualTo(CHECKSUM);
                        assertThat(duplicate.getStatus()).isEqualTo(status);
                    });
            }

            verify(repository, times(2)).findByTypeAndFileNameAndStatusIn(
                Type.SOURCE, FILE_NAME, List.of(Status.SUCCESS, Status.SUCCESS_NO_TRANSACTIONS));
            verifyNoMoreInteractions(repository);
        }

        @Test
        @DisplayName("Returns empty when no successful file matches the type and filename")
        void returnsEmptyWhenNoDuplicateExists() {
            when(repository.findByTypeAndFileNameAndStatusIn(
                Type.SOURCE_JSON, FILE_NAME, List.of(Status.SUCCESS, Status.SUCCESS_NO_TRANSACTIONS)))
                .thenReturn(Optional.empty());

            assertThat(service.getDuplicateFile(Type.SOURCE_JSON, FILE_NAME, CHECKSUM)).isEmpty();

            verify(repository).findByTypeAndFileNameAndStatusIn(
                Type.SOURCE_JSON, FILE_NAME, List.of(Status.SUCCESS, Status.SUCCESS_NO_TRANSACTIONS));
            verifyNoMoreInteractions(repository);
        }
    }
}
