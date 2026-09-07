package uk.gov.hmcts.opal.filehandler.service.extraction.model;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class DestinationDetailsTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void shouldSerializeUsingSnakeCase() throws IOException {
        DestinationDetails destinationDetails = getTypicalData();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(destinationDetails));

        assertTypicalData(json);
    }

    static DestinationDetails getTypicalData() {
        return DestinationDetails.builder()
            .bankDetails(BankDetailsTest.getTypicalData())
            .build();
    }

    static void assertTypicalData(JsonNode json) {
        BankDetailsTest.assertTypicalData(json.get("bank_details"));
    }
}


