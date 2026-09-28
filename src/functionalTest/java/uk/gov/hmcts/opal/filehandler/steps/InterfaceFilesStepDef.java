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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * Retrieves interface-file metadata using all supplied filters.
     *
     * @param filters query-parameter names and values.
     */
    @When("I request interface files with filters:")
    public void requestInterfaceFilesWithFilters(DataTable filters) {
        authorisedJsonRequest()
            .queryParams(filters.asMap(String.class, String.class))
            .when()
            .get(getTestUrl() + "/interface-files");
    }

    /**
     * Retrieves interface-file metadata filtered from an inclusive creation date.
     *
     * @param fromDate earliest creation date to include.
     */
    @When("I request interface files from date {string}")
    public void requestInterfaceFilesFromDate(String fromDate) {
        authorisedJsonRequest()
            .queryParam("from_date", fromDate)
            .when()
            .get(getTestUrl() + "/interface-files");
    }

    /**
     * Retrieves interface-file metadata filtered to an inclusive creation date.
     *
     * @param toDate latest creation date to include.
     */
    @When("I request interface files to date {string}")
    public void requestInterfaceFilesToDate(String toDate) {
        authorisedJsonRequest()
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
     * Asserts that the response contains exactly the expected number of interface files.
     *
     * @param expectedCount expected number of files.
     */
    @Then("exactly {int} interface files are returned")
    public void exactlyInterfaceFilesAreReturned(int expectedCount) {
        List<Map<String, Object>> interfaceFiles = getInterfaceFiles();
        int numberOfResults = lastResponse().jsonPath().getInt("number_of_results");

        assertAll(
            () -> assertEquals(expectedCount, interfaceFiles.size(), "Unexpected number of interface files"),
            () -> assertEquals(expectedCount, numberOfResults, "Unexpected number_of_results")
        );
    }

    /**
     * Asserts that returned interface files are ordered by creation date, oldest first.
     */
    @Then("the returned interface files are ordered by created datetime ascending")
    public void returnedInterfaceFilesAreOrderedByCreatedDatetimeAscending() {
        List<LocalDateTime> createdDatetimes = getCreatedDatetimes();
        List<LocalDateTime> sortedDatetimes = createdDatetimes.stream().sorted().toList();
        assertEquals(sortedDatetimes, createdDatetimes, "Interface files were not ordered by created datetime");
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
     * Asserts that every returned interface file contains the supplied business-unit code.
     *
     * @param businessUnitCode expected business-unit code.
     */
    @Then("every returned interface file contains business unit code {string}")
    public void everyReturnedInterfaceFileContainsBusinessUnitCode(String businessUnitCode) {
        List<Map<String, Object>> interfaceFiles = getInterfaceFiles();
        assertTrue(!interfaceFiles.isEmpty(), "No interface files were returned");
        interfaceFiles.forEach(interfaceFile -> {
            Object businessUnitCodes = interfaceFile.get("business_unit_codes");
            assertTrue(
                businessUnitCodes instanceof List<?> codes && codes.contains(businessUnitCode),
                "Interface file " + interfaceFile.get("interface_file_id")
                    + " does not contain business unit code " + businessUnitCode
            );
        });
    }

    /**
     * Asserts that every returned interface file includes at least one business-unit code.
     */
    @Then("every returned interface file has at least one business unit code")
    public void everyReturnedInterfaceFileHasAtLeastOneBusinessUnitCode() {
        List<Map<String, Object>> interfaceFiles = getInterfaceFiles();
        assertTrue(!interfaceFiles.isEmpty(), "No interface files were returned");
        interfaceFiles.forEach(interfaceFile -> {
            Object businessUnitCodes = interfaceFile.get("business_unit_codes");
            assertTrue(
                businessUnitCodes instanceof List<?> codes && !codes.isEmpty(),
                "Interface file " + interfaceFile.get("interface_file_id")
                    + " does not contain a business unit code"
            );
        });
    }

    /**
     * Asserts that every returned interface file has one of the supplied types.
     *
     * @param types comma-separated allowed types.
     */
    @Then("every returned interface file has a type in {string}")
    public void everyReturnedInterfaceFileHasTypeIn(String types) {
        Set<String> allowedTypes = commaSeparatedValues(types);
        List<Map<String, Object>> interfaceFiles = getInterfaceFiles();
        assertTrue(!interfaceFiles.isEmpty(), "No interface files were returned");
        interfaceFiles.forEach(interfaceFile -> assertTrue(
            allowedTypes.contains(String.valueOf(interfaceFile.get("type"))),
            "Unexpected type for interface file " + interfaceFile.get("interface_file_id")
        ));
    }

    /**
     * Asserts that no returned interface file has one of the supplied statuses.
     *
     * @param statuses comma-separated excluded statuses.
     */
    @Then("no returned interface file has a status in {string}")
    public void noReturnedInterfaceFileHasStatusIn(String statuses) {
        Set<String> excludedStatuses = commaSeparatedValues(statuses);
        getInterfaceFiles().forEach(interfaceFile -> assertTrue(
            !excludedStatuses.contains(String.valueOf(interfaceFile.get("status"))),
            "Excluded status returned for interface file " + interfaceFile.get("interface_file_id")
        ));
    }

    /**
     * Asserts that no returned interface file has the excluded target.
     *
     * @param target excluded target.
     */
    @Then("no returned interface file has target {string}")
    public void noReturnedInterfaceFileHasTarget(String target) {
        getInterfaceFiles().forEach(interfaceFile -> assertTrue(
            !target.equals(interfaceFile.get("target")),
            "Excluded target returned for interface file " + interfaceFile.get("interface_file_id")
        ));
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

    /**
     * Asserts that every returned interface file was created on or after the inclusive boundary.
     *
     * @param fromDate earliest expected creation date.
     */
    @Then("every returned interface file was created on or after date {string}")
    public void everyReturnedInterfaceFileWasCreatedOnOrAfterDate(String fromDate) {
        LocalDateTime expectedFromDate = LocalDateTime.parse(fromDate);
        List<LocalDateTime> createdDatetimes = getCreatedDatetimes();

        assertAll(
            () -> assertTrue(!createdDatetimes.isEmpty(), "No interface files were returned"),
            () -> assertTrue(
                createdDatetimes.stream().allMatch(date -> !date.isBefore(expectedFromDate)),
                "An interface file was created before " + expectedFromDate
            )
        );
    }

    /**
     * Asserts that every returned interface file was created on or before the inclusive boundary.
     *
     * @param toDate latest expected creation date.
     */
    @Then("every returned interface file was created on or before date {string}")
    public void everyReturnedInterfaceFileWasCreatedOnOrBeforeDate(String toDate) {
        LocalDateTime expectedToDate = LocalDateTime.parse(toDate);
        List<LocalDateTime> createdDatetimes = getCreatedDatetimes();

        assertAll(
            () -> assertTrue(!createdDatetimes.isEmpty(), "No interface files were returned"),
            () -> assertTrue(
                createdDatetimes.stream().allMatch(date -> !date.isAfter(expectedToDate)),
                "An interface file was created after " + expectedToDate
            )
        );
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

    private static List<LocalDateTime> getCreatedDatetimes() {
        return getInterfaceFiles().stream()
            .map(interfaceFile -> LocalDateTime.parse(String.valueOf(interfaceFile.get("created_datetime"))))
            .toList();
    }

    private static Set<String> commaSeparatedValues(String values) {
        return Stream.of(values.split(","))
            .map(String::trim)
            .collect(Collectors.toSet());
    }
}
