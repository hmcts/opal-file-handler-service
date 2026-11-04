package uk.gov.hmcts.opal.filehandler.support;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import java.io.ByteArrayInputStream;
import java.util.UUID;

public class UtilBlobStoreService {

    private final BlobServiceClient blobServiceClient;

    public UtilBlobStoreService() {
        blobServiceClient = new BlobServiceClientBuilder()
            .connectionString(TestContainerConfig.azuriteConnectionString())
            .buildClient();
    }

    public String storeBlob(byte[] file, String containerName, UUID uuid) {
        return storeBlob(file, containerName, uuid.toString());
    }

    public String storeBlob(byte[] file, String containerName, String uuid) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        container.createIfNotExists();
        if (!container.exists()) {
            throw new IllegalArgumentException("Blob container does not exist");
        }
        BlobClient blob = container.getBlobClient(uuid);
        blob.upload(new ByteArrayInputStream(file), file.length, true);
        return blob.getVersionId();
    }

    public void createContainerIfNotExists(String containerName) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        container.createIfNotExists();
    }

    public String getBlobVersion(String containerName, String uuid) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        if (!container.exists()) {
            throw new IllegalArgumentException("Blob container does not exist");
        }
        BlobClient blob = container.getBlobClient(uuid);
        return blob.getVersionId();
    }

    public Boolean getBlobExists(String containerName, UUID uuid) {
        return getBlobExists(containerName, uuid.toString());
    }

    public Boolean getBlobExists(String containerName, String uuid) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        if (!container.exists()) {
            throw new IllegalArgumentException("Blob container does not exist");
        }
        BlobClient blob = container.getBlobClient(uuid);
        return blob.exists();
    }

    public void deleteContainer(String containerName) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        if (container.exists()) {
            blobServiceClient.deleteBlobContainer(containerName);
        }
    }
}
