package uk.gov.hmcts.opal.filehandler.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationEntity;

@Service
public class VpfpoAcknowledgementXmlService {

    private static final String NAMESPACE = "http://www.dca.gov.uk/xmlschemas/libra";

    public byte[] generate(List<VpfpoRegistrationEntity> registrations) {
        registrations.forEach(this::validate);

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            XMLStreamWriter writer = XMLOutputFactory.newFactory()
                .createXMLStreamWriter(output, StandardCharsets.UTF_8.name());

            try {
                writer.writeStartDocument(StandardCharsets.UTF_8.name(), "1.0");
                writer.writeStartElement("AcknowledgementMessage");
                writer.writeDefaultNamespace(NAMESPACE);
                writer.writeAttribute("Flow", "AcknowledgementsFromTheCourt");
                writer.writeAttribute("Interface", "LibraFixedPenaltyOffice");
                writer.writeAttribute("SchemaVersion", "0.3c");

                for (VpfpoRegistrationEntity registration : registrations) {
                    writer.writeStartElement("Acknowledgement");
                    writeElement(writer, "PSACode", String.format(Locale.ROOT, "%04d", registration.getLjaCode()));
                    writeElement(writer, "CTOCode", String.format(Locale.ROOT, "%02d", registration.getCtoCode()));
                    writeElement(writer, "TicketNumber", registration.getTicketNumber());
                    writeElement(writer, "FineAccountNumber", registration.getDefendantAccountId().toString());
                    writer.writeEndElement();
                }

                writer.writeEndElement();
                writer.writeEndDocument();
            } finally {
                writer.close();
            }

            return output.toByteArray();
        } catch (XMLStreamException exception) {
            throw new IllegalStateException("Could not generate VPFPO acknowledgement XML", exception);
        }
    }

    private void validate(VpfpoRegistrationEntity registration) {
        if (registration.getCtoCode() == null || registration.getCtoCode() < 0 || registration.getCtoCode() > 99
            || registration.getLjaCode() == null || registration.getLjaCode() < 0 || registration.getLjaCode() > 9999
            || registration.getTicketNumber() == null || registration.getTicketNumber().isBlank()
            || registration.getDefendantAccountId() == null) {
            throw new IllegalArgumentException("Incomplete or invalid VPFPO acknowledgement data for registration "
                + registration.getVpfpoRegistrationId());
        }
    }

    private void writeElement(XMLStreamWriter writer, String name, String value) throws XMLStreamException {
        writer.writeStartElement(name);
        writer.writeCharacters(value);
        writer.writeEndElement();
    }
}
