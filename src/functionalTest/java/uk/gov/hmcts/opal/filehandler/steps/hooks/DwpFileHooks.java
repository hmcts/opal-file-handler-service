package uk.gov.hmcts.opal.filehandler.steps.hooks;

import static uk.gov.hmcts.opal.filehandler.support.BaisReportTestData.DWP;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.db.DatabaseClient;
import uk.gov.hmcts.opal.filehandler.support.BaisReportFixture;

/** Sets up the DWP file and its business-unit bank account. */
public class DwpFileHooks {

    private final BaisReportFixture fixture = new BaisReportFixture(DWP);

    @Before("@DwpFileFixture")
    public void setUp() {
        executeScript("db/dwp/setup.sql");
        fixture.setUp();
    }

    @After("@DwpFileFixture")
    public void tearDown() {
        try {
            fixture.tearDown();
        } finally {
            executeScript("db/dwp/cleanup.sql");
        }
    }

    private static void executeScript(String resource) {
        if (!TestEnvironment.isDatabaseManagedByPipeline()) {
            try (DatabaseClient database = new DatabaseClient()) {
                database.executeScript(resource);
            }
        }
    }
}
