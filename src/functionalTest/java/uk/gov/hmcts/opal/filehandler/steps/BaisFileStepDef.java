package uk.gov.hmcts.opal.filehandler.steps;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.opal.filehandler.support.SftpRetry.withRetry;

import com.google.common.io.Resources;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import uk.gov.hmcts.opal.filehandler.sftp.SftpClient;
import uk.gov.hmcts.opal.filehandler.support.BaisFileFixture;
import uk.gov.hmcts.opal.filehandler.support.BaisFileTestConfig;
import uk.gov.hmcts.opal.filehandler.support.BaisFileTestData;
import uk.gov.hmcts.opal.filehandler.support.BaisInterfaceFileClient;
import uk.gov.hmcts.opal.filehandler.support.BaisPaymentAssertions;
import uk.gov.hmcts.opal.filehandler.support.TestHttpClient.TestHttpResponse;
import uk.gov.hmcts.opal.filehandler.testsupport.TestSupportApiClient;

/**
 * Defines the shared end-to-end journey for BAIS file ingestion.
 */
public class BaisFileStepDef extends BaseStepDef {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);
    private static final Duration IGNORED_FILE_CHECK_DURATION = Duration.ofSeconds(5);
    private static final String INVALID_CONTENT_RESOURCE = "test-data/bais/invalid-content.txt";

    private final TestSupportApiClient testSupportApiClient = new TestSupportApiClient();
    private TestHttpResponse taskResponse;
    private Map<String, Object> successfulInterfaceFile;
    private Map<String, Object> duplicateInterfaceFile;
    private BaisFileTestConfig selectedConfig;

    @Given("the configured {string} file {string} is available on bais")
    public void configuredNamedFileIsAvailable(String displayName, String fileName) {
        assertEquals("Allpay", displayName, "Named file fixtures are supported for Allpay");
        selectedConfig = BaisFileTestData.allpayFile(fileName);
        placeSelectedFile(displayName);
    }

    @Given("the configured {string} file {string} with transaction codes in order is available on bais")
    public void configuredFileWithTransactionCodesIsAvailable(
        String displayName, String fileName, DataTable expectedCodes
    ) throws IOException {
        assertEquals("Allpay", displayName, "Named file fixtures are supported for Allpay");
        selectedConfig = BaisFileTestData.allpayFile(fileName);
        List<String> transactionRows = readTransactionRows(selectedConfig);
        assertEquals(expectedCodes.asList(String.class),
            transactionRows.stream().map(line -> line.substring(15, 17)).toList(),
            "Transaction codes in " + fileName + " differ from the scenario");
        assertEquals(1L, transactionRows.stream().map(line -> line.substring(0, 14)).distinct().count(),
            "Destination bank details must match so the scenario isolates mixed transaction codes");
        placeSelectedFile(displayName);
    }

    private static List<String> readTransactionRows(BaisFileTestConfig config) throws IOException {
        List<String> rows = Resources.readLines(
            Resources.getResource(config.resourcePath()), StandardCharsets.UTF_8
        ).stream()
            .filter(line -> line.length() == 100 || line.length() == 106)
            .toList();
        assertFalse(rows.isEmpty(), "Expected transaction rows in " + config.fileName());
        return rows;
    }

    private void placeSelectedFile(String displayName) {
        scenarioContext().remember(BaisFileTestData.SELECTED_ALLPAY_FILE, selectedConfig.fileName());
        new BaisFileFixture(BaisFileTestData.ALLPAY).remove();
        new BaisFileFixture(selectedConfig).setUp();
        configuredFileIsAvailable(displayName);
    }

    private BaisFileTestConfig forDisplayName(String displayName) {
        return selectedConfig != null && selectedConfig.displayName().equals(displayName)
            ? selectedConfig : BaisFileTestData.forDisplayName(displayName);
    }

    private BaisFileTestConfig forSource(String source) {
        return selectedConfig != null && selectedConfig.source().equals(source)
            ? selectedConfig : BaisFileTestData.forSource(source);
    }

    @Given("the configured {string} report/file is available on bais")
    public void configuredFileIsAvailable(String displayName) {
        BaisFileTestConfig config = forDisplayName(displayName);
        assertSftpFilePresence(config, config.fileName(), true);
    }

    @Given("the configured {string} report/file has an unsupported file on bais")
    public void configuredFileHasUnsupportedFileOnBais(String displayName) {
        BaisFileTestConfig config = forDisplayName(displayName);
        withRetry(
            "place unsupported " + config.displayName() + " file for user " + config.sftpUsername(),
            () -> {
                try (SftpClient sftpClient = new SftpClient(config.sftpUsername())) {
                    sftpClient.deleteIfExists(config.fileName());
                    sftpClient.uploadResource(config.resourcePath(), config.unsupportedFileName());
                }
            }
        );
    }

    @Given("the configured {string} report/file has invalid content on bais")
    public void configuredFileHasInvalidContentOnBais(String displayName) {
        BaisFileTestConfig config = forDisplayName(displayName);
        withRetry(
            "place invalid " + config.displayName() + " file content for user " + config.sftpUsername(),
            () -> {
                try (SftpClient sftpClient = new SftpClient(config.sftpUsername())) {
                    sftpClient.deleteIfExists(config.fileName());
                    sftpClient.uploadResource(INVALID_CONTENT_RESOURCE, config.fileName());
                }
            }
        );
    }

    @Given("the configured {string} report/file has already been processed")
    public void configuredFileHasAlreadyBeenProcessed(String displayName) {
        // Initialise the default test user so this composite step can be used independently.
        BearerTokenStepDef.getToken();
        configuredFileIsAvailable(displayName);
        fileIngestionJobIsRequested(displayName);
        testingSupportRequestIsAccepted();
        successfulInterfaceFileIsStored(forDisplayName(displayName).source());
        configuredFileNoLongerExists(displayName);
    }

    @When("the {string} report/file ingestion job is requested through testing support")
    public void fileIngestionJobIsRequested(String displayName) {
        BaisFileTestConfig config = forDisplayName(displayName);
        taskResponse = testSupportApiClient.post("/automated-jobs/" + config.automatedTaskName());
    }

    @Then("the testing-support request is accepted")
    public void testingSupportRequestIsAccepted() {
        assertNotNull(taskResponse, "The testing-support endpoint was not called");
        assertEquals(
            202,
            taskResponse.statusCode(),
            "The testing-support endpoint did not accept the job. Response body: " + taskResponse.body()
        );
    }

    @Then("a successful {string} interface file is stored")
    public void successfulInterfaceFileIsStored(String source) {
        BaisFileTestConfig config = forSource(source);
        successfulInterfaceFile = awaitSuccessfulInterfaceFile(config, "SOURCE");

        assertEquals(config.source(), successfulInterfaceFile.get("source"));
        assertEquals("OPAL", successfulInterfaceFile.get("target"));
        assertEquals("SOURCE", successfulInterfaceFile.get("type"));
        assertEquals(config.expectedDomain(), successfulInterfaceFile.get("domain"));
        assertEquals(config.fileName(), successfulInterfaceFile.get("file_name"));
        assertEquals(config.checksum(), successfulInterfaceFile.get("checksum"));
        assertNotNull(successfulInterfaceFile.get("filestore_uuid"));
        scenarioContext().remember(BaisFileTestData.SOURCE_INTERFACE_FILE_ID,
            successfulInterfaceFile.get("interface_file_id").toString());
    }

    @Then("a failed {string} interface file is recorded with its source blob")
    public void failedInterfaceFileIsRecordedWithSourceBlob(String source) {
        // EI2 BAIS processors validate after the source blob is uploaded, unlike the EI1 report
        // processors. Keep this assertion visible for the separate decision on failed-blob cleanup.
        BaisFileTestConfig config = forSource(source);
        Map<String, Object> failedInterfaceFile = awaitInterfaceFile(config, "SOURCE", "FAILED");

        assertEquals(config.source(), failedInterfaceFile.get("source"));
        assertEquals(config.fileName(), failedInterfaceFile.get("file_name"));
        assertNotNull(failedInterfaceFile.get("filestore_uuid"));
        assertNotNull(failedInterfaceFile.get("errors"));
    }

    @Then("a failed {string} interface file is recorded without a source blob")
    public void failedInterfaceFileIsRecordedWithoutSourceBlob(String source) {
        BaisFileTestConfig config = forSource(source);
        Map<String, Object> failedInterfaceFile = awaitInterfaceFile(config, "SOURCE", "FAILED");

        assertEquals(config.source(), failedInterfaceFile.get("source"));
        assertEquals(config.fileName(), failedInterfaceFile.get("file_name"));
        assertNull(failedInterfaceFile.get("filestore_uuid"));
        assertNotNull(failedInterfaceFile.get("errors"));
    }

    @Then("the {string} file failure reports a nonnumeric transaction code")
    public void fileFailureReportsNonnumericTransactionCode(String displayName) {
        Map<String, Object> failedInterfaceFile = awaitInterfaceFile(forDisplayName(displayName), "SOURCE", "FAILED");
        assertNotNull(failedInterfaceFile.get("errors"));
        String errors = failedInterfaceFile.get("errors").toString();
        assertTrue(errors.contains("BACS Standard 18 transaction code at index 15 to 17 must be numeric"),
            "Expected a nonnumeric transaction-code validation error, but got: " + errors);
    }

    @Then("the {string} file failure reports a missing originator account reference")
    public void fileFailureReportsMissingOriginatorAccountReference(String displayName) {
        Map<String, Object> failedInterfaceFile = awaitInterfaceFile(forDisplayName(displayName), "SOURCE", "FAILED");
        assertNotNull(failedInterfaceFile.get("errors"));
        String errors = failedInterfaceFile.get("errors").toString();
        assertTrue(errors.contains("BACS Standard 18 originator account reference is required at index 64 to 82"),
            "Expected a missing originator account reference validation error, but got: " + errors);
    }

    @Then("the {string} file failure reports no destination bank mapping for sort code {string} "
        + "and account number {string}")
    public void fileFailureReportsMissingDestinationBankMapping(
        String displayName, String sortCode, String accountNumber
    ) {
        BaisFileTestConfig config = forDisplayName(displayName);
        Map<String, Object> failedInterfaceFile = awaitInterfaceFile(config, "SOURCE", "FAILED");
        assertNotNull(failedInterfaceFile.get("errors"));
        String errors = failedInterfaceFile.get("errors").toString();
        String expectedError = "Business unit bank account with sort code '%s' and account number '%s' "
            + "could not be located for file_name '%s'";
        assertTrue(errors.contains(expectedError.formatted(sortCode, accountNumber, config.fileName())),
            "Expected a missing destination bank mapping error, but got: " + errors);
    }

    @Then("the stored {string} JSON file has payment type {string} and transaction codes in order")
    public void storedJsonFileHasPaymentTypeAndTransactionCodes(
        String displayName, String paymentType, DataTable expectedCodes
    ) {
        BaisFileTestConfig config = forDisplayName(displayName);
        Map<String, Object> jsonFile = awaitSuccessfulInterfaceFile(config, "SOURCE_JSON");
        assertEquals("OPAL", jsonFile.get("target"));
        assertEquals(config.expectedDomain(), jsonFile.get("domain"));
        assertNotNull(jsonFile.get("filestore_uuid"));

        Response response = authorisedJsonRequest().accept("application/octet-stream").when()
            .get(getTestUrl() + "/interface-files/" + jsonFile.get("interface_file_id") + "/content");
        assertEquals(200, response.statusCode());
        assertEquals(jsonFile.get("checksum"), DigestUtils.md5Hex(response.asByteArray()));
        assertEquals(config.fileName(), response.jsonPath().getString("file_name"));
        assertEquals(paymentType, response.jsonPath().getString("payment_type"));
        assertEquals(expectedCodes.asList(String.class),
            response.jsonPath().getList("transactions.transaction_code"));
    }

    @Then("no {string} interface file is stored for the unsupported report/file")
    public void noInterfaceFileIsStoredForUnsupportedFile(String source) {
        BaisFileTestConfig config = forSource(source);
        long deadline = System.nanoTime() + IGNORED_FILE_CHECK_DURATION.toNanos();
        do {
            Response response = authorisedJsonRequest()
                .queryParam("source", config.source())
                .when()
                .get(getTestUrl() + "/interface-files");
            assertEquals(200, response.statusCode(), "Interface-file metadata could not be retrieved");

            List<Map<String, Object>> matchingRecords = response.jsonPath()
                .<Map<String, Object>>getList("interface_files")
                .stream()
                .filter(record -> config.unsupportedFileName().equals(record.get("file_name")))
                .toList();
            assertTrue(
                matchingRecords.isEmpty(),
                "No interface-file record should be created for unsupported file "
                    + config.unsupportedFileName() + ": " + matchingRecords
            );
            pauseBeforeRetry();
        } while (System.nanoTime() < deadline);
    }

    @Then("the stored {string} report/file can be retrieved by interface file ID")
    public void storedFileCanBeRetrievedByInterfaceFileId(String displayName) {
        assertNotNull(successfulInterfaceFile, "Successful interface-file metadata was not retrieved");
        long interfaceFileId = ((Number) successfulInterfaceFile.get("interface_file_id")).longValue();

        Response response = authorisedJsonRequest()
            .when()
            .get(getTestUrl() + "/interface-files/" + interfaceFileId);

        assertEquals(200, response.statusCode(), "Interface-file details could not be retrieved by ID");

        Map<String, Object> responseBody = response.jsonPath().getMap("$");
        assertEquals(interfaceFileId, ((Number) responseBody.get("interface_file_id")).longValue());
        assertEquals(forDisplayName(displayName).source(), responseBody.get("source"));
        assertEquals("OPAL", responseBody.get("target"));
        assertEquals("SOURCE", responseBody.get("type"));
        assertEquals(forDisplayName(displayName).expectedDomain(), responseBody.get("domain"));
        assertEquals(forDisplayName(displayName).fileName(), responseBody.get("file_name"));
        assertEquals(forDisplayName(displayName).checksum(), responseBody.get("checksum"));
        assertEquals(successfulInterfaceFile.get("filestore_uuid"), responseBody.get("filestore_uuid"));
        assertEquals("SUCCESS", responseBody.get("status"));
    }

    @Then("the stored {string} report/file content matches the bais {string}")
    public void storedFileContentMatches(String displayName, String fileDescription) throws IOException {
        BaisFileTestConfig config = forDisplayName(displayName);
        assertNotNull(successfulInterfaceFile, "Successful interface-file metadata was not retrieved");
        long interfaceFileId = ((Number) successfulInterfaceFile.get("interface_file_id")).longValue();

        Response response = authorisedJsonRequest()
            .accept("application/octet-stream")
            .when()
            .get(getTestUrl() + "/interface-files/" + interfaceFileId + "/content");

        URL expectedResource = Resources.getResource(config.resourcePath());
        assertEquals(
            200,
            response.statusCode(),
            "Stored " + config.displayName() + " file content could not be retrieved. "
                + "Response body: " + response.asString()
        );
        assertArrayEquals(
            Resources.toByteArray(expectedResource),
            response.getBody().asByteArray(),
            "Stored " + config.displayName() + " file content differs from the SFTP " + fileDescription
        );
    }

    @Then("the {string} file is processed successfully")
    public void fileIsProcessedSuccessfully(String displayName) throws IOException {
        testingSupportRequestIsAccepted();
        successfulInterfaceFileIsStored(forDisplayName(displayName).source());
        storedFileCanBeRetrievedByInterfaceFileId(displayName);
        storedFileContentMatches(displayName, "file");
        configuredFileNoLongerExists(displayName);
    }

    @Then("the extracted {string} payments are ready for the external interface")
    public void extractedPaymentsAreReady(String displayName) {
        new BaisPaymentAssertions(interfaceFiles()).assertPaymentsReady(forDisplayName(displayName));
    }

    @Then("the stored {string} JSON file contains the account references in order")
    public void storedJsonFileContainsAccountReferences(String displayName, DataTable expectedReferences) {
        BaisFileTestConfig config = forDisplayName(displayName);
        Map<String, Object> jsonFile = awaitSuccessfulInterfaceFile(config, "SOURCE_JSON");
        assertEquals(config.expectedDomain(), jsonFile.get("domain"));
        assertNotNull(jsonFile.get("filestore_uuid"));

        Response response = authorisedJsonRequest().accept("application/octet-stream").when()
            .get(getTestUrl() + "/interface-files/" + jsonFile.get("interface_file_id") + "/content");
        assertEquals(200, response.statusCode());
        assertEquals(jsonFile.get("checksum"), DigestUtils.md5Hex(response.asByteArray()));
        assertEquals(config.fileName(), response.jsonPath().getString("file_name"));
        assertEquals(expectedReferences.asList(String.class),
            response.jsonPath().getList("transactions.originator_details.account_reference"));
    }

    @Then("the DWP JSON file contains the extracted payments and bank details")
    public void dwpJsonContainsExtractedPayments() {
        assertNotNull(successfulInterfaceFile, "Source metadata was not retrieved");
        Map<String, Object> jsonFile = awaitSuccessfulInterfaceFile(forSource("DWP"), "SOURCE_JSON");
        assertEquals("OPAL", jsonFile.get("target"));
        assertEquals("MAINTENANCE", jsonFile.get("domain"));
        assertNotNull(jsonFile.get("filestore_uuid"));
        Response response = authorisedJsonRequest().accept("application/octet-stream").when()
            .get(getTestUrl() + "/interface-files/" + jsonFile.get("interface_file_id") + "/content");
        assertEquals(200, response.statusCode());
        assertEquals(jsonFile.get("checksum"), DigestUtils.md5Hex(response.asByteArray()));
        assertEquals(forSource("DWP").fileName(), response.jsonPath().getString("file_name"));
        assertEquals("DWP1234567", response.jsonPath().getString("dwp_court_code"));
        assertEquals("CASH", response.jsonPath().getString("payment_type"));
        assertEquals("010101", response.jsonPath().getString("destination_details.bank_details.sort_code"));
        assertEquals("12341234", response.jsonPath().getString("destination_details.bank_details.account_number"));
        assertEquals(List.of(2125, 2125, 2125, 2125, 2001),
            response.jsonPath().getList("transactions.amount"));
        assertEquals(List.of("99", "99", "99", "99", "99"),
            response.jsonPath().getList("transactions.transaction_code"));
    }

    @Then("the configured {string} report/file no longer exists on bais")
    public void configuredFileNoLongerExists(String displayName) {
        BaisFileTestConfig config = forDisplayName(displayName);
        assertSftpFilePresence(config, config.fileName(), false);
    }

    @Then("the unsupported {string} report/file remains on bais")
    public void unsupportedFileRemainsOnBais(String displayName) {
        BaisFileTestConfig config = forDisplayName(displayName);
        assertSftpFilePresence(config, config.unsupportedFileName(), true);
    }

    @Then("the invalid {string} report/file remains on bais")
    public void invalidFileRemainsOnBais(String displayName) {
        BaisFileTestConfig config = forDisplayName(displayName);
        assertSftpFilePresence(config, config.fileName(), true);
    }

    @When("the configured {string} report/file is placed on bais")
    public void configuredFileIsPlacedOnBais(String displayName) {
        BaisFileTestConfig config = forDisplayName(displayName);
        withRetry(
            "upload " + config.displayName() + " file for user " + config.sftpUsername(),
            () -> {
                try (SftpClient sftpClient = new SftpClient(config.sftpUsername())) {
                    sftpClient.uploadResource(config.resourcePath(), config.fileName());
                }
            }
        );
    }

    @Then("a duplicate {string} interface file is recorded")
    public void duplicateInterfaceFileIsRecorded(String source) {
        BaisFileTestConfig config = forSource(source);
        duplicateInterfaceFile = awaitInterfaceFile(config, "SOURCE", "DUPLICATE");
        assertEquals(config.source(), duplicateInterfaceFile.get("source"));
        assertEquals(config.fileName(), duplicateInterfaceFile.get("file_name"));
        assertEquals(config.checksum(), duplicateInterfaceFile.get("checksum"));
    }

    @Then("the duplicate {string} interface file reuses the original blob")
    public void duplicateInterfaceFileReusesOriginalBlob(String source) {
        assertNotNull(successfulInterfaceFile, "Successful interface-file metadata was not retrieved");
        assertNotNull(duplicateInterfaceFile, "Duplicate interface-file metadata was not retrieved");
        assertEquals(
            successfulInterfaceFile.get("filestore_uuid"),
            duplicateInterfaceFile.get("filestore_uuid"),
            "The duplicate should reuse the original blob rather than upload another blob"
        );
    }

    private Map<String, Object> awaitSuccessfulInterfaceFile(BaisFileTestConfig config, String type) {
        return awaitInterfaceFile(config, type, "SUCCESS");
    }

    private Map<String, Object> awaitInterfaceFile(BaisFileTestConfig config, String type, String status) {
        return interfaceFiles().awaitInterfaceFile(config, type, status);
    }

    private BaisInterfaceFileClient interfaceFiles() {
        return new BaisInterfaceFileClient(this::authorisedJsonRequest, getTestUrl());
    }

    private static void pauseBeforeRetry() {
        try {
            Thread.sleep(POLL_INTERVAL.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for interface-file metadata", exception);
        }
    }

    private static void assertSftpFilePresence(
        BaisFileTestConfig config,
        String fileName,
        boolean expected
    ) {
        withRetry(
            "check " + config.displayName() + " file presence for user " + config.sftpUsername(),
            () -> {
                try (SftpClient sftpClient = new SftpClient(config.sftpUsername())) {
                    if (expected) {
                        assertTrue(sftpClient.exists(fileName), "Expected SFTP file to exist: " + fileName);
                    } else {
                        assertFalse(sftpClient.exists(fileName), "Expected SFTP file to be removed: " + fileName);
                    }
                }
            }
        );
    }
}
