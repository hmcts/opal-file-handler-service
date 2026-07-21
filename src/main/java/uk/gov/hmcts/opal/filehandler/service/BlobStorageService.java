package uk.gov.hmcts.opal.filehandler.service;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobRequestConditions;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.filehandler.exception.BlobChecksumValidationException;

@Service
@RequiredArgsConstructor
public class BlobStorageService {

    private final BlobServiceClient blobServiceClient;

    public UUID upload(String containerName, Path file, String expectedChecksum) {
        UUID filestoreUuid = UUID.randomUUID();
        BlobClient blobClient = blobServiceClient
            .getBlobContainerClient(containerName)
            .getBlobClient(filestoreUuid.toString());

        try {
            byte[] expectedMd5 = HexFormat.of().parseHex(expectedChecksum);
            BlobHttpHeaders headers = new BlobHttpHeaders()
                .setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .setContentMd5(expectedMd5);
            BlobUploadFromFileOptions options = new BlobUploadFromFileOptions(file.toString())
                .setHeaders(headers)
                .setRequestConditions(new BlobRequestConditions().setIfNoneMatch("*"));

            blobClient.uploadFromFileWithResponse(options, null, Context.NONE);

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
