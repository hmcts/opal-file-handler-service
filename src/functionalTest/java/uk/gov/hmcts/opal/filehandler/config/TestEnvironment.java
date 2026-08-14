package uk.gov.hmcts.opal.filehandler.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Resolves environment-specific settings used by the functional-test framework.
 */
public final class TestEnvironment {

    private static final String DEFAULT_TEST_URL = "http://localhost:4075";
    private static final String DEFAULT_USER_SERVICE_URL = "http://localhost:4555";
    private static final String DEFAULT_DATABASE_URL = "http://localhost:5432";
    private static final String DEFAULT_DATABASE_NAME = "opal-file-handler-db";
    private static final String DEFAULT_DATABASE_PORT = "5432";
    private static final String DEFAULT_DATABASE_USERNAME = "opal-db-user";
    private static final String DEFAULT_DATABASE_PASSWORD = "opal-db-password";
    private static final String DATABASE_MANAGED_BY_PIPELINE = "FUNCTIONAL_TEST_DB_MANAGED_BY_PIPELINE";
    private static final String OPAL_DATABASE_HOST = "OPAL_FILE_HANDLER_DB_HOST";
    private static final String OPAL_DATABASE_NAME = "OPAL_FILE_HANDLER_DB_NAME";
    private static final String OPAL_DATABASE_OPTIONS = "OPAL_FILE_HANDLER_DB_OPTIONS";
    private static final String OPAL_DATABASE_PASSWORD = "OPAL_FILE_HANDLER_DB_PASSWORD";
    private static final String OPAL_DATABASE_PORT = "OPAL_FILE_HANDLER_DB_PORT";
    private static final String OPAL_DATABASE_USERNAME = "OPAL_FILE_HANDLER_DB_USERNAME";
    private static final String DEFAULT_BLOB_CONTAINER_NAME = "bteckoh-report";
    private static final String DEFAULT_BLOB_ACCOUNT_NAME = "devstoreaccount1";
    // Azurite's standard local-development account key; it is not a production secret.
    private static final String DEFAULT_BLOB_ACCOUNT_KEY =
        "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
    private static final String DEFAULT_BLOB_ENDPOINT = "http://127.0.0.1:10000/devstoreaccount1";
    private static final String FILE_STORE_STORAGE_ACCOUNT_NAME = "FILE_STORE_STORAGE_ACCOUNT_NAME";
    private static final String FILE_STORE_STORAGE_KEY = "FILE_STORE_STORAGE_KEY";
    private static final String FILE_STORE_STORAGE_URL = "FILE_STORE_STORAGE_URL";
    private static final String DEFAULT_SFTP_HOST = "localhost";
    private static final String DEFAULT_SFTP_PORT = "2222";
    private static final String DEFAULT_SFTP_USERNAME = "BTEckoh-report";
    private static final Path DEFAULT_SFTP_DATA_PATH = Path.of(
        "../opal-shared-infrastructure/bais-emulator/data");
    private static final String DEFAULT_APPLICATION_JAR = "build/libs/opal-file-handler-service.jar";

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
     * @return configured functional-test database URL, the deployed application's database URL,
     *     or the local PostgreSQL default in JDBC format.
     */
    public static String getDatabaseUrl() {
        return get("FUNCTIONAL_TEST_DB_URL")
            .map(TestEnvironment::toPostgresJdbcUrl)
            .orElseGet(TestEnvironment::getApplicationDatabaseUrl);
    }

    /**
     * Returns the database username used by database-backed functional-test fixtures and checks.
     *
     * @return configured functional-test database username, the deployed application's database
     *     username, or the local default when neither is set.
     */
    public static String getDatabaseUsername() {
        return get("FUNCTIONAL_TEST_DB_USERNAME")
            .or(() -> get(OPAL_DATABASE_USERNAME))
            .orElse(DEFAULT_DATABASE_USERNAME);
    }

    /**
     * Returns the database password used by database-backed functional-test fixtures and checks.
     *
     * @return configured functional-test database password, the deployed application's database
     *     password, or the local default when neither is set.
     */
    public static String getDatabasePassword() {
        return get("FUNCTIONAL_TEST_DB_PASSWORD")
            .or(() -> get(OPAL_DATABASE_PASSWORD))
            .orElse(DEFAULT_DATABASE_PASSWORD);
    }

    /**
     * Indicates whether Jenkins has prepared the database fixtures inside the deployed database
     * pod, removing the need for the functional-test JVM to connect directly.
     *
     * @return {@code true} when database fixture setup and cleanup are managed by the pipeline.
     */
    public static boolean isDatabaseManagedByPipeline() {
        return get(DATABASE_MANAGED_BY_PIPELINE)
            .map(Boolean::parseBoolean)
            .orElse(false);
    }

    private static String getApplicationDatabaseUrl() {
        return get(OPAL_DATABASE_HOST)
            .map(host -> "jdbc:postgresql://" + host
                + ":" + get(OPAL_DATABASE_PORT).orElse(DEFAULT_DATABASE_PORT)
                + "/" + get(OPAL_DATABASE_NAME).orElse(DEFAULT_DATABASE_NAME)
                + get(OPAL_DATABASE_OPTIONS).orElse(""))
            .orElseGet(() -> toPostgresJdbcUrl(DEFAULT_DATABASE_URL));
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
        return get("FUNCTIONAL_TEST_SFTP_HOST").orElse(DEFAULT_SFTP_HOST);
    }

    /**
     * Returns the SFTP port used by reusable SFTP checks.
     *
     * @return configured SFTP port, or 22 when none is set.
     */
    public static int getSftpPort() {
        return Integer.parseInt(get("FUNCTIONAL_TEST_SFTP_PORT").orElse(DEFAULT_SFTP_PORT));
    }

    /**
     * Returns the SFTP username used by reusable SFTP checks.
     *
     * @return configured SFTP username.
     */
    public static String getSftpUsername() {
        return get("FUNCTIONAL_TEST_SFTP_USERNAME").orElse(DEFAULT_SFTP_USERNAME);
    }

    /**
     * Returns the SFTP password used by reusable SFTP checks.
     *
     * @return configured SFTP password.
     */
    public static Optional<String> getSftpPassword() {
        return get("FUNCTIONAL_TEST_SFTP_PASSWORD");
    }

    /**
     * Returns the private key content used to authenticate to the functional-test SFTP server.
     *
     * @return configured functional-test key, or the application's BAIS key when available.
     */
    public static Optional<String> getSftpPrivateKey() {
        return get("FUNCTIONAL_TEST_SFTP_PRIVATE_KEY").or(() -> get("BAIS_SFTP_PRIVATE_KEY"));
    }

    /**
     * Returns a local private-key path when key content has not been supplied directly.
     *
     * @return configured key path, or the shared-infrastructure local key when it exists.
     */
    public static Optional<Path> getSftpPrivateKeyPath() {
        return getSftpPrivateKeyPath(getSftpUsername());
    }

    /**
     * Returns a local private-key path for the supplied SFTP user when key content has not been
     * supplied directly.
     *
     * @param sftpUsername SFTP user whose shared-infrastructure key should be used.
     * @return configured key path, or the user's shared-infrastructure local key when it exists.
     */
    public static Optional<Path> getSftpPrivateKeyPath(String sftpUsername) {
        Path defaultPrivateKeyPath = DEFAULT_SFTP_DATA_PATH.resolve(sftpUsername)
            .resolve(Path.of(".ssh", "keys", "bais-sftp-key"));
        return get("FUNCTIONAL_TEST_SFTP_PRIVATE_KEY_PATH")
            .map(Path::of)
            .or(() -> Files.isRegularFile(defaultPrivateKeyPath)
                ? Optional.of(defaultPrivateKeyPath) : Optional.empty());
    }

    /**
     * Returns the executable application JAR used by local batch-task functional tests.
     *
     * @return configured application JAR path, or the standard Gradle bootJar output.
     */
    public static Path getApplicationJar() {
        return Path.of(get("FUNCTIONAL_TEST_APPLICATION_JAR").orElse(DEFAULT_APPLICATION_JAR));
    }

    /**
     * Returns the maximum number of seconds a locally triggered batch task may run.
     *
     * @return configured task timeout, or 90 seconds when none is set.
     */
    public static long getTaskTimeoutSeconds() {
        return Long.parseLong(get("FUNCTIONAL_TEST_TASK_TIMEOUT_SECONDS").orElse("90"));
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
