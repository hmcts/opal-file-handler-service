package uk.gov.hmcts.opal.filehandler.service.blobstore;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import jakarta.persistence.EntityNotFoundException;
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

    public BinaryData fetchInterfaceFile(long interfaceFileId,UUID fileUUID, String containerName) {
        BlobContainerClient container = blobServiceClient.getBlobContainerClient(containerName);
        if (!container.exists()) {
            throw new BlobStorageContainerNotFoundException(String.format("Blob container \"%s\"does not exist", containerName));
        }

        BlobClient blob = container.getBlobClient(fileUUID.toString());
        if (!blob.exists()) {
            throw new BlobNotFoundException(String.format("Expected interface file id: %d to exist in blobstore "
                + "container: \"%s\" with name \"%s\" but this could not be located.",
                interfaceFileId, containerName, fileUUID));
        }

        return blob.downloadContent();
    }
}
