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
    void isCreatedCorrectlyWhenDetailedReasonIsProvided() {
        String detailedReason = "some detailed reason";
        InterfaceFileNotFoundException e = new InterfaceFileNotFoundException(detailedReason);

        assertAll(
            () -> assertEquals("404 NOT_FOUND \"some detailed reason\"", e.getMessage()),
            () -> assertEquals(detailedReason, e.getReason()),
            () -> assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode())
        );
    }

    @Test
    void isCreatedCorrectlyWhenIdIsProvided() {
        Long id = 512L;
        String detailedReason = "Interface file with id 512 could not be located.";
        InterfaceFileNotFoundException e = new InterfaceFileNotFoundException(id);

        assertAll(
            () -> assertEquals("404 NOT_FOUND \"" + detailedReason + "\"", e.getMessage()),
            () -> assertEquals(detailedReason, e.getReason()),
            () -> assertEquals(HttpStatus.NOT_FOUND, e.getStatusCode())
        );
    }
}
