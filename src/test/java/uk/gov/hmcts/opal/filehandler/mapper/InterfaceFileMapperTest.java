package uk.gov.hmcts.opal.filehandler.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileTypeEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.StatusEnumInterfaceFile;

public class InterfaceFileMapperTest {
    private final InterfaceFileMapper mapper = Mappers.getMapper(InterfaceFileMapper.class);

    @Test
    void toInterfaceFileObject_allFields() {
        UUID fileStoreUuid = UUID.randomUUID();
        Date created = new GregorianCalendar(2026, Calendar.FEBRUARY, 1).getTime();
        InterfaceFileEntity entity = InterfaceFileEntity.builder()
            .interfaceFileId(200L)
            .source(Interface.CAPS_REPORT)
            .target(Interface.OPAL)
            .type(Type.SOURCE)
            .opalDomain(Domain.FILE_HANDLER)
            .status(Status.SUCCESS)
            .fileName("CapFa.GB.1.xml")
            .filestoreUuid(fileStoreUuid)
            .checksum("A123")
            .errors("XXXX-ERROR-XXXX")
            .createdDatetime(created)
            .build();

        InterfaceFileObjectInterfaceFile mappedObject = mapper.toInterfaceFileObject(entity);

        assertAll(
            () -> assertEquals(200L, mappedObject.getInterfaceFileId()),
            () -> assertEquals(InterfaceFileEnumInterfaceFile.CAPS_REPORT, mappedObject.getSource()),
            () -> assertEquals(InterfaceFileEnumInterfaceFile.OPAL, mappedObject.getTarget()),
            () -> assertEquals(InterfaceFileTypeEnumInterfaceFile.SOURCE, mappedObject.getType()),
            () -> assertEquals(DomainEnumTypes.FILE_HANDLER, mappedObject.getDomain()),
            () -> assertEquals(StatusEnumInterfaceFile.SUCCESS, mappedObject.getStatus()),
            () -> assertEquals("CapFa.GB.1.xml", mappedObject.getFileName()),
            () -> assertEquals(fileStoreUuid, mappedObject.getFilestoreUuid()),
            () -> assertEquals("A123", mappedObject.getChecksum()),
            () -> assertEquals("XXXX-ERROR-XXXX", mappedObject.getErrors()),
            () -> assertEquals(created.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(), mappedObject.getCreatedDatetime())
        );
    }

    @Test
    void toInterfaceFileObject_allMandatoryFields() {
        UUID fileStoreUuid = UUID.randomUUID();
        Date created = new GregorianCalendar(2026, Calendar.MARCH, 10).getTime();
        InterfaceFileEntity entity = InterfaceFileEntity.builder()
            .interfaceFileId(300L)
            .source(Interface.BTECKOH_REPORT)
            .target(Interface.OPAL)
            .type(Type.SOURCE)
            .opalDomain(Domain.FINES)
            .status(Status.INGESTED)
            .fileName("CapFa.GB.2.xml")
            .createdDatetime(created)
            .build();

        InterfaceFileObjectInterfaceFile mappedObject = mapper.toInterfaceFileObject(entity);

        assertAll(
            () -> assertEquals(300L, mappedObject.getInterfaceFileId()),
            () -> assertEquals(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, mappedObject.getSource()),
            () -> assertEquals(InterfaceFileEnumInterfaceFile.OPAL, mappedObject.getTarget()),
            () -> assertEquals(InterfaceFileTypeEnumInterfaceFile.SOURCE, mappedObject.getType()),
            () -> assertEquals(DomainEnumTypes.FINES, mappedObject.getDomain()),
            () -> assertEquals(StatusEnumInterfaceFile.INGESTED, mappedObject.getStatus()),
            () -> assertEquals("CapFa.GB.2.xml", mappedObject.getFileName()),
            () -> assertNull(mappedObject.getFilestoreUuid()),
            () -> assertNull(mappedObject.getChecksum()),
            () -> assertNull(mappedObject.getErrors()),
            () -> assertEquals(created.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime(), mappedObject.getCreatedDatetime())
        );
    }
}
