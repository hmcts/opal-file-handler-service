package uk.gov.hmcts.opal.filehandler.service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import java.io.InputStream;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.filehandler.exception.BlobChecksumValidationException;

@Service
@RequiredArgsConstructor
public class BlobStorageService {

    private final BlobServiceClient blobServiceClient;

    public UUID upload(String containerName, InputStream stream, String expectedChecksum) {
        UUID filestoreUuid = UUID.randomUUID();
        BlobClient blobClient = blobServiceClient
            .getBlobContainerClient(containerName)
            .getBlobClient(filestoreUuid.toString());

        try {
            blobClient.upload(stream);
            validateChecksum(blobClient, filestoreUuid, expectedChecksum);

            return filestoreUuid;
        } catch (RuntimeException exception) {
            deleteFailedUpload(blobClient, exception);
            throw exception;
        }
    }

    private void validateChecksum(BlobClient blobClient, UUID filestoreUuid, String expectedChecksum) {
        byte[] actualMd5 = blobClient.getProperties().getContentMd5();
        String actualChecksum = actualMd5 == null ? null : HexFormat.of().formatHex(actualMd5);

        if (!expectedChecksum.equalsIgnoreCase(actualChecksum)) {
            throw new BlobChecksumValidationException(
                filestoreUuid, expectedChecksum, actualChecksum);
        }
    }

    private void deleteFailedUpload(BlobClient blobClient, RuntimeException uploadException) {
        try {
            blobClient.deleteIfExists();
        } catch (RuntimeException deletionException) {
            uploadException.addSuppressed(deletionException);
        }
    }
}
