package uk.gov.hmcts.opal.filehandler.support;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.io.Resources;
import java.io.IOException;
import java.util.List;
import org.apache.commons.codec.digest.DigestUtils;
import uk.gov.hmcts.opal.filehandler.blob.BlobStorageClient;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.db.DatabaseClient;
import uk.gov.hmcts.opal.filehandler.db.ConnectedFinesTestDatabaseClient;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient;
import uk.gov.hmcts.opal.filehandler.sftp.SftpClient;

/** Owns isolated reference data and malformed inputs for rejection checks. */
public class ConnectedBankingFixture implements AutoCloseable {
    private BaisFileTestConfig config;
    private BaisFileTestConfig baseline;
    private String folder;
    private short businessUnit;
    private boolean finesClaimed;
    private boolean handlerClaimed;
    private boolean baselineRemoved;
    private boolean inputClaimed;

    public void prepare(String supplier, String condition) throws IOException {
        if (!"invalid".equals(condition)) {
            throw new IllegalArgumentException("Only invalid-file rejection is supported by this fixture");
        }
        baseline = BaisFileTestData.forDisplayName(supplier);
        folder = "test-data/banking-cycle/" + supplier.toLowerCase();
        businessUnit = switch (supplier) {
            case "DWP" -> 7240;
            case "Jacobs" -> 7241;
            case "Cder" -> 7242;
            case "Marston" -> 7243;
            default -> throw new IllegalArgumentException("Unsupported connected supplier " + supplier);
        };
        String fileName = switch (supplier) {
            case "DWP" -> "7244000040_dat_7240000001_20261105_120001.txt";
            case "Jacobs" -> "7244000041_dat_7241000001_20261105_120101.txt";
            case "Cder" -> "7244000042_dat_7242000001_20261105_120201.txt";
            case "Marston" -> "7244000043_dat_7243000001_20261105_120301.txt";
            default -> throw new IllegalArgumentException("Unsupported connected supplier " + supplier);
        };
        String input = "test-data/bais/invalid-content.txt";
        config = new BaisFileTestConfig(supplier, baseline.source(), baseline.automatedTaskName(),
            baseline.sftpUsername(), baseline.sftpUsernameEnvironmentVariable(), baseline.blobContainerName(),
            baseline.blobContainerEnvironmentVariable(), baseline.jobFlagEnvironmentVariable(), fileName,
            fileName + ".unsupported", DigestUtils.md5Hex(Resources.toByteArray(Resources.getResource(input))), input);
        try (DatabaseClient fines = finesDatabase(); DatabaseClient handler = new DatabaseClient()) {
            assertTrue(fines.queryFirstColumn("SELECT business_unit_id FROM business_units WHERE business_unit_id = ?",
                businessUnit).isEmpty(), "Connected Fines business unit is already in use: " + businessUnit);
            assertTrue(handler.queryFirstColumn(
                "SELECT business_unit_id FROM business_unit_bank_account WHERE business_unit_id = ?",
                businessUnit).isEmpty(), "Connected file-handler business unit is already in use: " + businessUnit);
            try (InterfaceFileTestDatabaseClient files = new InterfaceFileTestDatabaseClient()) {
                assertTrue(files.findByFileName(fileName).isEmpty(),
                    "Connected filename is already in use: " + fileName);
            }
            // Existing fixture scripts use isolated IDs and fail on collisions; shared mappings are not replaced.
            fines.executeScript(folder + "/fines-setup.sql");
            finesClaimed = true;
            handler.executeScript(folder + "/handler-setup.sql");
            handlerClaimed = true;
        }
        new BlobStorageClient(config.blobContainerName()).createContainerIfNotExists();
        try (SftpClient sftp = new SftpClient(config.sftpUsername())) {
            List<String> otherFiles = sftp.listDirectory(".").stream().filter(file -> file.isRegularFile())
                .map(file -> file.getName()).filter(name -> !baseline.fileName().equals(name)).toList();
            assertTrue(otherFiles.isEmpty(), "Connected job folder contains other inputs: " + otherFiles);
            if (sftp.exists(baseline.fileName())) {
                assertArrayEquals(Resources.toByteArray(Resources.getResource(baseline.resourcePath())),
                    sftp.download(baseline.fileName()), "Baseline SFTP input differs; do not replace shared data");
                sftp.deleteIfExists(baseline.fileName());
                baselineRemoved = true;
            }
            assertTrue(!sftp.exists(config.fileName()), "Connected SFTP filename is already in use");
            inputClaimed = true;
            sftp.uploadResource(config.resourcePath(), config.fileName());
        }
    }

    public BaisFileTestConfig config() {
        return config;
    }

    public short businessUnit() {
        return businessUnit;
    }

    public static DatabaseClient finesDatabase() {
        return new DatabaseClient(TestEnvironment.getFinesDatabaseUrl(), TestEnvironment.getFinesDatabaseUsername(),
            TestEnvironment.getFinesDatabasePassword());
    }

    @Override
    public void close() {
        if (config == null) {
            return;
        }
        RuntimeException cleanupFailure = null;
        try {
            restoreSftpInputs();
        } catch (RuntimeException exception) {
            cleanupFailure = exception;
        }
        try {
            cleanOwnedData();
        } catch (RuntimeException exception) {
            if (cleanupFailure == null) {
                cleanupFailure = exception;
            } else {
                cleanupFailure.addSuppressed(exception);
            }
        }
        if (cleanupFailure != null) {
            throw cleanupFailure;
        }
    }

    private void restoreSftpInputs() {
        if (inputClaimed || baselineRemoved) {
            SftpRetry.withRetry("restore connected " + config.displayName() + " SFTP inputs", () -> {
                try (SftpClient sftp = new SftpClient(config.sftpUsername())) {
                    if (inputClaimed) {
                        sftp.deleteIfExists(config.fileName());
                    }
                    if (baselineRemoved) {
                        sftp.uploadResource(baseline.resourcePath(), baseline.fileName());
                    }
                }
            });
        }
    }

    private void cleanOwnedData() {
        if (!finesClaimed && !handlerClaimed) {
            return;
        }
        try (InterfaceFileTestDatabaseClient files = new InterfaceFileTestDatabaseClient()) {
            var owned = files.findByFileName(config.fileName());
            // A failed consumer may still own a queued message. Retain its referenced data for diagnosis;
            // do not purge a shared queue or delete blobs underneath an in-flight retry.
            var extracted = owned.stream().filter(row -> "SOURCE_JSON".equals(row.type())).toList();
            boolean pending = false;
            if (!extracted.isEmpty()) {
                try (var fines = new ConnectedFinesTestDatabaseClient()) {
                    var processed = fines.findByFileName(businessUnit, config.fileName());
                    pending = extracted.stream().anyMatch(source -> processed.stream().noneMatch(file ->
                        file.sourceId() == source.id() && owned.stream().anyMatch(output ->
                            "TRANSFORMED_JSON".equals(output.type()) && output.id() == file.transformedId())));
                }
            }
            if (pending) {
                throw new IllegalStateException("Connected output retained for pending consumer: " + config.fileName()
                    + "; business unit " + businessUnit + ". Resolve the consumer failure before cleanup/rerun.");
            }
            var blobs = new BlobStorageClient(config.blobContainerName());
            owned.stream().filter(row -> row.filestoreUuid() != null)
                .forEach(row -> blobs.deleteIfExists(row.filestoreUuid().toString()));
            files.deleteByFileName(config.fileName());
        }
        if (finesClaimed) {
            try (DatabaseClient fines = finesDatabase()) {
                fines.executeScript(folder + "/fines-cleanup.sql");
                assertTrue(fines.queryFirstColumn(
                    "SELECT business_unit_id FROM business_units WHERE business_unit_id = ?", businessUnit).isEmpty());
            }
        }
        if (handlerClaimed) {
            try (DatabaseClient handler = new DatabaseClient()) {
                handler.update("DELETE FROM business_unit_bank_account WHERE business_unit_id = ?", businessUnit);
            }
        }
    }
}
