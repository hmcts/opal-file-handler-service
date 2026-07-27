package uk.gov.hmcts.opal.filehandler.service.blobstore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.filehandler.exception.BlobNotFoundException;
import uk.gov.hmcts.opal.filehandler.exception.BlobStorageContainerNotFoundException;

@ExtendWith(MockitoExtension.class)
public class InterfaceFileBlobStoreServiceTest {

    @Mock
    private FileHandlerAzureStorageConfig config;

    @Mock
    private BlobServiceClient blobServiceClient;

    @Mock
    private BlobContainerClient container;

    @Mock
    private BlobClient blob;

    private InterfaceFileBlobStoreService interfaceFilesBlobStoreService;

    private final UUID fileUUID = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        interfaceFilesBlobStoreService = new InterfaceFileBlobStoreService(blobServiceClient);
    }

    @Test
    public void fetchInterfaceFile() {
        when(blobServiceClient.getBlobContainerClient("container")).thenReturn(container);
        when(container.getBlobClient(eq(fileUUID.toString()))).thenReturn(blob);
        when(container.exists()).thenReturn(true);
        when(blob.exists()).thenReturn(true);
        BinaryData mockResult = mock(BinaryData.class);
        when(blob.downloadContent()).thenReturn(mockResult);

        BinaryData response = interfaceFilesBlobStoreService.fetchInterfaceFile(1L, fileUUID, "container");

        assertEquals(response, mockResult);
        verify(blobServiceClient).getBlobContainerClient(eq("container"));
        verify(container).getBlobClient(eq(fileUUID.toString()));
    }

    @Test
    public void fetchInterfaceFile_containerDoesNotExist_throwError() {
        
        when(blobServiceClient.getBlobContainerClient("container")).thenReturn(container);
        when(container.exists()).thenReturn(false);

        Exception e = assertThrows(
            BlobStorageContainerNotFoundException.class,
            () -> interfaceFilesBlobStoreService.fetchInterfaceFile(1L, fileUUID,"container")
        );
        assertEquals(
            "500 INTERNAL_SERVER_ERROR \"Blob container \"container\" does not exist\"",
            e.getMessage()
        );
    }

    @Test
    public void fetchInterfaceFile_blobDoesNotExist_throwError() {
        
        when(blobServiceClient.getBlobContainerClient("container")).thenReturn(container);
        when(container.exists()).thenReturn(true);
        when(container.getBlobClient(eq(fileUUID.toString()))).thenReturn(blob);
        when(blob.exists()).thenReturn(false);

        Exception e = assertThrows(
            BlobNotFoundException.class,
            () -> interfaceFilesBlobStoreService.fetchInterfaceFile(1L, fileUUID,"container")
        );
        assertEquals(
            "500 INTERNAL_SERVER_ERROR \"Expected interface file id: 1 to exist in blobstore "
            + "container: \"container\" with name \"" + fileUUID.toString() + "\" but this could "
            + "not be located.\"",
            e.getMessage()
        );
    }

    @Test
    public void getBlobContainerClient_returnsContainer() {
        
        when(blobServiceClient.getBlobContainerClient("container")).thenReturn(container);
        when(container.exists()).thenReturn(true);

        BlobContainerClient actual = interfaceFilesBlobStoreService.getBlobContainerClient("container");

        assertEquals(container, actual);
    }

    @Test
    public void getBlobContainerClient_notFoundThrowsException() {
        
        when(blobServiceClient.getBlobContainerClient("container")).thenReturn(container);
        when(container.exists()).thenReturn(false);

        Exception e = assertThrows(
            BlobStorageContainerNotFoundException.class,
            () -> interfaceFilesBlobStoreService.getBlobContainerClient("container")
        );
        assertEquals(
            "500 INTERNAL_SERVER_ERROR \"Blob container \"container\" does not exist\"",
            e.getMessage()
        );
    }

    @Test
    public void getBlobClient_returnsBlob() {
        
        when(container.getBlobClient("filename")).thenReturn(blob);
        when(blob.exists()).thenReturn(true);

        BlobClient actual = interfaceFilesBlobStoreService.getBlobClient(container, "filename");

        assertEquals(blob, actual);
    }

    @Test
    public void getBlobClient_returnsNull() {
        
        when(container.getBlobClient("filename")).thenReturn(blob);
        when(blob.exists()).thenReturn(false);

        BlobClient actual = interfaceFilesBlobStoreService.getBlobClient(container, "filename");

        assertEquals(null, actual);
    }

    @Test
    public void getFileContents() {
        
        BinaryData mockResult = mock(BinaryData.class);
        when(blob.downloadContent()).thenReturn(mockResult);

        BinaryData response = interfaceFilesBlobStoreService.getFileContents(blob);

        assertEquals(response, mockResult);
    }

}