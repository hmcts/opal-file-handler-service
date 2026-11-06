package uk.gov.hmcts.opal.filehandler.service.queue;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.dto.AccountDto;
import uk.gov.hmcts.opal.filehandler.dto.AccountNoteDto;
import uk.gov.hmcts.opal.filehandler.dto.AliasDto;
import uk.gov.hmcts.opal.filehandler.dto.DebtorDetailDto;
import uk.gov.hmcts.opal.filehandler.dto.DefendantDto;
import uk.gov.hmcts.opal.filehandler.dto.EnforcementDto;
import uk.gov.hmcts.opal.filehandler.dto.EnforcementResultResponseDto;
import uk.gov.hmcts.opal.filehandler.dto.FileHandlerDraftAccountPayloadDTO;
import uk.gov.hmcts.opal.filehandler.dto.FpTicketDetailDto;
import uk.gov.hmcts.opal.filehandler.dto.ImpositionDto;
import uk.gov.hmcts.opal.filehandler.dto.MinorCreditorDto;
import uk.gov.hmcts.opal.filehandler.dto.OffenceDto;
import uk.gov.hmcts.opal.filehandler.dto.ParentGuardianDto;
import uk.gov.hmcts.opal.filehandler.dto.PaymentTermsDto;
import uk.gov.hmcts.opal.filehandler.types.Enums.EnforcementResultId;
import uk.gov.hmcts.opal.filehandler.types.Enums.InstalmentPeriod;
import uk.gov.hmcts.opal.filehandler.types.Enums.LanguageCode;
import uk.gov.hmcts.opal.filehandler.types.Enums.NoteType;
import uk.gov.hmcts.opal.filehandler.types.Enums.OriginatorType;
import uk.gov.hmcts.opal.filehandler.types.Enums.PaymentTermsTypeCode;

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
    private AccountDto createAccount() {
        return AccountDto.builder()
            .accountType("Fine")
            .defendantType("Adult")
            .originatorName("LJS")
            .originatorId(1001)
            .originatorType(OriginatorType.NEW)
            .prosecutorCaseReference("PCR123456")
            .enforcementCourtId(2001)
            .collectionOrderMade(true)
            .collectionOrderMadeToday(false)
            .collectionOrderDate(LocalDate.now())
            .suspendedCommittalDate(LocalDate.now().plusDays(30))
            .paymentCardRequest(true)
            .accountSentenceDate(LocalDate.now())
            .defendant(getDefendant(getDebtorDetail(getAlias()), getParentGuardian(getDebtorDetail(getAlias()))))
            .offences(List.of(getOffence(getImposition(getMinorCreditor()))))
            .fpTicketDetail(getFpTicketDetail())
            .paymentTerms(getPaymentTerms(getEnforcement()))
            .accountNotes(List.of(getAccountNote()))
            .build();
    }

    private static EnforcementDto getEnforcement() {
        EnforcementDto enforcement = EnforcementDto.builder()
            .resultId(EnforcementResultId.PRIS)
            .enforcementResultResponses(List.of(getEnforcementResponse()))
            .build();
        return enforcement;
    }

    private static EnforcementResultResponseDto getEnforcementResponse() {
        EnforcementResultResponseDto enforcementResponse =
            EnforcementResultResponseDto.builder()
                .parameterName("Days")
                .response("30")
                .build();
        return enforcementResponse;
    }

    private static OffenceDto getOffence(ImpositionDto imposition) {
        OffenceDto offence = OffenceDto.builder()
            .dateOfSentence(LocalDate.of(2026, 1, 1))
            .imposingCourtId(456)
            .offenceId(789)
            .impositions(List.of(imposition))
            .build();
        return offence;
    }

    private static AliasDto getAlias() {
        AliasDto alias = AliasDto.builder()
            .aliasForenames("John")
            .aliasSurname("Smith")
            .aliasCompanyName("Alias Company")
            .build();
        return alias;
    }

    private static DebtorDetailDto getDebtorDetail(AliasDto alias) {
        DebtorDetailDto debtorDetail = DebtorDetailDto.builder()
            .vehicleMake("Ford")
            .vehicleRegistrationMark("AB12CDE")
            .documentLanguage(LanguageCode.EN)
            .hearingLanguage(LanguageCode.EN)
            .employeeReference("EMP001")
            .employerCompanyName("HMCTS")
            .employerAddressLine1("Employer Line 1")
            .employerAddressLine2("Employer Line 2")
            .employerAddressLine3("Employer Line 3")
            .employerAddressLine4("Employer Line 4")
            .employerAddressLine5("Employer Line 5")
            .employerPostCode("SW1A 1AA")
            .employerTelephoneNumber("01234567890")
            .employerEmailAddress("employer@test.com")
            .aliases(List.of(alias))
            .build();
        return debtorDetail;
    }

    private static ParentGuardianDto getParentGuardian(DebtorDetailDto debtorDetail) {
        ParentGuardianDto parentGuardian = ParentGuardianDto.builder()
            .companyFlag(false)
            .companyName("Guardian Ltd")
            .surname("Guardian")
            .forenames("Peter")
            .dob(LocalDate.of(1970, 1, 1))
            .nationalInsuranceNumber("QQ123456C")
            .addressLine1("Guardian Address 1")
            .addressLine2("Guardian Address 2")
            .addressLine3("Guardian Address 3")
            .addressLine4("Guardian Address 4")
            .addressLine5("Guardian Address 5")
            .postCode("GU11GU")
            .telephoneNumberHome("02070000001")
            .telephoneNumberBusiness("02070000002")
            .telephoneNumberMobile("07123456789")
            .emailAddress1("guardian@test.com")
            .emailAddress2("guardian2@test.com")
            .debtorDetail(debtorDetail)
            .build();
        return parentGuardian;
    }

    private static DefendantDto getDefendant(DebtorDetailDto debtorDetail, ParentGuardianDto parentGuardian) {
        DefendantDto defendant = DefendantDto.builder()
            .companyFlag(false)
            .title("Mr")
            .surname("Doe")
            .companyName(null)
            .forenames("John")
            .dob(LocalDate.of(1990, 1, 1))
            .addressLine1("1 High Street")
            .addressLine2("Line 2")
            .addressLine3("Line 3")
            .addressLine4("Line 4")
            .addressLine5("Line 5")
            .postCode("AB12CD")
            .telephoneNumberHome("02070000003")
            .telephoneNumberBusiness("02070000004")
            .telephoneNumberMobile("07999999999")
            .emailAddress1("john@test.com")
            .emailAddress2("john2@test.com")
            .nationalInsuranceNumber("AA123456A")
            .drivingLicenceNumber("DOEJ901010AB9CD")
            .pncId("PNC1234")
            .nationality1("British")
            .nationality2("Irish")
            .ethnicitySelfDefined("White")
            .ethnicityObserved("White")
            .croNumber("CRO123")
            .occupation("Developer")
            .gender("Male")
            .custodyStatus("N")
            .prisonNumber("A1234BC")
            .interpreterLang("EN")
            .debtorDetail(debtorDetail)
            .parentGuardian(parentGuardian)
            .build();
        return defendant;
    }

    private static ImpositionDto getImposition(MinorCreditorDto minorCreditor) {
        ImpositionDto imposition = ImpositionDto.builder()
            .resultId("FCOST")
            .amountImposed(BigDecimal.valueOf(100.00))
            .amountPaid(BigDecimal.valueOf(25.00))
            .majorCreditorId(123)
            .minorCreditor(minorCreditor)
            .build();
        return imposition;
    }

    private static MinorCreditorDto getMinorCreditor() {
        MinorCreditorDto minorCreditor = MinorCreditorDto.builder()
            .companyFlag(false)
            .title("Mrs")
            .companyName("Creditor Ltd")
            .surname("Brown")
            .forenames("Sarah")
            .dob(LocalDate.of(1980, 1, 1))
            .addressLine1("Creditor Address 1")
            .addressLine2("Address 2")
            .addressLine3("Address 3")
            .addressLine4("Address 4")
            .addressLine5("Address 5")
            .postCode("CR12CR")
            .telephone("02070000005")
            .emailAddress("creditor@test.com")
            .payoutHold(false)
            .payByBacs(true)
            .bankAccountType("CURRENT")
            .bankSortCode("112233")
            .bankAccountNumber("12345678")
            .bankAccountName("S Brown")
            .bankAccountRef("REF123")
            .build();
        return minorCreditor;
    }

    private static PaymentTermsDto getPaymentTerms(EnforcementDto enforcement) {
        PaymentTermsDto paymentTerms = PaymentTermsDto.builder()
            .paymentTermsTypeCode(PaymentTermsTypeCode.I)
            .effectiveDate(LocalDate.now())
            .instalmentPeriod(InstalmentPeriod.M)
            .lumpSumAmount(BigDecimal.valueOf(50.00))
            .instalmentAmount(BigDecimal.valueOf(25.00))
            .defaultDaysInJail(30)
            .enforcements(List.of(enforcement))
            .build();
        return paymentTerms;
    }

    private static AccountNoteDto getAccountNote() {
        AccountNoteDto accountNote = AccountNoteDto.builder()
            .accountNoteSerial(1)
            .accountNoteText("Test note")
            .noteType(NoteType.AC)
            .build();
        return accountNote;
    }

    private static FpTicketDetailDto getFpTicketDetail() {
        FpTicketDetailDto fpTicketDetail = FpTicketDetailDto.builder()
            .noticeNumber("NOTICE123")
            .dateOfIssue(LocalDate.now())
            .timeOfIssue("09:30")
            .fpRegistrationNumber("REG123")
            .noticeToOwnerHirer("Owner")
            .placeOfOffence("London")
            .fpDrivingLicenceNumber("DOEJ901010AB9CD")
            .build();
        return fpTicketDetail;
    }
}