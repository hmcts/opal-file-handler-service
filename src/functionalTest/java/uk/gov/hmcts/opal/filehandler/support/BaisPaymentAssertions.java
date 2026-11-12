package uk.gov.hmcts.opal.filehandler.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.restassured.response.Response;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;

/** Checks the extracted payment contract for the established BAIS fixtures. */
public class BaisPaymentAssertions {
    private final BaisInterfaceFileClient files;

    public BaisPaymentAssertions(BaisInterfaceFileClient files) {
        this.files = files;
    }

    public void assertPaymentsReady(BaisFileTestConfig config) {
        Map<String, Object> jsonFile = files.awaitInterfaceFile(config, "SOURCE_JSON", "SUCCESS");
        assertEquals(config.source(), jsonFile.get("source"));
        assertEquals("OPAL", jsonFile.get("target"));
        assertEquals(config.expectedDomain(), jsonFile.get("domain"));
        assertNotNull(jsonFile.get("filestore_uuid"));
        Response response = files.content(jsonFile);
        assertEquals(jsonFile.get("checksum"), DigestUtils.md5Hex(response.asByteArray()));
        assertEquals(config.fileName(), response.jsonPath().getString("file_name"));
        expectedPayments(config.displayName()).forEach((field, value) ->
            assertEquals(value, response.jsonPath().getString(field), config.displayName() + " extracted " + field));
        if ("DWP".equals(config.source())) {
            assertEquals("DWP1234567", response.jsonPath().getString("dwp_court_code"));
            assertEquals("CASH", response.jsonPath().getString("payment_type"));
            assertEquals("010101", response.jsonPath().getString("destination_details.bank_details.sort_code"));
            assertEquals("12341234", response.jsonPath().getString("destination_details.bank_details.account_number"));
            assertEquals(List.of(2125, 2125, 2125, 2125, 2001),
                response.jsonPath().getList("transactions.amount"));
            assertEquals(List.of("99", "99", "99", "99", "99"),
                response.jsonPath().getList("transactions.transaction_code"));
        }
    }

    private static Map<String, String> expectedPayments(String displayName) {
        return switch (displayName) {
            case "Marston" -> Map.ofEntries(
                Map.entry("payment_type", "CASH"),
                Map.entry("destination_details.bank_details.sort_code", "560038"),
                Map.entry("destination_details.bank_details.account_number", "27048532"),
                Map.entry("transactions.size()", "1"),
                Map.entry("transactions[0].amount", "1000"),
                Map.entry("transactions[0].transaction_code", "99"),
                Map.entry("dwp_court_code", "DWP1234568"),
                Map.entry("transactions[0].originator_details.account_reference", "23000106E"),
                Map.entry("transactions[0].date_entry_applied", "03/09/2023")
            );
            case "NatWest" -> Map.ofEntries(
                Map.entry("payment_type", "CASH"),
                Map.entry("destination_details.bank_details.sort_code", "560035"),
                Map.entry("destination_details.bank_details.account_number", "27048529"),
                Map.entry("transactions.size()", "2"),
                Map.entry("transactions[0].amount", "1500"),
                Map.entry("transactions[0].transaction_code", "99"),
                Map.entry("transactions[1].amount", "500"),
                Map.entry("transactions[1].transaction_code", "93")
            );
            case "Cder" -> Map.ofEntries(
                Map.entry("payment_type", "CASH"),
                Map.entry("destination_details.bank_details.sort_code", "560036"),
                Map.entry("destination_details.bank_details.account_number", "27048530"),
                Map.entry("transactions.size()", "2"),
                Map.entry("transactions[0].amount", "1000"),
                Map.entry("transactions[0].transaction_code", "99"),
                Map.entry("dwp_court_code", "0000031715"),
                Map.entry("transactions[0].originator_details.account_reference", "23000106E"),
                Map.entry("transactions[0].date_entry_applied", "03/09/2023"),
                Map.entry("transactions[1].amount", "5000"),
                Map.entry("transactions[1].transaction_code", "99"),
                Map.entry("transactions[1].originator_details.account_reference", "23000113P"),
                Map.entry("transactions[1].date_entry_applied", "01/09/2023")
            );
            case "Allpay" -> Map.ofEntries(
                Map.entry("payment_type", "CASH"),
                Map.entry("destination_details.bank_details.sort_code", "010101"),
                Map.entry("destination_details.bank_details.account_number", "12341234"),
                Map.entry("transactions.size()", "2"),
                Map.entry("transactions[0].amount", "1500"),
                Map.entry("transactions[0].transaction_code", "99"),
                Map.entry("transactions[1].amount", "500"),
                Map.entry("transactions[1].transaction_code", "93")
            );
            case "BTEckoh transfer" -> Map.ofEntries(
                Map.entry("payment_type", "CASH"),
                Map.entry("destination_details.bank_details.sort_code", "010102"),
                Map.entry("destination_details.bank_details.account_number", "12341235"),
                Map.entry("transactions.size()", "2"),
                Map.entry("transactions[0].amount", "1500"),
                Map.entry("transactions[0].transaction_code", "99"),
                Map.entry("transactions[1].amount", "500"),
                Map.entry("transactions[1].transaction_code", "93")
            );
            case "DWP" -> Map.ofEntries(
                Map.entry("transactions[0].date_entry_applied", "07/04/2026"),
                Map.entry("transactions[1].date_entry_applied", "06/04/2026"),
                Map.entry("transactions[2].date_entry_applied", "08/04/2026"),
                Map.entry("transactions[3].date_entry_applied", "10/04/2026"),
                Map.entry("transactions[4].date_entry_applied", "01/04/2026")
            );
            case "Barclaycard" -> Map.ofEntries(
                Map.entry("payment_type", "CASH"),
                Map.entry("destination_details.bank_details.sort_code", "560034"),
                Map.entry("destination_details.bank_details.account_number", "27048528"),
                Map.entry("transactions.size()", "2"),
                Map.entry("transactions[0].amount", "1500"),
                Map.entry("transactions[0].transaction_code", "99"),
                Map.entry("transactions[1].amount", "500"),
                Map.entry("transactions[1].transaction_code", "93")
            );
            case "Jacobs" -> Map.ofEntries(
                Map.entry("payment_type", "CASH"),
                Map.entry("destination_details.bank_details.sort_code", "560037"),
                Map.entry("destination_details.bank_details.account_number", "27048531"),
                Map.entry("transactions.size()", "2"),
                Map.entry("transactions[0].amount", "1000"),
                Map.entry("transactions[0].transaction_code", "99"),
                Map.entry("dwp_court_code", "0000031716"),
                Map.entry("transactions[0].originator_details.account_reference", "23000106E"),
                Map.entry("transactions[0].date_entry_applied", "03/09/2023"),
                Map.entry("transactions[1].amount", "5000"),
                Map.entry("transactions[1].transaction_code", "99"),
                Map.entry("transactions[1].originator_details.account_reference", "23000113P"),
                Map.entry("transactions[1].date_entry_applied", "01/09/2023")
            );
            default -> throw new IllegalArgumentException("No payment expectations for " + displayName);
        };
    }
}
