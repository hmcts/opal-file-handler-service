package uk.gov.hmcts.opal.filehandler.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Account {

    @JsonProperty("account_type")
    private String accountType;

    @JsonProperty("defendant_type")
    private String defendantType;

    @JsonProperty("originator_name")
    private String originatorName;

    @JsonProperty("originator_id")
    private Integer originatorId;

    @JsonProperty("originator_type")
    private String originatorType;

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
    private Defendant defendant;

    @JsonProperty("offences")
    private List<Offence> offences;

    @JsonProperty("fp_ticket_detail")
    private FpTicketDetail fpTicketDetail;

    @JsonProperty("payment_terms")
    private PaymentTerms paymentTerms;

    @JsonProperty("account_notes")
    private List<AccountNote> accountNotes;
}
