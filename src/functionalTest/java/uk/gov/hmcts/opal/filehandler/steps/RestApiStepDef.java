package uk.gov.hmcts.opal.filehandler.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import net.serenitybdd.rest.SerenityRest;

/**
 * Provides generic REST step definitions that can be reused across file-handler scenarios.
 */
public class RestApiStepDef extends BaseStepDef {

    /**
     * Queues a header to be applied to the next outgoing request.
     *
     * @param name header name.
     * @param value header value.
     */
    @Given("I set header {string} to {string}")
    public void setHeader(String name, String value) {
        scenarioContext().queueHeader(name, value);
    }

    /**
     * Stores a JSON request body for the next outgoing request.
     *
     * @param requestBody JSON request body to remember.
     */
    @Given("I prepare JSON request body:")
    public void prepareJsonRequestBody(String requestBody) {
        scenarioContext().setRequestBody(requestBody);
    }

    /**
     * Executes a GET request against the supplied relative path.
     *
     * @param path relative API path to call.
     */
    @When("I call GET {string}")
    public void callGet(String path) {
        jsonRequestWithOptionalToken(BearerTokenStepDef.getTokenOrNull())
            .when()
            .get(getTestUrl() + path);
    }

    /**
     * Executes a POST request against the supplied relative path using the remembered request
     * body.
     *
     * @param path relative API path to call.
     */
    @When("I call POST {string}")
    public void callPost(String path) {
        jsonRequestWithOptionalToken(BearerTokenStepDef.getTokenOrNull())
            .body(orEmpty(scenarioContext().consumeRequestBody()))
            .when()
            .post(getTestUrl() + path);
    }

    /**
     * Executes a PATCH request against the supplied relative path using the remembered request
     * body.
     *
     * @param path relative API path to call.
     */
    @When("I call PATCH {string}")
    public void callPatch(String path) {
        jsonRequestWithOptionalToken(BearerTokenStepDef.getTokenOrNull())
            .body(orEmpty(scenarioContext().consumeRequestBody()))
            .when()
            .patch(getTestUrl() + path);
    }

    /**
     * Executes an unauthenticated call to the health endpoint.
     */
    @When("I call the health endpoint")
    public void callHealthEndpoint() {
        SerenityRest.given()
            .accept("*/*")
            .when()
            .get(getTestUrl() + "/health");
    }

    /**
     * Normalises a potentially null request body into a non-null string.
     *
     * @param body request body to normalise.
     * @return original request body, or an empty string when the supplied value is {@code null}.
     */
    private static String orEmpty(String body) {
        return body == null ? "" : body;
    }
}
