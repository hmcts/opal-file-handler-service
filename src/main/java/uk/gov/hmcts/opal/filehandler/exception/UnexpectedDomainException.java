package uk.gov.hmcts.opal.filehandler.exception;

import lombok.Getter;

@Getter
public class UnexpectedDomainException extends RuntimeException {

    private static final String TITLE = "Unexpected domain";

    private final String title;
    private final String detail;

    public UnexpectedDomainException(String detail) {
        super("%s: %s".formatted(TITLE, detail));
        this.title = TITLE;
        this.detail = detail;
    }
}
