package uk.gov.hmcts.opal.filehandler.types;

public class FileHandlerTypes {

    public enum OriginatorType {
        NEW,
        TFO,
        FP
    }

    public enum EnforcementType {
        B,
        P,
        I
    }

    public enum ResultType {
        W,
        M,
        F
    }

    public enum AccountType {
        AC,
        AA,
        AN
    }

    public enum LanguageType {
        CY,
        EN
    }

    public enum RecipientType {
        PRIS,
        NOENF,
        COLLO
    }
}