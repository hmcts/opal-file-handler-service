package uk.gov.hmcts.opal.filehandler.service.extraction.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class BankDetailsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeUsingSnakeCase() throws IOException {
        BankDetails bankDetails = getTypicalData();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(bankDetails));

        assertTypicalData(json);
    }

    static BankDetails getTypicalData() {
        return BankDetails.builder()
            .accountNumber("27048527")
            .sortCode("560033")
            .name("HMCTS")
            .type("SAVINGS")
            .build();
    }

    static void assertTypicalData(JsonNode json) {
        assertThat(json.get("account_number").asText()).isEqualTo("27048527");
        assertThat(json.get("sort_code").asText()).isEqualTo("560033");
        assertThat(json.get("name").asText()).isEqualTo("HMCTS");
        assertThat(json.get("type").asText()).isEqualTo("SAVINGS");
    }
}


