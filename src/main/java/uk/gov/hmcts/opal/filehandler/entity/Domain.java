package uk.gov.hmcts.opal.filehandler.entity;

import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;

public enum Domain {
    FINES,
    CONFISCATION,
    MAINTENANCE,
    FILE_HANDLER;

    public static Domain valueOf(DomainEnumTypes domain) {
        if (domain == null) {
            return null;
        }
        return Domain.valueOf(domain.name());
    }

    public uk.gov.hmcts.opal.common.user.authorisation.model.Domain toCommonDomain() {
        if (this == FILE_HANDLER) {
            return uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FILE_HANDLING;
        }
        return uk.gov.hmcts.opal.common.user.authorisation.model.Domain.valueOf(this.name());
    }
}
