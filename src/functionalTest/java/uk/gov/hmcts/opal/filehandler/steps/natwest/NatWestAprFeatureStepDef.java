package uk.gov.hmcts.opal.filehandler.steps.natwest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.serenitybdd.core.Serenity;
import org.apache.commons.codec.digest.DigestUtils;
import org.assertj.core.api.SoftAssertions;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.opal.filehandler.blob.BlobStorageClient;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.db.DatabaseClient;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient.InterfaceFileRecord;
import uk.gov.hmcts.opal.filehandler.db.FinesInterfaceFileTestDatabaseClient;
import uk.gov.hmcts.opal.filehandler.db.FinesInterfaceFileTestDatabaseClient.PreprocessingResult;
import uk.gov.hmcts.opal.filehandler.steps.BaseStepDef;
import uk.gov.hmcts.opal.filehandler.support.BaisInterfaceFileClient;
import uk.gov.hmcts.opal.filehandler.sftp.SftpClient;
import uk.gov.hmcts.opal.filehandler.support.BaisFileTestData;
import uk.gov.hmcts.opal.filehandler.support.FinesPreprocessingFixture;

/** Sets up feature-owned NatWest data and verifies APR translation through TRANSFORMED_JSON. */
public class NatWestAprFeatureStepDef extends BaseStepDef {

    private static final String SOURCE = "NATWEST";
    private static final Object CLEANUP_LOCK = new Object();
    private static boolean previousRunCleaned;
    private String scenarioFileName;
    private String expectedPaymentType;
    private static final String SFTP_USERNAME = TestEnvironment.getReportSftpUsername("NATWEST", "NATWEST");
    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);

    private Short businessUnitId;
    private String businessUnitCode;
    private String destinationSortCode;
    private String destinationAccountNumber;
    private String destinationName;
    private String destinationAccountType;
    private List<Map<String, Object>> expectedTransformedTransactions;
    private final SoftAssertions transformedAssertions = new SoftAssertions();
    private final Map<AprKey, Long> aprDefendantAccounts = new LinkedHashMap<>();
    private final Map<Long, String> defendantAccountNumbers = new LinkedHashMap<>();
    private final List<PaymentInput> payments = new ArrayList<>();
    private byte[] sourceFileContent;
    private Map<String, Object> sourceFile;
    private Map<String, Object> sourceJson;
    private Map<String, Object> transformedFile;
    private JsonPath transformedContent;
    private PreprocessingResult preprocessingResult;

    @Before("@NatWest")
    public void cleanPreviousNatWestRun() {
        // Cucumber creates fresh step instances per scenario; retain cleanup state for this test JVM.
        synchronized (CLEANUP_LOCK) {
            if (!previousRunCleaned) {
                cleanPreviousProcessedFiles();
                previousRunCleaned = true;
            }
        }
    }

    @Given("the following destination bank account is configured:")
    public void configureDestinationBank(DataTable table) {
        Map<String, String> row = table.asMaps(String.class, String.class).getFirst();
        businessUnitId = Short.parseShort(row.get("businessUnitId"));
        businessUnitCode = row.get("businessUnitCode");
        destinationSortCode = row.get("destinationSortCode");
        destinationAccountNumber = row.get("destinationAccountNumber");
        destinationName = row.get("destinationName");
        destinationAccountType = row.get("destinationAccountType");
        assertEquals("0", destinationAccountType, "The scenario requires BACS destination account type 0");
        assertTrue(businessUnitId > 0, "Business unit ID must be positive");
        assertEquals(6, destinationSortCode.length(), "Destination sort code must contain six digits");
        assertEquals(8, destinationAccountNumber.length(), "Destination account number must contain eight digits");

        configureFileHandlerBankMapping();
        configureFinesBusinessUnitAndBank();
    }

    @Given("the following destination bank account is unmapped:")
    public void unmappedDestinationBank(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        assertEquals(1, rows.size(), "Provide one unmapped destination bank account");
        Map<String, String> row = rows.getFirst();
        destinationSortCode = row.get("destinationSortCode");
        destinationAccountNumber = row.get("destinationAccountNumber");
        destinationName = row.get("destinationName");
        destinationAccountType = row.get("destinationAccountType");
        assertEquals(6, destinationSortCode.length(), "Destination sort code must contain six digits");
        assertEquals(8, destinationAccountNumber.length(), "Destination account number must contain eight digits");
        assertEquals("0", destinationAccountType, "BACS destination account type must be 0");
        try (DatabaseClient handler = new DatabaseClient()) {
            assertTrue(handler.queryFirstColumn(
                "SELECT business_unit_id FROM public.business_unit_bank_account "
                    + "WHERE bank_sort_code = ? AND bank_account_number = ?",
                destinationSortCode, destinationAccountNumber).isEmpty(),
                "Destination bank account is already mapped in file-handler; choose an unmapped account");
        }
    }

    @And("the following APRs are linked to defendant accounts:")
    public void configureAprMappings(DataTable table) {
        assertNotNull(businessUnitId, "Configure the destination bank before APR mappings");
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        assertFalse(rows.isEmpty(), "At least one NatWest APR mapping is required");

        try (DatabaseClient fines = finesDatabase()) {
            removePriorRunAprFixtures(fines, rows);
            fines.inTransaction(() -> {
                for (Map<String, String> row : rows) {
                    String apr = row.get("aprReference");
                    short rowBusinessUnitId = Short.parseShort(row.get("businessUnitId"));
                    long defendantAccountId = Long.parseLong(row.get("defendantAccountId"));
                    String accountNumber = row.get("defendantAccountNumber");
                    AprKey aprKey = new AprKey(rowBusinessUnitId, apr);
                    assertFalse(aprDefendantAccounts.containsKey(aprKey),
                        "Duplicate APR in feature data for business unit " + rowBusinessUnitId + ": " + apr);
                    assertFalse(fines.queryFirstColumn(
                        "SELECT business_unit_id FROM public.business_units WHERE business_unit_id = ?",
                        rowBusinessUnitId).isEmpty(), "APR business unit does not exist: " + rowBusinessUnitId);
                    assertTrue(fines.queryFirstColumn(
                        "SELECT defendant_account_id FROM public.defendant_accounts WHERE defendant_account_id = ?",
                        defendantAccountId).isEmpty(),
                        "Defendant account ID is already in use: " + defendantAccountId);
                    assertTrue(fines.queryFirstColumn(
                        "SELECT defendant_account_id FROM public.defendant_accounts "
                            + "WHERE business_unit_id = ? AND account_number = ?",
                        rowBusinessUnitId, accountNumber).isEmpty(),
                        "Defendant account number is already in use: " + accountNumber);
                    assertTrue(fines.queryFirstColumn(
                        "SELECT alternate_payment_reference_id FROM public.alternate_payment_references "
                            + "WHERE business_unit_id = ? AND apr_text = ?",
                        rowBusinessUnitId, apr).isEmpty(),
                        "APR is already in use in this business unit: " + apr);

                    fines.update("INSERT INTO public.defendant_accounts "
                            + "(defendant_account_id,business_unit_id,account_number,amount_imposed,amount_paid,"
                            + "account_balance,account_status,account_type,version_number) "
                            + "VALUES (?, ?, ?, 0, 0, 0, 'L', 'Fine', 1)",
                        defendantAccountId, rowBusinessUnitId, accountNumber);
                    fines.update("INSERT INTO public.alternate_payment_references "
                            + "(defendant_account_id,relationship,category,business_unit_id,apr_text,"
                            + "created_datetime,updated_datetime,version_number) "
                            + "VALUES (?, 'APR', 'APR', ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)",
                        defendantAccountId, rowBusinessUnitId, apr);
                    aprDefendantAccounts.put(aprKey, defendantAccountId);
                    defendantAccountNumbers.put(defendantAccountId, accountNumber);
                }
            });
        }
    }

    @And("the following {word} payments are written to a unique NatWest {word} file:")
    public void writeNatWestFile(String paymentKind, String fileType, DataTable table) {
        assertTrue(List.of("cash", "cheque").contains(paymentKind), "Payment kind must be cash or cheque");
        assertTrue(List.of("CARS", "FPCAS").contains(fileType), "NatWest file type must be CARS or FPCAS");
        expectedPaymentType = paymentKind.toUpperCase(Locale.ROOT);
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        assertFalse(rows.isEmpty(), "The NatWest file must contain at least one payment");
        payments.clear();
        payments.addAll(rows.stream().map(PaymentInput::from).toList());

        sourceFileContent = buildBacsStandard18File(payments, fileType);
        try (SftpClient sftp = new SftpClient(SFTP_USERNAME)) {
            scenarioFileName = uniqueFileName(sftp, fileType);
            Serenity.recordReportData().withTitle("Generated NatWest filename").andContents(scenarioFileName);
            NatWestSftpUploader.upload(SFTP_USERNAME, scenarioFileName, sourceFileContent);
        }
    }

    @Given("a unique NatWest {word} file contains no transactions")
    public void emptyNatWestFile(String fileType) {
        assertTrue(List.of("CARS", "FPCAS").contains(fileType), "NatWest file type must be CARS or FPCAS");
        sourceFileContent = buildBacsStandard18File(List.of(), fileType);
        try (SftpClient sftp = new SftpClient(SFTP_USERNAME)) {
            scenarioFileName = uniqueFileName(sftp, fileType);
            Serenity.recordReportData().withTitle("Generated empty NatWest filename").andContents(scenarioFileName);
            NatWestSftpUploader.upload(SFTP_USERNAME, scenarioFileName, sourceFileContent);
        }
    }

    @Then("the stored NatWest SOURCE and SOURCE_JSON match the payments above")
    public void storedSourceAndJsonMatchFeatureData() {
        assertNotNull(sourceFileContent, "Write the NatWest file before starting ingestion");
        List<Map<String, Object>> records = awaitSourceRecords();
        sourceFile = records.stream()
            .filter(record -> "SOURCE".equals(record.get("type")) && "SUCCESS".equals(record.get("status")))
            .findFirst().orElseThrow(() -> new AssertionError("Successful NatWest SOURCE file was not recorded: "
                + records));
        sourceJson = records.stream()
            .filter(record -> "SOURCE_JSON".equals(record.get("type")) && "SUCCESS".equals(record.get("status")))
            .findFirst().orElseThrow(() -> new AssertionError("Successful NatWest SOURCE_JSON was not recorded: "
                + records));

        assertEquals(scenarioFileName, sourceFile.get("file_name"));
        assertEquals(SOURCE, sourceFile.get("source"));
        assertEquals("SOURCE", sourceFile.get("type"));
        assertEquals("FINES", sourceFile.get("domain"));
        assertEquals(List.of(businessUnitId.intValue()), sourceFile.get("business_unit_ids"));
        assertEquals(List.of(businessUnitId.intValue()), sourceJson.get("business_unit_ids"));
        assertNotNull(sourceFile.get("filestore_uuid"));
        assertEquals(DigestUtils.md5Hex(sourceFileContent), sourceFile.get("checksum"));
        assertArrayEquals(sourceFileContent, interfaceFiles().content(sourceFile).asByteArray(),
            "Stored NatWest SOURCE content differs from the feature's payment data");

        JsonPath sourceJsonContent = jsonContent(sourceJson);
        assertEquals(expectedPaymentType, sourceJsonContent.getString("payment_type"));
        assertEquals(destinationSortCode,
            sourceJsonContent.getString("destination_details.bank_details.sort_code"));
        assertEquals(destinationAccountNumber,
            sourceJsonContent.getString("destination_details.bank_details.account_number"));
        assertEquals(expectedSourcePayments().stream().map(PaymentInput::transactionCode).toList(),
            sourceJsonContent.getList("transactions.transaction_code"));
        assertEquals(expectedSourcePayments().stream().map(PaymentInput::amountInPence).toList(),
            sourceJsonContent.getList("transactions.amount", Long.class));
        assertEquals(expectedSourcePayments().stream().map(PaymentInput::accountReference).toList(),
            sourceJsonContent.getList("transactions.originator_details.account_reference"));
    }

    @Then("the NatWest ingestion result matches:")
    public void natWestIngestionResultMatches(DataTable table) {
        assertNotNull(sourceFileContent, "Write the CARS file before starting ingestion");
        List<Map<String, String>> expectations = table.asMaps(String.class, String.class);
        assertEquals(1, expectations.size(), "Provide one expected NatWest ingestion result");
        Map<String, String> expected = expectations.getFirst();
        String expectedStatus = expected.get("sourceStatus");
        assertNotNull(expectedStatus, "Expected sourceStatus is required");
        String expectedError = expected.get("errorContains");
        boolean expectBlob = expectedBoolean(expected.get("sourceBlobPreserved"));
        boolean expectSourceJson = expectedBoolean(expected.get("sourceJsonCreated"));
        boolean expectInputOnBais = expectedBoolean(expected.get("inputFileRemainsOnBais"));
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        List<Map<String, Object>> records = List.of();
        Map<String, Object> actualSource;
        do {
            records = interfaceFiles().records(SOURCE, scenarioFileName);
            actualSource = records.stream()
                .filter(record -> "SOURCE".equals(record.get("type"))
                    && expectedStatus.equals(record.get("status")))
                .findFirst().orElse(null);
            if (actualSource != null) {
                assertEquals(scenarioFileName, actualSource.get("file_name"));
                assertEquals(SOURCE, actualSource.get("source"));
                if (expectedError == null || expectedError.isBlank()) {
                    assertEquals(null, actualSource.get("errors"), "SOURCE has an unexpected error");
                } else {
                    assertTrue(String.valueOf(actualSource.get("errors")).contains(expectedError),
                        "SOURCE error did not contain the expected text. Actual: " + actualSource.get("errors"));
                }
                assertEquals(expectBlob, actualSource.get("filestore_uuid") != null,
                    "SOURCE blob preservation differs from the feature table");
                if (expectBlob) {
                    assertEquals(DigestUtils.md5Hex(sourceFileContent), actualSource.get("checksum"));
                    assertArrayEquals(sourceFileContent,
                        blobStorage().download(actualSource.get("filestore_uuid").toString()),
                        "SOURCE blob differs from the input CARS file");
                }
                assertEquals(expectSourceJson,
                    records.stream().anyMatch(record -> "SOURCE_JSON".equals(record.get("type"))),
                    "SOURCE_JSON creation differs from the feature table");
                try (SftpClient sftp = new SftpClient(SFTP_USERNAME)) {
                    assertEquals(expectInputOnBais, sftp.exists(scenarioFileName),
                        "Input file presence on BAIS differs from the feature table");
                }
                return;
            }
            pause();
        } while (System.nanoTime() < deadline);
        throw new AssertionError("NatWest SOURCE did not reach expected status " + expectedStatus
            + " within 60 seconds: " + records);
    }

    @When("the same NatWest {word} file is placed on bais again")
    public void replaceOriginalNatWestFile(String fileType) {
        assertNotNull(sourceFile, "Verify the first NatWest SOURCE before retrying the file");
        assertTrue(scenarioFileName.contains("." + fileType + "."), "The file type differs from the original file");
        assertNotNull(preprocessingResult, "Wait for the first Fines preprocessing result before retrying");
        try (SftpClient sftp = new SftpClient(SFTP_USERNAME)) {
            assertFalse(sftp.exists(scenarioFileName), "The first ingestion must remove its BAIS file");
        }
        NatWestSftpUploader.upload(SFTP_USERNAME, scenarioFileName, sourceFileContent);
    }

    @Then("the duplicate NatWest ingestion result matches:")
    public void duplicateNatWestResultMatches(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        assertEquals(1, rows.size(), "Provide one expected duplicate result");
        Map<String, String> expected = rows.getFirst();
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        List<Map<String, Object>> records = List.of();
        Map<String, Object> duplicate = null;
        do {
            records = interfaceFiles().records(SOURCE, scenarioFileName);
            duplicate = records.stream()
                .filter(record -> "SOURCE".equals(record.get("type"))
                    && expected.get("sourceStatus").equals(record.get("status")))
                .findFirst().orElse(null);
            if (duplicate != null) {
                break;
            }
            pause();
        } while (System.nanoTime() < deadline);
        assertNotNull(duplicate, "Duplicate NatWest SOURCE was not recorded: " + records);
        assertEquals(scenarioFileName, duplicate.get("file_name"));
        assertEquals(sourceFile.get("checksum"), duplicate.get("checksum"));
        assertEquals(expectedBoolean(expected.get("sourceBlobReused")),
            sourceFile.get("filestore_uuid").equals(duplicate.get("filestore_uuid")),
            "Duplicate SOURCE blob reuse differs from the feature table");

        long sourceJsonId = asLong(sourceJson.get("interface_file_id"));
        long observationDeadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        try (InterfaceFileTestDatabaseClient files = new InterfaceFileTestDatabaseClient();
             FinesInterfaceFileTestDatabaseClient fines = new FinesInterfaceFileTestDatabaseClient()) {
            do {
                List<InterfaceFileRecord> jsonFiles = files.findByFileNameAndSource(scenarioFileName, SOURCE)
                    .stream().filter(record -> "SOURCE_JSON".equals(record.type())).toList();
                assertEquals(Integer.parseInt(expected.get("sourceJsonCount")), jsonFiles.size(),
                    "SOURCE_JSON count differs from the feature table");
                assertEquals(sourceJsonId, jsonFiles.getFirst().id(),
                    "Duplicate ingestion must retain the original SOURCE_JSON");
                List<PreprocessingResult> results = fines.findBySourceJsonId(sourceJsonId);
                assertEquals(Integer.parseInt(expected.get("finesPreprocessingCount")), results.size(),
                    "Fines preprocessing count differs from the feature table");
                assertEquals(preprocessingResult.jobId(), results.getFirst().jobId());
                assertEquals(preprocessingResult.transformedJsonId(), results.getFirst().transformedJsonId());
                pause();
            } while (System.nanoTime() < observationDeadline);
        }
        try (SftpClient sftp = new SftpClient(SFTP_USERNAME)) {
            assertEquals(expectedBoolean(expected.get("inputFileRemainsOnBais")), sftp.exists(scenarioFileName),
                "Duplicate file presence on BAIS differs from the feature table");
        }
    }

    @And("the generated NatWest {word} filename has the unsupported suffix {string}")
    public void generatedNatWestFileHasUnsupportedSuffix(String fileType, String suffix) {
        assertEquals(".txt", suffix, "The scenario expects an unsupported .txt suffix");
        assertNotNull(scenarioFileName, "Generate the NatWest CARS file first");
        assertTrue(scenarioFileName.contains("." + fileType + "."), "The file type differs from the original file");
        String unsupportedName = scenarioFileName + suffix;
        try (DatabaseClient database = new DatabaseClient();
             SftpClient sftp = new SftpClient(SFTP_USERNAME)) {
            assertTrue(database.queryFirstColumn(
                "SELECT interface_file_id FROM public.interface_files WHERE file_name = ?", unsupportedName).isEmpty(),
                "Unsupported filename already has interface-file records");
            assertFalse(sftp.exists(unsupportedName), "Unsupported filename already exists on BAIS");
            sftp.deleteIfExists(scenarioFileName);
        }
        NatWestSftpUploader.upload(SFTP_USERNAME, unsupportedName, sourceFileContent);
        scenarioFileName = unsupportedName;
        Serenity.recordReportData().withTitle("Unsupported NatWest filename").andContents(scenarioFileName);
    }

    @Then("the ignored NatWest file result matches:")
    public void ignoredNatWestFileResultMatches(DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        assertEquals(1, rows.size(), "Provide one expected ignored-file result");
        Map<String, String> expected = rows.getFirst();
        int expectedCount = Integer.parseInt(expected.get("interfaceFileCount"));
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        do {
            assertEquals(expectedCount, interfaceFiles().records(SOURCE, scenarioFileName).size(),
                "Unsupported NatWest filename produced interface-file records");
            pause();
        } while (System.nanoTime() < deadline);
        try (SftpClient sftp = new SftpClient(SFTP_USERNAME)) {
            assertEquals(expectedBoolean(expected.get("inputFileRemainsOnBais")), sftp.exists(scenarioFileName),
                "Unsupported file presence on BAIS differs from the feature table");
        }
    }

    @And("the following NatWest {word} header is replaced:")
    public void replaceNatWestHeader(String fileType, DataTable table) {
        List<Map<String, String>> rows = table.asMaps(String.class, String.class);
        assertEquals(1, rows.size(), "Provide one BACS header replacement");
        assertNotNull(sourceFileContent, "Generate the NatWest CARS file first");
        assertTrue(scenarioFileName.contains("." + fileType + "."), "The file type differs from the original file");
        Map<String, String> row = rows.getFirst();
        String original = row.get("originalPrefix");
        String replacement = row.get("replacementPrefix");
        assertEquals(4, original.length(), "BACS header prefix must have four characters");
        assertEquals(4, replacement.length(), "Replacement prefix must have four characters");
        String content = new String(sourceFileContent, StandardCharsets.UTF_8);
        assertTrue(content.startsWith(original), "Generated CARS file does not start with " + original);
        sourceFileContent = (replacement + content.substring(original.length())).getBytes(StandardCharsets.UTF_8);
        try (SftpClient sftp = new SftpClient(SFTP_USERNAME)) {
            sftp.deleteIfExists(scenarioFileName);
        }
        NatWestSftpUploader.upload(SFTP_USERNAME, scenarioFileName, sourceFileContent);
    }

    @Then("Fines preprocessing creates a TRANSFORMED_JSON for the SOURCE_JSON")
    public void finesPreprocessingCreatesTransformedJson() {
        assertNotNull(sourceJson, "Verify the successful SOURCE_JSON before waiting for Fines");
        preprocessingResult = FinesPreprocessingFixture.awaitResult(asLong(sourceJson.get("interface_file_id")));
        assertEquals(businessUnitId.shortValue(), preprocessingResult.businessUnitId());
        transformedFile = interfaceFiles().records(SOURCE, scenarioFileName).stream()
            .filter(record -> "TRANSFORMED_JSON".equals(record.get("type")))
            .filter(record -> preprocessingResult.transformedJsonId().equals(asLong(record.get("interface_file_id"))))
            .findFirst().orElseThrow(() -> new AssertionError("Fines TRANSFORMED_JSON record was not found"));
        assertTrue(List.of("INGESTED", "SUCCESS").contains(transformedFile.get("status")));
        assertEquals(List.of(businessUnitId.intValue()), transformedFile.get("business_unit_ids"),
            "TRANSFORMED_JSON metadata must identify the resolved Fines business unit");
        transformedContent = jsonContentFromBlob(transformedFile);
    }

    @And("the expected transformed NatWest payments are:")
    public void transformedNatWestPayments(DataTable table) throws JsonProcessingException {
        assertNotNull(transformedContent, "Wait for Fines preprocessing before asserting translated payments");
        List<Map<String, String>> expected = table.asMaps(String.class, String.class);
        List<PaymentInput> extractedPayments = payments.stream()
            .filter(PaymentInput::expectedInSourceJson)
            .filter(PaymentInput::expectedInTransformedJson)
            .toList();
        assertEquals(extractedPayments.size(), expected.size());
        expectedTransformedTransactions = new ArrayList<>();
        for (int index = 0; index < expected.size(); index++) {
            Map<String, String> row = expected.get(index);
            PaymentInput input = extractedPayments.get(index);
            String expectedReference = row.get("expectedAccountReference");
            transformedAssertions.assertThat(row.get("inputAccountReference"))
                .as("Expected row %s inputAccountReference matches the input payment", index + 1)
                .isEqualTo(input.accountReference());
            AprKey destinationApr = new AprKey(businessUnitId, input.accountReference());
            if (!"11".equals(input.transactionCode()) && aprDefendantAccounts.containsKey(destinationApr)) {
                transformedAssertions.assertThat(expectedReference)
                    .as("Expected row %s reference matches the APR mapping in destination business unit %s",
                        index + 1, businessUnitId)
                    .isEqualTo(aprDefendantAccounts.get(destinationApr).toString());
            }
            expectedTransformedTransactions.add(Map.of(
                "transaction_code", row.get("transactionCode"),
                "amount", Long.parseLong(row.get("amountInPence")),
                "date_entry_applied", row.get("dateEntryApplied"),
                "cheque", expectedBoolean(row.get("cheque")),
                "valid_transaction", expectedBoolean(row.get("validTransaction")),
                "originator_details", Map.of(
                    "name", input.originatorName(),
                    "account_reference", expectedReference,
                    "bank_details", Map.of("sort_code", input.originatorSortCode(),
                        "account_number", input.originatorAccountNumber()))));
        }
        // Defer assertions until the complete JSON step so totals and all field mismatches are reported together.
    }

    @And("the complete transformed NatWest JSON has payment type {string}, record count {int} and total amount {long}")
    public void transformedNatWestTotals(String paymentType, int count, long totalAmount) throws JsonProcessingException {
        assertNotNull(transformedContent, "Wait for Fines preprocessing before asserting totals");
        assertNotNull(expectedTransformedTransactions, "Declare the expected transformed payments first");
        Map<String, Object> expected = Map.of(
            "destination_details", Map.of("bank_details", Map.of(
                "account_number", destinationAccountNumber,
                "sort_code", destinationSortCode,
                "name", destinationName,
                "type", destinationAccountType)),
            "file_name", scenarioFileName,
            "payment_type", paymentType,
            "total_amount", totalAmount,
            "transactions", expectedTransformedTransactions);
        assertCompleteJsonEquals(expected, transformedContent.getMap(""));
        transformedAssertions.assertThat(transformedContent.getList("transactions").size())
            .as("Transformed JSON transaction count").isEqualTo(count);
        transformedAssertions.assertThat(preprocessingResult.recordCount().intValue())
            .as("Fines preprocessing record count").isEqualTo(count);
        transformedAssertions.assertThat(preprocessingResult.totalAmount().longValueExact())
            .as("Fines preprocessing total amount").isEqualTo(totalAmount);
        transformedAssertions.assertAll();
    }

    private static boolean expectedBoolean(String value) {
        assertTrue(List.of("true", "false").contains(value), "Expected boolean must be true or false: " + value);
        return Boolean.parseBoolean(value);
    }

    private void assertCompleteJsonEquals(Object expected, Object actual) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        // STRICT rejects extra fields and preserves array order while ignoring JSON object key order.
        try {
            var comparison = JSONCompare.compareJSON(
                mapper.writeValueAsString(expected), mapper.writeValueAsString(actual), JSONCompareMode.STRICT);
            transformedAssertions.assertThat(comparison.passed())
                .withFailMessage("Transformed NatWest JSON mismatch:%n%s", comparison.getMessage())
                .isTrue();
        } catch (JSONException exception) {
            throw new IllegalStateException("Could not compare transformed NatWest JSON", exception);
        }
    }

    private void configureFileHandlerBankMapping() {
        try (DatabaseClient handler = new DatabaseClient()) {
            handler.inTransaction(() -> {
                List<BankAccountMapping> existing = handler.query(
                    "SELECT business_unit_id,business_unit_code,opal_domain::text "
                        + "FROM public.business_unit_bank_account "
                        + "WHERE bank_sort_code = ? AND bank_account_number = ?",
                    row -> new BankAccountMapping(row.getShort(1), row.getString(2), row.getString(3)),
                    destinationSortCode, destinationAccountNumber);
                if (!existing.isEmpty()) {
                    assertEquals(1, existing.size(), "Destination bank has multiple file-handler mappings");
                    BankAccountMapping mapping = existing.getFirst();
                    assertEquals(businessUnitId.shortValue(), mapping.businessUnitId());
                    assertEquals(businessUnitCode, mapping.businessUnitCode());
                    assertEquals("FINES", mapping.domain());
                    return;
                }
                List<String> unitCodes = handler.queryFirstColumn(
                    "SELECT business_unit_code FROM public.business_unit_bank_account WHERE business_unit_id = ?",
                    businessUnitId);
                if (!unitCodes.isEmpty()) {
                    assertEquals(List.of(businessUnitCode), unitCodes);
                    handler.update("UPDATE public.business_unit_bank_account SET bank_sort_code = ?, "
                            + "bank_account_number = ? WHERE business_unit_id = ? AND business_unit_code = ?",
                        destinationSortCode, destinationAccountNumber, businessUnitId, businessUnitCode);
                    return;
                }
                handler.update("INSERT INTO public.business_unit_bank_account "
                        + "(business_unit_id,business_unit_code,opal_domain,bank_sort_code,bank_account_number,"
                        + "dwp_court_code) VALUES (?, ?, 'FINES', ?, ?, NULL)",
                    businessUnitId, businessUnitCode, destinationSortCode, destinationAccountNumber);
            });
        }
    }

    private void configureFinesBusinessUnitAndBank() {
        try (DatabaseClient fines = finesDatabase()) {
            assertFalse(fines.queryFirstColumn(
                "SELECT business_unit_id FROM public.business_units WHERE business_unit_id = ?",
                businessUnitId).isEmpty(),
                "The feature Fines business unit must exist: " + businessUnitId);
            List<String> mappedUnits = fines.queryFirstColumn("""
                SELECT DISTINCT c.business_unit_id
                FROM public.configuration_items c
                CROSS JOIN LATERAL jsonb_array_elements(
                    CASE WHEN jsonb_typeof(c.item_values::jsonb) = 'array'
                         THEN c.item_values::jsonb ELSE '[]'::jsonb END
                ) bank_account
                WHERE c.item_name = 'BANK_ACCOUNTS'
                  AND bank_account->>'sort_code' = ?
                  AND bank_account->>'account_number' = ?
                """, destinationSortCode, destinationAccountNumber);
            if (mappedUnits.isEmpty()) {
                fines.update("""
                    INSERT INTO public.configuration_items
                        (configuration_item_id, business_unit_id, item_name, item_value, item_values)
                    VALUES (nextval('public.configuration_item_id_seq'), ?, 'BANK_ACCOUNTS',
                        'NATWEST_APR_FEATURE_BANK', json_build_array(json_build_object(
                            'sort_code', ?::text, 'account_number', ?::text, 'name', 'NatWest APR test bank')))
                    """, businessUnitId, destinationSortCode, destinationAccountNumber);
            } else {
                assertEquals(List.of(businessUnitId.toString()), mappedUnits,
                    "Destination bank must resolve only to the feature Fines business unit");
            }
        }
    }

    private List<Map<String, Object>> awaitSourceRecords() {
        long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
        List<Map<String, Object>> records = List.of();
        do {
            records = interfaceFiles().records(SOURCE, scenarioFileName);
            if (records.stream().anyMatch(record -> "SOURCE".equals(record.get("type"))
                && "FAILED".equals(record.get("status")))) {
                throw new AssertionError("NatWest SOURCE processing failed: " + records);
            }
            boolean sourceReady = records.stream().anyMatch(record -> "SOURCE".equals(record.get("type"))
                && "SUCCESS".equals(record.get("status")));
            boolean sourceJsonReady = records.stream().anyMatch(record -> "SOURCE_JSON".equals(record.get("type"))
                && "SUCCESS".equals(record.get("status")));
            if (sourceReady && sourceJsonReady) {
                return records;
            }
            pause();
        } while (System.nanoTime() < deadline);
        throw new AssertionError("NatWest SOURCE and SOURCE_JSON did not complete; records: " + records);
    }

    private BaisInterfaceFileClient interfaceFiles() {
        return new BaisInterfaceFileClient(this::authorisedJsonRequest, getTestUrl());
    }

    private String uniqueFileName(SftpClient sftp, String fileType) {
        try (DatabaseClient database = new DatabaseClient()) {
            for (int attempt = 0; attempt < 100; attempt++) {
                String candidate = "CARS".equals(fileType)
                    ? String.format("Y01A.CARS.#D.SBURZ%02d.D%06d",
                        ThreadLocalRandom.current().nextInt(100), ThreadLocalRandom.current().nextInt(1_000_000))
                    : String.format("Y01A.FPCAS.#D.C%06d.D%06d",
                        ThreadLocalRandom.current().nextInt(1_000_000), ThreadLocalRandom.current().nextInt(1_000_000));
                boolean stored = !database.queryFirstColumn(
                    "SELECT interface_file_id FROM public.interface_files WHERE file_name = ?", candidate).isEmpty();
                if (!stored && !sftp.exists(candidate)) {
                    return candidate;
                }
            }
        }
        throw new IllegalStateException("Could not find an unused NatWest " + fileType + " filename after 100 attempts");
    }

    /** Clears prior NatWest processing data before setup; keeps the current run for inspection. */
    private void cleanPreviousProcessedFiles() {
        List<String> removedFiles = new ArrayList<>();
        try (SftpClient sftp = new SftpClient(SFTP_USERNAME)) {
            for (var entry : sftp.listDirectory(".")) {
                String name = entry.getName();
                if (!entry.isDirectory() && (name.startsWith("Y01A.CARS.") || name.startsWith("Y01A.FPCAS."))) {
                    sftp.deleteIfExists(name);
                }
            }
        }
        try (DatabaseClient database = new DatabaseClient();
             InterfaceFileTestDatabaseClient files = new InterfaceFileTestDatabaseClient()) {
            List<String> candidates = database.queryFirstColumn(
                "SELECT DISTINCT file_name FROM public.interface_files WHERE source::text = 'NATWEST'");
            for (String fileName : candidates) {
                List<InterfaceFileRecord> records = files.findByFileNameAndSource(fileName, SOURCE);
                FinesPreprocessingFixture.cleanUp(records);
                // Refresh after waiting: preprocessing may have created its transformed file while we waited.
                records = files.findByFileNameAndSource(fileName, SOURCE);
                for (InterfaceFileRecord record : records) {
                    if (record.filestoreUuid() != null) {
                        blobStorage().deleteIfExists(record.filestoreUuid().toString());
                    }
                }
                files.deleteByFileNameAndSource(fileName, SOURCE);
                removedFiles.add(fileName);
            }
        }
        Serenity.recordReportData().withTitle("Previous NatWest files removed before setup")
            .andContents(removedFiles.isEmpty() ? "No previous NatWest files" : String.join("\n", removedFiles));
    }

    private void removePriorRunAprFixtures(DatabaseClient fines, List<Map<String, String>> rows) {
        fines.inTransaction(() -> {
            for (Map<String, String> row : rows) {
                long accountId = Long.parseLong(row.get("defendantAccountId"));
                short accountBusinessUnitId = Short.parseShort(row.get("businessUnitId"));
                String accountNumber = row.get("defendantAccountNumber");
                String apr = row.get("aprReference");
                boolean exactPriorRunFixture = !fines.queryFirstColumn(
                    "SELECT defendant_account_id FROM public.defendant_accounts "
                        + "WHERE defendant_account_id = ? AND business_unit_id = ? AND account_number = ?",
                    accountId, accountBusinessUnitId, accountNumber).isEmpty()
                    && !fines.queryFirstColumn(
                        "SELECT alternate_payment_reference_id FROM public.alternate_payment_references "
                            + "WHERE defendant_account_id = ? AND business_unit_id = ? AND apr_text = ?",
                        accountId, accountBusinessUnitId, apr).isEmpty();
                if (exactPriorRunFixture) {
                    fines.update("DELETE FROM public.alternate_payment_references "
                            + "WHERE defendant_account_id = ? AND business_unit_id = ? AND apr_text = ?",
                        accountId, accountBusinessUnitId, apr);
                    fines.update("DELETE FROM public.defendant_accounts "
                            + "WHERE defendant_account_id = ? AND business_unit_id = ? AND account_number = ?",
                        accountId, accountBusinessUnitId, accountNumber);
                }
            }
        });
    }

    private JsonPath jsonContent(Map<String, Object> record) {
        assertNotNull(record.get("filestore_uuid"), "Interface file has no stored content: " + record);
        Response response = interfaceFiles().content(record);
        byte[] content = response.asByteArray();
        assertEquals(record.get("checksum"), DigestUtils.md5Hex(content),
            "Stored file checksum does not match its content");
        return JsonPath.from(new String(content, StandardCharsets.UTF_8));
    }

    private JsonPath jsonContentFromBlob(Map<String, Object> record) {
        assertNotNull(record.get("filestore_uuid"), "Interface file has no stored content: " + record);
        byte[] content = blobStorage().download(record.get("filestore_uuid").toString());
        assertEquals(record.get("checksum"), DigestUtils.md5Hex(content),
            "Stored file checksum does not match its content");
        return JsonPath.from(new String(content, StandardCharsets.UTF_8));
    }

    private static Long asLong(Object value) {
        assertNotNull(value, "Interface-file API metadata is missing a numeric ID");
        return value instanceof Number number ? number.longValue() : Long.valueOf(value.toString());
    }

    private BlobStorageClient blobStorage() {
        return new BlobStorageClient(BaisFileTestData.NATWEST.blobContainerName());
    }

    private static DatabaseClient finesDatabase() {
        return new DatabaseClient(TestEnvironment.getFinesDatabaseUrl(), TestEnvironment.getFinesDatabaseUsername(),
            TestEnvironment.getFinesDatabasePassword());
    }

    private List<PaymentInput> expectedSourcePayments() {
        return payments.stream().filter(PaymentInput::expectedInSourceJson).toList();
    }

    private byte[] buildBacsStandard18File(List<PaymentInput> inputPayments, String fileType) {
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

    private static void pause() {
        try {
            Thread.sleep(POLL_INTERVAL.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for NatWest processing", exception);
        }
    }

    private record PaymentInput(
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
        private static PaymentInput from(Map<String, String> row) {
            return new PaymentInput(row.get("code"), Long.parseLong(row.get("amountPence")),
                row.get("originSort"), row.get("originAccount"), row.get("originatorName"),
                row.get("accountReference") == null ? "" : row.get("accountReference"), row.get("dateEntryApplied"),
                Integer.parseInt(row.getOrDefault("referenceLeadingSpaces", "0")),
                Integer.parseInt(row.getOrDefault("referenceTrailingSpaces", "0")),
                expectedBoolean(row.getOrDefault("expectedInSourceJson", "true")),
                expectedBoolean(row.getOrDefault("expectedInTransformedJson", "true")));
        }

        private String toLine(String destinationSortCode, String destinationAccountNumber,
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
                throw new IllegalArgumentException("NatWest date must be between 2000 and 2099: " + dateEntryApplied);
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

    private record AprKey(short businessUnitId, String reference) {
    }

    private record BankAccountMapping(short businessUnitId, String businessUnitCode, String domain) {
    }
}
