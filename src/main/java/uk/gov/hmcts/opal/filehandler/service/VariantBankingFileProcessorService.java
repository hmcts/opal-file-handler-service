package uk.gov.hmcts.opal.filehandler.service;

import static uk.gov.hmcts.opal.filehandler.util.StreamUtil.calculateChecksum;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.config.FileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.exception.BlobChecksumValidationException;
import uk.gov.hmcts.opal.filehandler.exception.BlobUploadException;
import uk.gov.hmcts.opal.filehandler.exception.InvalidReportFileException;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.extraction.VariantBacsStandard18BaisExtractionService;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.InterfaceFileCommonDataExtract;
import uk.gov.hmcts.opal.filehandler.service.queue.FinesInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import java.util.List;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.filehandler.util.FeatureFlagUtil;

@Slf4j
@Service
public class VariantBankingFileProcessorService
    extends AbstractBaisInterfaceFileProcessorWithExtractionService<InterfaceFileCommonDataExtract> {

    public VariantBankingFileProcessorService(
        Clock clock,
        FeatureFlagUtil featureFlagUtil,
        BaisSftpClient baisSftpClient,
        InterfaceFileBlobStoreService interfaceFileBlobStoreService,
        InterfaceFilesRepository interfaceFilesRepository,
        TransactionTemplate transactionTemplate,
        ObjectMapper objectMapper,
        VariantBacsStandard18BaisExtractionService extractionService,
        FinesInterfaceFilePreprocessQueueService finesQueueService,
        MaintenanceInterfaceFilePreprocessQueueService maintenanceQueueService
    ) {
        super(clock, featureFlagUtil, baisSftpClient, interfaceFileBlobStoreService, interfaceFilesRepository,
            transactionTemplate, objectMapper, extractionService, finesQueueService, maintenanceQueueService);
    }

    @Override
    protected List<String> selectFilesToProcess(BaisFileProcessorConfiguration config) {
        log.info("Variant Banking files are supplied by Add Interface Files API. "
                + "Skipping BAIS SFTP file selection.");

        return List.of();
    }

    public void processUploadedFile(FileProcessorConfiguration config, String fileName,
        byte[] fileBytes, Domain domain, Status status
    ) throws IOException {

        ingestUploadedFile(config, fileName, fileBytes,domain, status);
    }

    /**
     * Processes a Variant Banking file uploaded via the Add Interface Files API.
     */
    protected void ingestUploadedFile(FileProcessorConfiguration config, String fileName,
        byte[] fileBytes, Domain domain, Status status)
        throws IOException {

        validateFileName(config, fileName);
        String fileChecksum = calculateChecksum(new ByteArrayInputStream(fileBytes));

        Optional<InterfaceFileEntity> duplicate = interfaceFilesRepository.findByFileNameAndStatus(fileName, status);
        InterfaceFileEntity entity;

        try {
            if (duplicate.isPresent()) {
                entity = createDuplicateInterfaceFile(config, fileName, fileChecksum, duplicate.get(),domain);
            } else {
                validateFile(new ByteArrayInputStream(fileBytes));

                UUID fileStoreUuid = UUID.randomUUID();
                interfaceFileBlobStoreService.uploadBaisFile(fileStoreUuid, config.getFeatureFlag(),
                    new ByteArrayInputStream(fileBytes), fileChecksum);

                entity = createNewInterfaceFile(config, fileName, fileChecksum, fileStoreUuid,domain);
            }
        } catch (InvalidReportFileException e) {
            entity = createFailureInterfaceFile(config, fileName, fileChecksum, e.getMessage(),domain);
        } catch (BlobChecksumValidationException e) {
            entity = createFailureInterfaceFile(config, fileName, fileChecksum, e.getMessage(),domain);
        } catch (BlobUploadException e) {
            entity = createFailureInterfaceFile(config, fileName, fileChecksum, "Blob upload failed for file '%s': %s"
                .formatted(fileName, e.getMessage()),domain);
        }

        entity = saveInitialFile(entity);

        if (entity.getStatus().equals(Status.INGESTED)) {
            processIngestedFile((BaisFileProcessorConfiguration) config, entity, new ByteArrayInputStream(fileBytes),
                getCurrentUserId());
        }
    }

    private void validateFileName(
        FileProcessorConfiguration config,
        String fileName
    ) {
        if (!config.getFileNameRegex().matcher(fileName).matches()) {
            throw new InvalidReportFileException(
                "Filename '%s' does not match expected pattern"
                    .formatted(fileName),
                null
            );
        }
    }
}
