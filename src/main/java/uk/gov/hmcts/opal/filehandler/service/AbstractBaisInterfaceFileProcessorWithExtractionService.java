package uk.gov.hmcts.opal.filehandler.service;

import static uk.gov.hmcts.opal.filehandler.util.StringUtil.isBlank;

import java.time.Clock;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.BusinessUnitBankAccountEntity;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
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
public abstract class AbstractBaisInterfaceFileProcessorWithExtractionService<T extends InterfaceFileCommonDataExtract>
    extends AbstractInterfaceFileProcessorWithExtractionService<T> {

    private static final String BUSINESS_UNIT_065 = "065";

    private final EnumMap<Domain, InterfaceFilePreprocessQueueService> queueServiceMap;

    protected AbstractBaisInterfaceFileProcessorWithExtractionService(
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
            transactionTemplate, objectMapper, extractionService);
        this.queueServiceMap = new EnumMap<>(Domain.class);
        this.queueServiceMap.put(Domain.FINES, finesQueueService);
        this.queueServiceMap.put(Domain.MAINTENANCE, maintenanceQueueService);
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

    @Override
    public boolean preProcessExtract(BaisFileProcessorConfiguration config,
        InterfaceFileEntity sourceInterfaceFile,
        T extract) {

        BusinessUnitBankAccountEntity businessUnitBankAccount =
            extractionService.getBusinessUnitBankAccount(extract);
        Domain domain = businessUnitBankAccount.getDomain();

        validateSupportedDomain(sourceInterfaceFile, domain);
        updateSourceBusinessUnitAndDomain(sourceInterfaceFile, businessUnitBankAccount, domain);

        if (BUSINESS_UNIT_065.equals(businessUnitBankAccount.getBusinessUnitCode())) {
            return false;
        }

        populateMissingDestinationBankDetails(extract, businessUnitBankAccount);
        return true;
    }

    @Override
    public void postProcessExtract(BaisFileProcessorConfiguration config, InterfaceFileEntity sourceJson, T extract) {
        if (sourceJson.getStatus() == Status.SUCCESS) {
            sendToQueue(sourceJson, sourceJson.getOpalDomain());
            if (sourceJson.getStatus() == Status.FAILED) {
                interfaceFilesRepository.save(sourceJson);
            }
        }
    }

    @Override
    protected String[] getBusinessUnitsFromExtract(BaisFileProcessorConfiguration config, T extract) {
        return new String[] {extractionService.getBusinessUnitBankAccount(extract).getBusinessUnitCode()};
    }

    @Override
    protected Domain getDomainFromExtract(BaisFileProcessorConfiguration config, T extract) {
        return extractionService.getBusinessUnitBankAccount(extract).getDomain();
    }
}
