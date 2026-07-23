package uk.gov.hmcts.opal.filehandler.support;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

public class UtilBlobStoreService {

    private final BlobServiceClient blobServiceClient;

    public UtilBlobStoreService() {
        blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(TestContainerConfig.azuriteConnectionString())
            .buildClient();
    }

    public String storeReport(String report, String containerName, String uuid) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        container.createIfNotExists();
        if (!container.exists()) {
            throw new IllegalArgumentException("Blob container does not exist");
        }
        BlobClient blob = container.getBlobClient(uuid);
        byte[] bytes = report.getBytes(StandardCharsets.UTF_8);
        blob.upload(new ByteArrayInputStream(bytes), bytes.length, true);
        return blob.getVersionId();
    }

    public String getBlobVersion(String containerName, String uuid) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        if (!container.exists()) {
            throw new IllegalArgumentException("Blob container does not exist");
        }
        BlobClient blob = container.getBlobClient(uuid);
        return blob.getVersionId();
    }
}
