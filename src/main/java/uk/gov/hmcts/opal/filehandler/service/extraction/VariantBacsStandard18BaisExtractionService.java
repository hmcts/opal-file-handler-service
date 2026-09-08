package uk.gov.hmcts.opal.filehandler.service.extraction;

import jakarta.persistence.EntityNotFoundException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.filehandler.entity.BusinessUnitBankAccountEntity;
import uk.gov.hmcts.opal.filehandler.repository.BusinessUnitBankAccountRepository;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.InterfaceFileCommonDataExtract;

@Service
public class VariantBacsStandard18BaisExtractionService extends BacsStandard18BaisExtractionService {

    private final Pattern fileNamePattern = Pattern.compile("^a121_\\d{6}VB(?<buCode>\\d{3})_\\d{2}\\.dat$");

    public VariantBacsStandard18BaisExtractionService(InterfaceFilesRepository interfaceFilesRepository,
        BusinessUnitBankAccountRepository businessUnitBankAccountRepository) {
        super(interfaceFilesRepository, businessUnitBankAccountRepository);
    }

    @Override
    public BusinessUnitBankAccountEntity getBusinessUnitBankAccount(InterfaceFileCommonDataExtract extractedData) {
        Matcher matcher = fileNamePattern.matcher(extractedData.getFileName());
        if (!matcher.find()) {
            throw new EntityNotFoundException(
                String.format("Business unit bank account code cannot be found for file_name '%s'",
                extractedData.getFileName())
            );
        }
        String businessUnitCode = matcher.group("buCode");

        return businessUnitBankAccountRepository.findByBusinessUnitCode(businessUnitCode)
            .orElseThrow(() -> new EntityNotFoundException(
                String.format("Business unit bank account with business unit code '%s' could not be "
                        + "located for file_name '%s'",
                    businessUnitCode, extractedData.getFileName())
            ));
    }
}
