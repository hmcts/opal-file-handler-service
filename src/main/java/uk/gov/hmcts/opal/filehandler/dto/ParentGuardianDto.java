package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ParentGuardianDto extends PersonAddressDto {

    @JsonProperty("national_insurance_number")
    private String nationalInsuranceNumber;

    @JsonProperty("telephone_number_home")
    private String telephoneNumberHome;

    @JsonProperty("telephone_number_business")
    private String telephoneNumberBusiness;

    @JsonProperty("telephone_number_mobile")
    private String telephoneNumberMobile;

    @JsonProperty("email_address_1")
    private String emailAddress1;

    @JsonProperty("email_address_2")
    private String emailAddress2;

    @JsonProperty("debtor_detail")
    private DebtorDetailDto debtorDetail;
}
