package uk.gov.hmcts.opal.filehandler.dto;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ParentGuardianDto extends CommonPersonDto {

    private DebtorContactDetailsDto contactDetails;
}