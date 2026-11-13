package uk.gov.hmcts.opal.filehandler.steps.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import java.util.Locale;
import java.util.Map;
import uk.gov.hmcts.opal.filehandler.context.ScenarioContextHolder;
import uk.gov.hmcts.opal.filehandler.db.DatabaseClient;
import uk.gov.hmcts.opal.filehandler.support.BaisFileFixture;
import uk.gov.hmcts.opal.filehandler.support.BaisFileTestConfig;
import uk.gov.hmcts.opal.filehandler.support.BaisFileTestData;
import uk.gov.hmcts.opal.filehandler.support.FinesSeedFixture;

/** Applies the common BAIS fixture lifecycle to every BAIS ingestion scenario. */
public class BaisFileHooks {

    private boolean fixtureStarted;

    private static final Map<String, BaisFileFixtureDefinition> FIXTURES = Map.of(
        "bteckoh report", definition(BaisFileTestData.BTECKOH),
        "caps report", definition(BaisFileTestData.CAPS),
        "dwp", definition(BaisFileTestData.DWP, "db/dwp/setup.sql", "db/dwp/cleanup.sql"),
        "bteckoh transfer", definition(
            BaisFileTestData.BTECKOH_TRANSFER,
            "db/bteckoh-transfer/setup.sql",
            "db/bteckoh-transfer/cleanup.sql"),
        "natwest", definition(BaisFileTestData.NATWEST, "db/natwest/setup.sql", "db/natwest/cleanup.sql"),
        "barclaycard", definition(
            BaisFileTestData.BARCLAYCARD,
            "db/barclaycard/setup.sql",
            "db/barclaycard/cleanup.sql"),
        "allpay", definition(BaisFileTestData.ALLPAY, "db/allpay/setup.sql", "db/allpay/cleanup.sql"),
        "jacobs", definition(BaisFileTestData.JACOBS, "db/jacobs/setup.sql", "db/jacobs/cleanup.sql"),
        "cder", definition(BaisFileTestData.CDER, "db/cder/setup.sql", "db/cder/cleanup.sql"),
        "marston", definition(BaisFileTestData.MARSTON, "db/marston/setup.sql", "db/marston/cleanup.sql")
    );

    @Before("@BaisFileFixture")
    public void setUpBaisFile(Scenario scenario) {
        BaisFileFixtureDefinition fixture = fixtureFor(scenario);
        if ("ALLPAY".equals(fixture.config().source())) {
            FinesSeedFixture.ensurePrepared();
        }
        fixtureStarted = true;
        executeScript(fixture.setupScript());
        new BaisFileFixture(fixture.config()).setUp();
    }

    @After("@BaisFileFixture")
    public void tearDownBaisFile(Scenario scenario) {
        if (!fixtureStarted) {
            return;
        }
        BaisFileFixtureDefinition fixture = fixtureFor(scenario);
        String selectedFile = ScenarioContextHolder.current().recall(BaisFileTestData.SELECTED_ALLPAY_FILE);
        if (selectedFile != null) {
            new BaisFileFixture(BaisFileTestData.allpayFile(selectedFile)).remove();
            new BaisFileFixture(BaisFileTestData.ALLPAY).setUp();
        } else {
            new BaisFileFixture(fixture.config()).tearDown();
        }
        // Keep bank mappings available if downstream cleanup preserved pending input.
        executeScript(fixture.cleanupScript());
    }

    private static BaisFileFixtureDefinition definition(BaisFileTestConfig config) {
        return new BaisFileFixtureDefinition(config, null, null);
    }

    private static BaisFileFixtureDefinition definition(
        BaisFileTestConfig config,
        String setupScript,
        String cleanupScript
    ) {
        return new BaisFileFixtureDefinition(config, setupScript, cleanupScript);
    }

    private static void executeScript(String resource) {
        if (resource != null) {
            try (DatabaseClient database = new DatabaseClient()) {
                database.executeScript(resource);
            }
        }
    }

    private static BaisFileFixtureDefinition fixtureFor(Scenario scenario) {
        String scenarioName = scenario.getName().toLowerCase(Locale.ROOT);
        return FIXTURES.entrySet().stream()
            .filter(entry -> scenarioName.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unable to identify BAIS file from scenario: " + scenario.getName()));
    }

    private record BaisFileFixtureDefinition(
        BaisFileTestConfig config,
        String setupScript,
        String cleanupScript
    ) {
    }
}
