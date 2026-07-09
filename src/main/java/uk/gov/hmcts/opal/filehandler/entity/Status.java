package uk.gov.hmcts.opal.filehandler.entity;

public enum Status {
    DUPLICATE,
    INGESTED,
    SUCCESS,
    SUCCESS_NO_TRANSACTIONS,
    SUPERSEDED,
    FAILED,
    FAILED_SUPERSEDED
}
