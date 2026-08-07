package uk.gov.hmcts.opal.filehandler.service.extraction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.generated.pacs.PacsTppSchedule;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.InterfaceFileCommonDataExtract;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.Transaction;
import uk.gov.hmcts.opal.filehandler.testutil.StreamTestUtil;
import uk.gov.hmcts.opal.filehandler.utils.XmlSchemaUnmarshalService;

class PacsTTPBaisExtractionServiceTest {

    private static final String FILE_NAME = "0000015232_dat_0000000612_08011008_111355.txt";

    private final InterfaceFilesRepository repository = mock(InterfaceFilesRepository.class);
    private final XmlSchemaUnmarshalService xmlService = mock(XmlSchemaUnmarshalService.class);
    private final PacsTTPBaisExtractionService service = new PacsTTPBaisExtractionService(repository, xmlService);

    @Nested
    class ExtractStandardData {

        @Test
        void shouldExtractCommonDataFromPacsTtpFile() {
            PacsTppSchedule schedule = new XmlSchemaUnmarshalService().unmarshal(
                StreamTestUtil.resourceStream("/fixtures/pacs-ttp/typical.xml"),
                PacsTppSchedule.class,
                "xsd/pacs-tpp-schedule-v0.0d.xsd",
                "PACS TTP file"
            );
            when(xmlService.unmarshal(
                any(),
                eq(PacsTppSchedule.class),
                eq("xsd/pacs-tpp-schedule-v0.0d.xsd"),
                eq("PACS TTP file")
            )).thenReturn(schedule);

            List<InterfaceFileCommonDataExtract> extracts = service.extractStandardData(
                sourceFile(),
                StreamTestUtil.stream("ignored")
            );

            assertThat(extracts).hasSize(1);
            InterfaceFileCommonDataExtract extract = extracts.getFirst();
            assertThat(extract.getFileName()).isEqualTo(FILE_NAME);
            assertThat(extract.getPaymentType()).isEqualTo(InterfaceFileCommonDataExtract.PaymentType.CASH);
            assertThat(extract.getDwpCourtCode()).isEqualTo("0000031714");
            assertThat(extract.getDestinationDetails()).isNull();
            assertThat(extract.getTransactions()).hasSize(2);

            Transaction first = extract.getTransactions().get(0);
            assertThat(first.getTransactionCode()).isEqualTo("99");
            assertThat(first.getOriginatorDetails().getAccountReference()).isEqualTo("23000106E");
            assertThat(first.getOriginatorDetails().getName()).isNull();
            assertThat(first.getOriginatorDetails().getBankDetails()).isNull();
            assertThat(first.getAmount()).isEqualTo(1000L);
            assertThat(first.getDateEntryApplied()).isEqualTo("03/09/2023");

            Transaction second = extract.getTransactions().get(1);
            assertThat(second.getTransactionCode()).isEqualTo("99");
            assertThat(second.getOriginatorDetails().getAccountReference()).isEqualTo("23000113P");
            assertThat(second.getAmount()).isEqualTo(5000L);
            assertThat(second.getDateEntryApplied()).isEqualTo("01/09/2023");

            verify(xmlService).unmarshal(
                any(),
                eq(PacsTppSchedule.class),
                eq("xsd/pacs-tpp-schedule-v0.0d.xsd"),
                eq("PACS TTP file")
            );
        }

        @Test
        void shouldExcludeNegativeTransactionsWhenPacsFileContainsMixedSigns() {
            PacsTppSchedule schedule = new XmlSchemaUnmarshalService().unmarshal(
                StreamTestUtil.resourceStream("/fixtures/pacs-ttp/typical.xml"),
                PacsTppSchedule.class,
                PacsTTPBaisExtractionService.PACS_SCHEMA,
                PacsTTPBaisExtractionService.PACS_SOURCE_DESCRIPTION
            );
            schedule.getDocumentDetail().getFirst().setDetailAmountSign("-");
            when(xmlService.unmarshal(
                any(),
                eq(PacsTppSchedule.class),
                eq(PacsTTPBaisExtractionService.PACS_SCHEMA),
                eq(PacsTTPBaisExtractionService.PACS_SOURCE_DESCRIPTION)
            )).thenReturn(schedule);

            List<InterfaceFileCommonDataExtract> extracts = service.extractStandardData(
                sourceFile(),
                StreamTestUtil.stream("ignored")
            );

            assertThat(extracts).singleElement().satisfies(extract ->
                assertThat(extract.getTransactions()).extracting(transaction ->
                    transaction.getOriginatorDetails().getAccountReference()
                ).containsExactly("23000113P")
            );
        }

        @Test
        void shouldMarkSourceFileSuccessfulWhenAllTransactionsAreNegative() {
            PacsTppSchedule schedule = new XmlSchemaUnmarshalService().unmarshal(
                StreamTestUtil.resourceStream("/fixtures/pacs-ttp/all-negative.xml"),
                PacsTppSchedule.class,
                PacsTTPBaisExtractionService.PACS_SCHEMA,
                PacsTTPBaisExtractionService.PACS_SOURCE_DESCRIPTION
            );
            InterfaceFileEntity sourceFile = sourceFile();
            when(xmlService.unmarshal(
                any(),
                eq(PacsTppSchedule.class),
                eq(PacsTTPBaisExtractionService.PACS_SCHEMA),
                eq(PacsTTPBaisExtractionService.PACS_SOURCE_DESCRIPTION)
            )).thenReturn(schedule);

            List<InterfaceFileCommonDataExtract> extracts = service.extractStandardData(
                sourceFile,
                StreamTestUtil.stream("ignored")
            );

            assertThat(extracts).isEmpty();
            assertThat(sourceFile.getStatus()).isEqualTo(Status.SUCCESS_NO_TRANSACTIONS);
            verify(repository).save(sourceFile);
        }
    }

    @Nested
    class ValidateInputs {

        @Test
        void shouldRejectNullSourceFile() {
            assertThatThrownBy(() -> service.validateInputs(null, StreamTestUtil.stream()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source interface file is required");
        }

        @Test
        void shouldRejectBlankSourceFileName() {
            InterfaceFileEntity sourceFile = sourceFile();
            sourceFile.setFileName(" ");

            assertThatThrownBy(() -> service.validateInputs(sourceFile, StreamTestUtil.stream()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source interface file name is required");
        }

        @Test
        void shouldRejectNullFileContents() {
            assertThatThrownBy(() -> service.validateInputs(sourceFile(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PACS TTP file contents are required");
        }
    }

    @Nested
    class MapTransaction {

        @Test
        void shouldMapPacsDocumentDetailToCommonTransaction() {
            PacsTppSchedule schedule = new XmlSchemaUnmarshalService().unmarshal(
                StreamTestUtil.resourceStream("/fixtures/pacs-ttp/typical.xml"),
                PacsTppSchedule.class,
                PacsTTPBaisExtractionService.PACS_SCHEMA,
                PacsTTPBaisExtractionService.PACS_SOURCE_DESCRIPTION
            );

            Transaction transaction = service.mapTransaction(schedule.getDocumentDetail().getFirst());

            assertThat(transaction.getTransactionCode()).isEqualTo("99");
            assertThat(transaction.getOriginatorDetails().getAccountReference()).isEqualTo("23000106E");
            assertThat(transaction.getOriginatorDetails().getName()).isNull();
            assertThat(transaction.getOriginatorDetails().getBankDetails()).isNull();
            assertThat(transaction.getAmount()).isEqualTo(1000L);
            assertThat(transaction.getDateEntryApplied()).isEqualTo("03/09/2023");
        }
    }

    @Nested
    class IsNegative {

        @Test
        void shouldIdentifyNegativeTransaction() {
            PacsTppSchedule schedule = new XmlSchemaUnmarshalService().unmarshal(
                StreamTestUtil.resourceStream("/fixtures/pacs-ttp/typical.xml"),
                PacsTppSchedule.class,
                PacsTTPBaisExtractionService.PACS_SCHEMA,
                PacsTTPBaisExtractionService.PACS_SOURCE_DESCRIPTION
            );
            schedule.getDocumentDetail().getFirst().setDetailAmountSign("-");

            assertThat(service.isNegative(schedule.getDocumentDetail().getFirst())).isTrue();
        }

        @Test
        void shouldIgnorePositiveTransaction() {
            PacsTppSchedule schedule = new XmlSchemaUnmarshalService().unmarshal(
                StreamTestUtil.resourceStream("/fixtures/pacs-ttp/typical.xml"),
                PacsTppSchedule.class,
                PacsTTPBaisExtractionService.PACS_SCHEMA,
                PacsTTPBaisExtractionService.PACS_SOURCE_DESCRIPTION
            );

            assertThat(service.isNegative(schedule.getDocumentDetail().getFirst())).isFalse();
        }
    }

    @Nested
    class FormatDate {

        @Test
        void shouldFormatXmlDateAsDayMonthYear() throws Exception {
            String formattedDate = service.formatDate(DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
                2023,
                9,
                3,
                DatatypeConstants.FIELD_UNDEFINED
            ));

            assertThat(formattedDate).isEqualTo("03/09/2023");
        }
    }

    private static InterfaceFileEntity sourceFile() {
        return InterfaceFileEntity.builder()
            .interfaceFileId(123L)
            .source(Interface.DWP)
            .target(Interface.OPAL)
            .type(Type.SOURCE)
            .opalDomain(Domain.FINES)
            .fileName(FILE_NAME)
            .status(Status.INGESTED)
            .createdDatetime(new Date())
            .build();
    }
}
