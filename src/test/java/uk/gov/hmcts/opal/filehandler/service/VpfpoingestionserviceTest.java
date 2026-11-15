package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.http.rest.PagedIterable;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.filehandler.config.VpfpoConfiguration;

@ExtendWith(MockitoExtension.class)
class VpfpoingestionserviceTest {

    private static final String CONTAINER_NAME = "vpfpo";
    private static final String FILE_NAME = "test-file.dat";

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private BlobContainerClient blobContainerClient;

    @Mock
    private BlobClient blobClient;
    @Mock
    private BlobItem blobItem;
    @Mock
    private PagedIterable<BlobItem> pagedIterable;
    @Mock
    private VpfpoConfiguration config;

    private Vpfpoingestionservice service;

    @BeforeEach
    void setUp() {
        service = new Vpfpoingestionservice(blobServiceClient, config);

        when(config.getContainerName()).thenReturn(CONTAINER_NAME);
        when(blobServiceClient.getBlobContainerClient(CONTAINER_NAME))
            .thenReturn(blobContainerClient);
    }

    @Nested
    @DisplayName("getFiles()")
    class GetFiles {

        @Test
        @DisplayName("Returns all blob names from the configured container")
        void returnsBlobNames() {

            when(blobItem.getName()).thenReturn(FILE_NAME);
            when(blobContainerClient.listBlobs()).thenReturn(pagedIterable);
            when(pagedIterable.stream()).thenReturn(Stream.of(blobItem));

            List<String> result = service.getFiles();

            assertThat(result).containsExactly(FILE_NAME);

            verify(blobServiceClient).getBlobContainerClient(CONTAINER_NAME);
            verify(blobContainerClient).listBlobs();
        }
    }

    @Nested
    @DisplayName("getFile()")
    class GetFile {

        @Test
        @DisplayName("Downloads the requested blob into a stream")
        void downloadsFileContents() {
            when(blobContainerClient.getBlobClient(FILE_NAME))
                .thenReturn(blobClient);

            ByteArrayOutputStream result = service.getFile(FILE_NAME);

            assertThat(result).isNotNull();

            verify(blobContainerClient).getBlobClient(FILE_NAME);
            verify(blobClient).downloadStream(result);
        }
    }

    @Nested
    @DisplayName("deleteFile()")
    class DeleteFile {

        @Test
        @DisplayName("Deletes the requested blob")
        void deletesFile() {
            when(blobContainerClient.getBlobClient(FILE_NAME))
                .thenReturn(blobClient);
            when(blobClient.deleteIfExists()).thenReturn(true);

            boolean result = service.deleteFile(FILE_NAME);

            assertThat(result).isTrue();

            verify(blobContainerClient).getBlobClient(FILE_NAME);
            verify(blobClient).deleteIfExists();
        }
    }
}