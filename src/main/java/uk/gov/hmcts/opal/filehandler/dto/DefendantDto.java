package uk.gov.hmcts.opal.filehandler.dto;

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
public class DefendantDto extends CommonPersonDto {


    private String pncId;
    private String nationality1;
    private String nationality2;
    private String ethnicitySelfDefined;
    private String ethnicityObserved;
    private String croNumber;
    private String occupation;
    private String custodyStatus;
    private String prisonNumber;
    private String interpreterLang;
    private ParentGuardianDto parentGuardian;
}