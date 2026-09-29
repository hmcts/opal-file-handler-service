package uk.gov.hmcts.opal.filehandler.entity;

import uk.gov.hmcts.opal.logging.integration.dto.IdentifierType;

public enum PdplIdentifierType implements IdentifierType {
    OPAL_USER_ID;

    @Override
    public String getType() {
        return this.name();
    }
}
