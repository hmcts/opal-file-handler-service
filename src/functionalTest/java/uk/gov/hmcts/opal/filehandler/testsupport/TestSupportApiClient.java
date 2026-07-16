package uk.gov.hmcts.opal.filehandler.testsupport;

import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.steps.BearerTokenStepDef;
import uk.gov.hmcts.opal.filehandler.support.TestHttpClient;
import uk.gov.hmcts.opal.filehandler.support.TestHttpClient.TestHttpResponse;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight client for future file-handler `/testing-support/**` endpoints.
 */
public class TestSupportApiClient {

    /**
     * Executes a GET request against a test-support path.
     *
     * @param path path relative to `/testing-support`.
     * @return response returned by the endpoint.
     */
    public TestHttpResponse get(String path) {
        return TestHttpClient.get(testSupportUrl(path), defaultHeaders());
    }

    /**
     * Executes a POST request against a test-support path.
     *
     * @param path path relative to `/testing-support`.
     * @param body request body to send.
     * @return response returned by the endpoint.
     */
    public TestHttpResponse post(String path, String body) {
        return TestHttpClient.request("POST", testSupportUrl(path), defaultHeaders(), body);
    }

    /**
     * Executes a PATCH request against a test-support path.
     *
     * @param path path relative to `/testing-support`.
     * @param body request body to send.
     * @return response returned by the endpoint.
     */
    public TestHttpResponse patch(String path, String body) {
        return TestHttpClient.request("PATCH", testSupportUrl(path), defaultHeaders(), body);
    }

    /**
     * Resolves a full URL for a test-support path.
     *
     * @param path path relative to `/testing-support`.
     * @return full URL for the test-support endpoint.
     */
    private static String testSupportUrl(String path) {
        return TestEnvironment.getTestUrl() + "/testing-support" + path;
    }

    /**
     * Builds the default headers used for test-support requests.
     *
     * @return default request headers, including a bearer token when one is available.
     */
    private static Map<String, String> defaultHeaders() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "*/*");
        headers.put("Content-Type", "application/json");
        String token = BearerTokenStepDef.getTokenOrNull();
        if (token != null && !token.isBlank()) {
            headers.put("Authorization", "Bearer " + token);
        }
        return headers;
    }
}
