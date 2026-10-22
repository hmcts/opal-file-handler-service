package uk.gov.hmcts.opal.filehandler.steps.hooks;

import static uk.gov.hmcts.opal.filehandler.support.BaisReportTestData.BTECKOH;
import static uk.gov.hmcts.opal.filehandler.support.BaisReportTestData.CAPS;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import uk.gov.hmcts.opal.filehandler.support.BaisReportFixture;

/**
 * Applies isolated fixture lifecycle handling to each BAIS report scenario family.
 */
public class BaisReportHooks {

    private final BaisReportFixture bteckohFixture = new BaisReportFixture(BTECKOH);
    private final BaisReportFixture capsFixture = new BaisReportFixture(CAPS);

    @Before("@BteckohReportFixture")
    public void setUpBteckohReport() {
        bteckohFixture.setUp();
    }

    @After("@BteckohReportFixture")
    public void tearDownBteckohReport() {
        bteckohFixture.tearDown();
    }

    @Before("@CapsReportFixture")
    public void setUpCapsReport() {
        capsFixture.setUp();
    }

    @After("@CapsReportFixture")
    public void tearDownCapsReport() {
        capsFixture.tearDown();
    }
}
