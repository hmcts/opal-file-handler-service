package uk.gov.hmcts.opal.filehandler.steps.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.db.DatabaseClient;

/**
 * Creates and removes database fixtures used by interface-file scenarios.
 */
public class InterfaceFileContentDataHooks {

    private static final String SETUP_SCRIPT = "db/interface-file-content/setup.sql";
    private static final String CLEANUP_SCRIPT = "db/interface-file-content/cleanup.sql";

    /**
     * Replaces any stale test-owned rows with the fixtures required by a local or staging scenario.
     * Jenkins prepares the disposable PR database before the functional-test stage instead.
     */
    @Before("@InterfaceFileDbFixture")
    public void setUpInterfaceFileContentData() {
        if (!TestEnvironment.isDatabaseManagedByPipeline()) {
            executeScript(SETUP_SCRIPT);
        }
    }

    /**
     * Removes only the rows reserved for local and staging interface-file functional tests.
     * Jenkins cleans the disposable PR database after the functional-test stage instead.
     */
    @After("@InterfaceFileDbFixture")
    public void cleanUpInterfaceFileContentData() {
        if (!TestEnvironment.isDatabaseManagedByPipeline()) {
            executeScript(CLEANUP_SCRIPT);
        }
    }

    private static void executeScript(String resourcePath) {
        try (DatabaseClient databaseClient = new DatabaseClient()) {
            databaseClient.executeScript(resourcePath);
        }
    }
}
