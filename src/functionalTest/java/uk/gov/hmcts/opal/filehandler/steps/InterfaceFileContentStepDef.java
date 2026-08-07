package uk.gov.hmcts.opal.filehandler.steps;

import static net.serenitybdd.rest.SerenityRest.lastResponse;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.io.Resources;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.IOException;
import java.net.URL;
import org.springframework.http.MediaType;

/**
 * Defines Cucumber steps for retrieving interface-file content.
 */
public class InterfaceFileContentStepDef extends BaseStepDef {

    /**
     * Retrieves the binary content for an interface file.
     *
     * @param interfaceFileId interface-file database identifier.
     */
    @When("I request the content for interface file {long}")
    public void requestInterfaceFileContent(long interfaceFileId) {
        authorisedJsonRequest()
            .accept(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .when()
            .get(getTestUrl() + "/interface-files/" + interfaceFileId + "/content");
    }

    /**
     * Asserts that the interface-file response contains binary data.
     */
    @Then("the interface file content type is application octet stream")
    public void interfaceFileContentTypeIsApplicationOctetStream() {
        assertEquals(
            MediaType.APPLICATION_OCTET_STREAM_VALUE,
            lastResponse().getContentType(),
            "Unexpected interface-file content type"
        );
    }

    /**
     * Asserts that the retrieved content is byte-for-byte equal to a classpath fixture.
     *
     * @param resourcePath classpath path of the expected file content.
     * @throws IOException when the expected fixture cannot be read.
     */
    @Then("the interface file content matches the classpath resource {string}")
    public void interfaceFileContentMatchesClasspathResource(String resourcePath) throws IOException {
        URL expectedResource = Resources.getResource(resourcePath);
        assertArrayEquals(
            Resources.toByteArray(expectedResource),
            lastResponse().getBody().asByteArray(),
            "Unexpected interface-file content"
        );
    }

    /**
     * Asserts that the problem detail contains the expected text when interface-file content cannot be retrieved.
     *
     * @param expectedDetail expected problem-detail text.
     */
    @Then("the interface file response detail contains {string}")
    public void interfaceFileResponseDetailContains(String expectedDetail) {
        String actualDetail = lastResponse().jsonPath().getString("detail");
        assertTrue(
            actualDetail != null && actualDetail.contains(expectedDetail),
            "Expected interface-file response detail to contain '" + expectedDetail + "' but was '" + actualDetail + "'"
        );
    }
}
