package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.filehandler.types.FileHandlerTypes;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DebtorDetailDto {

    @JsonProperty("vehicle_make")
    private String vehicleMake;

    @JsonProperty("vehicle_registration_mark")
    private String vehicleRegistrationMark;

    @JsonProperty("document_language")
    private FileHandlerTypes documentLanguage;

    @JsonProperty("hearing_language")
    private FileHandlerTypes hearingLanguage;

    @JsonProperty("employee_reference")
    private String employeeReference;

    @JsonProperty("employer_company_name")
    private String employerCompanyName;

    @JsonProperty("employer_address_line_1")
    private String employerAddressLine1;

    @JsonProperty("employer_address_line_2")
    private String employerAddressLine2;

    @JsonProperty("employer_address_line_3")
    private String employerAddressLine3;

    @JsonProperty("employer_address_line_4")
    private String employerAddressLine4;

    @JsonProperty("employer_address_line_5")
    private String employerAddressLine5;

    @JsonProperty("employer_post_code")
    private String employerPostCode;

    @JsonProperty("employer_telephone_number")
    private String employerTelephoneNumber;

    @JsonProperty("employer_email_address")
    private String employerEmailAddress;

    @JsonProperty("aliases")
    private List<AliasDto> aliases;
}