package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DefendantDto extends PersonAddressDto {

    @JsonProperty("title")
    private String title;

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