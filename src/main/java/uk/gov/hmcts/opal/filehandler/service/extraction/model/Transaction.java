package uk.gov.hmcts.opal.filehandler.service.extraction.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    private static final Set<String> TOTALS_CODES = Set.of("44", "54");

    public static boolean isTotalCode(String transactionCode) {
        return TOTALS_CODES.contains(transactionCode);
    }

    private String transactionCode;
    private OriginatorDetails originatorDetails;
    private Long amount;
    private String dateEntryApplied;
}

