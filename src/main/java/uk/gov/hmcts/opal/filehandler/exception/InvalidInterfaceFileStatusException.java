package uk.gov.hmcts.opal.filehandler.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Getter
public class InvalidInterfaceFileStatusException extends ResponseStatusException {

    public InvalidInterfaceFileStatusException(String detailedReason) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, detailedReason);
    }
}