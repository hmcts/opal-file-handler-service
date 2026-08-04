package uk.gov.hmcts.opal.filehandler.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Getter
public class BlobNotFoundException extends ResponseStatusException {

    public BlobNotFoundException(String detailedReason) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, detailedReason);
    }
}