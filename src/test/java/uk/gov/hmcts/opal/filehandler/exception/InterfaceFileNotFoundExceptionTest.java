package uk.gov.hmcts.opal.filehandler.exception;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class InterfaceFileNotFoundExceptionTest {

    @Test
    void isCreatedCorrectly() {
        InterfaceFileNotFoundException e = new InterfaceFileNotFoundException("some detailed reason");

        assertAll(
            () -> assertEquals("404 NOT_FOUND \"some detailed reason\"", e.getMessage()),
            () -> assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode())
        );
    }
}
