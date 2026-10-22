package uk.gov.hmcts.opal.filehandler.steps.hooks;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import uk.gov.hmcts.opal.filehandler.blob.BlobStorageClient;

/**
 * Creates and removes blob fixtures used by interface-file content scenarios.
 */
public class InterfaceFileContentBlobHooks {

    private static final String AVAILABLE_BLOB_NAME = "f0000000-0000-0000-0000-000000000001";
    private static final String MISSING_BLOB_NAME = "f0000000-0000-0000-0000-000000000002";
    private static final String BTECKOH_RESOURCE = "test-data/bteckoh-report/bteckoh-test-file.xlsx";

    private final BlobStorageClient blobStorageClient = new BlobStorageClient();

    /**
     * Uploads the expected BTECKOH content and ensures the missing-blob fixture is absent.
     */
    @Before("@InterfaceFileContentBlobFixture")
    public void setUpInterfaceFileContentBlobs() {
        blobStorageClient.uploadResource(AVAILABLE_BLOB_NAME, BTECKOH_RESOURCE);
        blobStorageClient.deleteIfExists(MISSING_BLOB_NAME);
    }

    /**
     * Removes all blobs reserved for interface-file content functional tests.
     */
    @After("@InterfaceFileContentBlobFixture")
    public void cleanUpInterfaceFileContentBlobs() {
        blobStorageClient.deleteIfExists(AVAILABLE_BLOB_NAME);
        blobStorageClient.deleteIfExists(MISSING_BLOB_NAME);
    }
}
