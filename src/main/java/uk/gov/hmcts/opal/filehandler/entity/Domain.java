package uk.gov.hmcts.opal.filehandler.entity;

import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;

public enum Domain {
    FINES,
    CONFISCATION,
    MAINTENANCE,
    FILE_HANDLER;

    public static Domain valueOf(DomainEnumTypes domain) {
        if(domain == null){
            return null;
        }
        return Domain.valueOf(domain.name());
    }
}
