package uk.gov.hmcts.opal.filehandler.service.extraction.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    private static final Set<String> TOTAL_CODES = Set.of("44", "54");

    public static boolean isTotalCode(String transactionCode) {
        return TOTAL_CODES.contains(transactionCode);
    }

    private String transactionCode;
    private OriginatorDetails originatorDetails;
    private Long amount;
    private String dateEntryApplied;
}
