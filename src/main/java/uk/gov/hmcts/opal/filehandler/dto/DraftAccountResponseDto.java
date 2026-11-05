package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DraftAccountResponseDto {

    @JsonProperty("draft_account_id")
    private Long draftAccountId;

    @JsonProperty("business_unit_id")
    private Short businessUnitId;

    @JsonProperty("submitted_by")
    private String submittedBy;

    @JsonProperty("submitted_by_name")
    private String submittedByName;

    @JsonProperty("account")
    private String account;

    @JsonProperty("account_snapshot")
    private String accountSnapshot;

    @JsonProperty("status_message")
    private String statusMessage;

    @JsonProperty("timeline_data")
    private String timelineData;

    @JsonProperty("account_number")
    private String accountNumber;

    @JsonProperty("account_id")
    private Long accountId;
}