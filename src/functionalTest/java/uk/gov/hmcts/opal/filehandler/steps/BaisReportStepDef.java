package uk.gov.hmcts.opal.filehandler.steps;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.opal.filehandler.support.BaisReportTestData.forDisplayName;
import static uk.gov.hmcts.opal.filehandler.support.BaisReportTestData.forSource;

import com.google.common.io.Resources;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import uk.gov.hmcts.opal.filehandler.sftp.SftpClient;
import uk.gov.hmcts.opal.filehandler.support.BaisReportTestConfig;
import uk.gov.hmcts.opal.filehandler.support.TestHttpClient.TestHttpResponse;
import uk.gov.hmcts.opal.filehandler.testsupport.TestSupportApiClient;

/**
 * Defines the shared end-to-end journey for BAIS report ingestion.
 */
public class BaisReportStepDef extends BaseStepDef {

    private static final Duration INGESTION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);

    private final TestSupportApiClient testSupportApiClient = new TestSupportApiClient();
    private TestHttpResponse taskResponse;
    private Map<String, Object> successfulInterfaceFile;

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
        successfulInterfaceFile = awaitSuccessfulInterfaceFile(config);

        assertEquals(config.source(), successfulInterfaceFile.get("source"));
        assertEquals("OPAL", successfulInterfaceFile.get("target"));
        assertEquals("SOURCE", successfulInterfaceFile.get("type"));
        assertEquals("MAINTENANCE", successfulInterfaceFile.get("domain"));
        assertEquals(config.fileName(), successfulInterfaceFile.get("file_name"));
        assertEquals(config.checksum(), successfulInterfaceFile.get("checksum"));
        assertNotNull(successfulInterfaceFile.get("filestore_uuid"));
    }

    @Then("^the stored (BTEckoh|CAPS) report content matches the bais (workbook|file)$")
    public void storedReportContentMatches(String displayName, String fileDescription) throws IOException {
        BaisReportTestConfig config = forDisplayName(displayName);
        assertNotNull(successfulInterfaceFile, "Successful interface-file metadata was not retrieved");
        long interfaceFileId = ((Number) successfulInterfaceFile.get("interface_file_id")).longValue();

        Response response = authorisedJsonRequest()
            .accept("application/octet-stream")
            .when()
            .get(getTestUrl() + "/interface-files/" + interfaceFileId + "/content");

        URL expectedResource = Resources.getResource(config.resourcePath());
        assertEquals(200, response.statusCode(), "Stored report content could not be retrieved");
        assertArrayEquals(
            Resources.toByteArray(expectedResource),
            response.getBody().asByteArray(),
            "Stored " + config.displayName() + " report content differs from the SFTP " + fileDescription
        );
    }

    @Then("^the configured (BTEckoh|CAPS) report no longer exists on bais$")
    public void configuredReportNoLongerExists(String displayName) {
        BaisReportTestConfig config = forDisplayName(displayName);
        assertSftpFilePresence(config, config.fileName(), false);
    }

    private Map<String, Object> awaitSuccessfulInterfaceFile(BaisReportTestConfig config) {
        long deadline = System.nanoTime() + INGESTION_TIMEOUT.toNanos();
        do {
            Response response = authorisedJsonRequest()
                .queryParam("source", config.source())
                .queryParam("status", "SUCCESS")
                .when()
                .get(getTestUrl() + "/interface-files");
            assertEquals(200, response.statusCode(), "Interface-file metadata could not be retrieved");

            List<Map<String, Object>> matches = response.jsonPath()
                .<Map<String, Object>>getList("interface_files")
                .stream()
                .filter(record -> config.fileName().equals(record.get("file_name")))
                .toList();
            if (matches.size() == 1) {
                return matches.getFirst();
            }
            pauseBeforeRetry();
        } while (System.nanoTime() < deadline);

        throw new AssertionError(
            "Expected one successful " + config.displayName() + " interface-file record within "
                + INGESTION_TIMEOUT.toSeconds() + " seconds"
        );
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
