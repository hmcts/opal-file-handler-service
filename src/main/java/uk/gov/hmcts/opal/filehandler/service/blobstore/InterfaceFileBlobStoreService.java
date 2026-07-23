package uk.gov.hmcts.opal.filehandler.service.blobstore;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.filehandler.exception.BlobNotFoundException;
import uk.gov.hmcts.opal.filehandler.exception.BlobStorageContainerNotFoundException;

@Service
@Slf4j(topic = "opal.InterfaceFilesBlobStoreService")
@AllArgsConstructor
public class InterfaceFileBlobStoreService {

    private final BlobServiceClient blobServiceClient;

    private BlobContainerClient getBlobContainerClient(String containerName) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        if (!container.exists()) {
            throw new BlobStorageContainerNotFoundException(
                String.format("Blob container \"%s\"does not exist", containerName)
            );
        }
        return container;
    }

    private BlobClient getBlobClient(BlobContainerClient blobContainerClient, String file) {
        BlobClient blob = blobContainerClient.getBlobClient(file);
        if (!blob.exists()) {
            return null;
        }
        return blob;
    }

    private BinaryData getFileContents(BlobClient blob) {
        return blob.downloadContent();
    }

    public BinaryData fetchInterfaceFile(long interfaceFileId,UUID fileUUID, String containerName) {
        BlobContainerClient container = getBlobContainerClient(containerName);
        BlobClient blob = getBlobClient(container, fileUUID.toString());
        if (blob == null) {
            throw new BlobNotFoundException(String.format("Expected interface file id: %d to exist in blobstore "
                    + "container: \"%s\" with name \"%s\" but this could not be located.",
                interfaceFileId, containerName, fileUUID));
        }

        return getFileContents(blob);
    }
}
