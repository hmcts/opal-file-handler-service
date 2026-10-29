package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import java.time.Instant;
import java.time.ZoneId;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.config.VariantBankingFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.exception.InvalidReportFileException;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.extraction.VariantBacsStandard18BaisExtractionService;
import uk.gov.hmcts.opal.filehandler.service.queue.FinesInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.filehandler.util.FeatureFlagUtil;

@ExtendWith(MockitoExtension.class)
class VariantBankingFileProcessorServiceTest {

    @Mock
    private Clock clock;

    @Mock
    private FeatureFlagUtil featureFlagUtil;

    @Mock
    private BaisSftpClient baisSftpClient;

    @Mock
    private InterfaceFileBlobStoreService interfaceFileBlobStoreService;

    @Mock
    private InterfaceFilesRepository interfaceFilesRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private VariantBacsStandard18BaisExtractionService extractionService;

    @Mock
    private FinesInterfaceFilePreprocessQueueService finesQueueService;

    @Mock
    private MaintenanceInterfaceFilePreprocessQueueService maintenanceQueueService;

    @Mock
    private VariantBankingFileProcessorConfig config;

    @InjectMocks
    private VariantBankingFileProcessorService service;

    @BeforeEach
    void setUp() {
        lenient().when(config.getFileNameRegex())
            .thenReturn(
                Pattern.compile("^a121_\\d{6}VB\\d{3}_\\d{2}\\.dat$")
            );

        lenient().when(clock.getZone())
            .thenReturn(ZoneId.systemDefault());

        lenient().when(clock.instant())
            .thenReturn(Instant.now());
    }


    @Test
    @DisplayName("Should return empty list when selecting files to process")
    void shouldReturnEmptyListWhenSelectingFiles() {

        List<String> files = service.selectFilesToProcess(config);

        assertThat(files).isEmpty();
    }

    @Test
    @DisplayName("Should delegate uploaded files to ingestUploadedFile")
    void shouldDelegateUploadedFileProcessing() throws Exception {

        VariantBankingFileProcessorService spyService = spy(service);

        byte[] fileBytes = "test content".getBytes();

        doNothing().when(spyService)
            .ingestUploadedFile(config, "a121_240101VB001_01.dat", fileBytes,Domain.MAINTENANCE, Status.SUCCESS);

        spyService.processUploadedFile(config,"a121_240101VB001_01.dat", fileBytes, Domain.MAINTENANCE, Status.SUCCESS);
        verify(spyService).ingestUploadedFile(config,"a121_240101VB001_01.dat", fileBytes, Domain.MAINTENANCE, Status.SUCCESS);

    }

    @Test
    @DisplayName("Should create duplicate interface file when file already exists")
    void shouldCreateDuplicateFileWhenDuplicateExists() throws Exception {

        InterfaceFileEntity existingFile = InterfaceFileEntity.builder()
            .source(Interface.VARIANT_BANKING)
            .target(Interface.OPAL)
            .type(Type.SOURCE_JSON)
            .opalDomain(Domain.MAINTENANCE)
            .fileName("a121_240101VB001_01.dat")
            .checksum("existing-checksum")
            .status(Status.SUCCESS)
            .createdDatetime(LocalDateTime.now())
            .build();

        InterfaceFileEntity duplicateFile = InterfaceFileEntity.builder()
            .source(Interface.VARIANT_BANKING)
            .target(Interface.OPAL)
            .type(Type.SOURCE_JSON)
            .opalDomain(Domain.MAINTENANCE)
            .fileName("a121_240101VB001_01.dat")
            .checksum("duplicate-checksum")
            .status(Status.DUPLICATE)
            .createdDatetime(LocalDateTime.now())
            .build();

        VariantBankingFileProcessorService spyService = spy(service);

        when(interfaceFilesRepository.findByFileNameAndStatus(
            "a121_240101VB001_01.dat",
            Status.SUCCESS
        )).thenReturn(Optional.of(existingFile));

        doReturn(duplicateFile).when(spyService).createDuplicateInterfaceFile(
            any(),
            anyString(),
            anyString(),
            eq(existingFile),
            eq(Domain.MAINTENANCE)
        );
        doReturn(duplicateFile).when(spyService).saveInitialFile(any());
        spyService.ingestUploadedFile(config,"a121_240101VB001_01.dat",
            "content".getBytes(), Domain.MAINTENANCE, Status.SUCCESS);

        verify(spyService)
            .createDuplicateInterfaceFile(
                any(),
                eq("a121_240101VB001_01.dat"),
                anyString(),
                eq(existingFile),
                eq(Domain.MAINTENANCE)
            );
    }

    @Test
    @DisplayName("Should upload file to blob store when no duplicate exists")
    void shouldUploadFileWhenNoDuplicateExists() throws Exception {

        VariantBankingFileProcessorService spyService = spy(service);
        doReturn(1L).when(spyService).getCurrentUserId();
        when(interfaceFilesRepository.findByFileNameAndStatus(
            "a121_240101VB001_01.dat",
            Status.SUCCESS
        )).thenReturn(Optional.empty());

        when(config.getFeatureFlag()).thenReturn("variant-banking");

        InterfaceFileEntity ingestedEntity = InterfaceFileEntity.builder()
            .source(Interface.VARIANT_BANKING)
            .target(Interface.OPAL)
            .type(Type.SOURCE_JSON)
            .opalDomain(Domain.MAINTENANCE)
            .fileName("a121_240101VB001_01.dat")
            .checksum("checksum")
            .status(Status.INGESTED)
            .createdDatetime(LocalDateTime.now())
            .build();

        doReturn(ingestedEntity).when(spyService).createNewInterfaceFile(
            any(),
            anyString(),
            anyString(),
            any(UUID.class),
            eq(Domain.MAINTENANCE)
        );

        doReturn(ingestedEntity).when(spyService).saveInitialFile(any());

        doNothing().when(spyService).processIngestedFile(
            any(),
            any(),
            any(),
            anyLong()
        );

        byte[] fileBytes = "content".getBytes();

        spyService.ingestUploadedFile(
            config,
            "a121_240101VB001_01.dat",
            fileBytes,
            Domain.MAINTENANCE,
            Status.SUCCESS
        );

        verify(interfaceFileBlobStoreService).uploadBaisFile(
            any(UUID.class),
            eq("variant-banking"),
            any(),
            anyString()
        );
    }

    @Test
    void shouldReturnEmptyWhenNoFilenameChecksumMatchExists() {

        when(interfaceFilesRepository.findByFileNameAndChecksumAndStatus(
            "a121_240101VB001_01.dat",
            "checksum",
            Status.SUCCESS
        )).thenReturn(Optional.empty());

        Optional<InterfaceFileEntity> result =
            service.findDuplicateFile(
                "a121_240101VB001_01.dat",
                "checksum",
                Status.SUCCESS
            );

        assertThat(result).isEmpty();

        verify(interfaceFilesRepository)
            .findByFileNameAndChecksumAndStatus(
                "a121_240101VB001_01.dat",
                "checksum",
                Status.SUCCESS
            );
    }
    @Test
    void shouldCreateFailureFileWhenInvalidReportFileExceptionThrown() throws Exception {

        VariantBankingFileProcessorService spyService = spy(service);

        InterfaceFileEntity failedEntity = InterfaceFileEntity.builder()
            .fileName("a121_240101VB001_01.dat")
            .status(Status.FAILED)
            .errors("Invalid file")
            .createdBy(1L)
            .createdDatetime(LocalDateTime.now())
            .build();

        doThrow(
            new InvalidReportFileException(
                "Invalid file",
                new RuntimeException("Test cause")
            )
        ).when(spyService).validateFile(any());

        doReturn(failedEntity)
            .when(spyService)
            .createFailureInterfaceFile(
                any(),
                anyString(),
                anyString(),
                anyString(),
                eq(Domain.MAINTENANCE)
            );

        doNothing().when(spyService)
            .validateFileName(any(), anyString());
        doReturn(failedEntity)
            .when(spyService)
            .saveInitialFile(any(InterfaceFileEntity.class));

        spyService.processUploadedFile(config,"a121_240101VB001_01.dat",
            "content".getBytes(), Domain.MAINTENANCE, Status.SUCCESS);

        verify(spyService).createFailureInterfaceFile(
            any(),
            eq("a121_240101VB001_01.dat"),
            anyString(),
            eq("Invalid file"),
            eq(Domain.MAINTENANCE)
        );
    }

}
