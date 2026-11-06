package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTermsType {

    @JsonProperty("payment_terms_type")
    private PaymentTermsType paymentTermsType;

    @JsonProperty("effective_date")
    private LocalDate effectiveDate;

    @JsonProperty("instalment_amount")
    private BigDecimal instalmentAmount;
}