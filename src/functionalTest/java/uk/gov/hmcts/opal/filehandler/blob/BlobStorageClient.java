package uk.gov.hmcts.opal.filehandler.blob;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import com.google.common.io.Resources;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;

/**
 * Azure Blob Storage helper for functional-test fixtures.
 */
public class BlobStorageClient {

    private final BlobContainerClient containerClient;

    /**
     * Creates a client for the configured functional-test blob container.
     */
    public BlobStorageClient() {
        this(TestEnvironment.getBlobContainerName());
    }

    /**
     * Creates a client for the supplied report-specific blob container.
     *
     * @param containerName blob container to access.
     */
    public BlobStorageClient(String containerName) {
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(
            TestEnvironment.getBlobAccountName(),
            TestEnvironment.getBlobAccountKey()
        );
        BlobServiceClient serviceClient = new BlobServiceClientBuilder()
            .endpoint(TestEnvironment.getBlobEndpoint())
            .credential(credential)
            .buildClient();
        containerClient = serviceClient.getBlobContainerClient(containerName);
    }

    /** Creates the report container in the disposable emulator before ingestion. */
    public void createContainerIfAbsent() {
        containerClient.createIfNotExists();
    }

    /** Returns blob names and ETags so negative scenarios also detect overwrites. */
    public Map<String, String> snapshot() {
        Map<String, String> blobs = new TreeMap<>();
        containerClient.listBlobs().forEach(blob -> blobs.put(blob.getName(), blob.getProperties().getETag()));
        return blobs;
    }

    /**
     * Uploads a classpath resource as a blob, replacing a stale test-owned blob when present.
     *
     * @param blobName blob name to create.
     * @param resourcePath classpath resource to upload.
     */
    public void uploadResource(String blobName, String resourcePath) {
        try {
            URL resource = Resources.getResource(resourcePath);
            byte[] content = Resources.toByteArray(resource);
            containerClient.createIfNotExists();
            containerClient.getBlobClient(blobName)
                .upload(new ByteArrayInputStream(content), content.length, true);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read blob fixture resource: " + resourcePath, e);
        }
    }

    /**
     * Removes a blob when it exists.
     *
     * @param blobName blob name to remove.
     */
    public void deleteIfExists(String blobName) {
        BlobClient blobClient = containerClient.getBlobClient(blobName);
        blobClient.deleteIfExists();
    }

    /**
     * Compares stored blob content with a classpath resource.
     *
     * @param blobName blob to download.
     * @param resourcePath classpath resource containing the expected bytes.
     * @return {@code true} when both contents are byte-for-byte equal.
     */
    public boolean contentMatchesResource(String blobName, String resourcePath) {
        try {
            URL resource = Resources.getResource(resourcePath);
            byte[] expected = Resources.toByteArray(resource);
            byte[] actual = containerClient.getBlobClient(blobName).downloadContent().toBytes();
            return Arrays.equals(expected, actual);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read expected blob resource: " + resourcePath, e);
        }
    }
}
