package uk.gov.hmcts.opal.filehandler.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.filehandler.dto.types.FileHandlerTypes.EnforcementType;
import uk.gov.hmcts.opal.filehandler.dto.types.FileHandlerTypes.ResultType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTermsDto {

    private EnforcementType paymentTermsTypeCode;
    private LocalDate effectiveDate;
    private ResultType instalmentPeriod;
    private BigDecimal lumpSumAmount;
    private BigDecimal instalmentAmount;
    private Integer defaultDaysInJail;
    private List<EnforcementDto> enforcements;
}