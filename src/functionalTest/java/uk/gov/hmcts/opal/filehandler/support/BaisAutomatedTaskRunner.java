package uk.gov.hmcts.opal.filehandler.support;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;

/**
 * Starts a report-specific BAIS automated task in a separate non-web JVM for local functional tests.
 */
public class BaisAutomatedTaskRunner {

    /**
     * Runs the configured report ingestion task and waits for it to finish.
     *
     * @param config report-specific task configuration.
     * @return captured application output for the Serenity report.
     */
    public String run(BaisReportTestConfig config) {
        Path applicationJar = TestEnvironment.getApplicationJar().toAbsolutePath().normalize();
        if (!Files.isRegularFile(applicationJar)) {
            throw new IllegalStateException(config.displayName() + " functional-test application JAR does not exist: "
                + applicationJar);
        }

        Path outputFile = createOutputFile(config);
        ProcessBuilder processBuilder = new ProcessBuilder(
            Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-jar",
            applicationJar.toString(),
            "AutomatedTask:" + config.automatedTaskName()
        );
        processBuilder.redirectErrorStream(true);
        processBuilder.redirectOutput(outputFile.toFile());
        configureEnvironment(processBuilder.environment(), config);

        try {
            Process process = processBuilder.start();
            boolean finished = process.waitFor(TestEnvironment.getTaskTimeoutSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                process.waitFor();
                throw new IllegalStateException(config.displayName() + " ingestion task timed out after "
                    + TestEnvironment.getTaskTimeoutSeconds() + " seconds\n" + readOutput(outputFile, config));
            }

            String output = readOutput(outputFile, config);
            if (process.exitValue() != 0) {
                throw new IllegalStateException(config.displayName() + " ingestion task exited with code "
                    + process.exitValue() + "\n" + output);
            }
            return output;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start the " + config.displayName() + " ingestion task", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Interrupted while waiting for the " + config.displayName() + " ingestion task", e);
        } finally {
            deleteOutputFile(outputFile);
        }
    }

    private static void configureEnvironment(Map<String, String> environment, BaisReportTestConfig config) {
        environment.put("SPRING_DATASOURCE_URL", TestEnvironment.getDatabaseUrl());
        environment.put("SPRING_DATASOURCE_USERNAME", TestEnvironment.getDatabaseUsername());
        environment.put("SPRING_DATASOURCE_PASSWORD", TestEnvironment.getDatabasePassword());
        environment.put("FILE_STORE_STORAGE_ACCOUNT_NAME", TestEnvironment.getBlobAccountName());
        environment.put("FILE_STORE_STORAGE_URL", TestEnvironment.getBlobEndpoint());
        environment.put("FILE_STORE_STORAGE_KEY", TestEnvironment.getBlobAccountKey());
        environment.put(config.blobContainerEnvironmentVariable(), config.blobContainerName());
        environment.put("BAIS_SFTP_CONNECTION_HOST", TestEnvironment.getSftpHost());
        environment.put("BAIS_SFTP_CONNECTION_PORT", Integer.toString(TestEnvironment.getSftpPort()));
        environment.put(config.sftpUsernameEnvironmentVariable(), config.sftpUsername());
        environment.put("BAIS_SFTP_PRIVATE_KEY", resolvePrivateKey(config));
        environment.put("RUN_DB_MIGRATION_ON_STARTUP", "false");
        environment.put("LAUNCH_DARKLY_ENABLED", "false");
        environment.put("RELEASE_1C_BANKING_INTERFACES_ENABLED", "true");
        environment.put(config.jobFlagEnvironmentVariable(), "true");
    }

    private static String resolvePrivateKey(BaisReportTestConfig config) {
        if (TestEnvironment.getSftpPrivateKey().isPresent()) {
            return TestEnvironment.getSftpPrivateKey().orElseThrow();
        }

        Path privateKeyPath = TestEnvironment.getSftpPrivateKeyPath(config.sftpUsername()).orElseThrow(() ->
            new IllegalStateException("The local " + config.displayName() + " batch task requires "
                + "FUNCTIONAL_TEST_SFTP_PRIVATE_KEY, BAIS_SFTP_PRIVATE_KEY or "
                + "FUNCTIONAL_TEST_SFTP_PRIVATE_KEY_PATH"));
        try {
            return Files.readString(privateKeyPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read the SFTP private key: " + privateKeyPath, e);
        }
    }

    private static Path createOutputFile(BaisReportTestConfig config) {
        try {
            Path outputDirectory = Path.of("build", "functional-task-output");
            Files.createDirectories(outputDirectory);
            String prefix = config.displayName().toLowerCase(Locale.ROOT) + "-report-";
            return Files.createTempFile(outputDirectory, prefix, ".log");
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to create " + config.displayName() + " task output file", e);
        }
    }

    private static String readOutput(Path outputFile, BaisReportTestConfig config) {
        try {
            return Files.readString(outputFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + config.displayName() + " task output", e);
        }
    }

    private static void deleteOutputFile(Path outputFile) {
        try {
            Files.deleteIfExists(outputFile);
        } catch (IOException ignored) {
            // The task result is more important than removal of its temporary diagnostic output.
        }
    }
}
