package uk.gov.hmcts.opal.filehandler.steps;

import io.cucumber.java.en.Then;
import uk.gov.hmcts.opal.filehandler.support.TestHttpClient.TestHttpResponse;

import static net.serenitybdd.rest.SerenityRest.lastResponse;
import static net.serenitybdd.rest.SerenityRest.then;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Provides shared response assertions for functional-test scenarios.
 */
public class CommonResponseStepDef extends BaseStepDef {

    /**
     * Asserts that the latest response returned the expected HTTP status code.
     *
     * @param statusCode expected HTTP status code.
     */
    @Then("the response status code is {int}")
    @Then("the response status is {int}")
    public void responseStatusCodeIs(int statusCode) {
        TestHttpResponse rawResponse = scenarioContext().consumeLatestHttpResponse();
        if (rawResponse != null) {
            assertEquals(statusCode, rawResponse.statusCode(), "Unexpected HTTP status");
            return;
        }

        then().log().ifValidationFails().statusCode(statusCode);
    }

    /**
     * Asserts that the latest response body contains the supplied text fragment.
     *
     * @param expectedValue text expected to appear in the response body.
     */
    @Then("the response body contains {string}")
    public void responseBodyContains(String expectedValue) {
        TestHttpResponse rawResponse = scenarioContext().consumeLatestHttpResponse();
        if (rawResponse != null) {
            assertTrue(rawResponse.body().contains(expectedValue));
            return;
        }

        assertTrue(lastResponse().getBody().asString().contains(expectedValue));
    }
}
