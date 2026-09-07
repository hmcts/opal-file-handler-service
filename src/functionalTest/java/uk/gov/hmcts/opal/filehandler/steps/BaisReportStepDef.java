package uk.gov.hmcts.opal.filehandler.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.opal.filehandler.support.BaisReportTestData.forDisplayName;
import static uk.gov.hmcts.opal.filehandler.support.BaisReportTestData.forSource;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.List;
import uk.gov.hmcts.opal.filehandler.blob.BlobStorageClient;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient;
import uk.gov.hmcts.opal.filehandler.db.InterfaceFileTestDatabaseClient.InterfaceFileRecord;
import uk.gov.hmcts.opal.filehandler.sftp.SftpClient;
import uk.gov.hmcts.opal.filehandler.support.BaisReportTestConfig;
import uk.gov.hmcts.opal.filehandler.support.TestHttpClient.TestHttpResponse;
import uk.gov.hmcts.opal.filehandler.testsupport.TestSupportApiClient;

/**
 * Defines the shared end-to-end journey for BAIS report ingestion.
 */
public class BaisReportStepDef {

    private final TestSupportApiClient testSupportApiClient = new TestSupportApiClient();
    private TestHttpResponse taskResponse;

    @Given("^the configured (BTEckoh|CAPS) report is available on bais$")
    public void configuredReportIsAvailable(String displayName) {
        BaisReportTestConfig config = forDisplayName(displayName);
        assertSftpFilePresence(config, config.fileName(), true);
    }

    @When("^the (BTEckoh|CAPS) report ingestion job is requested through testing support$")
    public void reportIngestionJobIsRequested(String displayName) {
        BaisReportTestConfig config = forDisplayName(displayName);
        taskResponse = testSupportApiClient.post("/automated-jobs/" + config.automatedTaskName());
    }

    @Then("the testing-support request is accepted")
    public void testingSupportRequestIsAccepted() {
        assertNotNull(taskResponse, "The testing-support endpoint was not called");
        assertEquals(202, taskResponse.statusCode(), "The testing-support endpoint did not accept the job");
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
