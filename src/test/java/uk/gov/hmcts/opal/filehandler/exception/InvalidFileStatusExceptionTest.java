package uk.gov.hmcts.opal.filehandler.exception;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class InvalidFileStatusExceptionTest {

    @Test
    void isCreatedCorrectly() {
        InvalidInterfaceFileStatusException e = new InvalidInterfaceFileStatusException("some detailed reason");

        assertAll(
            () -> assertEquals("422 UNPROCESSABLE_CONTENT \"some detailed reason\"", e.getMessage()),
            () -> assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, e.getStatusCode())
        );
    }
}
