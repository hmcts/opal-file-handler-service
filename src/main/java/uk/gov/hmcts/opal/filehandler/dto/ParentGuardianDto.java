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
public class ParentGuardianDto extends CommonPersonDto {

    private String telephoneNumberHome;
    private String telephoneNumberBusiness;
    private String telephoneNumberMobile;
    private String nationalInsuranceNumber;
    private String emailAddress1;
    private String emailAddress2;
    private DebtorDetailDto debtorDetail;

}