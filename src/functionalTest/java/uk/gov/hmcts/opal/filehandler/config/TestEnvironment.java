package uk.gov.hmcts.opal.filehandler.config;

import java.util.Optional;

/**
 * Resolves environment-specific settings used by the functional-test framework.
 */
public final class TestEnvironment {

    private static final String DEFAULT_TEST_URL = "http://localhost:4075";
    private static final String DEFAULT_USER_SERVICE_URL = "http://localhost:4555";
    private static final String DEFAULT_DATABASE_URL = "http://localhost:5432";
    private static final String DEFAULT_DATABASE_NAME = "opal-file-handler-db";
    private static final String DEFAULT_DATABASE_USERNAME = "opal-db-user";
    private static final String DEFAULT_DATABASE_PASSWORD = "opal-db-password";
    private static final String DEFAULT_BLOB_CONTAINER_NAME = "bteckoh-report";
    private static final String DEFAULT_BLOB_ACCOUNT_NAME = "devstoreaccount1";
    // Azurite's standard local-development account key; it is not a production secret.
    private static final String DEFAULT_BLOB_ACCOUNT_KEY =
        "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
    private static final String DEFAULT_BLOB_ENDPOINT = "http://127.0.0.1:10000/devstoreaccount1";
    private static final String FILE_STORE_STORAGE_ACCOUNT_NAME = "FILE_STORE_STORAGE_ACCOUNT_NAME";
    private static final String FILE_STORE_STORAGE_KEY = "FILE_STORE_STORAGE_KEY";
    private static final String FILE_STORE_STORAGE_URL = "FILE_STORE_STORAGE_URL";

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
     * Returns the database URL used by database-backed functional-test fixtures and checks.
     *
     * @return configured functional-test database URL in PostgreSQL JDBC format.
     */
    public static String getDatabaseUrl() {
        return toPostgresJdbcUrl(get("FUNCTIONAL_TEST_DB_URL").orElse(DEFAULT_DATABASE_URL));
    }

    /**
     * Returns the database username used by database-backed functional-test fixtures and checks.
     *
     * @return configured functional-test database username, or the local default when none is set.
     */
    public static String getDatabaseUsername() {
        return get("FUNCTIONAL_TEST_DB_USERNAME").orElse(DEFAULT_DATABASE_USERNAME);
    }

    /**
     * Returns the database password used by database-backed functional-test fixtures and checks.
     *
     * @return configured functional-test database password, or the local default when none is set.
     */
    public static String getDatabasePassword() {
        return get("FUNCTIONAL_TEST_DB_PASSWORD").orElse(DEFAULT_DATABASE_PASSWORD);
    }

    /**
     * Converts the local HTTP-style host and port shorthand to the JDBC URL required by the
     * PostgreSQL driver. Fully specified PostgreSQL JDBC URLs are returned unchanged.
     *
     * @param databaseUrl configured functional-test database URL.
     * @return PostgreSQL JDBC URL.
     */
    private static String toPostgresJdbcUrl(String databaseUrl) {
        if (databaseUrl.startsWith("jdbc:postgresql://")) {
            return databaseUrl;
        }
        if (databaseUrl.startsWith("http://")) {
            String hostAndPort = databaseUrl.substring("http://".length());
            return "jdbc:postgresql://" + hostAndPort + "/" + DEFAULT_DATABASE_NAME;
        }
        throw new IllegalArgumentException("Unsupported functional-test database URL: " + databaseUrl);
    }

    /**
     * Returns the blob container used by interface-file content fixtures.
     *
     * @return configured blob container name, or the local Azurite default when none is set.
     */
    public static String getBlobContainerName() {
        return get("FUNCTIONAL_TEST_BLOB_CONTAINER_NAME").orElse(DEFAULT_BLOB_CONTAINER_NAME);
    }

    /**
     * Returns the storage account used by interface-file content fixtures.
     *
     * @return configured storage account name, the deployed application's storage account name,
     *     or the local Azurite default when neither is set.
     */
    public static String getBlobAccountName() {
        return get("FUNCTIONAL_TEST_BLOB_ACCOUNT_NAME")
            .or(() -> get(FILE_STORE_STORAGE_ACCOUNT_NAME))
            .orElse(DEFAULT_BLOB_ACCOUNT_NAME);
    }

    /**
     * Returns the storage account key used by interface-file content fixtures.
     *
     * @return configured storage account key, the deployed application's storage account key,
     *     or the local Azurite default when neither is set.
     */
    public static String getBlobAccountKey() {
        return get("FUNCTIONAL_TEST_BLOB_ACCOUNT_KEY")
            .or(() -> get(FILE_STORE_STORAGE_KEY))
            .orElse(DEFAULT_BLOB_ACCOUNT_KEY);
    }

    /**
     * Returns the blob service endpoint used by interface-file content fixtures.
     *
     * @return configured blob endpoint, the deployed application's blob endpoint, or the local
     *     Azurite default when neither is set.
     */
    public static String getBlobEndpoint() {
        return get("FUNCTIONAL_TEST_BLOB_ENDPOINT")
            .or(() -> get(FILE_STORE_STORAGE_URL))
            .orElse(DEFAULT_BLOB_ENDPOINT);
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
