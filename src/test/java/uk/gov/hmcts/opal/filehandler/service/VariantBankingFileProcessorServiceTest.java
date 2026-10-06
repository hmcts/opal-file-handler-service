package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
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
    private BaisFileProcessorConfiguration config;

    @InjectMocks
    private VariantBankingFileProcessorService service;

    @Test
    @DisplayName("Should return empty list when selecting files to process")
    void shouldReturnEmptyListWhenSelectingFiles() {

        List<String> files = service.selectFilesToProcess(config);

        assertThat(files).isEmpty();
    }

    @Test
    @DisplayName("Should find duplicate using filename only")
    void shouldFindDuplicateUsingFilenameOnly() {

        InterfaceFileEntity entity = InterfaceFileEntity.builder()
            .source(Interface.VARIANT_BANKING)
            .target(Interface.OPAL)
            .type(Type.SOURCE_JSON)
            .opalDomain(Domain.MAINTENANCE)
            .fileName("VB001.dat")
            .checksum("checksum")
            .status(Status.SUCCESS)
            .createdDatetime(LocalDateTime.now())
            .build();

        when(interfaceFilesRepository.findByFileNameAndStatus(
            "VB001.dat",
            Status.SUCCESS
        )).thenReturn(Optional.of(entity));

        Optional<InterfaceFileEntity> result =
            service.findDuplicateFile(
                "VB001.dat",
                "different-checksum"
            );

        assertThat(result).contains(entity);

        verify(interfaceFilesRepository)
            .findByFileNameAndStatus(
                "VB001.dat",
                Status.SUCCESS
            );
    }

    @Test
    @DisplayName("Should delegate uploaded files to ingestUploadedFile")
    void shouldDelegateUploadedFileProcessing() throws Exception {

        VariantBankingFileProcessorService spyService = spy(service);

        byte[] fileBytes = "test content".getBytes();

        doNothing().when(spyService)
            .ingestUploadedFile(config, "VB001.dat", fileBytes);

        spyService.processUploadedFile(
            config,
            "VB001.dat",
            fileBytes
        );

        verify(spyService)
            .ingestUploadedFile(
                config,
                "VB001.dat",
                fileBytes
            );
    }

    @Test
    @DisplayName("Should create duplicate interface file when file already exists")
    void shouldCreateDuplicateFileWhenDuplicateExists() throws Exception {

        InterfaceFileEntity existingFile = InterfaceFileEntity.builder()
            .source(Interface.VARIANT_BANKING)
            .target(Interface.OPAL)
            .type(Type.SOURCE_JSON)
            .opalDomain(Domain.MAINTENANCE)
            .fileName("VB001.dat")
            .checksum("existing-checksum")
            .status(Status.SUCCESS)
            .createdDatetime(LocalDateTime.now())
            .build();

        InterfaceFileEntity duplicateFile = InterfaceFileEntity.builder()
            .source(Interface.VARIANT_BANKING)
            .target(Interface.OPAL)
            .type(Type.SOURCE_JSON)
            .opalDomain(Domain.MAINTENANCE)
            .fileName("VB001.dat")
            .checksum("duplicate-checksum")
            .status(Status.DUPLICATE)
            .createdDatetime(LocalDateTime.now())
            .build();

        VariantBankingFileProcessorService spyService = spy(service);

        doReturn(Optional.of(existingFile))
            .when(spyService)
            .findDuplicateFile(anyString(), anyString());

        doReturn(duplicateFile)
            .when(spyService)
            .createDuplicateInterfaceFile(
                any(),
                anyString(),
                anyString(),
                eq(existingFile)
            );

        doReturn(duplicateFile)
            .when(spyService)
            .saveInitialFile(any());

        spyService.ingestUploadedFile(
            config,
            "VB001.dat",
            "content".getBytes()
        );

        verify(spyService)
            .createDuplicateInterfaceFile(
                any(),
                eq("VB001.dat"),
                anyString(),
                eq(existingFile)
            );
    }

    @Test
    @DisplayName("Should upload file to blob store when no duplicate exists")
    void shouldUploadFileWhenNoDuplicateExists() throws Exception {

        when(config.getContainerName()).thenReturn("variant-banking");

        VariantBankingFileProcessorService spyService = spy(service);
        doReturn(Optional.empty()).when(spyService).findDuplicateFile(anyString(), anyString());

        InterfaceFileEntity ingestedEntity = InterfaceFileEntity.builder()
            .source(Interface.VARIANT_BANKING)
            .target(Interface.OPAL)
            .type(Type.SOURCE_JSON)
            .opalDomain(Domain.MAINTENANCE)
            .fileName("VB001.dat")
            .checksum("checksum")
            .status(Status.INGESTED)
            .createdDatetime(LocalDateTime.now())
            .build();

        doReturn(ingestedEntity).when(spyService).createNewInterfaceFile(any(),
            anyString(), anyString(), any(UUID.class));

        doReturn(ingestedEntity).when(spyService).saveInitialFile(any());

        doNothing().when(spyService).processIngestedFile(any(), any(), any());
        byte[] fileBytes = "content".getBytes();

        spyService.ingestUploadedFile(config,"VB001.dat", fileBytes);

        verify(interfaceFileBlobStoreService).uploadBaisFile(any(UUID.class),
                eq("variant-banking"), any(), anyString());
    }
}
