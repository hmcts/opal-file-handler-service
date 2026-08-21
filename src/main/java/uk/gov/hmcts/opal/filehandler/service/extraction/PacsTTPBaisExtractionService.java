package uk.gov.hmcts.opal.filehandler.service.extraction;

import jakarta.persistence.EntityNotFoundException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.filehandler.entity.BusinessUnitBankAccountEntity;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.PaymentType;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.generated.pacs.DocumentDetail;
import uk.gov.hmcts.opal.filehandler.generated.pacs.PacsTppSchedule;
import uk.gov.hmcts.opal.filehandler.repository.BusinessUnitBankAccountRepository;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.InterfaceFileCommonDataExtract;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.OriginatorDetails;
import uk.gov.hmcts.opal.filehandler.service.extraction.model.Transaction;
import uk.gov.hmcts.opal.filehandler.utils.XmlSchemaUnmarshalService;

@Service
@RequiredArgsConstructor
public class PacsTTPBaisExtractionService implements ExtractionService<InterfaceFileCommonDataExtract> {

    static final String PACS_SCHEMA = "xsd/pacs-tpp-schedule-v0.0d.xsd";
    static final String PACS_SOURCE_DESCRIPTION = "PACS TTP file";
    static final String TRANSACTION_CODE = "99";
    private static final DateTimeFormatter OUTPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final InterfaceFilesRepository interfaceFilesRepository;
    private final BusinessUnitBankAccountRepository businessUnitBankAccountRepository;
    private final XmlSchemaUnmarshalService xmlSchemaUnmarshalService;

    @Override
    public List<InterfaceFileCommonDataExtract> extractStandardData(
        InterfaceFileEntity sourceInterfaceFile,
        InputStream fileContents
    ) {
        validateInputs(sourceInterfaceFile, fileContents);
        PacsTppSchedule schedule = xmlSchemaUnmarshalService.unmarshal(
            fileContents,
            PacsTppSchedule.class,
            PACS_SCHEMA,
            PACS_SOURCE_DESCRIPTION
        );

        List<Transaction> transactions = schedule.getDocumentDetail().stream()
            .filter(detail -> !isNegative(detail))
            .map(this::mapTransaction)
            .toList();

        if (transactions.isEmpty()) {
            sourceInterfaceFile.setStatus(Status.SUCCESS_NO_TRANSACTIONS);
            interfaceFilesRepository.save(sourceInterfaceFile);
            return List.of();
        }

        InterfaceFileCommonDataExtract extract = InterfaceFileCommonDataExtract.builder()
            .fileName(sourceInterfaceFile.getFileName())
            .paymentType(PaymentType.CASH)
            .transactions(transactions)
            .dwpCourtCode(schedule.getDocumentHeader().getCreditorID())
            .build();

        return List.of(extract);
    }

    @Override
    public BusinessUnitBankAccountEntity getBusinessUnitBankAccount(InterfaceFileCommonDataExtract extractedData) {
        return businessUnitBankAccountRepository.findByDwpCourtCode(extractedData.getDwpCourtCode())
            .orElseThrow(() -> new EntityNotFoundException(
                String.format("Business unit bank account with dwp_court_code '%s' "
                        + "could not be located for file_name '%s'",
                    extractedData.getDwpCourtCode(), extractedData.getFileName())
            ));
    }

    void validateInputs(InterfaceFileEntity sourceInterfaceFile, InputStream fileContents) {
        if (sourceInterfaceFile == null) {
            throw new IllegalArgumentException("Source interface file is required");
        }
        if (sourceInterfaceFile.getFileName() == null || sourceInterfaceFile.getFileName().isBlank()) {
            throw new IllegalArgumentException("Source interface file name is required");
        }
        if (fileContents == null) {
            throw new IllegalArgumentException("PACS TTP file contents are required");
        }
    }

    boolean isNegative(DocumentDetail detail) {
        return "-".equals(detail.getDetailAmountSign());
    }

    Transaction mapTransaction(DocumentDetail detail) {
        return Transaction.builder()
            .transactionCode(TRANSACTION_CODE)
            .originatorDetails(OriginatorDetails.builder()
                .accountReference(detail.getCustomerRef())
                .build())
            .amount(detail.getDetailAmountType())
            .dateEntryApplied(formatDate(detail.getDateFrom()))
            .build();
    }

    String formatDate(XMLGregorianCalendar date) {
        LocalDate localDate = date.toGregorianCalendar().toZonedDateTime().toLocalDate();
        return localDate.format(OUTPUT_DATE_FORMATTER);
    }
}
