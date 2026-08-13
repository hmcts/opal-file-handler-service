package uk.gov.hmcts.opal.filehandler.steps;

import static net.serenitybdd.rest.SerenityRest.lastResponse;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Defines Cucumber steps for retrieving interface-file metadata.
 */
public class InterfaceFilesStepDef extends BaseStepDef {

    /**
     * Retrieves all interface-file metadata visible to the current user.
     */
    @When("I request all interface files")
    public void requestAllInterfaceFiles() {
        authorisedJsonRequest()
            .when()
            .get(getTestUrl() + "/interface-files");
    }

    /**
     * Retrieves interface-file metadata filtered by source and status.
     *
     * @param source interface-file source filter.
     * @param status interface-file status filter.
     */
    @When("I request interface files with source {string} and status {string}")
    public void requestInterfaceFilesWithSourceAndStatus(String source, String status) {
        authorisedJsonRequest()
            .queryParam("source", source)
            .queryParam("status", status)
            .when()
            .get(getTestUrl() + "/interface-files");
    }

    /**
     * Retrieves interface-file metadata filtered by target and type.
     *
     * @param target interface-file target filter.
     * @param type interface-file type filter.
     */
    @When("I request interface files with target {string} and type {string}")
    public void requestInterfaceFilesWithTargetAndType(String target, String type) {
        authorisedJsonRequest()
            .queryParam("target", target)
            .queryParam("type", type)
            .when()
            .get(getTestUrl() + "/interface-files");
    }

    /**
     * Retrieves interface-file metadata filtered by domain.
     *
     * @param domain interface-file domain filter.
     */
    @When("I request interface files with domain {string}")
    public void requestInterfaceFilesWithDomain(String domain) {
        authorisedJsonRequest()
            .queryParam("domain", domain)
            .when()
            .get(getTestUrl() + "/interface-files");
    }

    /**
     * Retrieves interface-file metadata filtered by inclusive creation dates.
     *
     * @param fromDate earliest creation date to include.
     * @param toDate latest creation date to include.
     */
    @When("I request interface files from date {string} to date {string}")
    public void requestInterfaceFilesFromDateToDate(String fromDate, String toDate) {
        authorisedJsonRequest()
            .queryParam("from_date", fromDate)
            .queryParam("to_date", toDate)
            .when()
            .get(getTestUrl() + "/interface-files");
    }

    /**
     * Asserts that the response contains at least the expected number of interface files.
     *
     * @param minimumCount minimum expected number of files.
     */
    @Then("at least {int} interface files are returned")
    public void atLeastInterfaceFilesAreReturned(int minimumCount) {
        List<Map<String, Object>> interfaceFiles = getInterfaceFiles();
        int numberOfResults = lastResponse().jsonPath().getInt("number_of_results");

        assertAll(
            () -> assertTrue(interfaceFiles.size() >= minimumCount, "Too few interface files returned"),
            () -> assertEquals(interfaceFiles.size(), numberOfResults, "Unexpected number_of_results")
        );
    }

    /**
     * Asserts all supplied metadata fields for the interface file identified by its filestore UUID.
     * Use the value {@code null} in the table when a response field is expected to be absent.
     *
     * @param filestoreUuid filestore UUID used to locate the response item.
     * @param expectedDetails expected response field values.
     */
    @Then("the interface file with filestore UUID {string} has details:")
    public void interfaceFileWithFilestoreUuidHasDetails(String filestoreUuid, DataTable expectedDetails) {
        Map<String, Object> interfaceFile = getInterfaceFiles().stream()
            .filter(file -> filestoreUuid.equals(file.get("filestore_uuid")))
            .findFirst()
            .orElse(null);

        assertNotNull(interfaceFile, "Interface file was not returned for filestore UUID " + filestoreUuid);
        assertExpectedDetails(interfaceFile, expectedDetails.asMap(String.class, String.class));
    }

    /**
     * Asserts that every returned interface file has all supplied metadata field values.
     *
     * @param expectedDetails expected response field values.
     */
    @Then("every returned interface file has details:")
    public void everyReturnedInterfaceFileHasDetails(DataTable expectedDetails) {
        List<Map<String, Object>> interfaceFiles = getInterfaceFiles();
        assertTrue(!interfaceFiles.isEmpty(), "No interface files were returned");

        Map<String, String> expectedValues = expectedDetails.asMap(String.class, String.class);
        interfaceFiles.forEach(interfaceFile -> assertExpectedDetails(interfaceFile, expectedValues));
    }

    /**
     * Asserts that every returned interface file was created within the inclusive date range.
     *
     * @param fromDate earliest expected creation date.
     * @param toDate latest expected creation date.
     */
    @Then("every returned interface file was created from date {string} to date {string}")
    public void everyReturnedInterfaceFileWasCreatedFromDateToDate(String fromDate, String toDate) {
        LocalDateTime expectedFromDate = LocalDateTime.parse(fromDate);
        LocalDateTime expectedToDate = LocalDateTime.parse(toDate);

        getInterfaceFiles().forEach(interfaceFile -> {
            LocalDateTime createdDate = LocalDateTime.parse(String.valueOf(interfaceFile.get("created_datetime")));
            assertTrue(
                !createdDate.isBefore(expectedFromDate) && !createdDate.isAfter(expectedToDate),
                "Interface file " + interfaceFile.get("interface_file_id") + " was created outside the expected range"
            );
        });
    }

    private static void assertExpectedDetails(
        Map<String, Object> interfaceFile,
        Map<String, String> expectedDetails
    ) {
        expectedDetails.forEach((field, expectedValue) -> {
            Object actualValue = interfaceFile.get(field);
            if ("null".equals(expectedValue)) {
                assertNull(actualValue, "Unexpected value for " + field);
            } else {
                assertEquals(
                    expectedValue,
                    String.valueOf(actualValue),
                    "Unexpected value for " + field + " in interface file " + interfaceFile.get("interface_file_id")
                );
            }
        });
    }

    private static List<Map<String, Object>> getInterfaceFiles() {
        return lastResponse().jsonPath().getList("interface_files");
    }
}
