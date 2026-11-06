package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DefendantDto {

    @JsonProperty("company_flag")
    private Boolean companyFlag;

    @JsonProperty("title")
    private String title;

    @JsonProperty("surname")
    private String surname;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("forenames")
    private String forenames;

    @JsonProperty("dob")
    private LocalDate dob;

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

    @JsonProperty("national_insurance_number")
    private String nationalInsuranceNumber;

    @JsonProperty("driving_licence_number")
    private String drivingLicenceNumber;

    @JsonProperty("pnc_id")
    private String pncId;

    @JsonProperty("nationality_1")
    private String nationality1;

    @JsonProperty("nationality_2")
    private String nationality2;

    @JsonProperty("ethnicity_self_defined")
    private String ethnicitySelfDefined;

    @JsonProperty("ethnicity_observed")
    private String ethnicityObserved;

    @JsonProperty("cro_number")
    private String croNumber;

    @JsonProperty("occupation")
    private String occupation;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("custody_status")
    private String custodyStatus;

    @JsonProperty("prison_number")
    private String prisonNumber;

    @JsonProperty("interpreter_lang")
    private String interpreterLang;

    @JsonProperty("debtor_detail")
    private DebtorDetailDto debtorDetail;

    @JsonProperty("parent_guardian")
    private ParentGuardianDto parentGuardian;
}