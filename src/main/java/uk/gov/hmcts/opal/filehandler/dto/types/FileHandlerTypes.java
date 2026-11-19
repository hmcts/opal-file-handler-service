package uk.gov.hmcts.opal.filehandler.dto.types;

public enum FileHandlerTypes {
    ;

    public enum LanguageType {
        CY,
        EN
    }

    public enum EnforcementType {
        B,
        P,
        I
    }

    public enum AccountType {
        AC,
        AA,
        AN
    }

    public enum RecipientType {
        PRIS,
        NOENF,
        COLLO
    }

    public enum OriginatorType {
        NEW,
        TFO,
        FP
    }

    public enum ResultType {
        W,
        M,
        F
    }
}
