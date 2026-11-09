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
public class MinorCreditorDto extends PersonAddressDto {

    @JsonProperty("telephone")
    private String telephone;

    @JsonProperty("email_address")
    private String emailAddress;

    @JsonProperty("payout_hold")
    private Boolean payoutHold;

    @JsonProperty("pay_by_bacs")
    private Boolean payByBacs;

    @JsonProperty("bank_account_type")
    private String bankAccountType;

    @JsonProperty("bank_sort_code")
    private String bankSortCode;

    @JsonProperty("bank_account_number")
    private String bankAccountNumber;

    @JsonProperty("bank_account_name")
    private String bankAccountName;

    @JsonProperty("bank_account_ref")
    private String bankAccountRef;

}
