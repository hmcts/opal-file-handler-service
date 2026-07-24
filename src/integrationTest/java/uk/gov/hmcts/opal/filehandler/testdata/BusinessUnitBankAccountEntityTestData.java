package uk.gov.hmcts.opal.filehandler.testdata;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.entity.BusinessUnitBankAccountEntity;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.repository.BusinessUnitBankAccountRepository;

@Component
@RequiredArgsConstructor
public class BusinessUnitBankAccountEntityTestData {

    private final BusinessUnitBankAccountRepository repository;

    public BusinessUnitBankAccountEntity getTypicalBusinessUnitBankAccount(long id, String businessUnitCode) {
        return BusinessUnitBankAccountEntity.builder()
            .id(id)
            .businessUnitCode(businessUnitCode)
            .domain(Domain.FINES)
            .bankSortCode("560033")
            .bankAccountNumber("27048527")
            .build();
    }

    public BusinessUnitBankAccountEntity getMaximumBusinessUnitBankAccount(long id) {
        return BusinessUnitBankAccountEntity.builder()
            .id(id)
            .businessUnitCode("Z999")
            .domain(Domain.FILE_HANDLER)
            .bankSortCode("999999")
            .bankAccountNumber("1234567890")
            .dwpCourtCode("DWP1234567")
            .build();
    }

    public BusinessUnitBankAccountEntity saveTypicalBusinessUnitBankAccount(long id, String businessUnitCode) {
        return repository.save(getTypicalBusinessUnitBankAccount(id, businessUnitCode));
    }

    public BusinessUnitBankAccountEntity saveAndFlushBusinessUnitBankAccount(BusinessUnitBankAccountEntity entity) {
        return repository.saveAndFlush(entity);
    }

    public void clear() {
        repository.deleteAll();
    }
}

