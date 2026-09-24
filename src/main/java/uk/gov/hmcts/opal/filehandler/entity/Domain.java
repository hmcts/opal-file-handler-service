package uk.gov.hmcts.opal.filehandler.entity;

public enum Domain {
    FINES,
    CONFISCATION,
    MAINTENANCE,
    FILE_HANDLER;

    public uk.gov.hmcts.opal.common.user.authorisation.model.Domain toCommonDomain() {
        if (this == FILE_HANDLER) {
            return uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FILE_HANDLING;
        }
        return uk.gov.hmcts.opal.common.user.authorisation.model.Domain.valueOf(this.name());
    }
}
