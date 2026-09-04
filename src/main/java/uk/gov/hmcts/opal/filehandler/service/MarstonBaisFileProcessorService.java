package uk.gov.hmcts.opal.filehandler.service;

import java.time.Clock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.config.MarstonBaisFileBaisFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.extraction.ExtractionService;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.InterfaceFileCommonDataExtract;
import uk.gov.hmcts.opal.filehandler.service.queue.InterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.filehandler.util.FeatureFlagUtil;

@Slf4j
@Service
public class MarstonBaisFileProcessorService
    extends AbstractBaisInterfaceFileProcessorWithExtractionService<InterfaceFileCommonDataExtract> {

    private final MarstonBaisFileBaisFileProcessorConfig config;

    public MarstonBaisFileProcessorService(
        Clock clock,
        FeatureFlagUtil featureFlagUtil,
        BaisSftpClient baisSftpClient,
        InterfaceFileBlobStoreService interfaceFileBlobStoreService,
        InterfaceFilesRepository interfaceFilesRepository,
        TransactionTemplate transactionTemplate,
        ObjectMapper objectMapper,
        ExtractionService<InterfaceFileCommonDataExtract> extractionService,
        InterfaceFilePreprocessQueueService finesQueueService,
        InterfaceFilePreprocessQueueService maintenanceQueueService,
        MarstonBaisFileBaisFileProcessorConfig config
    ) {
        super(
            clock,
            featureFlagUtil,
            baisSftpClient,
            interfaceFileBlobStoreService,
            interfaceFilesRepository,
            transactionTemplate,
            objectMapper,
            extractionService,
            finesQueueService,
            maintenanceQueueService
        );

        this.config = config;
    }

    public void run() {
        super.run(config);
    }
}