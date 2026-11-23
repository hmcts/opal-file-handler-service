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
public class MinorCreditorDto extends CommonPersonDto {

    private TitleDto  title;
    private String telephone;
    private String emailAddress;
    private boolean payoutHold;
    private boolean payByBacs;
    private String bankAccountType;
    private String bankSortCode;
    private String bankAccountNumber;
    private String bankAccountName;
    private String bankAccountRef;

}
