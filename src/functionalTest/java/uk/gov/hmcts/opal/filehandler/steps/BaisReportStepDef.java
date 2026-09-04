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
