package uk.gov.hmcts.opal.filehandler.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidInterfaceFileStatusException extends ResponseStatusException {

    public InvalidInterfaceFileStatusException(String detailedReason) {
        super(HttpStatus.UNPROCESSABLE_CONTENT, detailedReason);
    }
}