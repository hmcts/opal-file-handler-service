package uk.gov.hmcts.opal.filehandler.utils;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbookType;
import org.xml.sax.SAXException;
import uk.gov.hmcts.opal.filehandler.exception.InvalidReportFileException;

public final class ReportFileValidator {

    private ReportFileValidator() {
    }

    public static void validateXml(InputStream inputStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.newDocumentBuilder().parse(inputStream);
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new InvalidReportFileException("CAPS report was not valid XML", ex);
        }
    }

    public static void validateXlsx(InputStream inputStream) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(inputStream)) {
            if (workbook.getWorkbookType() != XSSFWorkbookType.XLSX) {
                throw new IOException("Expected an XLSX workbook");
            }
        } catch (IOException | RuntimeException ex) {
            throw new InvalidReportFileException("BTEckoh report was not a valid XLSX workbook", ex);
        }
    }
}
