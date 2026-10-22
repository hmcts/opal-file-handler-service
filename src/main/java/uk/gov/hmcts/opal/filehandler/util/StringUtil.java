package uk.gov.hmcts.opal.filehandler.util;

public class StringUtil {

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
