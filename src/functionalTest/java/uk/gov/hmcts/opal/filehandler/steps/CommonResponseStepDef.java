package uk.gov.hmcts.opal.filehandler.steps;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Then;
import java.util.Map;
import org.junit.jupiter.api.function.Executable;
import uk.gov.hmcts.opal.filehandler.support.TestHttpClient.TestHttpResponse;

import static org.junit.jupiter.api.Assertions.assertAll;
import static net.serenitybdd.rest.SerenityRest.lastResponse;
import static net.serenitybdd.rest.SerenityRest.then;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Provides shared response assertions for functional-test scenarios.
 */
public class CommonResponseStepDef extends BaseStepDef {

    /**
     * Asserts that the latest response returned the expected HTTP status code.
     *
     * @param statusCode expected HTTP status code.
     */
    @Then("the response status code is {int}")
    @Then("the response status is {int}")
    public void responseStatusCodeIs(int statusCode) {
        TestHttpResponse rawResponse = scenarioContext().getLatestHttpResponse();
        if (rawResponse != null) {
            assertEquals(statusCode, rawResponse.statusCode(), "Unexpected HTTP status");
            return;
        }

        then().log().ifValidationFails().statusCode(statusCode);
    }

    /**
     * Asserts that the latest response body contains the supplied text fragment.
     *
     * @param expectedValue text expected to appear in the response body.
     */
    @Then("the response body contains {string}")
    public void responseBodyContains(String expectedValue) {
        TestHttpResponse rawResponse = scenarioContext().consumeLatestHttpResponse();
        if (rawResponse != null) {
            assertTrue(rawResponse.body().contains(expectedValue));
            return;
        }

        assertTrue(lastResponse().getBody().asString().contains(expectedValue));
    }

    /**
     * Asserts expected top-level JSON field values in the latest response.
     * Use the value {@code null} when a field is expected to be JSON null.
     *
     * @param expectedFields expected JSON field names and values.
     */
    @Then("the response is as expected:")
    public void responseIsAsExpected(DataTable expectedFields) {
        Map<String, String> expectedValues = expectedFields.asMap(String.class, String.class);
        TestHttpResponse rawResponse = scenarioContext().consumeLatestHttpResponse();

        if (rawResponse != null) {
            assertAll(
                "Unexpected response fields",
                expectedValues.entrySet().stream()
                    .map(entry -> (Executable) () -> assertRawJsonField(
                        rawResponse, entry.getKey(), entry.getValue()))
                    .toList()
            );
            return;
        }

        assertAll(
            "Unexpected response fields",
            expectedValues.entrySet().stream()
                .map(entry -> (Executable) () -> assertJsonField(entry.getKey(), entry.getValue()))
                .toList()
        );
    }

    private static void assertRawJsonField(TestHttpResponse response, String field, String expectedValue) {
        String actualValue = response.jsonPath(field);
        if ("null".equals(expectedValue)) {
            assertNull(actualValue, "Unexpected value for " + field);
        } else {
            assertEquals(expectedValue, actualValue, "Unexpected value for " + field);
        }
    }

    private static void assertJsonField(String field, String expectedValue) {
        Object actualValue = lastResponse().jsonPath().get(field);
        if ("null".equals(expectedValue)) {
            assertNull(actualValue, "Unexpected value for " + field);
        } else {
            assertEquals(
                expectedValue,
                String.valueOf(actualValue),
                "Unexpected value for " + field
            );
        }
    }
}
