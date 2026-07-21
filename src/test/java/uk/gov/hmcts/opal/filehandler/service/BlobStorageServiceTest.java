package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.util.Context;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import com.azure.storage.blob.options.BlobUploadFromFileOptions;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import uk.gov.hmcts.opal.filehandler.exception.BlobChecksumValidationException;

@ExtendWith(MockitoExtension.class)
class BlobStorageServiceTest {

    private static final String CONTAINER_NAME = "caps-report";
    private static final String CHECKSUM = "1fa7130130167122bb83decf6cb3bdb1";
    private static final String DIFFERENT_CHECKSUM = "00000000000000000000000000000000";
    private static final byte[] FILE_CONTENT = "CAPS report".getBytes(StandardCharsets.UTF_8);

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;

    @Mock
    private BlobProperties blobProperties;

    @TempDir
    private Path temporaryDirectory;

    private BlobStorageService service;
    private Path file;

    @BeforeEach
    void setUp() throws Exception {
        when(blobServiceClient.getBlobContainerClient(CONTAINER_NAME)).thenReturn(blobContainerClient);
        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        service = new BlobStorageService(blobServiceClient);
        file = Files.write(temporaryDirectory.resolve("report.xml"), FILE_CONTENT);
    }

    @Test
    void uploadStoresOriginalFileWithMd5AndReturnsUuid() {
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getContentMd5()).thenReturn(HexFormat.of().parseHex(CHECKSUM));

        UUID filestoreUuid = service.upload(CONTAINER_NAME, file, CHECKSUM);

        assertThat(filestoreUuid).isNotNull();
        verify(blobContainerClient).getBlobClient(filestoreUuid.toString());

        ArgumentCaptor<BlobUploadFromFileOptions> optionsCaptor =
            ArgumentCaptor.forClass(BlobUploadFromFileOptions.class);
        verify(blobClient).uploadFromFileWithResponse(optionsCaptor.capture(), isNull(), eq(Context.NONE));

        BlobUploadFromFileOptions options = optionsCaptor.getValue();
        assertThat(options.getFilePath()).isEqualTo(file.toString());
        assertThat(options.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        assertThat(options.getHeaders().getContentMd5()).containsExactly(HexFormat.of().parseHex(CHECKSUM));
        assertThat(options.getRequestConditions().getIfNoneMatch()).isEqualTo("*");
        verify(blobClient, never()).deleteIfExists();
    }

    @Test
    void uploadDeletesBlobAndThrowsTypedExceptionWhenChecksumDiffers() {
        when(blobClient.getProperties()).thenReturn(blobProperties);
        when(blobProperties.getContentMd5()).thenReturn(HexFormat.of().parseHex(DIFFERENT_CHECKSUM));

        BlobChecksumValidationException exception = catchThrowableOfType(
            BlobChecksumValidationException.class,
            () -> service.upload(CONTAINER_NAME, file, CHECKSUM));

        assertThat(exception.getFilestoreUuid()).isNotNull();
        assertThat(exception.getExpectedChecksum()).isEqualTo(CHECKSUM);
        assertThat(exception.getActualChecksum()).isEqualTo(DIFFERENT_CHECKSUM);
        assertThat(exception)
            .hasMessage("Blob checksum validation failed for filestore UUID '%s': expected '%s' but was '%s'"
                .formatted(exception.getFilestoreUuid(), CHECKSUM, DIFFERENT_CHECKSUM));
        verify(blobClient).deleteIfExists();
    }

    @Test
    void uploadDeletesPartialBlobAndPropagatesUploadFailure() {
        IllegalStateException uploadFailure = new IllegalStateException("upload failed");
        doThrow(uploadFailure).when(blobClient)
            .uploadFromFileWithResponse(any(BlobUploadFromFileOptions.class), isNull(), eq(Context.NONE));

        assertThatThrownBy(() -> service.upload(CONTAINER_NAME, file, CHECKSUM))
            .isSameAs(uploadFailure);
        verify(blobClient).deleteIfExists();
    }

}
