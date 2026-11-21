package uk.gov.hmcts.opal.filehandler.support;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Dynamic BACS records shared by NatWest and direct Variant Banking uploads. */
public final class BacsStandard18TestData {
    private BacsStandard18TestData() {
    }

    public static byte[] buildFile(List<PaymentInput> inputPayments, String fileType,
                                   String destinationSortCode, String destinationAccountNumber,
                                   String destinationName, String destinationAccountType) {
        List<String> lines = new ArrayList<>("FPCAS".equals(fileType)
            ? List.of("VOL1A00001                           ****830000                                3",
                "HDR1 A123456Z0001                      F     01 09026              081520",
                "UHL1 08350123456    00000000         000              HMCS - LIBRA")
            : List.of("VOL1BACSSTANDARD18", "HDR1BACSSTANDARD18", "HDR2BACSSTANDARD18",
                "UHL1BACSSTANDARD18"));
        inputPayments.forEach(payment -> lines.add(payment.toLine(destinationSortCode, destinationAccountNumber,
            destinationName, destinationAccountType)));
        lines.addAll(List.of("EOF1BACSSTANDARD18", "EOF2BACSSTANDARD18", "UTL1BACSSTANDARD18"));
        String lineEnding = "FPCAS".equals(fileType) ? "\r\n" : "\n";
        return (String.join(lineEnding, lines) + lineEnding).getBytes(StandardCharsets.UTF_8);
    }

    private static boolean expectedBoolean(String value) {
        assertTrue(List.of("true", "false").contains(value), "Expected boolean must be true or false: " + value);
        return Boolean.parseBoolean(value);
    }

    public record PaymentInput(
        String transactionCode,
        long amountInPence,
        String originatorSortCode,
        String originatorAccountNumber,
        String originatorName,
        String accountReference,
        String dateEntryApplied,
        int referenceLeadingSpaces,
        int referenceTrailingSpaces,
        boolean expectedInSourceJson,
        boolean expectedInTransformedJson
    ) {
        public static PaymentInput from(Map<String, String> row) {
            return new PaymentInput(row.get("code"), Long.parseLong(row.get("amountPence")),
                row.get("originSort"), row.get("originAccount"), row.get("originatorName"),
                row.get("accountReference") == null ? "" : row.get("accountReference"), row.get("dateEntryApplied"),
                Integer.parseInt(row.getOrDefault("referenceLeadingSpaces", "0")),
                Integer.parseInt(row.getOrDefault("referenceTrailingSpaces", "0")),
                expectedBoolean(row.getOrDefault("expectedInSourceJson", "true")),
                expectedBoolean(row.getOrDefault("expectedInTransformedJson", "true")));
        }

        public String toLine(String destinationSortCode, String destinationAccountNumber,
                              String destinationName, String destinationAccountType) {
            assertTrue(referenceLeadingSpaces >= 0 && referenceTrailingSpaces >= 0,
                "Reference padding counts must be nonnegative");
            String paddedReference = " ".repeat(referenceLeadingSpaces) + accountReference
                + " ".repeat(referenceTrailingSpaces);
            String body = destinationSortCode + destinationAccountNumber + destinationAccountType + transactionCode
                + originatorSortCode + originatorAccountNumber + "    " + leftPad(amountInPence, 11)
                + rightPad(originatorName, 18) + rightPad(paddedReference, 18)
                + rightPad(destinationName, 18);
            LocalDate date = LocalDate.parse(dateEntryApplied,
                DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
            if (date.getYear() < 2000 || date.getYear() > 2099) {
                throw new IllegalArgumentException("BACS date must be between 2000 and 2099: " + dateEntryApplied);
            }
            String bankDate = String.format(Locale.ROOT, "%02d%03d", date.getYear() % 100, date.getDayOfYear());
            return body + " " + bankDate;
        }

        private static String leftPad(long value, int size) {
            String text = Long.toString(value);
            if (text.length() > size) {
                throw new IllegalArgumentException("Payment amount exceeds BACS field width: " + value);
            }
            return " ".repeat(size - text.length()) + text;
        }

        private static String rightPad(String value, int size) {
            if (value.length() > size) {
                throw new IllegalArgumentException("BACS text field exceeds " + size + " characters: " + value);
            }
            return value + " ".repeat(size - value.length());
        }
    }

}
