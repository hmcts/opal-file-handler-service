package uk.gov.hmcts.opal.filehandler.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.restassured.path.json.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.apache.commons.codec.digest.DigestUtils;
import uk.gov.hmcts.opal.filehandler.blob.BlobStorageClient;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.db.FinesInterfaceFileTestDatabaseClient;
import uk.gov.hmcts.opal.filehandler.db.FinesInterfaceFileTestDatabaseClient.PreprocessingResult;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient.InterfaceFileRecord;
import uk.gov.hmcts.opal.filehandler.support.BaisFileTestConfig;
import uk.gov.hmcts.opal.filehandler.support.BaisFileTestData;
import uk.gov.hmcts.opal.filehandler.support.FinesPreprocessingFixture;

/** Verifies asynchronous Fines transformation before scenario cleanup removes its input. */
public class FinesPreprocessingStepDef extends BaseStepDef {

    private InterfaceFileRecord sourceJson;
    private PreprocessingResult preprocessingResult;
    private JsonPath transformedJson;

    @Then("Fines preprocessing completes for the stored {string} file")
    public void finesPreprocessingCompletes(String displayName) {
        short expectedBusinessUnit = Short.parseShort(
            TestEnvironment.get("FUNCTIONAL_TEST_FINES_BUSINESS_UNIT_ID").orElse("77"));
        verifyPreprocessing(displayName, expectedBusinessUnit);
    }

    @Then("Fines preprocessing completes for the stored {string} file in business unit {int}")
    public void finesPreprocessingCompletesForBusinessUnit(String displayName, int businessUnitId) {
        assertTrue(businessUnitId > 0 && businessUnitId <= Short.MAX_VALUE, "Invalid Fines business unit ID");
        verifyPreprocessing(displayName, (short) businessUnitId);
    }

    private void verifyPreprocessing(String displayName, short expectedBusinessUnit) {
        BaisFileTestConfig config = config(displayName);
        sourceJson = awaitSourceJson(config);
        preprocessingResult = FinesPreprocessingFixture.awaitResult(sourceJson.id());
        assertEquals(sourceJson.id(), preprocessingResult.sourceJsonId());
        assertEquals(expectedBusinessUnit, preprocessingResult.businessUnitId());

        InterfaceFileRecord transformedFile;
        try (InterfaceFileTestDatabaseClient database = new InterfaceFileTestDatabaseClient()) {
            transformedFile = database.findByFileName(config.fileName()).stream()
                .filter(record -> record.id() == preprocessingResult.transformedJsonId()).findFirst()
                .orElseThrow(() -> new AssertionError("Transformed file " + preprocessingResult.transformedJsonId()
                    + " is missing for source JSON " + sourceJson.id()));
        }
        assertEquals("TRANSFORMED_JSON", transformedFile.type());
        assertEquals(Long.valueOf(sourceJson.id()), transformedFile.relatedInterfaceFileId());
        assertEquals(config.source(), transformedFile.source());
        assertEquals("OPAL", transformedFile.target());
        assertEquals("FINES", transformedFile.domain());
        assertTrue(Set.of("INGESTED", "SUCCESS").contains(transformedFile.status()),
            "Transformed upload is not available: " + transformedFile);
        transformedJson = downloadJson(config, transformedFile);
        JsonPath sourceContent = downloadJson(config, sourceJson);
        assertEquals(config.fileName(), transformedJson.getString("file_name"));
        assertEquals(sourceContent.getString("payment_type"), transformedJson.getString("payment_type"));

        List<Long> amounts = transformedJson.getList("transactions.amount", Long.class);
        assertNotNull(amounts, "Transformed JSON has no transaction list");
        assertNotNull(preprocessingResult.recordCount());
        assertEquals(amounts.size(), preprocessingResult.recordCount().intValue());
        long total = amounts.stream().mapToLong(Long::longValue).sum();
        assertEquals(total, transformedJson.getLong("total_amount"));
        assertNotNull(preprocessingResult.totalAmount());
        assertEquals(0, BigDecimal.valueOf(total).compareTo(preprocessingResult.totalAmount()),
            "Fines stored total differs from transformed transaction amounts");
    }

    @Then("the transformed {string} file contains the account references in order")
    public void transformedFileContainsReferences(String displayName, DataTable expectedReferences) {
        assertTransformationAvailable(displayName);
        assertEquals(expectedReferences.asList(String.class),
            transformedJson.getList("transactions.originator_details.account_reference"));
    }

    @Then("the transformed {string} file has payment type {string} and transaction codes in order")
    public void transformedFileContainsTransactionCodes(
        String displayName, String paymentType, DataTable expectedCodes
    ) {
        assertTransformationAvailable(displayName);
        assertEquals(paymentType, transformedJson.getString("payment_type"));
        assertEquals(expectedCodes.asList(String.class), transformedJson.getList("transactions.transaction_code"));
    }

    @Then("Fines preprocessing reuses the original result for the stored {string} file")
    public void finesPreprocessingReusesOriginalResult(String displayName) {
        assertTransformationAvailable(displayName);
        BaisFileTestConfig config = config(displayName);
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        try (FinesInterfaceFileTestDatabaseClient fines = new FinesInterfaceFileTestDatabaseClient();
             InterfaceFileTestDatabaseClient fileHandler = new InterfaceFileTestDatabaseClient()) {
            do {
                List<InterfaceFileRecord> jsonFiles = fileHandler.findByFileName(config.fileName()).stream()
                    .filter(record -> "SOURCE_JSON".equals(record.type())).toList();
                assertEquals(List.of(sourceJson.id()), jsonFiles.stream().map(InterfaceFileRecord::id).toList(),
                    "Duplicate ingestion created another source JSON");
                List<PreprocessingResult> results = fines.findBySourceJsonId(sourceJson.id());
                assertEquals(1, results.size(), "Expected the original Fines preprocessing result only");
                assertEquals(preprocessingResult.jobId(), results.getFirst().jobId());
                assertEquals(preprocessingResult.transformedJsonId(), results.getFirst().transformedJsonId());
                pause();
            } while (System.nanoTime() < deadline);
        }
    }

    private void assertTransformationAvailable(String displayName) {
        assertNotNull(transformedJson, "Wait for Fines preprocessing before checking its transformed output");
        assertEquals(config(displayName).fileName(), transformedJson.getString("file_name"));
    }

    private InterfaceFileRecord awaitSourceJson(BaisFileTestConfig config) {
        String sourceId = scenarioContext().recall(BaisFileTestData.SOURCE_INTERFACE_FILE_ID);
        assertNotNull(sourceId, "Successful source metadata must be verified before Fines preprocessing");
        long deadline = System.nanoTime()
            + Duration.ofSeconds(TestEnvironment.getFinesPreprocessingTimeoutSeconds()).toNanos();
        List<InterfaceFileRecord> records = List.of();
        try (InterfaceFileTestDatabaseClient database = new InterfaceFileTestDatabaseClient()) {
            do {
                records = database.findByFileName(config.fileName()).stream()
                    .filter(record -> "SOURCE_JSON".equals(record.type()))
                    .filter(record -> Long.valueOf(sourceId).equals(record.relatedInterfaceFileId())).toList();
                assertTrue(records.size() <= 1, "Multiple source JSON files for source " + sourceId + ": " + records);
                if (!records.isEmpty()) {
                    InterfaceFileRecord record = records.getFirst();
                    assertEquals("SUCCESS", record.status(), "Source JSON cannot reach Fines: " + record);
                    return record;
                }
                pause();
            } while (System.nanoTime() < deadline);
        }
        throw new AssertionError("No SOURCE_JSON linked to source " + sourceId + " within "
            + TestEnvironment.getFinesPreprocessingTimeoutSeconds() + " seconds; records: " + records);
    }

    private BaisFileTestConfig config(String displayName) {
        assertEquals("Allpay", displayName, "Fines fixture assertions currently cover Allpay");
        String selectedFile = scenarioContext().recall(BaisFileTestData.SELECTED_ALLPAY_FILE);
        return selectedFile == null ? BaisFileTestData.ALLPAY : BaisFileTestData.allpayFile(selectedFile);
    }

    private static JsonPath downloadJson(BaisFileTestConfig config, InterfaceFileRecord record) {
        assertNotNull(record.filestoreUuid(), "JSON file has no stored blob: " + record);
        byte[] content = new BlobStorageClient(config.blobContainerName()).download(record.filestoreUuid().toString());
        assertEquals(record.checksum(), DigestUtils.md5Hex(content), "Stored JSON checksum differs from metadata");
        return new JsonPath(new String(content, StandardCharsets.UTF_8));
    }

    private static void pause() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for Fines preprocessing", exception);
        }
    }
}
