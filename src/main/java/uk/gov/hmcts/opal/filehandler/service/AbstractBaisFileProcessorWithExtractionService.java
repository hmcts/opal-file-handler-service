package uk.gov.hmcts.opal.filehandler.service;

import static uk.gov.hmcts.opal.filehandler.util.StringUtil.isBlank;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.BusinessUnitBankAccountEntity;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.exception.BlobChecksumValidationException;
import uk.gov.hmcts.opal.filehandler.exception.UnexpectedDomainException;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.extraction.ExtractionService;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.BankDetails;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.DestinationDetails;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.InterfaceFileCommonDataExtract;
import uk.gov.hmcts.opal.filehandler.service.queue.InterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.filehandler.util.FeatureFlagUtil;

@Slf4j
public abstract class AbstractBaisFileProcessorWithExtractionService<T extends InterfaceFileCommonDataExtract>
    extends AbstractBaisFileProcessorService {

    private static final String BUSINESS_UNIT_065 = "065";

    @Value("${opal.file-handler-service.extraction-service.max-retries}")
    private int maxRetries;

    private final ExtractionService<T> extractionService;

    private final EnumMap<Domain, InterfaceFilePreprocessQueueService> queueServiceMap;

    protected AbstractBaisFileProcessorWithExtractionService(
        Clock clock,
        FeatureFlagUtil featureFlagUtil,
        BaisSftpClient baisSftpClient,
        InterfaceFileBlobStoreService interfaceFileBlobStoreService,
        InterfaceFilesRepository interfaceFilesRepository,
        TransactionTemplate transactionTemplate,
        ObjectMapper objectMapper,
        ExtractionService<T> extractionService,
        InterfaceFilePreprocessQueueService finesQueueService,
        InterfaceFilePreprocessQueueService maintenanceQueueService
    ) {
        super(clock, featureFlagUtil, baisSftpClient, interfaceFileBlobStoreService, interfaceFilesRepository,
            transactionTemplate, objectMapper);
        this.extractionService = extractionService;
        this.queueServiceMap = new EnumMap<>(Domain.class);
        this.queueServiceMap.put(Domain.FINES, finesQueueService);
        this.queueServiceMap.put(Domain.MAINTENANCE, maintenanceQueueService);
    }

    @Override
    protected List<String> selectFilesToProcess(BaisFileProcessorConfiguration config) {
        List<InterfaceFileEntity> sourceFilesToRetry =
            interfaceFilesRepository.findSourceFilesWithJsonFailuresWithinRetryLimit(config.getSource(), maxRetries);

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
            BusinessUnitBankAccountEntity businessUnitBankAccount =
                extractionService.getBusinessUnitBankAccount(extract);
            Domain domain = businessUnitBankAccount.getDomain();

            validateSupportedDomain(sourceInterfaceFile, domain);
            updateSourceBusinessUnitAndDomain(sourceInterfaceFile, businessUnitBankAccount, domain);

            if (BUSINESS_UNIT_065.equals(businessUnitBankAccount.getBusinessUnitCode())) {
                continue;
            }

            processExtract(config, sourceInterfaceFile, extract, businessUnitBankAccount);
        }

        interfaceFilesRepository.save(sourceInterfaceFile);
    }

    void validateSupportedDomain(InterfaceFileEntity sourceInterfaceFile, Domain domain) {
        if (!queueServiceMap.containsKey(domain)) {
            throw new UnexpectedDomainException(
                ("Domain '%s' found for source file '%s' but only the following domains are allowed "
                    + queueServiceMap.keySet().stream().map(Enum::name).reduce((a, b) -> a + ", " + b).orElse("")
                )
                    .formatted(domain, sourceInterfaceFile.getInterfaceFileId())
            );
        }
    }

    void updateSourceBusinessUnitAndDomain(
        InterfaceFileEntity sourceInterfaceFile,
        BusinessUnitBankAccountEntity businessUnitBankAccount,
        Domain domain
    ) {
        Set<String> businessUnitCodes = new LinkedHashSet<>();
        if (sourceInterfaceFile.getBusinessUnitCode() != null) {
            businessUnitCodes.addAll(Arrays.asList(sourceInterfaceFile.getBusinessUnitCode()));
        }
        businessUnitCodes.add(businessUnitBankAccount.getBusinessUnitCode());

        sourceInterfaceFile.setBusinessUnitCode(businessUnitCodes.toArray(String[]::new));
        sourceInterfaceFile.setOpalDomain(domain);
    }

    void processExtract(
        BaisFileProcessorConfiguration config,
        InterfaceFileEntity sourceInterfaceFile,
        T extract,
        BusinessUnitBankAccountEntity businessUnitBankAccount
    ) {
        populateMissingDestinationBankDetails(extract, businessUnitBankAccount);

        byte[] jsonBytes = objectMapper.writeValueAsBytes(extract);
        String checksum = calculateExtractChecksum(jsonBytes);

        if (alreadyProcessedSuccessfully(sourceInterfaceFile, extract, checksum)) {
            return;
        }

        supersedeFailedSourceJson(sourceInterfaceFile, extract, checksum);

        InterfaceFileEntity sourceJson = createSourceJson(
            config, sourceInterfaceFile, extract, businessUnitBankAccount, checksum);

        uploadSourceJson(config, sourceJson, jsonBytes);
        sourceJson = interfaceFilesRepository.save(sourceJson);

        if (sourceJson.getStatus() == Status.SUCCESS) {
            sendToQueue(sourceJson, businessUnitBankAccount.getDomain());
            if (sourceJson.getStatus() == Status.FAILED) {
                interfaceFilesRepository.save(sourceJson);
            }
        }
    }

    void populateMissingDestinationBankDetails(
        InterfaceFileCommonDataExtract extract,
        BusinessUnitBankAccountEntity businessUnitBankAccount
    ) {
        DestinationDetails destinationDetails = extract.getDestinationDetails();
        if (destinationDetails == null) {
            destinationDetails = new DestinationDetails();
            extract.setDestinationDetails(destinationDetails);
        }

        BankDetails bankDetails = destinationDetails.getBankDetails();
        if (bankDetails == null) {
            bankDetails = new BankDetails();
            destinationDetails.setBankDetails(bankDetails);
        }

        if (isBlank(bankDetails.getAccountNumber()) && isBlank(bankDetails.getSortCode())) {
            bankDetails.setAccountNumber(businessUnitBankAccount.getBankAccountNumber());
            bankDetails.setSortCode(businessUnitBankAccount.getBankSortCode());
        }
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
        BusinessUnitBankAccountEntity businessUnitBankAccount,
        String checksum
    ) {
        return InterfaceFileEntity.builder()
            .source(config.getSource())
            .target(config.getTarget())
            .type(Type.SOURCE_JSON)
            .opalDomain(businessUnitBankAccount.getDomain())
            .fileName(extract.getFileName())
            .checksum(checksum)
            .status(Status.SUCCESS)
            .createdDatetime(LocalDateTime.now(clock))
            .relatedInterfaceFile(sourceInterfaceFile)
            .businessUnitCode(new String[] {businessUnitBankAccount.getBusinessUnitCode()})
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

    void sendToQueue(InterfaceFileEntity sourceJson, Domain domain) {
        try {
            queueService(domain).send(sourceJson.getInterfaceFileId());
        } catch (RuntimeException e) {
            markSourceJsonFailed(sourceJson, "Queue send failed: %s".formatted(e.getMessage()), e);
        }
    }

    InterfaceFilePreprocessQueueService queueService(Domain domain) {
        InterfaceFilePreprocessQueueService queue = queueServiceMap.get(domain);
        if (queue == null) {
            throw new IllegalStateException("Unsupported queue domain '%s'".formatted(domain));
        }
        return queue;
    }

    void markSourceJsonFailed(InterfaceFileEntity sourceJson, String message, RuntimeException exception) {
        sourceJson.setStatus(Status.FAILED);
        sourceJson.setErrors(errorJson(message));
        log.error("Error processing SOURCE_JSON interfaceFileId={} fileName={}",
            sourceJson.getInterfaceFileId(), sourceJson.getFileName(), exception);
    }
}
