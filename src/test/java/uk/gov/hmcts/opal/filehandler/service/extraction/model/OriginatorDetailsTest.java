package uk.gov.hmcts.opal.filehandler.service.extraction.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class OriginatorDetailsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeUsingSnakeCase() throws IOException {
        OriginatorDetails originatorDetails = getTypicalData();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(originatorDetails));

        assertTypicalData(json);
    }

    static OriginatorDetails getTypicalData() {
        return OriginatorDetails.builder()
            .name("Mrs D Richardson")
            .accountReference("08000066I")
            .bankDetails(BankDetailsTest.getTypicalData())
            .build();
    }

    static void assertTypicalData(JsonNode json) {
        assertThat(json.get("name").asText()).isEqualTo("Mrs D Richardson");
        assertThat(json.get("account_reference").asText()).isEqualTo("08000066I");
        BankDetailsTest.assertTypicalData(json.get("bank_details"));
    }
}


