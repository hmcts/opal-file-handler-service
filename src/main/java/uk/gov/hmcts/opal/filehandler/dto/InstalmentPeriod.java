package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstalmentPeriod {

    @JsonProperty("payment_terms_type_code")
    private String paymentTermsTypeCode;

    @JsonProperty("instalment_period")
    private InstalmentPeriod instalmentPeriod;
}