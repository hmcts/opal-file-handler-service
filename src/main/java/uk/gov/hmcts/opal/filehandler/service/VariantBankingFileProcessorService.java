package uk.gov.hmcts.opal.filehandler.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.config.DWPBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.extraction.PacsTTPBaisExtractionService;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.InterfaceFileCommonDataExtract;
import uk.gov.hmcts.opal.filehandler.service.queue.FinesInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.filehandler.util.FeatureFlagUtil;

@Slf4j
@Service
public class VariantBankingFileProcessorService
    extends AbstractBankingInterfaceFileProcessorWithExtractionService<InterfaceFileCommonDataExtract> {

    public VariantBankingFileProcessorService(
        Clock clock,
        FeatureFlagUtil featureFlagUtil,
        BaisSftpClient baisSftpClient,
        InterfaceFileBlobStoreService interfaceFileBlobStoreService,
        InterfaceFilesRepository interfaceFilesRepository,
        TransactionTemplate transactionTemplate,
        ObjectMapper objectMapper,
        PacsTTPBaisExtractionService extractionService,
        FinesInterfaceFilePreprocessQueueService finesQueueService,
        MaintenanceInterfaceFilePreprocessQueueService maintenanceQueueService,
        DWPBaisFileProcessorConfiguration config
    ) {
        super(clock, featureFlagUtil, interfaceFileBlobStoreService, interfaceFilesRepository,
            transactionTemplate, objectMapper, extractionService,
            new SftpFileIngestionService(config, baisSftpClient),
            config, finesQueueService, maintenanceQueueService);
    }

    @Override
    protected List<String> selectFilesToProcess() {
        log.info("Variant Banking files are supplied by Add Interface Files API. "
            + "Skipping BAIS SFTP file selection.");

        return List.of();
    }

    @Override
    protected boolean alreadyProcessedSuccessfully(
        InterfaceFileEntity sourceInterfaceFile,
        InterfaceFileCommonDataExtract extract,
        String checksum
    ) {
        boolean duplicateExists = interfaceFilesRepository
            .findByRelatedInterfaceFileInterfaceFileIdAndTypeAndFileNameAndStatusIn(
                sourceInterfaceFile.getInterfaceFileId(),
                Type.SOURCE_JSON,
                extract.getFileName(),
                List.of(Status.SUCCESS, Status.SUCCESS_NO_TRANSACTIONS)
            )
            .isPresent();

        if (duplicateExists) {
            log.warn("SOURCE_JSON object with name '{}' for associated SOURCE '{}' already "
                    + "processed skipping",
                extract.getFileName(), sourceInterfaceFile.getInterfaceFileId());
        }

        return duplicateExists;
    }

    @Override
    protected Optional<InterfaceFileEntity> getDuplicateFile(
        Type type,
        String fileName,
        String fileChecksum) {
        return interfaceFilesRepository
            .findByTypeAndFileNameAndStatusIn(
                type, fileName, List.of(Status.SUCCESS, Status.SUCCESS_NO_TRANSACTIONS));
    }
}
