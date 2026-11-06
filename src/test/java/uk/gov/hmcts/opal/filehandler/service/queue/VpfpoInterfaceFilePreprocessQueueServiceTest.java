package uk.gov.hmcts.opal.filehandler.service.queue;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.dto.Account;
import uk.gov.hmcts.opal.filehandler.dto.AccountNote;
import uk.gov.hmcts.opal.filehandler.dto.Defendant;
import uk.gov.hmcts.opal.filehandler.dto.FileHandlerDraftAccountPayloadDTO;
import uk.gov.hmcts.opal.filehandler.dto.FpTicketDetail;
import uk.gov.hmcts.opal.filehandler.dto.InstalmentPeriod;
import uk.gov.hmcts.opal.filehandler.dto.Offence;
import uk.gov.hmcts.opal.filehandler.dto.PaymentTerms;
import uk.gov.hmcts.opal.filehandler.dto.PaymentTermsType;
import uk.gov.hmcts.opal.filehandler.dto.PostedDetails;

@ExtendWith(MockitoExtension.class)
class VpfpoInterfaceFilePreprocessQueueServiceTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private VpfpoInterfaceFilePreprocessQueueService service;

    @BeforeEach
    void setUp() {

        service = new VpfpoInterfaceFilePreprocessQueueService(jmsTemplate,"vpfpo-queue", objectMapper);

    }

    @Test
    void shouldSendPayloadToQueue() throws Exception {

        FileHandlerDraftAccountPayloadDTO payload =
            FileHandlerDraftAccountPayloadDTO.builder()
                .account(createAccount())
                .prosecutorCode("ABC")
                .build();

        String serialisedPayload =
            "{\"accountJson\":\"{\\\"accountNumber\\\":\\\"12345\\\"}\",\"prosecutorCode\":\"ABC\"}";

        when(objectMapper.writeValueAsString(payload)).thenReturn(serialisedPayload);
        service.send(payload);
        verify(jmsTemplate).convertAndSend("vpfpo-queue", serialisedPayload);
    }

    private Account createAccount() {
        return Account.builder()
            .accountType("FINE")
            .defendantType("ADULT")
            .originatorName("LJS")
            .originatorId(123)
            .originatorType("NEW")
            .prosecutorCaseReference("CASE123")
            .enforcementCourtId(456)
            .collectionOrderMade(true)
            .collectionOrderMadeToday(false)
            .collectionOrderDate(LocalDate.of(2024, 1, 1))
            .suspendedCommittalDate(LocalDate.of(2024, 2, 1))
            .paymentCardRequest(true)
            .accountSentenceDate(LocalDate.of(2024, 3, 1))
            .defendant(createDefendantStub())
            .offences(List.of(createOffenceStub()))
            .fpTicketDetail(createFpTicketDetailStub())
            .paymentTerms(createPaymentTermsStub())
            .accountNotes(List.of(new AccountNote()))
            .build();
    }

    private Defendant createDefendantStub() {
        return Defendant.builder()
            .defendantAccountId("DEF123")
            .organisation(false)
            .organisationName(null)
            .firstnames("John")
            .surname("Smith")
            .build();
    }

    private Offence createOffenceStub() {
        return Offence.builder()
            .offenceId(1001L)
            .cjsCode("CJ001")
            .offenceTitle("Speeding")
            .build();
    }

    private FpTicketDetail createFpTicketDetailStub() {
        return FpTicketDetail.builder()
            .noticeNumber("NOTICE123")
            .dateOfIssue(LocalDate.of(2024, 1, 15))
            .timeOfIssue("10:30")
            .fpRegistrationNumber("FP123456")
            .noticeToOwnerHirer("N")
            .placeOfOffence("Manchester")
            .fpDrivingLicenceNumber("SMITH123456789")
            .build();
    }

    private PaymentTerms createPaymentTermsStub() {
        return PaymentTerms.builder()
            .daysInDefault(14)
            .dateDaysInDefaultImposed(LocalDate.of(2024, 3, 1))
            .extension(false)
            .reasonForExtension(null)
            .paymentTermsType(createPaymentTermsTypeStub())
            .effectiveDate(LocalDate.of(2024, 3, 15))
            .instalmentPeriod(createInstalmentPeriodStub())
            .lumpSumAmount(BigDecimal.valueOf(250.00))
            .instalmentAmount(BigDecimal.valueOf(25.00))
            .postedDetails(createPostedDetailsStub())
            .build();
    }

    private PaymentTermsType createPaymentTermsTypeStub() {
        return PaymentTermsType.builder()
            .effectiveDate(LocalDate.of(2024, 3, 15))
            .instalmentAmount(BigDecimal.valueOf(25.00))
            .build();
    }

    private InstalmentPeriod createInstalmentPeriodStub() {
        return InstalmentPeriod.builder()
            .paymentTermsTypeCode("W")
            .build();
    }

    private PostedDetails createPostedDetailsStub() {
        return PostedDetails.builder()
            .postedDate(LocalDateTime.of(2024, 3, 10, 12, 0))
            .postedBy("test.user")
            .postedByName("Test User")
            .build();
    }

}