package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.filehandler.types.Enums.OriginatorType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {

    @JsonProperty("account_type")
    private String accountType;

    @JsonProperty("defendant_type")
    private String defendantType;

    @JsonProperty("originator_name")
    private String originatorName;

    @JsonProperty("originator_id")
    private Integer originatorId;

    @JsonProperty("originator_type")
    private OriginatorType originatorType;

    @JsonProperty("prosecutor_case_reference")
    private String prosecutorCaseReference;

    @JsonProperty("enforcement_court_id")
    private Integer enforcementCourtId;

    @JsonProperty("collection_order_made")
    private Boolean collectionOrderMade;

    @JsonProperty("collection_order_made_today")
    private Boolean collectionOrderMadeToday;

    @JsonProperty("collection_order_date")
    private LocalDate collectionOrderDate;

    @JsonProperty("suspended_committal_date")
    private LocalDate suspendedCommittalDate;

    @JsonProperty("payment_card_request")
    private Boolean paymentCardRequest;

    @JsonProperty("account_sentence_date")
    private LocalDate accountSentenceDate;

    @JsonProperty("defendant")
    private DefendantDto defendant;

    @JsonProperty("offences")
    private List<OffenceDto> offences;

    @JsonProperty("fp_ticket_detail")
    private FpTicketDetailDto fpTicketDetail;

    @JsonProperty("payment_terms")
    private PaymentTermsDto paymentTerms;

    @JsonProperty("account_notes")
    private List<AccountNoteDto> accountNotes;
}