package uk.gov.hmcts.opal.filehandler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebtorContactDetailsDto {

    private String telephoneNumberHome;
    private String telephoneNumberBusiness;
    private String telephoneNumberMobile;
    private String nationalInsuranceNumber;
    private String emailAddress1;
    private String emailAddress2;
    private DebtorDetailDto debtorDetail;
}
