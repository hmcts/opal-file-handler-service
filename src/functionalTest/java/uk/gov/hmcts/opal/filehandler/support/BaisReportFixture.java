package uk.gov.hmcts.opal.filehandler.support;

import java.util.List;
import java.util.UUID;
import uk.gov.hmcts.opal.filehandler.blob.BlobStorageClient;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient.InterfaceFileRecord;
import uk.gov.hmcts.opal.filehandler.sftp.SftpClient;

/**
 * Resets SFTP, database and blob state for one BAIS report functional-test definition.
 */
public class BaisReportFixture {

    private final BaisReportTestConfig config;

    public BaisReportFixture(BaisReportTestConfig config) {
        this.config = config;
    }

    /**
     * Ensures a scenario starts with only its baseline report fixture.
     */
    public void setUp() {
        cleanDatabaseAndBlobs();
        restoreBaselineSftpFile();
    }

    /**
     * Removes scenario output and restores the report consumed by successful ingestion.
     */
    public void tearDown() {
        RuntimeException cleanupFailure = null;
        try {
            cleanDatabaseAndBlobs();
        } catch (RuntimeException exception) {
            cleanupFailure = exception;
        }

        try {
            restoreBaselineSftpFile();
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
            List<InterfaceFileRecord> records = databaseClient.findByFileName(config.fileName());
            records.stream()
                .map(InterfaceFileRecord::filestoreUuid)
                .filter(uuid -> uuid != null)
                .map(UUID::toString)
                .forEach(blobStorageClient::deleteIfExists);
            databaseClient.deleteByFileName(config.fileName());
            databaseClient.deleteByFileName(config.unsupportedFileName());
        }
    }

    private void restoreBaselineSftpFile() {
        int maxAttempts = 3;
        RuntimeException lastException = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try (SftpClient sftpClient = new SftpClient(config.sftpUsername())) {
                sftpClient.deleteIfExists(config.unsupportedFileName());
                sftpClient.uploadResource(config.resourcePath(), config.fileName());
                return;
            } catch (RuntimeException e) {
                lastException = e;
                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        throw lastException;
    }
}
