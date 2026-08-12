package uk.gov.hmcts.opal.filehandler.steps;

import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.Assertions;
import uk.gov.hmcts.opal.filehandler.actions.HealthApiActions;
import uk.gov.hmcts.opal.filehandler.assertions.HealthApiAssertions;
import uk.gov.hmcts.opal.filehandler.support.TestHttpClient.TestHttpResponse;

/**
 * Defines Cucumber steps for the health endpoint.
 */
public class HealthApiStepDef extends BaseStepDef {

    private final HealthApiActions actions = new HealthApiActions();
    private final HealthApiAssertions assertions = new HealthApiAssertions();

    /**
     * Calls the file-handler-service health endpoint.
     */
    @When("I request the file handler api health status")
    public void requestFileHandlerApiHealthStatus() {
        scenarioContext().setLatestHttpResponse(actions.getHealth());
    }

    /**
     * Asserts that the most recent health request reported the service as UP.
     */
    @Then("the file handler service reports as up")
    public void fileHandlerServiceReportsAsUp() {
        TestHttpResponse response = scenarioContext().consumeLatestHttpResponse();
        Assertions.assertNotNull(response, "No health response is available to assert");
        assertions.assertServiceIsUp(response);
    }
}
