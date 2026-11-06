package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FpTicketDetailDto {

    @JsonProperty("notice_number")
    private String noticeNumber;

    @JsonProperty("date_of_issue")
    private LocalDate dateOfIssue;

    @JsonProperty("time_of_issue")
    private String timeOfIssue;

    @JsonProperty("fp_registration_number")
    private String fpRegistrationNumber;

    @JsonProperty("notice_to_owner_hirer")
    private String noticeToOwnerHirer;

    @JsonProperty("place_of_offence")
    private String placeOfOffence;

    @JsonProperty("fp_driving_licence_number")
    private String fpDrivingLicenceNumber;
}