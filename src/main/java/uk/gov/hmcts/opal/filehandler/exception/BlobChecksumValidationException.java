package uk.gov.hmcts.opal.filehandler.exception;

import java.util.UUID;
import lombok.Getter;

@Getter
public class BlobChecksumValidationException extends RuntimeException {

    private final UUID filestoreUuid;
    private final String expectedChecksum;
    private final String actualChecksum;

    public BlobChecksumValidationException(
        UUID filestoreUuid,
        String expectedChecksum,
        String actualChecksum
    ) {
        super("Blob checksum validation failed for filestore UUID '%s': expected '%s' but was '%s'"
            .formatted(filestoreUuid, expectedChecksum, actualChecksum == null ? "<missing>" : actualChecksum));
        this.filestoreUuid = filestoreUuid;
        this.expectedChecksum = expectedChecksum;
        this.actualChecksum = actualChecksum;
    }
}
