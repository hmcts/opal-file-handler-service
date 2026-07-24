package uk.gov.hmcts.opal.filehandler.service.extraction.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class TransactionTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeUsingSnakeCase() throws IOException {
        Transaction transaction = getTypicalData();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(transaction));

        assertTypicalData(json);
    }

    static Transaction getTypicalData() {
        return Transaction.builder()
            .transactionCode("99")
            .originatorDetails(OriginatorDetailsTest.getTypicalData())
            .amount(1500L)
            .dateEntryApplied("01/06/2026")
            .build();
    }

    static void assertTypicalData(JsonNode json) {
        assertThat(json.get("transaction_code").asText()).isEqualTo("99");
        OriginatorDetailsTest.assertTypicalData(json.get("originator_details"));
        assertThat(json.get("amount").asLong()).isEqualTo(1500L);
        assertThat(json.get("date_entry_applied").asText()).isEqualTo("01/06/2026");
    }
}


