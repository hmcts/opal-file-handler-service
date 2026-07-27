package uk.gov.hmcts.opal.filehandler.service.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.opal.filehandler.testutil.StringTestUtil.leftPad;
import static uk.gov.hmcts.opal.filehandler.testutil.StringTestUtil.put;
import static uk.gov.hmcts.opal.filehandler.testutil.StringTestUtil.rightPad;

import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.StreamUtil;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.BankDetails;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.InterfaceFileCommonDataExtract;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.OriginatorDetails;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.Transaction;
import uk.gov.hmcts.opal.filehandler.testutil.StreamTestUtil;

class BacsStandard18BaisExtractionServiceTest {

    private static final String FILE_NAME = "a121_00350005_300000.dat";
    private static final String MIXED_DESTINATION_OR_TRANSACTION_CODES_MESSAGE =
        "BACS Standard 18 transaction rows contained mixed destination bank details or mixed transaction codes";

    private final InterfaceFilesRepository repository = mock(InterfaceFilesRepository.class);
    private final BacsStandard18BaisExtractionService service = new BacsStandard18BaisExtractionService(repository);
    private static final BankDetails DESTINATION = destinationBankDetails("560033", "27048527", "Beneficiary Name 1");

    @Nested
    class ExtractStandardData {

        private final BacsStandard18BaisExtractionService extractionService = spy(
            new BacsStandard18BaisExtractionService(repository)
        );

        @Test
        void shouldBuildExtractFromParsedTransactionRows() {
            InterfaceFileEntity sourceFile = sourceFile(Interface.ALLPAY);
            List<String> lines = List.of("VOL1", "", "DATA1", "EOF1", "DATA2");
            BacsStandard18BaisExtractionService.ParsedTransaction first = parsedTransaction("99", DESTINATION);
            BacsStandard18BaisExtractionService.ParsedTransaction second = parsedTransaction("99", DESTINATION);
            stubSuccessfulExtraction(lines, first, second);
            doReturn(InterfaceFileCommonDataExtract.PaymentType.CASH).when(extractionService).paymentTypeFor("99");

            try (MockedStatic<StreamUtil> streamUtil = mockStatic(StreamUtil.class)) {
                streamUtil.when(() -> StreamUtil.readLines(any())).thenReturn(lines);

                List<InterfaceFileCommonDataExtract> extracts = extractionService.extractStandardData(
                    sourceFile,
                    StreamTestUtil.stream("ignored")
                );

                assertThat(extracts).hasSize(1);
                InterfaceFileCommonDataExtract extract = extracts.getFirst();
                assertThat(extract.getFileName()).isEqualTo(FILE_NAME);
                assertThat(extract.getDestinationDetails().getBankDetails()).isEqualTo(DESTINATION);
                assertThat(extract.getPaymentType()).isEqualTo(InterfaceFileCommonDataExtract.PaymentType.CASH);
                assertThat(extract.getTransactions()).containsExactly(first.transaction(), second.transaction());
                verify(extractionService).validateInputs(eq(sourceFile), any());
                streamUtil.verify(() -> StreamUtil.readLines(any()));
                verify(extractionService).validateHeaders(lines);
                verify(extractionService).validateConsistentDestinationDetails(List.of(first, second), first);
                verify(extractionService).applyAllpayDdSourceUpdate(sourceFile, first.transaction());
            }
        }

        @Test
        void shouldMarkSourceFileSuccessfulWhenFileHasNoTransactionRows() {
            InterfaceFileEntity sourceFile = sourceFile(Interface.ALLPAY);
            List<String> lines = List.of("VOL1", "", "EOF1");
            doNothing().when(extractionService).validateInputs(eq(sourceFile), any());
            doNothing().when(extractionService).validateHeaders(anyList());
            doReturn(true).when(extractionService).isControlRow("VOL1");
            doReturn(true).when(extractionService).isControlRow("EOF1");

            try (MockedStatic<StreamUtil> streamUtil = mockStatic(StreamUtil.class)) {
                streamUtil.when(() -> StreamUtil.readLines(any())).thenReturn(lines);

                List<InterfaceFileCommonDataExtract> extracts = extractionService.extractStandardData(
                    sourceFile,
                    StreamTestUtil.stream("ignored")
                );

                assertThat(extracts).isEmpty();
                assertThat(sourceFile.getStatus()).isEqualTo(Status.SUCCESS_NO_TRANSACTIONS);
                verify(repository).save(sourceFile);
                streamUtil.verify(() -> StreamUtil.readLines(any()));
                verify(extractionService, never()).parseTransaction(any());
            }
        }

        @Test
        void shouldFilterBlankAndControlRowsBeforeParsingTransactions() {
            BacsStandard18BaisExtractionService.ParsedTransaction transaction = parsedTransaction("99", DESTINATION);
            List<String> lines = List.of("VOL1", "", "DATA1", "EOF1");
            stubSuccessfulExtraction(lines, transaction);

            try (MockedStatic<StreamUtil> streamUtil = mockStatic(StreamUtil.class)) {
                streamUtil.when(() -> StreamUtil.readLines(any())).thenReturn(lines);

                extractionService.extractStandardData(sourceFile(Interface.ALLPAY), StreamTestUtil.stream("ignored"));
            }

            verify(extractionService, never()).isControlRow("");
            verify(extractionService, never()).parseTransaction("VOL1");
            verify(extractionService, never()).parseTransaction("EOF1");
            verify(extractionService).parseTransaction("DATA1");
        }

        private void stubSuccessfulExtraction(
            List<String> lines,
            BacsStandard18BaisExtractionService.ParsedTransaction first,
            BacsStandard18BaisExtractionService.ParsedTransaction... additional
        ) {
            doNothing().when(extractionService).validateInputs(any(), any());
            doNothing().when(extractionService).validateHeaders(lines);
            lines.stream().filter(line -> !line.isBlank()).forEach(line ->
                doReturn(line.startsWith("VOL") || line.startsWith("EOF")).when(extractionService).isControlRow(line));
            doReturn(first).when(extractionService).parseTransaction("DATA1");
            if (additional.length > 0) {
                doReturn(additional[0]).when(extractionService).parseTransaction("DATA2");
            }
            List<BacsStandard18BaisExtractionService.ParsedTransaction> transactions = Stream.concat(
                Stream.of(first),
                Stream.of(additional)
            ).toList();
            doNothing().when(extractionService).validateConsistentDestinationDetails(transactions, first);
            doNothing().when(extractionService).applyAllpayDdSourceUpdate(any(), eq(first.transaction()));
            doReturn(InterfaceFileCommonDataExtract.PaymentType.CASH).when(extractionService).paymentTypeFor("99");
        }
    }

    @Nested
    class ValidateInputs {

        @Test
        void shouldAcceptSourceFileWithNameAndContents() {
            assertThatCode(() -> service.validateInputs(
                sourceFile(Interface.ALLPAY),
                StreamTestUtil.stream()
            )).doesNotThrowAnyException();
        }

        @Test
        void shouldRejectNullSourceFile() {
            assertThatThrownBy(() -> service.validateInputs(null, StreamTestUtil.stream()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source interface file is required");
        }

        @Test
        void shouldRejectBlankSourceFileName() {
            InterfaceFileEntity sourceFile = sourceFile(Interface.ALLPAY);
            sourceFile.setFileName(" ");

            assertThatThrownBy(
                () -> service.validateInputs(sourceFile, StreamTestUtil.stream()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source interface file name is required");
        }

        @Test
        void shouldRejectNullSourceFileName() {
            InterfaceFileEntity sourceFile = mock(InterfaceFileEntity.class);
            when(sourceFile.getFileName()).thenReturn(null);

            assertThatThrownBy(
                () -> service.validateInputs(sourceFile, StreamTestUtil.stream()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source interface file name is required");
        }

        @Test
        void shouldRejectNullFileContents() {
            assertThatThrownBy(() -> service.validateInputs(
                sourceFile(Interface.ALLPAY),
                null
            )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BACS Standard 18 file contents are required");
        }
    }

    @Nested
    class ValidateHeaders {

        @Test
        void shouldAcceptFileContainingRequiredHeaders() {
            assertThatCode(() -> service.validateHeaders(
                List.of("VOL1", "HDR1", "UHL1")
            )).doesNotThrowAnyException();
        }

        @Test
        void shouldRejectFileMissingRequiredHeader() {
            assertThatThrownBy(() -> service.validateHeaders(
                List.of("VOL1", "HDR1")
            )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expected headers");
        }
    }

    @Nested
    class IsControlRow {

        @ParameterizedTest
        @ValueSource(strings = {"VOL1BACSSTANDARD18", "HDR1BACSSTANDARD18", "HDR2BACSSTANDARD18", "UHL1BACSSTANDARD18",
            "EOF1BACSSTANDARD18", "EOF2BACSSTANDARD18", "UTL1BACSSTANDARD18", "CTL 1BACSSTANDARD18"})
        void shouldIdentifyKnownControlRows(String text) {
            assertThat(service.isControlRow(text)).isTrue();
        }

        @Test
        void shouldIgnoreTransactionRows() {
            assertThat(service.isControlRow("56003327048527")).isFalse();
        }

        @Test
        void shouldIgnoreShortRows() {
            assertThat(service.isControlRow("EOF")).isFalse();
        }
    }

    @Nested
    class ParseTransaction {

        private final BacsStandard18BaisExtractionService service = new BacsStandard18BaisExtractionService(
            mock(InterfaceFilesRepository.class)
        );

        @Test
        void shouldParseOneHundredAndSixCharacterTransactionRow() {
            String transactionRow =
                transactionRow("99", "123456", "99887766", "1500", "Mrs D Richardson", "08000066I", true);
            BacsStandard18BaisExtractionService.ParsedTransaction parsed = service.parseTransaction(transactionRow);
            assertThat(parsed.destinationBankDetails().getSortCode()).isEqualTo("560033");
            assertThat(parsed.destinationBankDetails().getAccountNumber()).isEqualTo("27048527");
            assertThat(parsed.destinationBankDetails().getType()).isEqualTo("0");
            assertThat(parsed.destinationBankDetails().getName()).isEqualTo("Beneficiary Name 1");
            assertThat(parsed.transaction().getTransactionCode()).isEqualTo("99");

            assertThat(parsed.transaction().getAmount()).isEqualTo(1500L);
            assertThat(parsed.transaction().getDateEntryApplied()).isEqualTo("01/06/2026");

            assertThat(parsed.transaction().getOriginatorDetails().getName()).isEqualTo("Mrs D Richardson");
            assertThat(parsed.transaction().getOriginatorDetails().getAccountReference()).isEqualTo("08000066I");
            assertThat(parsed.transaction().getOriginatorDetails().getBankDetails().getSortCode()).isEqualTo("123456");
            assertThat(parsed.transaction().getOriginatorDetails().getBankDetails().getAccountNumber()).isEqualTo(
                "99887766");
        }

        @Test
        void shouldOmitDateForOneHundredCharacterTransactionRow() {
            String transactionRow =
                transactionRow("99", "123456", "99887766", "1500", "Mrs D Richardson", "08000066I", false);
            BacsStandard18BaisExtractionService.ParsedTransaction parsed = service.parseTransaction(transactionRow);
            assertThat(parsed.transaction().getDateEntryApplied()).isNull();
        }

        @Test
        void shouldTrimBlankOriginatorNameToEmptyText() {
            String transactionRow =
                transactionRow("99", "123456", "99887766", "1500", "                  ", "08000066I", true);
            BacsStandard18BaisExtractionService.ParsedTransaction parsed = service.parseTransaction(transactionRow);
            assertThat(parsed.transaction().getOriginatorDetails().getName()).isEmpty();
        }

        @Test
        void shouldRejectDestinationAccountTypeOtherThanZero() {
            assertThatThrownBy(() -> service.parseTransaction(transactionRowWithReplacement(14, "1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BACS Standard 18 destination account type was 1 but expected 0");
        }
    }

    @Nested
    class ValidateTransactionLength {

        @Test
        void shouldAcceptSupportedTransactionLengths() {
            assertThatCode(() -> service.validateTransactionLength(" ".repeat(100)))
                .doesNotThrowAnyException();
            assertThatCode(() -> service.validateTransactionLength(" ".repeat(106)))
                .doesNotThrowAnyException();
        }

        @Test
        void shouldRejectUnsupportedTransactionLength() {
            assertThatThrownBy(() -> service.validateTransactionLength("12345"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BACS Standard 18 transaction line was 5 characters but expected 100 or 106");
        }
    }

    @Nested
    class NumericIdentifier {

        @Test
        void shouldExtractNumericIdentifierWithoutTrimming() {
            assertThat(service.numericIdentifier("123456", 0, 6, "sort code"))
                .isEqualTo("123456");
        }

        @Test
        void shouldRejectBlankIdentifier() {
            assertThatThrownBy(() -> service.numericIdentifier(
                "      ",
                0,
                6,
                "sort code"
            )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BACS Standard 18 sort code at index 0 to 6 must be numeric");
        }

        @Test
        void shouldRejectNonNumericIdentifier() {
            assertThatThrownBy(() -> service.numericIdentifier(
                "12345A",
                0,
                6,
                "sort code"
            )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BACS Standard 18 sort code at index 0 to 6 must be numeric");
        }
    }

    @Nested
    class PaddedNumericText {

        @Test
        void shouldTrimPaddedNumericText() {
            assertThat(service.paddedNumericText("     1500", 0, 9, "amount"))
                .isEqualTo("1500");
        }

        @Test
        void shouldRejectBlankPaddedNumericText() {
            assertThatThrownBy(() -> service.paddedNumericText(
                "         ",
                0,
                9,
                "amount"
            )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BACS Standard 18 amount at index 0 to 9 must be numeric");
        }

        @Test
        void shouldRejectNonNumericPaddedNumericText() {
            assertThatThrownBy(() -> service.paddedNumericText(
                "     ABCD",
                0,
                9,
                "amount"
            )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BACS Standard 18 amount at index 0 to 9 must be numeric");
        }
    }

    @Nested
    class RequiredText {

        @Test
        void shouldTrimRequiredText() {
            assertThat(service.requiredText("  ALLPAY DD  ", 0, 12, "originator name"))
                .isEqualTo("ALLPAY DD");
        }

        @Test
        void shouldRejectBlankRequiredText() {
            assertThatThrownBy(() -> service.requiredText(
                "            ",
                0,
                12,
                "originator name"
            )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BACS Standard 18 originator name is required at index 0 to 12");
        }
    }

    @Nested
    class Text {

        @Test
        void shouldTrimText() {
            assertThat(service.text("  Beneficiary Name 1  ", 0, 22))
                .isEqualTo("Beneficiary Name 1");
        }
    }

    @Nested
    class ParseAmount {

        @Test
        void shouldParseAmountInPence() {
            assertThat(service.parseAmount("1500")).isEqualTo(1500L);
        }

        @Test
        void shouldRejectAmountTooLargeForLong() {
            assertThatThrownBy(() -> service.parseAmount("999999999999999999999"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BACS Standard 18 amount is too large");
        }
    }

    @Nested
    class ParseDateEntryApplied {

        @Test
        void shouldParseBlankPrefixedJulianDate() {
            assertThat(service.parseDateEntryApplied(" 26152")).isEqualTo("01/06/2026");
        }

        @Test
        void shouldRejectNonBlankDatePrefix() {
            assertThatThrownBy(() -> service.parseDateEntryApplied("X26152"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BACS Standard 18 transaction date must be blank followed by YYDDD");
        }

        @Test
        void shouldRejectNonNumericOrdinal() {
            assertThatThrownBy(() -> service.parseDateEntryApplied(" 26ABC"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BACS Standard 18 transaction date ordinal must be numeric");
        }

        @Test
        void shouldRejectInvalidOrdinal() {
            assertThatThrownBy(() -> service.parseDateEntryApplied(" 26367"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("BACS Standard 18 transaction date ordinal is invalid");
        }
    }

    @Nested
    class PaymentTypeFor {

        @Test
        void shouldReturnChequeForTransactionCodeEleven() {
            assertThat(service.paymentTypeFor("11"))
                .isEqualTo(InterfaceFileCommonDataExtract.PaymentType.CHEQUE);
        }

        @Test
        void shouldReturnCashForOtherTransactionCodes() {
            assertThat(service.paymentTypeFor("99"))
                .isEqualTo(InterfaceFileCommonDataExtract.PaymentType.CASH);
        }
    }

    @Nested
    class ApplyAllpayDdSourceUpdate {

        private final InterfaceFilesRepository repository = mock(InterfaceFilesRepository.class);
        private final BacsStandard18BaisExtractionService service = new BacsStandard18BaisExtractionService(repository);

        @Test
        void shouldUpdateAndSaveAllpayDdSourceWhenFirstOriginatorNameMatches() {
            InterfaceFileEntity sourceFile = sourceFile(Interface.ALLPAY);

            service.applyAllpayDdSourceUpdate(sourceFile, transaction("99", "ALLPAY DD"));

            assertThat(sourceFile.getSource()).isEqualTo(Interface.ALLPAY_DD);
            verify(repository).save(sourceFile);
        }

        @Test
        void shouldLeaveSourceUnchangedWhenOriginatorNameDoesNotMatch() {
            InterfaceFileEntity sourceFile = sourceFile(Interface.ALLPAY);

            service.applyAllpayDdSourceUpdate(sourceFile, transaction("99", "Mrs D Richardson"));

            assertThat(sourceFile.getSource()).isEqualTo(Interface.ALLPAY);
            verify(repository, never()).save(sourceFile);
        }

        @Test
        void shouldLeaveSourceUnchangedWhenSourceNotAllPay() {
            InterfaceFileEntity sourceFile = sourceFile(Interface.BTECKOH);

            service.applyAllpayDdSourceUpdate(sourceFile, transaction("99", "ALLPAY DD"));

            assertThat(sourceFile.getSource()).isEqualTo(Interface.BTECKOH);
            verify(repository, never()).save(sourceFile);
        }
    }

    @Nested
    class ValidateConsistentDestinationDetails {

        @Test
        void shouldAcceptConsistentDestinationDetailsAndTransactionCodes() {
            BacsStandard18BaisExtractionService.ParsedTransaction expected = parsedTransaction("99", DESTINATION);

            assertThatCode(() -> service.validateConsistentDestinationDetails(
                List.of(expected, parsedTransaction("99", DESTINATION)),
                expected
            )).doesNotThrowAnyException();
        }

        @Test
        void shouldRejectMixedDestinationDetails() {
            BacsStandard18BaisExtractionService.ParsedTransaction expected = parsedTransaction("99", DESTINATION);

            assertThatThrownBy(() -> service.validateConsistentDestinationDetails(
                List.of(
                    expected,
                    parsedTransaction("99", destinationBankDetails("560033", "27048527", "Beneficiary Name 2"))
                ),
                expected
            )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MIXED_DESTINATION_OR_TRANSACTION_CODES_MESSAGE);
        }

        @Test
        void shouldRejectMixedTransactionCodes() {
            BacsStandard18BaisExtractionService.ParsedTransaction expected = parsedTransaction("99", DESTINATION);

            assertThatThrownBy(() -> service.validateConsistentDestinationDetails(
                List.of(expected, parsedTransaction("11", DESTINATION)),
                expected
            )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MIXED_DESTINATION_OR_TRANSACTION_CODES_MESSAGE);
        }
    }

    @Nested
    class HasExpectedDestinationDetails {

        @Test
        void shouldReturnTrueWhenDestinationDetailsMatch() {
            BankDetails destination = destinationBankDetails("560033", "27048527", "Beneficiary Name 1");

            assertThat(service.hasExpectedDestinationDetails(
                parsedTransaction("99", destination),
                destination
            )).isTrue();
        }

        @Test
        void shouldReturnFalseWhenDestinationDetailsDiffer() {
            assertThat(service.hasExpectedDestinationDetails(
                parsedTransaction("99", destinationBankDetails("560033", "27048527", "Beneficiary Name 2")),
                destinationBankDetails("560033", "27048527", "Beneficiary Name 1")
            )).isFalse();
        }
    }


    private static InterfaceFileEntity sourceFile(Interface source) {
        return InterfaceFileEntity.builder()
            .interfaceFileId(123L)
            .source(source)
            .target(Interface.OPAL)
            .type(Type.SOURCE)
            .opalDomain(Domain.FINES)
            .fileName(FILE_NAME)
            .status(Status.INGESTED)
            .createdDatetime(new Date())
            .build();
    }

    private static BankDetails destinationBankDetails(String sortCode, String accountNumber, String name) {
        return BankDetails.builder()
            .sortCode(sortCode)
            .accountNumber(accountNumber)
            .name(name)
            .type("0")
            .build();
    }

    private static Transaction transaction(String transactionCode, String originatorName) {
        return Transaction.builder()
            .transactionCode(transactionCode)
            .originatorDetails(OriginatorDetails.builder().name(originatorName).build())
            .build();
    }

    private static BacsStandard18BaisExtractionService.ParsedTransaction parsedTransaction(
        String transactionCode,
        BankDetails destinationBankDetails
    ) {
        return new BacsStandard18BaisExtractionService.ParsedTransaction(
            transaction(transactionCode, "Mrs D Richardson"),
            destinationBankDetails
        );
    }

    private static String transactionRow(
        String transactionCode,
        String originatorSortCode,
        String originatorAccountNumber,
        String amount,
        String originatorName,
        String accountReference,
        boolean includeDate
    ) {
        int length = includeDate ? 106 : 100;
        StringBuilder row = new StringBuilder(" ".repeat(length));
        put(row, 0, "560033");
        put(row, 6, "27048527");
        put(row, 14, "0");
        put(row, 15, transactionCode);
        put(row, 17, originatorSortCode);
        put(row, 23, originatorAccountNumber);
        put(row, 35, leftPad(amount, 11));
        put(row, 46, rightPad(originatorName, 18));
        put(row, 64, rightPad(accountReference, 18));
        put(row, 82, rightPad("Beneficiary Name 1", 18));
        if (includeDate) {
            put(row, 100, " 26152");
        }
        return row.toString();
    }

    private static String transactionRowWithReplacement(int startInclusive, String replacement) {
        StringBuilder row = new StringBuilder(
            transactionRow("99", "123456", "99887766", "1500", "Mrs D Richardson", "08000066I", true)
        );
        row.replace(startInclusive, startInclusive + replacement.length(), replacement);
        return row.toString();
    }
}
