package uk.gov.hmcts.opal.filehandler.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Getter
public class InterfaceFileNotFoundException extends ResponseStatusException {

    public InterfaceFileNotFoundException(String detailedReason) {
        super(HttpStatus.NOT_FOUND, detailedReason);
    }
}