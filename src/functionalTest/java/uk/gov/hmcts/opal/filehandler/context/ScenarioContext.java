package uk.gov.hmcts.opal.filehandler.context;

import uk.gov.hmcts.opal.filehandler.support.TestHttpClient.TestHttpResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds mutable per-scenario state for the functional-test framework.
 */
public class ScenarioContext {

    private final Map<String, String> queuedHeaders = new LinkedHashMap<>();
    private final Map<String, String> rememberedValues = new LinkedHashMap<>();

    private String currentUser = "";
    private String requestBody;
    private TestHttpResponse latestHttpResponse;

    /**
     * Resets the scenario context to a clean state.
     */
    public void reset() {
        queuedHeaders.clear();
        rememberedValues.clear();
        currentUser = "";
        requestBody = null;
        latestHttpResponse = null;
    }

    /**
     * Records the current scenario user.
     *
     * @param currentUser authenticated user driving the current scenario.
     */
    public void setCurrentUser(String currentUser) {
        this.currentUser = currentUser;
    }

    /**
     * Returns the current scenario user, or a supplied fallback when none has been recorded.
     *
     * @param fallbackUser user to return when no explicit current user has been set.
     * @return current scenario user, or the fallback when none has been recorded.
     */
    public String getCurrentUserOrDefault(String fallbackUser) {
        return currentUser == null || currentUser.isBlank() ? fallbackUser : currentUser;
    }

    /**
     * Stores the request body prepared for the next API call.
     *
     * @param requestBody request body to remember.
     */
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    /**
     * Returns and clears the remembered request body.
     *
     * @return remembered request body, or {@code null} when none has been set.
     */
    public String consumeRequestBody() {
        String body = requestBody;
        requestBody = null;
        return body;
    }

    /**
     * Queues a request header to be applied to the next outgoing request.
     *
     * @param name header name.
     * @param value header value.
     */
    public void queueHeader(String name, String value) {
        queuedHeaders.put(name, value);
    }

    /**
     * Returns and clears all queued request headers.
     *
     * @return queued request headers to apply to the next request.
     */
    public Map<String, String> consumeQueuedHeaders() {
        Map<String, String> headers = new LinkedHashMap<>(queuedHeaders);
        queuedHeaders.clear();
        return headers;
    }

    /**
     * Stores a named scenario value for later reuse.
     *
     * @param key logical name for the stored value.
     * @param value value to remember.
     */
    public void remember(String key, String value) {
        rememberedValues.put(key, value);
    }

    /**
     * Returns a previously remembered scenario value.
     *
     * @param key logical name used to store the value.
     * @return remembered value, or {@code null} when none exists.
     */
    public String recall(String key) {
        return rememberedValues.get(key);
    }

    /**
     * Stores the latest raw HTTP response produced outside Serenity.
     *
     * @param latestHttpResponse response to remember.
     */
    public void setLatestHttpResponse(TestHttpResponse latestHttpResponse) {
        this.latestHttpResponse = latestHttpResponse;
    }

    /**
     * Returns and clears the latest remembered raw HTTP response.
     *
     * @return latest remembered raw HTTP response, or {@code null} when none has been recorded.
     */
    public TestHttpResponse consumeLatestHttpResponse() {
        TestHttpResponse response = latestHttpResponse;
        latestHttpResponse = null;
        return response;
    }
}
