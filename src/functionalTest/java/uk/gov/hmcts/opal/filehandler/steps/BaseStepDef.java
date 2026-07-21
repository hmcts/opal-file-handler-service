package uk.gov.hmcts.opal.filehandler.steps;

import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.context.ScenarioContext;
import uk.gov.hmcts.opal.filehandler.context.ScenarioContextHolder;

import java.util.Map;

/**
 * Provides shared request-building helpers and environment access for functional-test step
 * definitions.
 */
public class BaseStepDef {

    /**
     * Returns the scenario context bound to the current test thread.
     *
     * @return current scenario context.
     */
    protected ScenarioContext scenarioContext() {
        return ScenarioContextHolder.current();
    }

    /**
     * Returns the base URL for the application under test.
     *
     * @return application-under-test base URL.
     */
    protected String getTestUrl() {
        return TestEnvironment.getTestUrl();
    }

    /**
     * Builds an authorised JSON request specification for the current scenario user.
     *
     * @return request specification configured with the current bearer token.
     */
    protected RequestSpecification authorisedJsonRequest() {
        return jsonRequestWithToken(BearerTokenStepDef.getToken());
    }

    /**
     * Builds a JSON request specification with an optional bearer token and any queued scenario
     * headers.
     *
     * @param token bearer token to include when present.
     * @return configured request specification.
     */
    protected RequestSpecification jsonRequestWithOptionalToken(String token) {
        RequestSpecification request = SerenityRest.given()
            .accept("*/*")
            .contentType("application/json");

        if (token != null && !token.isBlank()) {
            request.header("Authorization", "Bearer " + token);
        }

        for (Map.Entry<String, String> header : scenarioContext().consumeQueuedHeaders().entrySet()) {
            request.header(header.getKey(), header.getValue());
        }

        return request;
    }

    /**
     * Builds a JSON request specification with a mandatory bearer token.
     *
     * @param token bearer token to include on the request.
     * @return configured request specification.
     */
    protected RequestSpecification jsonRequestWithToken(String token) {
        return jsonRequestWithOptionalToken(token);
    }
}
