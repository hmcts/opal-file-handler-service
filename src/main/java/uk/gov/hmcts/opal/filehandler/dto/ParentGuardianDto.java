package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ParentGuardianDto {

    @JsonProperty("company_flag")
    private Boolean companyFlag;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("surname")
    private String surname;

    @JsonProperty("forenames")
    private String forenames;

    @JsonProperty("dob")
    private LocalDate dob;

    @JsonProperty("national_insurance_number")
    private String nationalInsuranceNumber;

    @JsonProperty("address_line_1")
    private String addressLine1;

    @JsonProperty("address_line_2")
    private String addressLine2;

    @JsonProperty("address_line_3")
    private String addressLine3;

    @JsonProperty("address_line_4")
    private String addressLine4;

    @JsonProperty("address_line_5")
    private String addressLine5;

    @JsonProperty("post_code")
    private String postCode;

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