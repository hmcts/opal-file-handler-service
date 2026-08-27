package uk.gov.hmcts.opal.filehandler.service;

import static uk.gov.hmcts.opal.filehandler.repository.specs.InterfaceFileSpecsFactory.sourceFilesWithJsonFailuresWithinRetryLimit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.exception.BlobChecksumValidationException;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.extraction.ExtractionService;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.InterfaceFileCommonDataExtract;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.filehandler.util.FeatureFlagUtil;

@Slf4j
public abstract class AbstractInterfaceFileProcessorWithExtractionService<T extends InterfaceFileCommonDataExtract>
    extends AbstractInterfaceFileProcessorService {

    @Value("${opal.file-handler-service.extraction-service.max-retries:5}")
    protected int maxRetries;

    protected final ExtractionService<T> extractionService;

    protected AbstractInterfaceFileProcessorWithExtractionService(
        Clock clock,
        FeatureFlagUtil featureFlagUtil,
        BaisSftpClient baisSftpClient,
        InterfaceFileBlobStoreService interfaceFileBlobStoreService,
        InterfaceFilesRepository interfaceFilesRepository,
        TransactionTemplate transactionTemplate,
        ObjectMapper objectMapper,
        ExtractionService<T> extractionService
    ) {
        super(clock, featureFlagUtil, baisSftpClient, interfaceFileBlobStoreService, interfaceFilesRepository,
            transactionTemplate, objectMapper);
        this.extractionService = extractionService;
    }

    @Override
    protected List<String> selectFilesToProcess(BaisFileProcessorConfiguration config) {
        List<InterfaceFileEntity> sourceFilesToRetry = interfaceFilesRepository.findAll(
            sourceFilesWithJsonFailuresWithinRetryLimit(config.getSource(), maxRetries));

        for (InterfaceFileEntity sourceFile : sourceFilesToRetry) {
            InputStream sourceStream = interfaceFileBlobStoreService.fetchInterfaceFile(
                sourceFile.getInterfaceFileId(),
                sourceFile.getFilestoreUuid(),
                config.getContainerName()).toStream();

            processFile(config, sourceFile, sourceStream);
        }

        return super.selectFilesToProcess(config);
    }

    @Override
    protected void processFile(
        BaisFileProcessorConfiguration config,
        InterfaceFileEntity sourceInterfaceFile,
        InputStream inputStream
    ) {
        List<T> extracts = extractionService.extractStandardData(sourceInterfaceFile, inputStream);

        if (extracts.isEmpty()) {
            sourceInterfaceFile.setStatus(Status.SUCCESS_NO_TRANSACTIONS);
            interfaceFilesRepository.save(sourceInterfaceFile);
            return;
        }

        for (T extract : extracts) {
            if (preProcessExtract(config, sourceInterfaceFile, extract)) {
                InterfaceFileEntity sourceJson = createAndUploadSourceJson(config, sourceInterfaceFile, extract);
                if (sourceJson != null) {
                    postProcessExtract(config, sourceJson, extract);
                }
            }
        }

        interfaceFilesRepository.save(sourceInterfaceFile);
    }


    InterfaceFileEntity createAndUploadSourceJson(BaisFileProcessorConfiguration config,
        InterfaceFileEntity sourceInterfaceFile, T extract) {
        byte[] jsonBytes = objectMapper.writeValueAsBytes(extract);
        String checksum = calculateExtractChecksum(jsonBytes);

        if (alreadyProcessedSuccessfully(sourceInterfaceFile, extract, checksum)) {
            return null;
        }

        supersedeFailedSourceJson(sourceInterfaceFile, extract, checksum);

        InterfaceFileEntity sourceJson = createSourceJson(
            config, sourceInterfaceFile, extract,
            getBusinessUnitsFromExtract(config, extract),
            getDomainFromExtract(config, extract),
            checksum);

        uploadSourceJson(config, sourceJson, jsonBytes);
        return interfaceFilesRepository.save(sourceJson);
    }






    boolean alreadyProcessedSuccessfully(
        InterfaceFileEntity sourceInterfaceFile,
        T extract,
        String checksum
    ) {
        boolean duplicateExists = interfaceFilesRepository
            .findByRelatedInterfaceFileInterfaceFileIdAndTypeAndFileNameAndChecksumAndStatus(
                sourceInterfaceFile.getInterfaceFileId(),
                Type.SOURCE_JSON,
                extract.getFileName(),
                checksum,
                Status.SUCCESS)
            .isPresent();

        if (duplicateExists) {
            log.warn("SOURCE_JSON object with name '{}' and checksum '{}' for associated SOURCE '{}' already "
                    + "processed skipping",
                extract.getFileName(), checksum, sourceInterfaceFile.getInterfaceFileId());
        }

        return duplicateExists;
    }

    void supersedeFailedSourceJson(
        InterfaceFileEntity sourceInterfaceFile,
        T extract,
        String checksum
    ) {
        List<InterfaceFileEntity> failedSourceJsonFiles = interfaceFilesRepository
            .findAllByRelatedInterfaceFileInterfaceFileIdAndTypeAndFileNameAndChecksumAndStatus(
                sourceInterfaceFile.getInterfaceFileId(),
                Type.SOURCE_JSON,
                extract.getFileName(),
                checksum,
                Status.FAILED);

        failedSourceJsonFiles.forEach(sourceJson -> sourceJson.setStatus(Status.FAILED_SUPERSEDED));

        if (!failedSourceJsonFiles.isEmpty()) {
            interfaceFilesRepository.saveAll(failedSourceJsonFiles);
        }
    }


    String calculateExtractChecksum(byte[] jsonBytes) {
        try {
            return calculateChecksum(new ByteArrayInputStream(jsonBytes));
        } catch (IOException e) {
            throw new IllegalStateException("Unable to calculate SOURCE_JSON checksum", e);
        }
    }

    InterfaceFileEntity createSourceJson(
        BaisFileProcessorConfiguration config,
        InterfaceFileEntity sourceInterfaceFile,
        T extract,
        String[] businessUnitCodes,
        Domain domain,
        String checksum
    ) {
        return InterfaceFileEntity.builder()
            .source(config.getSource())
            .target(config.getTarget())
            .type(Type.SOURCE_JSON)
            .opalDomain(domain)
            .fileName(extract.getFileName())
            .checksum(checksum)
            .status(Status.SUCCESS)
            .createdDatetime(LocalDateTime.now(clock))
            .relatedInterfaceFile(sourceInterfaceFile)
            .businessUnitCode(businessUnitCodes)
            .paymentType(extract.getPaymentType())
            .build();
    }

    void uploadSourceJson(
        BaisFileProcessorConfiguration config,
        InterfaceFileEntity sourceJson,
        byte[] jsonBytes
    ) {
        UUID filestoreUuid = UUID.randomUUID();

        try {
            interfaceFileBlobStoreService.uploadBaisFile(
                filestoreUuid,
                config.getContainerName(),
                new ByteArrayInputStream(jsonBytes),
                sourceJson.getChecksum()
            );
            sourceJson.setFilestoreUuid(filestoreUuid);
        } catch (BlobChecksumValidationException e) {
            markSourceJsonFailed(sourceJson, "Blob checksum validation failed: %s".formatted(e.getMessage()), e);
        } catch (RuntimeException e) {
            markSourceJsonFailed(sourceJson, "Blob upload failed: %s".formatted(e.getMessage()), e);
        }
    }

    void markSourceJsonFailed(InterfaceFileEntity sourceJson, String message, RuntimeException exception) {
        sourceJson.setStatus(Status.FAILED);
        sourceJson.setErrors(errorJson(message));
        log.error("Error processing SOURCE_JSON interfaceFileId={} fileName={}",
            sourceJson.getInterfaceFileId(), sourceJson.getFileName(), exception);
    }

    protected abstract String[] getBusinessUnitsFromExtract(BaisFileProcessorConfiguration config, T extract);

    protected abstract Domain getDomainFromExtract(BaisFileProcessorConfiguration config, T extract);

    protected abstract void postProcessExtract(
        BaisFileProcessorConfiguration config,
        InterfaceFileEntity sourceInterfaceFile,
        T extract
    );

    /**
     * Decides whether an extract should continue into SOURCE_JSON creation.
     *
     * @return true if the extract should be processed, false if it should be skipped
     */
    public abstract boolean preProcessExtract(
        BaisFileProcessorConfiguration config,
        InterfaceFileEntity sourceInterfaceFile,
        T extract
    );
}
