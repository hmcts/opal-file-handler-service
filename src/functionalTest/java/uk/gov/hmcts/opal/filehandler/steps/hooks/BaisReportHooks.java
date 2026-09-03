package uk.gov.hmcts.opal.filehandler.steps.hooks;

import static uk.gov.hmcts.opal.filehandler.support.BaisReportTestData.BTECKOH;
import static uk.gov.hmcts.opal.filehandler.support.BaisReportTestData.CAPS;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import uk.gov.hmcts.opal.filehandler.support.BaisReportFixture;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;

/**
 * Applies isolated fixture lifecycle handling to each BAIS report scenario family.
 */
public class BaisReportHooks {

    private final BaisReportFixture bteckohFixture = new BaisReportFixture(BTECKOH);
    private final BaisReportFixture capsFixture = new BaisReportFixture(CAPS);

    @Before(value = "@EI1", order = 0)
    public void requireIsolatedInfrastructure() {
        if (!TestEnvironment.get("FUNCTIONAL_TEST_EI1_ISOLATED").orElse("false").equals("true")) {
            throw new IllegalStateException("Run EI1 with bin/test-ei1.sh to provision disposable local services");
        }
    }

    @Before("@BteckohReportFixture")
    public void setUpBteckohReport() {
        bteckohFixture.setUp();
    }

    @After("@BteckohReportFixture")
    public void tearDownBteckohReport() {
        if (TestEnvironment.get("FUNCTIONAL_TEST_EI1_ISOLATED").orElse("false").equals("true")) {
            bteckohFixture.tearDown();
        }
    }

    @Before("@CapsReportFixture")
    public void setUpCapsReport() {
        capsFixture.setUp();
    }

    @After("@CapsReportFixture")
    public void tearDownCapsReport() {
        if (TestEnvironment.get("FUNCTIONAL_TEST_EI1_ISOLATED").orElse("false").equals("true")) {
            capsFixture.tearDown();
        }
    }
}
