package uk.gov.hmcts.opal.filehandler.dto;

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

    private String resultId;
    private BigDecimal amountImposed;
    private BigDecimal amountPaid;
    private Integer majorCreditorId;
    private MinorCreditorDto minorCreditor;
}