package uk.gov.hmcts.opal.filehandler.utils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.opal.filehandler.exception.InvalidReportFileException;

class ReportFileValidatorTest {

    @Test
    void acceptsWellFormedXml() {
        assertThatCode(() -> ReportFileValidator.validateXml(stream("<CapFa><CapFaPayment/></CapFa>")))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not XML", "<CapFa>", "<CapFa/>trailing content",
        "<!DOCTYPE CapFa [<!ENTITY external SYSTEM 'file:///unreadable'>]><CapFa>&external;</CapFa>"})
    void rejectsMalformedXmlAndExternalEntities(String content) {
        assertThatThrownBy(() -> ReportFileValidator.validateXml(stream(content)))
            .isInstanceOf(InvalidReportFileException.class)
            .hasMessage("CAPS report was not valid XML");
    }

    @Test
    void acceptsAnXlsxWorkbook() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Payments").createRow(0).createCell(0).setCellValue("Reference");
            workbook.write(output);
        }
        assertThatCode(() -> ReportFileValidator.validateXlsx(new ByteArrayInputStream(output.toByteArray())))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "not an XLSX workbook", "<CapFa/>"})
    void rejectsNonWorkbookContent(String content) {
        assertThatThrownBy(() -> ReportFileValidator.validateXlsx(stream(content)))
            .isInstanceOf(InvalidReportFileException.class)
            .hasMessage("BTEckoh report was not a valid XLSX workbook");
    }

    @Test
    void rejectsAMacroEnabledWorkbookWithAnXlsxFilename() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (XSSFWorkbook workbook = new XSSFWorkbook(XSSFWorkbookType.XLSM)) {
            workbook.createSheet("Payments");
            workbook.write(output);
        }
        assertThatThrownBy(() -> ReportFileValidator.validateXlsx(new ByteArrayInputStream(output.toByteArray())))
            .isInstanceOf(InvalidReportFileException.class);
    }

    @Test
    void rejectsAnOrdinaryZipArchive() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("report.txt"));
            zip.write("Not a spreadsheet".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        assertThatThrownBy(() -> ReportFileValidator.validateXlsx(new ByteArrayInputStream(output.toByteArray())))
            .isInstanceOf(InvalidReportFileException.class);
    }

    private static ByteArrayInputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }
}
