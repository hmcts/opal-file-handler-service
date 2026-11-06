package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImpositionDto {

    @JsonProperty("result_id")
    private String resultId;

    @JsonProperty("amount_imposed")
    private BigDecimal amountImposed;

    @JsonProperty("amount_paid")
    private BigDecimal amountPaid;

    @JsonProperty("major_creditor_id")
    private Integer majorCreditorId;

    @JsonProperty("minor_creditor")
    private MinorCreditorDto minorCreditor;
}