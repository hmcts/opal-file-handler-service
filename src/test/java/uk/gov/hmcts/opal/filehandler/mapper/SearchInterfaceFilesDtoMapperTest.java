package uk.gov.hmcts.opal.filehandler.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;
import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileTypeEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.StatusEnumInterfaceFile;

public class SearchInterfaceFilesDtoMapperTest {
    private final SearchInterfaceFilesDtoMapper mapper = Mappers.getMapper(SearchInterfaceFilesDtoMapper.class);

    @Test
    void toSearchInterfaceFilesDto_allFieldsProvided() {
        InterfaceFileEnumInterfaceFile source = InterfaceFileEnumInterfaceFile.BTECKOH_REPORT;
        InterfaceFileEnumInterfaceFile target = InterfaceFileEnumInterfaceFile.OPAL;
        InterfaceFileEnumInterfaceFile notTarget = InterfaceFileEnumInterfaceFile.NATWEST;
        List<InterfaceFileTypeEnumInterfaceFile> types = List.of(InterfaceFileTypeEnumInterfaceFile.SOURCE);
        DomainEnumTypes domain = DomainEnumTypes.FINES;
        StatusEnumInterfaceFile status = StatusEnumInterfaceFile.SUCCESS;
        List<StatusEnumInterfaceFile> notStatuses = List.of(StatusEnumInterfaceFile.FAILED);
        String buCode = "BU134";
        LocalDateTime fromDate = LocalDateTime.of(2026, Month.APRIL, 1, 9, 0);
        LocalDateTime toDate = LocalDateTime.of(2026, Month.MAY, 1, 0, 00);

        SearchInterfaceFilesDto searchDto = mapper.toSearchInterfaceFilesDto(
            source, target, notTarget, types, domain, status, notStatuses, buCode, fromDate, toDate
        );

        assertAll(
            () -> assertEquals(Interface.BTECKOH_REPORT, searchDto.getSource()),
            () -> assertEquals(Interface.OPAL, searchDto.getTarget()),
            () -> assertEquals(Interface.NATWEST, searchDto.getNotTarget()),
            () -> assertEquals(1, searchDto.getTypes().size()),
            () -> assertTrue(searchDto.getTypes().contains(Type.SOURCE)),
            () -> assertEquals(Domain.FINES, searchDto.getDomain()),
            () -> assertEquals(Status.SUCCESS, searchDto.getStatus()),
            () -> assertEquals(1, searchDto.getNotStatuses().size()),
            () -> assertTrue(searchDto.getNotStatuses().contains(Status.FAILED)),
            () -> assertEquals(buCode, searchDto.getBusinessUnitCode()),
            () -> assertEquals(fromDate, searchDto.getFromDate()),
            () -> assertEquals(toDate, searchDto.getToDate())
        );
    }

    @Test
    void toSearchInterfaceFilesDto_allFieldsNull() {
        SearchInterfaceFilesDto searchDto = mapper.toSearchInterfaceFilesDto(
            null, null, null, null, null, null, null, null, null, null
        );

        assertNotNull(searchDto);
        assertAll(
            () -> assertNull(searchDto.getSource()),
            () -> assertNull(searchDto.getTarget()),
            () -> assertNull(searchDto.getNotTarget()),
            () -> assertNull(searchDto.getTypes()),
            () -> assertNull(searchDto.getDomain()),
            () -> assertNull(searchDto.getStatus()),
            () -> assertNull(searchDto.getNotStatuses()),
            () -> assertNull(searchDto.getBusinessUnitCode()),
            () -> assertNull(searchDto.getFromDate()),
            () -> assertNull(searchDto.getToDate())
        );
    }
}
