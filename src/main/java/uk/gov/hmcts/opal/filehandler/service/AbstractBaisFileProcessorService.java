package uk.gov.hmcts.opal.filehandler.service;

import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.exception.BlobChecksumValidationException;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.filehandler.util.FeatureFlagUtil;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractBaisFileProcessorService {

    private final Clock clock;
    private final FeatureFlagUtil featureFlagUtil;
    private final BaisSftpClient baisSftpClient;
    private final BlobStorageService blobStorageService;
    private final InterfaceFilesRepository interfaceFilesRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    protected abstract void processFile(InterfaceFileEntity fileEntity, InputStream inputStream);

    public void run(BaisFileProcessorConfiguration config) {
        featureFlagUtil.requireEnabledFeature(FeatureFlags.RELEASE_1C_BANKING_INTERFACES);
        featureFlagUtil.requireEnabledFeature(config.getFeatureFlag());

        List<String> baisFiles = baisSftpClient.listRegularFiles(config.getSftpUsername());

        if (baisFiles.isEmpty()) {
            log.info("No files found in BAIS for user '{}' when processing source '{}'", config.getSftpUsername(),
                config.getSource());
            return;
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

        for (String fileName : matchingFiles) {
            try {
                ingestFile(config, fileName);
            } catch (IOException | RuntimeException e) {
                log.error("Failed to ingest file '{}'", fileName, e);
            }
        }
    }

    private void ingestFile(BaisFileProcessorConfiguration config, String fileName) throws IOException {
        Path temporary = Files.createTempFile("bais-", ".tmp");

        try {
            try (OutputStream outputStream = Files.newOutputStream(temporary)) {
                baisSftpClient.downloadFile(config.getSftpUsername(), fileName, outputStream);
            }

            String fileChecksum = calculateChecksum(temporary);
            Optional<InterfaceFileEntity> duplicate = interfaceFilesRepository.findByFileNameAndChecksumAndStatus(
                fileName, fileChecksum, Status.SUCCESS);
            List<InterfaceFileEntity> previousFailures = interfaceFilesRepository
                .findAllByFileNameAndChecksumAndStatus(fileName, fileChecksum, Status.FAILED);

            InterfaceFileEntity entity;

            try {
                UUID fileStoreUuid = blobStorageService.upload(config.getContainerName(), temporary, fileChecksum);

                if (duplicate.isPresent()) {
                    entity = ingestDuplicate(config, fileName, fileChecksum, fileStoreUuid, duplicate.get());
                } else {
                    entity = ingestNew(config, fileName, fileChecksum, fileStoreUuid);
                }
            } catch (BlobChecksumValidationException e) {
                entity = ingestFailure(config, fileName, fileChecksum, e.getMessage());
            } catch (RuntimeException e) {
                entity = ingestFailure(config, fileName, fileChecksum,
                    "Blob upload failed for file '%s': %s".formatted(fileName, e.getMessage()));
            }

            entity = saveInitialFile(entity, previousFailures);

            if (entity.getStatus().equals(Status.INGESTED)) {
                processIngestedFile(entity, temporary, previousFailures);
            }

            deleteRemoteFile(config, fileName, entity);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private InterfaceFileEntity ingestDuplicate(
        BaisFileProcessorConfiguration config,
        String fileName,
        String fileChecksum,
        UUID fileStoreUuid,
        InterfaceFileEntity duplicate
    ) {
        log.error("File with name '{}' and checksum '{}' for source '{}' is a duplicate of {}",
            fileName, fileChecksum, config.getSource(), duplicate.getInterfaceFileId());

        return InterfaceFileEntity.builder()
            .type(Type.SOURCE)
            .target(config.getTarget())
            .source(config.getSource())
            .opalDomain(config.getDomain())
            .fileName(fileName)
            .checksum(fileChecksum)
            .status(Status.DUPLICATE)
            .filestoreUuid(fileStoreUuid)
            .createdDatetime(Date.from(clock.instant()))
            .errors(errorJson("File with name '%s' and checksum '%s' for source '%s' already processed skipping"
                .formatted(fileName, fileChecksum, config.getSource())))
            .build();
    }

    private InterfaceFileEntity ingestNew(
        BaisFileProcessorConfiguration config,
        String fileName,
        String fileChecksum,
        UUID fileStoreUuid
    ) {
        return InterfaceFileEntity.builder()
            .type(Type.SOURCE)
            .target(config.getTarget())
            .source(config.getSource())
            .opalDomain(config.getDomain())
            .fileName(fileName)
            .checksum(fileChecksum)
            .status(Status.INGESTED)
            .filestoreUuid(fileStoreUuid)
            .createdDatetime(Date.from(clock.instant()))
            .build();
    }

    private InterfaceFileEntity ingestFailure(
        BaisFileProcessorConfiguration config,
        String fileName,
        String fileChecksum,
        String failureMessage
    ) {
        return InterfaceFileEntity.builder()
            .type(Type.SOURCE)
            .target(config.getTarget())
            .source(config.getSource())
            .opalDomain(config.getDomain())
            .fileName(fileName)
            .checksum(fileChecksum)
            .status(Status.FAILED)
            .createdDatetime(Date.from(clock.instant()))
            .errors(errorJson(failureMessage))
            .build();
    }

    private InterfaceFileEntity saveInitialFile(
        InterfaceFileEntity entity,
        List<InterfaceFileEntity> previousFailures
    ) {
        return transactionTemplate.execute(transactionStatus -> {
            InterfaceFileEntity savedEntity = interfaceFilesRepository.save(entity);

            if (savedEntity.getStatus().equals(Status.FAILED)) {
                supersedePreviousFailures(previousFailures);
            }

            return savedEntity;
        });
    }

    private void processIngestedFile(
        InterfaceFileEntity entity,
        Path temporary,
        List<InterfaceFileEntity> previousFailures
    ) {
        try (InputStream inputStream = Files.newInputStream(temporary)) {
            transactionTemplate.executeWithoutResult(transactionStatus -> {
                processFile(entity, inputStream);
                supersedePreviousFailures(previousFailures);
            });
        } catch (IOException | RuntimeException e) {
            transactionTemplate.executeWithoutResult(transactionStatus -> {
                entity.setStatus(Status.FAILED);
                entity.setErrors(errorJson("File '%s' could not be processed: %s"
                    .formatted(entity.getFileName(), e.getMessage())));
                interfaceFilesRepository.save(entity);
                supersedePreviousFailures(previousFailures);
            });
            log.error("Error processing interfaceFileId={} for file {}",
                entity.getInterfaceFileId(), entity.getFileName(), e);
        }
    }

    private void deleteRemoteFile(
        BaisFileProcessorConfiguration config,
        String fileName,
        InterfaceFileEntity entity
    ) {
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

    private void supersedePreviousFailures(List<InterfaceFileEntity> previousFailures) {
        if (previousFailures.isEmpty()) {
            return;
        }

        previousFailures.forEach(previousFailure -> previousFailure.setStatus(Status.FAILED_SUPERSEDED));
        interfaceFilesRepository.saveAll(previousFailures);
    }

    private String errorJson(String message) {
        return objectMapper.createObjectNode().put("message", message).toString();
    }

    private static String calculateChecksum(Path file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file)) {
            return DigestUtils.md5DigestAsHex(inputStream);
        }
    }
}
