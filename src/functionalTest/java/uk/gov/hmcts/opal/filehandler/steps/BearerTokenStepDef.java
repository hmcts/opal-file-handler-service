package uk.gov.hmcts.opal.filehandler.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.When;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.support.TestHttpClient;
import uk.gov.hmcts.opal.filehandler.support.TestHttpClient.TestHttpResponse;

import java.util.Map;

/**
 * Defines authentication-related Cucumber steps and shared bearer-token helpers.
 */
public class BearerTokenStepDef extends BaseStepDef {

    public static final String DEFAULT_USER = "opal-test@dev.platform.hmcts.net";
    private static final ThreadLocal<String> TOKEN = new ThreadLocal<>();
    private static final ThreadLocal<String> OVERRIDE_TOKEN = new ThreadLocal<>();

    /**
     * Clears scenario-scoped token state before each scenario starts.
     */
    @Before
    public void resetBearerTokenState() {
        clearTokenOverride();
        TOKEN.remove();
        scenarioContext().reset();
        scenarioContext().setCurrentUser(DEFAULT_USER);
    }

    /**
     * Returns the bearer token that should be used for the current scenario.
     *
     * @return current scenario bearer token.
     */
    public static String getToken() {
        if (OVERRIDE_TOKEN.get() != null) {
            return OVERRIDE_TOKEN.get();
        }

        if (TOKEN.get() == null) {
            TOKEN.set(fetchAccessToken(DEFAULT_USER));
        }

        return TOKEN.get();
    }

    /**
     * Returns the current scenario token when one has already been initialised.
     *
     * @return current scenario token, or {@code null} when none has been initialised.
     */
    public static String getTokenOrNull() {
        return OVERRIDE_TOKEN.get() != null ? OVERRIDE_TOKEN.get() : TOKEN.get();
    }

    /**
     * Returns an access token for the supplied user.
     *
     * @param user user alias or email used to resolve the bearer token.
     * @return access token for the supplied user.
     */
    public static String getAccessTokenForUser(String user) {
        return fetchAccessToken(user);
    }

    /**
     * Calls the user-service test-support endpoint to obtain a bearer token.
     *
     * @param user user alias or email used to resolve the bearer token.
     * @return access token returned by the user-service test-support endpoint.
     */
    private static String fetchAccessToken(String user) {
        TestHttpResponse response = TestHttpClient.get(
            TestEnvironment.getUserServiceUrl() + "/testing-support/token/user",
            Map.of(
                "Accept", "*/*",
                "Content-Type", "application/json",
                "X-User-Email", user
            )
        );

        if (response.statusCode() != 200) {
            throw new IllegalStateException("Failed to fetch access token, status: " + response.statusCode());
        }

        return response.jsonPath("access_token");
    }

    /**
     * Applies a scenario-scoped bearer-token override.
     *
     * @param token bearer token override for the current scenario.
     */
    public static void setTokenOverride(String token) {
        OVERRIDE_TOKEN.set(token);
    }

    /**
     * Clears any scenario-scoped bearer-token override.
     */
    public static void clearTokenOverride() {
        OVERRIDE_TOKEN.remove();
    }

    /**
     * Switches the current scenario to use the supplied user.
     *
     * @param user user alias or email used to resolve the bearer token.
     */
    @When("I am testing as the {string} user")
    public void setTokenWithUser(String user) {
        setTokenOverride(getAccessTokenForUser(user));
        scenarioContext().setCurrentUser(user);
    }

    /**
     * Calls an endpoint without an Authorization header.
     *
     * @param method HTTP method to invoke.
     * @param path relative API path to call.
     */
    @When("I call {word} {string} without a token")
    public void callWithoutToken(String method, String path) {
        scenarioContext().setLatestHttpResponse(
            TestHttpClient.request(method, getTestUrl() + path, Map.of("Accept", "*/*"), null)
        );
    }

    /**
     * Calls an endpoint with an invalid Authorization header.
     *
     * @param method HTTP method to invoke.
     * @param path relative API path to call.
     */
    @When("I call {word} {string} with an invalid token")
    public void callWithInvalidToken(String method, String path) {
        scenarioContext().setLatestHttpResponse(
            TestHttpClient.request(
                method,
                getTestUrl() + path,
                Map.of("Accept", "*/*", "Authorization", "Bearer invalid-token"),
                null
            )
        );
    }
}
