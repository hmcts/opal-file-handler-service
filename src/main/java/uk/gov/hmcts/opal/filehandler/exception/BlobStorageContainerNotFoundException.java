package uk.gov.hmcts.opal.filehandler.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Getter
public class BlobStorageContainerNotFoundException extends ResponseStatusException {

    public BlobStorageContainerNotFoundException(String detailedReason) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, detailedReason);
    }
}