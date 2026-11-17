package uk.gov.hmcts.opal.filehandler.service.extraction;

import uk.gov.hmcts.opal.filehandler.entity.BusinessUnitBankAccountEntity;

public interface BankingExtractionService<T> extends ExtractionService<T> {

    BusinessUnitBankAccountEntity getBusinessUnitBankAccount(T extractedData);
}
