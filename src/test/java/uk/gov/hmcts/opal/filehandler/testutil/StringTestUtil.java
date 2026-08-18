package uk.gov.hmcts.opal.filehandler.testutil;

public class StringTestUtil {

    public static void put(StringBuilder row, int startInclusive, String value) {
        row.replace(startInclusive, startInclusive + value.length(), value);
    }

    public static String leftPad(String value, int length) {
        return " ".repeat(length - value.length()) + value;
    }

    public static String rightPad(String value, int length) {
        return value + " ".repeat(length - value.length());
    }

}
