package uk.gov.hmcts.opal.filehandler.support;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.opal.filehandler.support.BacsStandard18TestData.PaymentInput;

/** Strict banking JSON comparison, including transaction order and unexpected fields. */
public final class BankingJsonAssertions {
    private BankingJsonAssertions() {
    }

    public static Map<String, Object> expectedTransaction(PaymentInput input, Map<String, String> row) {
        return Map.of(
            "transaction_code", row.get("transactionCode"),
            "amount", Long.parseLong(row.get("amountInPence")),
            "date_entry_applied", row.get("dateEntryApplied"),
            "cheque", expectedBoolean(row.get("cheque")),
            "valid_transaction", expectedBoolean(row.get("validTransaction")),
            "originator_details", Map.of(
                "name", input.originatorName(),
                "account_reference", row.get("expectedAccountReference"),
                "bank_details", Map.of("sort_code", input.originatorSortCode(),
                    "account_number", input.originatorAccountNumber())));
    }

    private static boolean expectedBoolean(String value) {
        assertTrue(List.of("true", "false").contains(value), "Expected boolean must be true or false: " + value);
        return Boolean.parseBoolean(value);
    }

    public static void assertCompleteJsonEquals(SoftAssertions transformedAssertions, Object expected, Object actual)
        throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        // STRICT rejects extra fields and preserves array order while ignoring JSON object key order.
        try {
            var comparison = JSONCompare.compareJSON(
                mapper.writeValueAsString(expected), mapper.writeValueAsString(actual), JSONCompareMode.STRICT);
            transformedAssertions.assertThat(comparison.passed())
                .withFailMessage("Transformed banking JSON mismatch:%n%s", comparison.getMessage())
                .isTrue();
        } catch (JSONException exception) {
            throw new IllegalStateException("Could not compare transformed banking JSON", exception);
        }
    }

}
