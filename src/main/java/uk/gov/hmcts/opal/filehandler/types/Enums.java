package uk.gov.hmcts.opal.filehandler.types;

public final class Enums {

    private Enums() {
    }

    public enum OriginatorType {
        NEW,
        TFO,
        FP
    }

    public enum PaymentTermsTypeCode {
        B,
        P,
        I
    }

    public enum InstalmentPeriod {
        W,
        M,
        F
    }

    public enum NoteType {
        AC,
        AA,
        AN
    }

    public enum LanguageCode {
        CY,
        EN
    }

    public enum EnforcementResultId {
        PRIS,
        NOENF,
        COLLO
    }
}