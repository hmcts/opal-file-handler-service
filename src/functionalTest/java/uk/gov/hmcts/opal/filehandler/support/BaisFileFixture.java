package uk.gov.hmcts.opal.filehandler.support;

import java.util.List;
import java.util.UUID;
import uk.gov.hmcts.opal.filehandler.blob.BlobStorageClient;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient.InterfaceFileRecord;
import uk.gov.hmcts.opal.filehandler.sftp.SftpClient;

/**
 * Resets SFTP, database and blob state for one BAIS file functional-test definition.
 */
public class BaisFileFixture {

    private final BaisFileTestConfig config;

    public BaisFileFixture(BaisFileTestConfig config) {
        this.config = config;
    }

    /**
     * Ensures a scenario starts with only its baseline file fixture.
     */
    public void setUp() {
        cleanDatabaseAndBlobs();
        restoreBaselineSftpFile();
    }

    /**
     * Removes scenario output and restores the file consumed by successful ingestion.
     */
    public void tearDown() {
        cleanUp(true);
    }

    /**
     * Removes an outline example's database, blob and SFTP fixtures without restoring its file.
     */
    public void remove() {
        cleanUp(false);
    }

    private void cleanUp(boolean restoreBaseline) {
        RuntimeException cleanupFailure = null;
        try {
            cleanDatabaseAndBlobs();
        } catch (RuntimeException exception) {
            if (config.verifiesFinesPreprocessing()) {
                // Preserve pending Fines input rather than restoring/removing its SFTP fixture.
                throw exception;
            }
            cleanupFailure = exception;
        }

        try {
            if (restoreBaseline) {
                restoreBaselineSftpFile();
            } else {
                SftpRetry.withRetry("remove " + config.fileName(), () -> {
                    try (SftpClient sftpClient = new SftpClient(config.sftpUsername())) {
                        sftpClient.deleteIfExists(config.fileName());
                        sftpClient.deleteIfExists(config.unsupportedFileName());
                    }
                });
            }
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

    private void cleanDatabaseAndBlobs() {
        BlobStorageClient blobStorageClient = new BlobStorageClient(config.blobContainerName());
        blobStorageClient.createContainerIfNotExists();
        try (InterfaceFileTestDatabaseClient databaseClient = new InterfaceFileTestDatabaseClient()) {
            List<InterfaceFileRecord> records = databaseClient.findByFileNameAndSource(
                config.fileName(), config.source());
            if (config.verifiesFinesPreprocessing()) {
                FinesPreprocessingFixture.cleanUp(records);
                // Fines may have uploaded transformed JSON while the cleanup wait was running.
                records = databaseClient.findByFileNameAndSource(config.fileName(), config.source());
            }
            records.stream()
                .map(InterfaceFileRecord::filestoreUuid)
                .filter(uuid -> uuid != null)
                .map(UUID::toString)
                .forEach(blobStorageClient::deleteIfExists);
            databaseClient.deleteByFileNameAndSource(config.fileName(), config.source());
            databaseClient.deleteByFileNameAndSource(config.unsupportedFileName(), config.source());
        }
    }

    private void restoreBaselineSftpFile() {
        SftpRetry.withRetry(
            "restore baseline " + config.displayName() + " file for user " + config.sftpUsername(),
            () -> {
                try (SftpClient sftpClient = new SftpClient(config.sftpUsername())) {
                    sftpClient.deleteIfExists(config.unsupportedFileName());
                    sftpClient.uploadResource(config.resourcePath(), config.fileName());
                }
            }
        );
    }
}
