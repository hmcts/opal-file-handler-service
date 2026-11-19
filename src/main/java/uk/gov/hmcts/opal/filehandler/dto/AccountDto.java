package uk.gov.hmcts.opal.filehandler.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {

    private String accountType;
    private String defendantType;
    private String originatorName;
    private Integer originatorId;
    private OriginatorType originatorType;
    private String prosecutorCaseReference;
    private Integer enforcementCourtId;
    private Boolean collectionOrderMade;
    private Boolean collectionOrderMadeToday;
    private LocalDate collectionOrderDate;
    private LocalDate suspendedCommittalDate;
    private Boolean paymentCardRequest;
    private LocalDate accountSentenceDate;
    private DefendantDto defendant;
    private List<OffenceDto> offences;
    private FpTicketDetailDto fpTicketDetail;
    private PaymentTermsDto paymentTerms;
    private List<AccountNoteDto> accountNotes;
}