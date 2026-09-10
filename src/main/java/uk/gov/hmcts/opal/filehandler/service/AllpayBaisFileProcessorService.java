package uk.gov.hmcts.opal.filehandler.service;

import java.io.InputStream;
import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.extraction.BacsStandard18BaisExtractionService;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.InterfaceFileCommonDataExtract;
import uk.gov.hmcts.opal.filehandler.service.queue.FinesInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.filehandler.util.FeatureFlagUtil;

@Slf4j
@Service
public class AllpayBaisFileProcessorService
    extends AbstractBaisInterfaceFileProcessorWithExtractionService<InterfaceFileCommonDataExtract> {

    private static final String DATA_FILE_EXTENSION = ".dat";

    public AllpayBaisFileProcessorService(Clock clock,
        FeatureFlagUtil featureFlagUtil,
        BaisSftpClient baisSftpClient,
        InterfaceFileBlobStoreService interfaceFileBlobStoreService,
        InterfaceFilesRepository interfaceFilesRepository,
        TransactionTemplate transactionTemplate,
        ObjectMapper objectMapper,
        BacsStandard18BaisExtractionService extractionService,
        FinesInterfaceFilePreprocessQueueService finesQueueService,
        MaintenanceInterfaceFilePreprocessQueueService maintenanceQueueService) {
        super(clock, featureFlagUtil, baisSftpClient, interfaceFileBlobStoreService, interfaceFilesRepository,
            transactionTemplate, objectMapper, extractionService, finesQueueService, maintenanceQueueService);
    }

    @Override
    protected void processFile(
        BaisFileProcessorConfiguration config,
        InterfaceFileEntity sourceInterfaceFile,
        InputStream inputStream
    ) {
        if (!sourceInterfaceFile.getFileName().endsWith(DATA_FILE_EXTENSION)) {
            return;
        }

        super.processFile(config, sourceInterfaceFile, inputStream);
    }

}
