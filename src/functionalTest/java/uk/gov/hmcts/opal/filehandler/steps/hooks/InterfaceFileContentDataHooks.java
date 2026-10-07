package uk.gov.hmcts.opal.filehandler.steps.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import uk.gov.hmcts.opal.filehandler.db.DatabaseClient;

/**
 * Creates and removes database fixtures used by interface-file scenarios.
 */
public class InterfaceFileContentDataHooks {

    private static final String SETUP_SCRIPT = "db/interface-file-content/setup.sql";
    private static final String CLEANUP_SCRIPT = "db/interface-file-content/cleanup.sql";

    /**
     * Replaces any stale test-owned rows with the fixtures required by a local or staging scenario.
     */
    @Before("@InterfaceFileDbFixture")
    public void setUpInterfaceFileContentData() {
        executeScript(SETUP_SCRIPT);
    }

    /**
     * Removes only the rows reserved for local and staging interface-file functional tests.
     */
    @After("@InterfaceFileDbFixture")
    public void cleanUpInterfaceFileContentData() {
        executeScript(CLEANUP_SCRIPT);
    }

    private static void executeScript(String resourcePath) {
        try (DatabaseClient databaseClient = new DatabaseClient()) {
            databaseClient.executeScript(resourcePath);
        }
    }
}
