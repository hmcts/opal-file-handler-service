package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MinorCreditorDto {

    @JsonProperty("company_flag")
    private Boolean companyFlag;

    @JsonProperty("title")
    private String title;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("surname")
    private String surname;

    @JsonProperty("forenames")
    private String forenames;

    @JsonProperty("dob")
    private LocalDate dob;

    @JsonProperty("address_line_1")
    private String addressLine1;

    @JsonProperty("address_line_2")
    private String addressLine2;

    @JsonProperty("address_line_3")
    private String addressLine3;

    @JsonProperty("address_line_4")
    private String addressLine4;

    @JsonProperty("address_line_5")
    private String addressLine5;

    @JsonProperty("post_code")
    private String postCode;

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