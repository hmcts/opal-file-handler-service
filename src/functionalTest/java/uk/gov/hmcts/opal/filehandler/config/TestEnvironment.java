package uk.gov.hmcts.opal.filehandler.config;

import java.util.Optional;

/**
 * Resolves environment-specific settings used by the functional-test framework.
 */
public final class TestEnvironment {

    private static final String DEFAULT_TEST_URL = "http://localhost:4075";
    private static final String DEFAULT_USER_SERVICE_URL = "http://localhost:4555";

    private TestEnvironment() {
    }

    /**
     * Returns the base URL for the application under test.
     *
     * @return configured application base URL, or the local default when none is set.
     */
    public static String getTestUrl() {
        return get("TEST_URL").orElse(DEFAULT_TEST_URL);
    }

    /**
     * Returns the base URL for the user-service test-support endpoint used to obtain bearer
     * tokens.
     *
     * @return configured user-service base URL, or the local default when none is set.
     */
    public static String getUserServiceUrl() {
        return get("OPAL_USER_SERVICE_API_URL")
            .or(() -> get("DEV_OPAL_USER_SERVICE_API_URL"))
            .orElse(DEFAULT_USER_SERVICE_URL);
    }

    /**
     * Returns the database URL for optional functional-test database checks.
     *
     * @return configured functional-test database URL.
     */
    public static String getDatabaseUrl() {
        return getRequired("FUNCTIONAL_TEST_DB_URL");
    }

    /**
     * Returns the database username for optional functional-test database checks.
     *
     * @return configured functional-test database username.
     */
    public static String getDatabaseUsername() {
        return getRequired("FUNCTIONAL_TEST_DB_USERNAME");
    }

    /**
     * Returns the database password for optional functional-test database checks.
     *
     * @return configured functional-test database password.
     */
    public static String getDatabasePassword() {
        return getRequired("FUNCTIONAL_TEST_DB_PASSWORD");
    }

    /**
     * Returns the SFTP host used by reusable SFTP checks.
     *
     * @return configured SFTP host name.
     */
    public static String getSftpHost() {
        return getRequired("FUNCTIONAL_TEST_SFTP_HOST");
    }

    /**
     * Returns the SFTP port used by reusable SFTP checks.
     *
     * @return configured SFTP port, or 22 when none is set.
     */
    public static int getSftpPort() {
        return Integer.parseInt(get("FUNCTIONAL_TEST_SFTP_PORT").orElse("22"));
    }

    /**
     * Returns the SFTP username used by reusable SFTP checks.
     *
     * @return configured SFTP username.
     */
    public static String getSftpUsername() {
        return getRequired("FUNCTIONAL_TEST_SFTP_USERNAME");
    }

    /**
     * Returns the SFTP password used by reusable SFTP checks.
     *
     * @return configured SFTP password.
     */
    public static String getSftpPassword() {
        return getRequired("FUNCTIONAL_TEST_SFTP_PASSWORD");
    }

    /**
     * Returns an optional environment-variable value when it is present and non-blank.
     *
     * @param key environment-variable name to resolve.
     * @return optional environment-variable value.
     */
    public static Optional<String> get(String key) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    /**
     * Returns a required environment-variable value.
     *
     * @param key environment-variable name to resolve.
     * @return environment-variable value.
     * @throws IllegalStateException when the variable is missing or blank.
     */
    public static String getRequired(String key) {
        return get(key)
            .orElseThrow(() -> new IllegalStateException("Missing required environment variable: " + key));
    }
}
