package uk.gov.hmcts.opal.filehandler.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FpTicketDetailDto {

    private String noticeNumber;
    private LocalDate dateOfIssue;
    private String timeOfIssue;
    private String fpRegistrationNumber;
    private String noticeToOwnerHirer;
    private String placeOfOffence;
    private String fpDrivingLicenceNumber;

}