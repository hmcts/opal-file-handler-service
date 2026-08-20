package uk.gov.hmcts.opal.filehandler.utils;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

@Service
public class XmlSchemaUnmarshalService {

    public <T> T unmarshal(
        InputStream fileContents,
        Class<T> targetType,
        String schemaResourcePath,
        String sourceDescription
    ) {
        Objects.requireNonNull(fileContents, "fileContents must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
        Objects.requireNonNull(schemaResourcePath, "schemaResourcePath must not be null");
        Objects.requireNonNull(sourceDescription, "sourceDescription must not be null");

        byte[] bytes = StreamUtil.readAllBytes(fileContents, sourceDescription);
        validateWellFormedXml(bytes, sourceDescription);

        try {
            Unmarshaller unmarshaller = JAXBContext.newInstance(targetType).createUnmarshaller();
            unmarshaller.setSchema(schema(schemaResourcePath));
            Object result = unmarshaller.unmarshal(new ByteArrayInputStream(bytes));
            if (targetType.isInstance(result)) {
                return targetType.cast(result);
            }
            if (result instanceof JAXBElement<?> element && targetType.isInstance(element.getValue())) {
                return targetType.cast(element.getValue());
            }
            throw new IllegalArgumentException(sourceDescription + " did not unmarshal to " + targetType.getName());
        } catch (JAXBException ex) {
            throw new IllegalArgumentException(sourceDescription + " did not match the expected schema", ex);
        }
    }

    private void validateWellFormedXml(byte[] bytes, String sourceDescription) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
            document.getDocumentElement().normalize();
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            throw new IllegalArgumentException(sourceDescription + " was not valid XML", ex);
        }
    }

    private Schema schema(String schemaResourcePath) {
        try (InputStream schemaStream = getClass().getClassLoader().getResourceAsStream(schemaResourcePath)) {
            if (schemaStream == null) {
                throw new IllegalArgumentException("XML schema resource could not be found: " + schemaResourcePath);
            }
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return factory.newSchema(new StreamSource(schemaStream));
        } catch (IOException | SAXException ex) {
            throw new IllegalArgumentException("XML schema resource could not be loaded: " + schemaResourcePath, ex);
        }
    }
}
