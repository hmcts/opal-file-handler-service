package uk.gov.hmcts.opal.filehandler.exception;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
public class BlobStorageContainerNotFoundExceptionTest {

    @Test
    public void isCreatedCorrectly() {
        BlobStorageContainerNotFoundException e = new BlobStorageContainerNotFoundException("some detailed reason");

        assertAll(
            () -> assertEquals("500 INTERNAL_SERVER_ERROR \"some detailed reason\"", e.getMessage()),
            () -> assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode())
        );
    }
}
