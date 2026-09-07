package uk.gov.hmcts.opal.filehandler.service.extraction.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class BankDetailsTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

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

