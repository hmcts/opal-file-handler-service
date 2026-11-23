package uk.gov.hmcts.opal.filehandler.steps;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import org.assertj.core.api.SoftAssertions;
import uk.gov.hmcts.opal.filehandler.db.FinesInterfaceFileTestDatabaseClient.PreprocessingResult;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient;
import uk.gov.hmcts.opal.filehandler.support.BacsStandard18TestData;
import uk.gov.hmcts.opal.filehandler.support.BacsStandard18TestData.PaymentInput;
import uk.gov.hmcts.opal.filehandler.support.BaisInterfaceFileClient;
import uk.gov.hmcts.opal.filehandler.support.BankingJsonAssertions;
import uk.gov.hmcts.opal.filehandler.support.FinesPreprocessingFixture;
import uk.gov.hmcts.opal.filehandler.support.InterfaceFileUploadClient;
import uk.gov.hmcts.opal.filehandler.support.VariantBankingFixture;

/** Direct upload journey; never uses BAIS, SFTP or an ingestion job. */
public class VariantBankingStepDef extends BaseStepDef {
    private VariantBankingFixture fixture;
    private String fileName;
    private short businessUnit;
    private byte[] content;
    private List<PaymentInput> payments;
    private Response upload;
    private Map<String, Object> sourceJson;
    private JsonPath transformed;
    private PreprocessingResult result;
    private List<Map<String, Object>> expectedTransactions;
    private String rejectionCondition;

    @Given("the following cash payments are written to a unique Variant Banking file for business unit {string}:")
    public void prepare(String unit, DataTable table) {
        fixture = new VariantBankingFixture();
        fileName = fixture.reserve(unit);
        businessUnit = Short.parseShort(unit);
        if (businessUnit == VariantBankingFixture.BUSINESS_UNIT_ID) {
            fixture.configureFinesBankMapping(businessUnit);
        }
        payments = table.asMaps().stream().map(PaymentInput::from).toList();
        content = BacsStandard18TestData.buildFile(payments, "FPCAS", VariantBankingFixture.SORT_CODE,
            VariantBankingFixture.ACCOUNT_NUMBER, VariantBankingFixture.BANK_NAME, "0");
    }

    @Given("the Variant Banking input has {string}")
    public void corruptInput(String condition) {
        rejectionCondition = condition;
        switch (condition) {
            case "an invalid filename" -> fileName += ".txt";
            case "missing headers" -> content = "not a BACS file".getBytes(StandardCharsets.UTF_8);
            case "a blank payment reference" -> {
                String[] lines = new String(content, StandardCharsets.UTF_8).split("\r\n");
                lines[3] = lines[3].substring(0, 64) + " ".repeat(18) + lines[3].substring(82);
                content = (String.join("\r\n", lines) + "\r\n").getBytes(StandardCharsets.UTF_8);
            }
            case "an unmapped business unit" -> fixture.requireMissingUnit(businessUnit);
            default -> throw new IllegalArgumentException("Unknown Variant input condition " + condition);
        }
    }

    @When("the Variant Banking file is uploaded through the Add Interface Files API")
    public void upload() {
        String metadata = """
            {"source":"VARIANT_BANKING","target":"OPAL","type":"SOURCE","domain":"FINES",
             "file_name":"%s","payment_type":"CASH","should_pre_process_file":true}
            """.formatted(fileName);
        // Omit business_unit_id so the filename is the only possible source of assignment.
        fixture.useFileName(fileName);
        upload = InterfaceFileUploadClient.upload(authorisedJsonRequest(), getTestUrl(), fileName, content, metadata);
    }

    @Then("the stored Variant Banking SOURCE and SOURCE_JSON match the payments and filename business unit")
    public void verifyExtraction() {
        fixture.requireBusinessUnit65(businessUnit);
        assertEquals(201, upload.statusCode(), upload.asString());
        assertEquals("SUCCESS", upload.jsonPath().getString("status"));
        Map<String, Object> source = files().awaitInterfaceFile(
            "VARIANT_BANKING", fileName, "Variant Banking", "SOURCE", "SUCCESS");
        assertEquals(upload.jsonPath().getLong("interface_file_id"), id(source));
        assertMetadata(source);
        assertArrayEquals(content, files().content(source).asByteArray());
        assertEquals(DigestUtils.md5Hex(content), source.get("checksum"));
        sourceJson = files().awaitInterfaceFile(
            "VARIANT_BANKING", fileName, "Variant Banking", "SOURCE_JSON", "SUCCESS");
        assertMetadata(sourceJson);
        assertRelated(sourceJson, id(source));
        JsonPath extracted = json(sourceJson, false);
        assertEquals(fileName, extracted.getString("file_name"));
        assertEquals("CASH", extracted.getString("payment_type"));
        assertEquals(VariantBankingFixture.SORT_CODE,
            extracted.getString("destination_details.bank_details.sort_code"));
        assertEquals(VariantBankingFixture.ACCOUNT_NUMBER,
            extracted.getString("destination_details.bank_details.account_number"));
        assertEquals(payments.stream().map(PaymentInput::transactionCode).toList(),
            extracted.getList("transactions.transaction_code"));
        assertEquals(payments.stream().map(PaymentInput::amountInPence).toList(),
            extracted.getList("transactions.amount", Long.class));
        assertEquals(payments.stream().map(PaymentInput::accountReference).toList(),
            extracted.getList("transactions.originator_details.account_reference"));
        assertEquals(payments.stream().map(PaymentInput::dateEntryApplied).toList(),
            extracted.getList("transactions.date_entry_applied"));
    }

    @Then("Fines preprocessing creates a TRANSFORMED_JSON for the Variant Banking file")
    public void awaitTransformation() {
        assertNotNull(sourceJson, "Verify extraction before waiting for Fines");
        result = FinesPreprocessingFixture.awaitResult(id(sourceJson));
        // BU 65 is tested with a test-owned Fines mapping before the upload.
        assertEquals(businessUnit, result.businessUnitId(), "Fines must preserve the filename's business unit ID");
        Map<String, Object> metadata = files().records("VARIANT_BANKING", fileName).stream()
            .filter(row -> id(row) == result.transformedJsonId()).findFirst().orElseThrow();
        assertMetadata(metadata);
        assertEquals("TRANSFORMED_JSON", metadata.get("type"));
        assertTrue(List.of("INGESTED", "SUCCESS").contains(metadata.get("status")));
        assertRelated(metadata, id(sourceJson));
        transformed = json(metadata, true);
    }

    @Then("the expected transformed Variant Banking payments are:")
    public void expectedPayments(DataTable table) {
        expectedTransactions = new ArrayList<>();
        List<Map<String, String>> rows = table.asMaps();
        List<PaymentInput> retained = payments.stream().filter(PaymentInput::expectedInTransformedJson).toList();
        assertEquals(retained.size(), rows.size());
        for (int index = 0; index < rows.size(); index++) {
            PaymentInput input = retained.get(index);
            Map<String, String> row = rows.get(index);
            expectedTransactions.add(BankingJsonAssertions.expectedTransaction(input, row));
        }
    }

    @Then("the complete transformed Variant Banking JSON has payment type {string}, record count {int} "
        + "and total amount {long}")
    public void verifyTransformation(String paymentType, int count, long total) throws JsonProcessingException {
        assertNotNull(transformed, "Wait for Fines preprocessing before checking transformed output");
        assertNotNull(expectedTransactions, "Declare expected payments first");
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("destination_details", Map.of("bank_details", Map.of("sort_code", VariantBankingFixture.SORT_CODE,
            "account_number", VariantBankingFixture.ACCOUNT_NUMBER, "name", VariantBankingFixture.BANK_NAME,
            "type", "0")));
        expected.put("file_name", fileName);
        expected.put("payment_type", paymentType);
        expected.put("transactions", expectedTransactions);
        expected.put("total_amount", total);
        SoftAssertions assertions = new SoftAssertions();
        BankingJsonAssertions.assertCompleteJsonEquals(assertions, expected, transformed.getMap(""));
        assertions.assertThat(transformed.getList("transactions")).hasSize(count);
        assertions.assertThat(result.recordCount().intValue()).isEqualTo(count);
        assertions.assertThat(result.totalAmount().longValueExact()).isEqualTo(total);
        assertions.assertAll();
    }

    @Then("the Variant Banking upload is rejected without persisted interface files")
    public void rejected() {
        // These common-lib mappings were verified against the running API, including rollback.
        int expectedStatus = switch (rejectionCondition) {
            case "an invalid filename", "an unmapped business unit" -> 404;
            case "missing headers", "a blank payment reference" -> 400;
            default -> throw new IllegalStateException("Set an input rejection condition before upload");
        };
        assertEquals(expectedStatus, upload.statusCode(), upload.asString());
        assertEquals(expectedStatus, upload.jsonPath().getInt("status"));
        String problem = expectedStatus == 404 ? "entity-not-found" : "illegal-argument";
        assertEquals("https://hmcts.gov.uk/problems/" + problem, upload.jsonPath().getString("type"));
        if ("an invalid filename".equals(rejectionCondition)) {
            assertEquals("Business unit bank account code cannot be found for file_name '" + fileName + "'",
                upload.jsonPath().getString("reason"));
        } else if ("an unmapped business unit".equals(rejectionCondition)) {
            assertEquals("Business unit bank account with business unit id '" + businessUnit
                + "' could not be located for file_name '" + fileName + "'", upload.jsonPath().getString("reason"));
        }
        try (InterfaceFileTestDatabaseClient database = new InterfaceFileTestDatabaseClient()) {
            assertTrue(database.findByFileName(fileName).isEmpty(), "Rejected upload must roll back all records");
        }
    }

    @After("@VariantBanking")
    public void cleanup() {
        if (fixture != null) {
            fixture.close();
        }
    }

    private void assertRelated(Map<String, Object> metadata, long parentId) {
        // The list API omits related_interface_file_id; verify the persisted relationship instead.
        try (InterfaceFileTestDatabaseClient database = new InterfaceFileTestDatabaseClient()) {
            var stored = database.findByFileName(fileName).stream()
                .filter(row -> row.id() == id(metadata)).findFirst().orElseThrow();
            assertEquals(Long.valueOf(parentId), stored.relatedInterfaceFileId());
        }
    }

    private void assertMetadata(Map<String, Object> metadata) {
        assertEquals(fileName, metadata.get("file_name"));
        assertEquals("VARIANT_BANKING", metadata.get("source"));
        assertEquals("OPAL", metadata.get("target"));
        assertEquals("FINES", metadata.get("domain"));
        assertEquals(List.of((int) businessUnit), metadata.get("business_unit_ids"));
    }

    private JsonPath json(Map<String, Object> metadata, boolean fromBlob) {
        assertNotNull(metadata.get("filestore_uuid"));
        byte[] bytes = fromBlob ? VariantBankingFixture.blobs().download(metadata.get("filestore_uuid").toString())
            : files().content(metadata).asByteArray();
        assertEquals(metadata.get("checksum"), DigestUtils.md5Hex(bytes));
        return JsonPath.from(new String(bytes, StandardCharsets.UTF_8));
    }

    private BaisInterfaceFileClient files() {
        return new BaisInterfaceFileClient(this::authorisedJsonRequest, getTestUrl());
    }

    private static long id(Map<String, Object> metadata) {
        return ((Number) metadata.get("interface_file_id")).longValue();
    }
}
