package uk.gov.hmcts.opal.filehandler.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DomainTest {

    @Test
    void toCommonDomainMapsFileHandlerToFileHandling() {
        assertEquals(
            uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FILE_HANDLING,
            Domain.FILE_HANDLER.toCommonDomain()
        );
    }

    @Test
    void toCommonDomainMapsMatchingDomainNames() {
        assertEquals(
            uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FINES,
            Domain.FINES.toCommonDomain()
        );
        assertEquals(
            uk.gov.hmcts.opal.common.user.authorisation.model.Domain.CONFISCATION,
            Domain.CONFISCATION.toCommonDomain()
        );
        assertEquals(
            uk.gov.hmcts.opal.common.user.authorisation.model.Domain.MAINTENANCE,
            Domain.MAINTENANCE.toCommonDomain()
        );
    }
}
