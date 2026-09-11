package uk.gov.hmcts.opal.filehandler.testsupport;

import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.steps.BearerTokenStepDef;
import uk.gov.hmcts.opal.filehandler.support.TestHttpClient;
import uk.gov.hmcts.opal.filehandler.support.TestHttpClient.TestHttpResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight client for file-handler `/testing-support/**` endpoints.
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
        return TestHttpClient.request("POST", testSupportUrl(path), headersForBody(body), body);
    }

    /**
     * Executes a body-less POST request against a test-support path.
     *
     * @param path path relative to `/testing-support`.
     * @return response returned by the endpoint.
     */
    public TestHttpResponse post(String path) {
        return TestHttpClient.request("POST", testSupportUrl(path), headersForBody(null), null);
    }

    /**
     * Executes a PATCH request against a test-support path.
     *
     * @param path path relative to `/testing-support`.
     * @param body request body to send.
     * @return response returned by the endpoint.
     */
    public TestHttpResponse patch(String path, String body) {
        return TestHttpClient.request("PATCH", testSupportUrl(path), headersForBody(body), body);
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

    private static Map<String, String> headersForBody(String body) {
        Map<String, String> headers = defaultHeaders();
        byte[] content = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-512").digest(content);
            headers.put("Content-Digest", "sha-512=:" + Base64.getEncoder().encodeToString(digest) + ":");
            return headers;
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-512 is not available", exception);
        }
    }
}
