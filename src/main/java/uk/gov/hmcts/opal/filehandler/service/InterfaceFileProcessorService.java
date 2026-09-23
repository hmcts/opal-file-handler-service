package uk.gov.hmcts.opal.filehandler.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.exception.BlobUploadException;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.filehandler.util.FeatureFlagUtil;
import uk.gov.hmcts.opal.filehandler.utils.StreamUtil;

@Slf4j
@RequiredArgsConstructor
@Component
public class InterfaceFileProcessorService {

    protected final Clock clock;
    private final FeatureFlagUtil featureFlagUtil;
    private final BaisSftpClient baisSftpClient;
    protected final InterfaceFileBlobStoreService interfaceFileBlobStoreService;
    protected final InterfaceFilesRepository interfaceFilesRepository;
    private final TransactionTemplate transactionTemplate;
    protected final ObjectMapper objectMapper;

    protected void processFile(
        BaisFileProcessorConfiguration config,
        InterfaceFileEntity fileEntity,
        InputStream inputStream
    ) {
        // Processors may implement their own processing logic for ingested files.
        // This method is called after the file has been successfully ingested and stored in the blob store.
    }

    protected void validateFile(InputStream inputStream) {
        // Processors may validate their input format before it is uploaded.
    }

    public void run(BaisFileProcessorConfiguration config) {
        featureFlagUtil.requireEnabledFeature(FeatureFlags.RELEASE_1C_BANKING_INTERFACES);
        featureFlagUtil.requireEnabledFeature(config.getFeatureFlag());

        List<String> baisFiles = selectFilesToProcess(config);

        for (String fileName : baisFiles) {
            try {
                ingestFile(config, fileName);
            } catch (IOException | RuntimeException e) {
                log.error("Failed to ingest file '{}'", fileName, e);
            }
        }
    }

    protected List<String> selectFilesToProcess(BaisFileProcessorConfiguration config) {
        log.info("Selecting files to process from BAIS for user '{}' and source '{}'",
            config.getSftpUsername(), config.getSource());
        List<String> baisFiles = baisSftpClient.listRegularFiles(config.getSftpUsername());

        if (baisFiles.isEmpty()) {
            log.info("No files found in BAIS for user '{}' when processing source '{}'", config.getSftpUsername(),
                config.getSource());
            return List.of();
        }

        Map<Boolean, List<String>> filesByMatch = baisFiles.stream()
            .collect(Collectors.partitioningBy(fileName ->
                config.getFileNameRegex().matcher(fileName).matches()));

        List<String> matchingFiles = filesByMatch.get(true);
        List<String> ignoringFiles = filesByMatch.get(false);

        if (!ignoringFiles.isEmpty()) {
            log.error("Found {} additional files in BAIS for user '{}' that did not match the regex for source '{}' "
                    + "and were ignored: {}",
                ignoringFiles.size(), config.getSftpUsername(), config.getSource(), String.join(", ", ignoringFiles));
        }

        return matchingFiles;
    }

    private void ingestFile(BaisFileProcessorConfiguration config, String fileName) throws IOException {
        final byte[] downloadedBytes;

        try (ByteArrayOutputStream downloadStream = new ByteArrayOutputStream()) {
            baisSftpClient.downloadFile(config.getSftpUsername(), fileName, downloadStream);
            downloadedBytes = downloadStream.toByteArray();
        }

        InterfaceFileEntity entity = ingestFile(
            fileName, downloadedBytes,
            config.getSource(),
            config.getTarget(),
            Type.SOURCE,
            Domain.MAINTENANCE,//TODO check
            config.getContainerName()
        );
        if (entity.getStatus().equals(Status.INGESTED)) {
            processIngestedFile(config, entity, new ByteArrayInputStream(downloadedBytes));
        }

        completeIngestion(config, fileName, entity);
    }

    public InterfaceFileEntity ingestFile(
        String fileName, byte[] sourceFileData,
        Interface source,
        Interface target,
        Type type,
        Domain domain,
        String containerName
    ) throws IOException {
        String fileChecksum = StreamUtil.calculateChecksum(new ByteArrayInputStream(sourceFileData));
        InterfaceFileEntity entity = InterfaceFileEntity.builder()
            .type(type)
            .source(source)
            .target(target)
            .fileName(fileName)
            .checksum(fileChecksum)
            .createdDatetime(LocalDateTime.now(clock))
            .opalDomain(domain)
            .build();
        Optional<InterfaceFileEntity> duplicateOpt = interfaceFilesRepository
            .findByTypeAndFileNameAndChecksumAndStatus(
                type, fileName, fileChecksum, Status.SUCCESS);
        try {
            if (duplicateOpt.isPresent()) {
                InterfaceFileEntity duplicate = duplicateOpt.get();
                String errorMessage = "File with name '%s' and checksum '%s' for source '%s' is a duplicate of %s"
                    .formatted(fileName, fileChecksum, source, duplicate.getInterfaceFileId());
                log.error(errorMessage);
                entity.setErrors(
                    errorJson(errorMessage));
                entity.setStatus(Status.DUPLICATE);
                entity.setFilestoreUuid(duplicate.getFilestoreUuid());
            } else {
                validateFile(new ByteArrayInputStream(sourceFileData));
                UUID fileStoreUuid = UUID.randomUUID();
                interfaceFileBlobStoreService.uploadBaisFile(
                    fileStoreUuid, containerName,
                    new ByteArrayInputStream(sourceFileData), fileChecksum);
                entity.setFilestoreUuid(fileStoreUuid);
                entity.setStatus(Status.INGESTED);
            }
        } catch (Exception e) {
            String errorMessage;
            if (e instanceof BlobUploadException) {
                errorMessage = "Blob upload failed for file '%s': %s".formatted(fileName, e.getMessage());
            } else {
                errorMessage = e.getMessage();
            }
            log.error(errorMessage, fileChecksum, e);
            entity.setErrors(errorJson(errorMessage));
            entity.setStatus(Status.FAILED);
        }
        return saveInitialFile(entity);
    }

    private InterfaceFileEntity saveInitialFile(InterfaceFileEntity entity) {
        return transactionTemplate.execute(transactionStatus -> {
            supersedePreviousFailures(entity.getFileName(), entity.getChecksum());
            return interfaceFilesRepository.save(entity);
        });
    }

    private void processIngestedFile(
        BaisFileProcessorConfiguration config,
        InterfaceFileEntity entity,
        InputStream inputStream
    ) {
        try {
            transactionTemplate.executeWithoutResult(transactionStatus -> processFile(config, entity, inputStream));
        } catch (RuntimeException e) {
            transactionTemplate.executeWithoutResult(transactionStatus -> {
                entity.setStatus(Status.FAILED);
                entity.setErrors(errorJson("File '%s' could not be processed: %s"
                    .formatted(entity.getFileName(), e.getMessage())));
                interfaceFilesRepository.save(entity);
            });
            log.error("Error processing interfaceFileId={} for file {}",
                entity.getInterfaceFileId(), entity.getFileName(), e);
        }
    }

    private void completeIngestion(BaisFileProcessorConfiguration config, String fileName, InterfaceFileEntity entity) {
        if (entity.getStatus().equals(Status.FAILED)) {
            return;
        }

        boolean deleted = baisSftpClient.deleteFile(config.getSftpUsername(), fileName);

        if (!deleted) {
            log.error("Unable to delete BAIS file '{}' for interfaceFileId={}",
                fileName, entity.getInterfaceFileId());
        }

        if (entity.getStatus().equals(Status.INGESTED)) {
            transactionTemplate.executeWithoutResult(transactionStatus -> {
                entity.setStatus(Status.SUCCESS);
                interfaceFilesRepository.save(entity);
            });
        }
    }

    private void supersedePreviousFailures(String fileName, String fileChecksum) {
        List<InterfaceFileEntity> previousFailures = interfaceFilesRepository
            .findAllByFileNameAndChecksumAndStatus(fileName, fileChecksum, Status.FAILED);

        previousFailures.forEach(previousFailure -> previousFailure.setStatus(Status.FAILED_SUPERSEDED));
    }

    protected String errorJson(String message) {
        return objectMapper.createObjectNode().put("message", message).toString();
    }
}
