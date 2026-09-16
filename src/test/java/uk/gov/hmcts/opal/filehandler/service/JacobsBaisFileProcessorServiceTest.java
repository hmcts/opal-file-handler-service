package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.extraction.PacsTTPBaisExtractionService;
import uk.gov.hmcts.opal.filehandler.service.queue.FinesInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.filehandler.util.FeatureFlagUtil;

@ExtendWith(MockitoExtension.class)
public class JacobsBaisFileProcessorServiceTest {

    private static final String FILE_NAME = "0000015232_dat_0000000612_08011008_111355";

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
    private PacsTTPBaisExtractionService extractionService;

    @Mock
    private FinesInterfaceFilePreprocessQueueService finesQueueService;

    @Mock
    private MaintenanceInterfaceFilePreprocessQueueService maintenanceQueueService;

    private JacobsBaisFileProcessorService service;

    @BeforeEach
    void setUp() {
        service = new JacobsBaisFileProcessorService(
            Clock.systemUTC(),
            featureFlagUtil,
            baisSftpClient,
            blobStoreService,
            repository,
            transactionTemplate,
            JsonMapper.builder().build(),
            extractionService,
            finesQueueService,
            maintenanceQueueService
        );
    }

    @Test
    void shouldProcessFile() {
        InterfaceFileEntity sourceFile = sourceFile(".txt");
        InputStream inputStream = InputStream.nullInputStream();

        when(extractionService.extractStandardData(sourceFile, inputStream)).thenReturn(List.of());

        service.processFile(config, sourceFile, inputStream);

        assertThat(sourceFile.getStatus()).isEqualTo(Status.SUCCESS_NO_TRANSACTIONS);
        verify(extractionService).extractStandardData(sourceFile, inputStream);
        verify(repository).save(sourceFile);
    }

    private InterfaceFileEntity sourceFile(String fileEnding) {
        return InterfaceFileEntity.builder()
            .interfaceFileId(100L)
            .source(Interface.JACOBS)
            .target(Interface.OPAL)
            .type(Type.SOURCE)
            .opalDomain(Domain.MAINTENANCE)
            .fileName(FILE_NAME + fileEnding)
            .status(Status.INGESTED)
            .createdDatetime(LocalDateTime.now())
            .build();
    }
}
