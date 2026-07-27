package uk.gov.hmcts.opal.filehandler.service.extraction.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class InterfaceFileCommonDataExtractTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializeToExpectedSnakeCaseJsonStructure() throws IOException {
        InterfaceFileCommonDataExtract extract = getTypicalData();

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(extract));

        assertTypicalData(json);
    }

    @Test
    void shouldDeserializeFromExampleJson() throws IOException {
        String json = """
            {
              "file_name": "a121_00350005_300000.dat",
              "destination_details": {
                "bank_details": {
                  "account_number": "27048527",
                  "sort_code": "560033"
                }
              },
              "payment_type": "CASH",
              "transactions": [
                {
                  "transaction_code": "99",
                  "originator_details": {
                    "name": "Mrs D Richardson",
                    "account_reference": "08000066I"
                  },
                  "amount": 1500,
                  "date_entry_applied": "01/06/2026"
                }
              ]
            }
            """;

        InterfaceFileCommonDataExtract extract = objectMapper.readValue(json, InterfaceFileCommonDataExtract.class);

        assertThat(extract.getFileName()).isEqualTo("a121_00350005_300000.dat");
        assertThat(extract.getDestinationDetails().getBankDetails().getAccountNumber()).isEqualTo("27048527");
        assertThat(extract.getPaymentType()).isEqualTo(InterfaceFileCommonDataExtract.PaymentType.CASH);
        assertThat(extract.getTransactions()).hasSize(1);
        assertThat(extract.getTransactions().getFirst().getOriginatorDetails().getAccountReference())
            .isEqualTo("08000066I");
        assertThat(extract.getTransactions().getFirst().getAmount()).isEqualTo(1500L);
    }

    private static InterfaceFileCommonDataExtract getTypicalData() {
        return InterfaceFileCommonDataExtract.builder()
            .fileName("a121_00350005_300000.dat")
            .destinationDetails(DestinationDetailsTest.getTypicalData())
            .paymentType(InterfaceFileCommonDataExtract.PaymentType.CASH)
            .transactions(List.of(TransactionTest.getTypicalData()))
            .dwpCourtCode("AB01")
            .build();
    }

    private static void assertTypicalData(JsonNode json) {
        assertThat(json.get("file_name").asText()).isEqualTo("a121_00350005_300000.dat");
        DestinationDetailsTest.assertTypicalData(json.get("destination_details"));
        assertThat(json.get("payment_type").asText()).isEqualTo("CASH");
        TransactionTest.assertTypicalData(json.get("transactions").get(0));
        assertThat(json.get("dwp_court_code").asText()).isEqualTo("AB01");
    }

}


