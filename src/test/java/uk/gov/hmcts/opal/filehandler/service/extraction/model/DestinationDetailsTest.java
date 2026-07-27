package uk.gov.hmcts.opal.filehandler.service.extraction.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class DestinationDetailsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

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



