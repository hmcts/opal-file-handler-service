package uk.gov.hmcts.opal.filehandler.steps.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import java.util.Locale;
import java.util.Map;
import uk.gov.hmcts.opal.filehandler.db.DatabaseClient;
import uk.gov.hmcts.opal.filehandler.support.BaisReportFixture;
import uk.gov.hmcts.opal.filehandler.support.BaisReportTestConfig;
import uk.gov.hmcts.opal.filehandler.support.BaisReportTestData;

/** Applies the common BAIS fixture lifecycle to every BAIS ingestion scenario. */
public class BaisReportHooks {

    private static final Map<String, BaisReportFixtureDefinition> FIXTURES = Map.of(
        "bteckoh report", definition(BaisReportTestData.BTECKOH),
        "caps report", definition(BaisReportTestData.CAPS),
        "dwp", definition(BaisReportTestData.DWP, "db/dwp/setup.sql", "db/dwp/cleanup.sql"),
        "bteckoh transfer", definition(
            BaisReportTestData.BTECKOH_TRANSFER,
            "db/bteckoh-transfer/setup.sql",
            "db/bteckoh-transfer/cleanup.sql"),
        "natwest", definition(BaisReportTestData.NATWEST, "db/natwest/setup.sql", "db/natwest/cleanup.sql"),
        "barclaycard", definition(
            BaisReportTestData.BARCLAYCARD,
            "db/barclaycard/setup.sql",
            "db/barclaycard/cleanup.sql"),
        "allpay", definition(BaisReportTestData.ALLPAY, "db/allpay/setup.sql", "db/allpay/cleanup.sql"),
        "jacobs", definition(BaisReportTestData.JACOBS, "db/jacobs/setup.sql", "db/jacobs/cleanup.sql"),
        "cder", definition(BaisReportTestData.CDER, "db/cder/setup.sql", "db/cder/cleanup.sql"),
        "marston", definition(BaisReportTestData.MARSTON, "db/marston/setup.sql", "db/marston/cleanup.sql")
    );

    @Before("@BaisReportFixture")
    public void setUpBaisReport(Scenario scenario) {
        BaisReportFixtureDefinition fixture = fixtureFor(scenario);
        executeScript(fixture.setupScript());
        new BaisReportFixture(fixture.config()).setUp();
    }

    @After("@BaisReportFixture")
    public void tearDownBaisReport(Scenario scenario) {
        BaisReportFixtureDefinition fixture = fixtureFor(scenario);
        try {
            new BaisReportFixture(fixture.config()).tearDown();
        } finally {
            executeScript(fixture.cleanupScript());
        }
    }

    private static BaisReportFixtureDefinition definition(BaisReportTestConfig config) {
        return new BaisReportFixtureDefinition(config, null, null);
    }

    private static BaisReportFixtureDefinition definition(
        BaisReportTestConfig config,
        String setupScript,
        String cleanupScript
    ) {
        return new BaisReportFixtureDefinition(config, setupScript, cleanupScript);
    }

    private static void executeScript(String resource) {
        if (resource != null) {
            try (DatabaseClient database = new DatabaseClient()) {
                database.executeScript(resource);
            }
        }
    }

    private static BaisReportFixtureDefinition fixtureFor(Scenario scenario) {
        String scenarioName = scenario.getName().toLowerCase(Locale.ROOT);
        return FIXTURES.entrySet().stream()
            .filter(entry -> scenarioName.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Unable to identify BAIS report from scenario: " + scenario.getName()));
    }

    private record BaisReportFixtureDefinition(
        BaisReportTestConfig config,
        String setupScript,
        String cleanupScript
    ) {
    }
}
