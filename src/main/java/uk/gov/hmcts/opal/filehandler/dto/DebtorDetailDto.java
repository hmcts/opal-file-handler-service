package uk.gov.hmcts.opal.filehandler.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.filehandler.dto.types.LanguageType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DebtorDetailDto {

    private String vehicleMake;
    private String vehicleRegistrationMark;
    private LanguageType documentLanguage;
    private LanguageType hearingLanguage;
    private String employeeReference;
    private String employerCompanyName;
    private String employerAddressLine1;
    private String employerAddressLine2;
    private String employerAddressLine3;
    private String employerAddressLine4;
    private String employerAddressLine5;
    private String employerPostCode;
    private String employerTelephoneNumber;
    private String employerEmailAddress;
    private List<AliasDto> aliases;
}