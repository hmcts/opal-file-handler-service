package uk.gov.hmcts.opal.filehandler.dto;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class CommonPersonDto {

    private Boolean companyFlag;
    private String companyName;
    private String surname;
    private String forenames;
    private LocalDate dob;
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String addressLine4;
    private String addressLine5;
    private String postCode;
    private String telephoneNumberHome;
    private String telephoneNumberBusiness;
    private String telephoneNumberMobile;
    private String emailAddress1;
    private String emailAddress2;
    private DebtorDetailDto debtorDetail;
    private String nationalInsuranceNumber;

}
