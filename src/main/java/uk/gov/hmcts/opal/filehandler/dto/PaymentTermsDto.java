package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.filehandler.types.FileHandlerTypes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTermsDto {

    @JsonProperty("payment_terms_type_code")
    private FileHandlerTypes paymentTermsTypeCode;

    @JsonProperty("effective_date")
    private LocalDate effectiveDate;

    @JsonProperty("instalment_period")
    private FileHandlerTypes instalmentPeriod;

    @JsonProperty("lump_sum_amount")
    private BigDecimal lumpSumAmount;

    @JsonProperty("instalment_amount")
    private BigDecimal instalmentAmount;

    @JsonProperty("default_days_in_jail")
    private Integer defaultDaysInJail;

    @JsonProperty("enforcements")
    private List<EnforcementDto> enforcements;
}