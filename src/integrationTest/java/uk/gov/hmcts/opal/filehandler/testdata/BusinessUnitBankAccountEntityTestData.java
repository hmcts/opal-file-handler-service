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

    public BusinessUnitBankAccountEntity getTypicalBusinessUnitBankAccount(short id, short businessUnitId) {
        return getTypicalBusinessUnitBankAccount(id, businessUnitId, null);
    }

    public BusinessUnitBankAccountEntity getTypicalBusinessUnitBankAccount(
        short id, short businessUnitCode, String dwpCourtCode) {
        return BusinessUnitBankAccountEntity.builder()
            .id(id)
            .businessUnitCode(businessUnitCode)
            .domain(Domain.FINES)
            .bankSortCode("560033")
            .bankAccountNumber("27048527")
            .dwpCourtCode(dwpCourtCode)
            .build();
    }

    public BusinessUnitBankAccountEntity getMaximumBusinessUnitBankAccount(short id) {
        return BusinessUnitBankAccountEntity.builder()
            .id(id)
            .businessUnitCode((short) 999)
            .domain(Domain.FILE_HANDLER)
            .bankSortCode("999999")
            .bankAccountNumber("1234567890")
            .dwpCourtCode("DWP1234567")
            .build();
    }

    public BusinessUnitBankAccountEntity saveTypicalBusinessUnitBankAccount(short id, short businessUnitId) {
        return repository.save(getTypicalBusinessUnitBankAccount(id, businessUnitId));
    }

    public BusinessUnitBankAccountEntity saveTypicalBusinessUnitBankAccount(
        short id, short businessUnitId, String dwpCourtCode) {
        return repository.save(getTypicalBusinessUnitBankAccount(id, businessUnitId, dwpCourtCode));
    }

    public BusinessUnitBankAccountEntity saveMaximumBusinessUnitBankAccount(short id) {
        return repository.save(getMaximumBusinessUnitBankAccount(id));
    }

    public BusinessUnitBankAccountEntity saveAndFlushBusinessUnitBankAccount(BusinessUnitBankAccountEntity entity) {
        return repository.saveAndFlush(entity);
    }

    public void clear() {
        repository.deleteAll();
    }
}

