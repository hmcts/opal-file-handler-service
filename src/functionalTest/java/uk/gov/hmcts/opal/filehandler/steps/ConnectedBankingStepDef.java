package uk.gov.hmcts.opal.filehandler.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import uk.gov.hmcts.opal.filehandler.support.BaisInterfaceFileClient;
import uk.gov.hmcts.opal.filehandler.support.ConnectedBankingAssertions;
import uk.gov.hmcts.opal.filehandler.support.ConnectedBankingFixture;
import uk.gov.hmcts.opal.filehandler.testsupport.TestSupportApiClient;

/** Thin adapters for connected invalid-file rejection checks. */
public class ConnectedBankingStepDef extends BaseStepDef {
    private final ConnectedBankingFixture fixture = new ConnectedBankingFixture();
    private final ConnectedBankingAssertions assertions = new ConnectedBankingAssertions(
        new BaisInterfaceFileClient(this::authorisedJsonRequest, getTestUrl()));

    @Given("a connected {string} banking file for the {string} case")
    public void prepare(String supplier, String condition) throws IOException {
        fixture.prepare(supplier, condition);
    }

    @When("the connected banking ingestion job runs")
    public void runJob() {
        var response = new TestSupportApiClient().post("/automated-jobs/" + fixture.config().automatedTaskName());
        assertEquals(202, response.statusCode(), "The configured automated job must be accepted");
    }

    @Then("the connected banking file is rejected with {string}")
    public void rejected(String error) throws IOException {
        assertions.rejectedInput(fixture, error);
    }

    @After("@ConnectedBanking")
    public void tearDown() {
        fixture.close();
    }
}
