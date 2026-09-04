package uk.gov.hmcts.opal.filehandler.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.opal.filehandler.support.BaisReportTestData.forDisplayName;
import static uk.gov.hmcts.opal.filehandler.support.BaisReportTestData.forSource;

import io.cucumber.java.en.Given;
import io.cucumber.datatable.DataTable;
import com.azure.storage.blob.models.BlobProperties;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.HexFormat;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import java.util.Map;
import net.serenitybdd.core.Serenity;
import uk.gov.hmcts.opal.filehandler.blob.BlobStorageClient;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient.InterfaceFileRecord;
import uk.gov.hmcts.opal.filehandler.sftp.SftpClient;
import uk.gov.hmcts.opal.filehandler.support.BaisAutomatedTaskRunner;
import uk.gov.hmcts.opal.filehandler.support.BaisReportTestConfig;

/**
 * Defines the shared end-to-end journey for BAIS report ingestion.
 */
public class BaisReportStepDef {

    private Map<String, Map<String, String>> blobsBefore;
    private List<InterfaceFileRecord> recordsBefore;

    private final BaisAutomatedTaskRunner taskRunner = new BaisAutomatedTaskRunner();

    @Given("^the configured (BTEckoh|CAPS) report is available on bais$")
    public void configuredReportIsAvailable(String displayName) {
        BaisReportTestConfig config = forDisplayName(displayName);
        assertSftpFilePresence(config, config.fileName(), true);
    }

    @Given("^a (BTEckoh|CAPS) report with an unsupported filename is available on bais$")
    public void unsupportedReportIsAvailable(String displayName) {
        BaisReportTestConfig config = forDisplayName(displayName);
        try (SftpClient sftpClient = new SftpClient(config.sftpUsername())) {
            sftpClient.deleteIfExists(config.fileName());
            sftpClient.uploadResource(config.resourcePath(), config.unsupportedFileName());
        }
    }

    @Given("^the configured (BTEckoh|CAPS) report has already been ingested successfully$")
    public void configuredReportHasAlreadyBeenIngested(String displayName) {
        BaisReportTestConfig config = forDisplayName(displayName);
        triggerTask(config);
        assertEquals(1, recordsWithStatus(config, "SUCCESS").size(),
            "Expected the first " + config.displayName() + " report ingestion to succeed");
    }

    @Given("^the same (BTEckoh|CAPS) report is uploaded again$")
    public void sameReportIsUploadedAgain(String displayName) {
        BaisReportTestConfig config = forDisplayName(displayName);
        try (SftpClient sftpClient = new SftpClient(config.sftpUsername())) {
            sftpClient.uploadResource(config.resourcePath(), config.fileName());
        }
    }

    @When("^the (BTEckoh|CAPS) report ingestion task is triggered$")
    public void reportIngestionTaskIsTriggered(String displayName) {
        triggerTask(forDisplayName(displayName));
    }

    @Then("^a successful (BTECKOH_REPORT|CAPS_REPORT) interface file is stored$")
    public void successfulInterfaceFileIsStored(String source) {
        BaisReportTestConfig config = forSource(source);
        List<InterfaceFileRecord> records = recordsWithStatus(config, "SUCCESS");
        assertEquals(1, records.size(),
            "Expected one successful " + config.displayName() + " interface-file record");

        InterfaceFileRecord record = records.getFirst();
        assertEquals(config.source(), record.source());
        assertEquals("OPAL", record.target());
        assertEquals("SOURCE", record.type());
        assertEquals("MAINTENANCE", record.domain());
        assertEquals(config.fileName(), record.fileName());
        assertEquals(config.checksum(), record.checksum());
        assertNotNull(record.filestoreUuid());
        assertNotNull(record.createdDatetime());
        assertNull(record.errors(), "Successful reports must not carry an error");
    }

    @Then("^the stored (BTEckoh|CAPS) report content matches the bais (workbook|file)$")
    public void storedReportContentMatches(String displayName, String fileDescription) {
        BaisReportTestConfig config = forDisplayName(displayName);
        InterfaceFileRecord success = recordsWithStatus(config, "SUCCESS").getFirst();
        assertTrue(new BlobStorageClient(config.blobContainerName()).contentMatchesResource(
            success.filestoreUuid().toString(), config.resourcePath()),
            "Stored " + config.displayName() + " report content differs from the SFTP " + fileDescription);
    }

    @Then("^the configured (BTEckoh|CAPS) report no longer exists on bais$")
    public void configuredReportNoLongerExists(String displayName) {
        BaisReportTestConfig config = forDisplayName(displayName);
        assertSftpFilePresence(config, config.fileName(), false);
    }

    @Then("^no interface file is created for the unsupported (BTEckoh|CAPS) filename$")
    public void unsupportedInterfaceFileIsNotCreated(String displayName) {
        BaisReportTestConfig config = forDisplayName(displayName);
        try (InterfaceFileTestDatabaseClient databaseClient = new InterfaceFileTestDatabaseClient()) {
            assertTrue(databaseClient.findByFileName(config.unsupportedFileName()).isEmpty(),
                "An interface-file record was created for an unsupported " + config.displayName() + " filename");
        }
    }

    @Then("^the unsupported (BTEckoh|CAPS) file remains on bais$")
    public void unsupportedReportRemainsOnSftp(String displayName) {
        BaisReportTestConfig config = forDisplayName(displayName);
        assertSftpFilePresence(config, config.unsupportedFileName(), true);
    }

    @Then("^one successful and one duplicate (BTEckoh|CAPS) interface file are stored$")
    public void successAndDuplicateAreStored(String displayName) {
        BaisReportTestConfig config = forDisplayName(displayName);
        assertEquals(1, recordsWithStatus(config, "SUCCESS").size(),
            "Expected one successful " + config.displayName() + " interface-file record");
        assertEquals(1, recordsWithStatus(config, "DUPLICATE").size(),
            "Expected one duplicate " + config.displayName() + " interface-file record");
        InterfaceFileRecord success = recordsWithStatus(config, "SUCCESS").getFirst();
        InterfaceFileRecord duplicate = recordsWithStatus(config, "DUPLICATE").getFirst();
        assertEquals(success.filestoreUuid(), duplicate.filestoreUuid(), "Duplicate must reference the original blob");
        assertEquals(success.checksum(), duplicate.checksum());
        assertNotNull(duplicate.createdDatetime());
        assertNotNull(duplicate.errors(), "Duplicate outcome must have an explanation");
    }

    @Given("^the (BTEckoh|CAPS) blobstore and interface records are recorded$")
    public void recordState(String displayName) {
        BaisReportTestConfig config = forDisplayName(displayName);
        blobsBefore = new BlobStorageClient(config.blobContainerName()).snapshotStorage();
        try (InterfaceFileTestDatabaseClient databaseClient = new InterfaceFileTestDatabaseClient()) {
            recordsBefore = databaseClient.findByFileName(config.fileName());
        }
    }

    @Given("^a malformed (BTEckoh|CAPS) report with a supported filename is available on bais$")
    public void malformedReportIsAvailable(String displayName) {
        BaisReportTestConfig config = forDisplayName(displayName);
        try (SftpClient sftpClient = new SftpClient(config.sftpUsername())) {
            sftpClient.uploadResource("test-data/bais/malformed.txt", config.fileName());
        }
    }

    @When("^the (BTEckoh|CAPS) report ingestion task is triggered with its feature flag disabled$")
    public void triggerDisabledTask(String displayName) {
        String output = taskRunner.run(forDisplayName(displayName), false);
        Serenity.recordReportData().withTitle("Disabled report job output").andContents(output);
    }

    @Then("^the (BTEckoh|CAPS) blobstore is unchanged$")
    public void blobstoreIsUnchanged(String displayName) {
        assertNotNull(blobsBefore, "Record blob state before triggering the job");
        assertEquals(blobsBefore,
            new BlobStorageClient(forDisplayName(displayName).blobContainerName()).snapshotStorage(),
            "The job must not create, delete or overwrite blobs in any container");
    }

    @Then("^the (BTEckoh|CAPS) interface records are unchanged$")
    public void interfaceRecordsAreUnchanged(String displayName) {
        assertNotNull(recordsBefore, "Record interface state before triggering the job");
        try (InterfaceFileTestDatabaseClient databaseClient = new InterfaceFileTestDatabaseClient()) {
            assertEquals(recordsBefore, databaseClient.findByFileName(forDisplayName(displayName).fileName()),
                "The job must not create or alter interface-file records");
        }
    }

    @Then("^a failed (BTEckoh|CAPS) report is recorded without a blob$")
    public void failedReportIsRecorded(String displayName) {
        List<InterfaceFileRecord> failures = recordsWithStatus(forDisplayName(displayName), "FAILED");
        assertEquals(1, failures.size(), "Expected one failed report record");
        InterfaceFileRecord failure = failures.getFirst();
        assertNull(failure.filestoreUuid(), "Rejected content must not reference a blob");
        assertNotNull(failure.createdDatetime());
        assertNotNull(failure.checksum());
        assertNotNull(failure.errors());
        assertTrue(failure.errors().contains("not valid XML") || failure.errors().contains("not a valid XLSX workbook"),
            "The record must explain the invalid report format");
        Serenity.recordReportData().withTitle(displayName + " failed interface-file metadata")
            .andContents(failure.toString());
    }

    @Then("^the (BTEckoh|CAPS) report has global-file metadata:$")
    public void globalReportMetadata(String displayName, DataTable expected) {
        InterfaceFileRecord record = recordsWithStatus(forDisplayName(displayName), "SUCCESS").getFirst();
        Map<String, String> actual = Map.of(
            "source", record.source(), "target", record.target(), "type", record.type(),
            "status", record.status(),
            "business_unit_codes", record.businessUnitCodes().isEmpty()
                ? "none" : String.join(",", record.businessUnitCodes()),
            "payment_type", record.paymentType() == null ? "none" : record.paymentType());
        assertEquals(expected.asMap(String.class, String.class), actual);
        Serenity.recordReportData().withTitle(displayName + " global interface-file metadata")
            .andContents(record.toString());
    }

    @Then("^the (BTEckoh|CAPS) report is stored only in its configured blob container$")
    public void reportStorageLocation(String displayName) throws IOException {
        BaisReportTestConfig config = forDisplayName(displayName);
        InterfaceFileRecord record = recordsWithStatus(config, "SUCCESS").getFirst();
        String blobName = record.filestoreUuid().toString();
        BlobStorageClient storage = new BlobStorageClient(config.blobContainerName());
        Map<String, Map<String, String>> after = storage.snapshotStorage();
        assertNotNull(blobsBefore, "Record storage state before ingestion");
        assertFalse(blobsBefore.get(config.blobContainerName()).containsKey(blobName));
        assertNotNull(after.get(config.blobContainerName()).remove(blobName),
            "Expected the blob in its report container");
        assertEquals(blobsBefore, after, "Only the expected report container may receive one new blob");

        BlobProperties properties = storage.properties(blobName);
        assertNotNull(properties.getContentMd5(), "Blob must expose its persisted checksum");
        assertEquals(record.checksum(), HexFormat.of().formatHex(properties.getContentMd5()));
        assertEquals(Resources.toByteArray(Resources.getResource(config.resourcePath())).length,
            properties.getBlobSize());
        Serenity.recordReportData().withTitle(displayName + " blob storage evidence").andContents(
            "Container: " + config.blobContainerName() + "\nBlob name / filestore UUID: " + blobName
                + "\nInterface file ID: " + record.id() + "\nOriginal filename: " + record.fileName()
                + "\nBlob and database MD5: " + record.checksum() + "\nSize (bytes): " + properties.getBlobSize()
                + "\nETag: " + properties.getETag());
    }

    @When("^the (BTEckoh|CAPS) report is replaced with a valid file$")
    public void replaceReportWithValidFile(String displayName) {
        sameReportIsUploadedAgain(displayName);
    }

    @Then("^the earlier failed (BTEckoh|CAPS) attempt remains traceable$")
    public void earlierFailureIsRetained(String displayName) {
        InterfaceFileRecord failure = recordsWithStatus(forDisplayName(displayName), "FAILED").getFirst();
        assertEquals(recordsBefore, List.of(failure), "Retain the earlier failure metadata after correcting the file");
    }

    @Then("^one earlier (BTEckoh|CAPS) failure is superseded without a blob$")
    public void previousFailureIsSuperseded(String displayName) {
        List<InterfaceFileRecord> previous = recordsWithStatus(forDisplayName(displayName), "FAILED_SUPERSEDED");
        assertEquals(1, previous.size());
        assertNull(previous.getFirst().filestoreUuid());
        assertEquals(recordsBefore.getFirst().id(), previous.getFirst().id());
        assertEquals(recordsBefore.getFirst().checksum(), previous.getFirst().checksum());
        assertEquals(recordsBefore.getFirst().errors(), previous.getFirst().errors());
    }

    private void triggerTask(BaisReportTestConfig config) {
        String output = taskRunner.run(config);
        Serenity.recordReportData().withTitle(config.displayName() + " ingestion task output").andContents(output);
    }

    private static List<InterfaceFileRecord> recordsWithStatus(BaisReportTestConfig config, String status) {
        try (InterfaceFileTestDatabaseClient databaseClient = new InterfaceFileTestDatabaseClient()) {
            return databaseClient.findByFileName(config.fileName()).stream()
                .filter(record -> status.equals(record.status()))
                .toList();
        }
    }

    private static void assertSftpFilePresence(
        BaisReportTestConfig config,
        String fileName,
        boolean expected
    ) {
        try (SftpClient sftpClient = new SftpClient(config.sftpUsername())) {
            if (expected) {
                assertTrue(sftpClient.exists(fileName), "Expected SFTP file to exist: " + fileName);
            } else {
                assertFalse(sftpClient.exists(fileName), "Expected SFTP file to be removed: " + fileName);
            }
        }
    }
}
