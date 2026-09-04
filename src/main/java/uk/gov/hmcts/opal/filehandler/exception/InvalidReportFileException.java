package uk.gov.hmcts.opal.filehandler.exception;

public class InvalidReportFileException extends RuntimeException {

    public InvalidReportFileException(String message, Exception cause) {
        super(message, cause);
    }
}
