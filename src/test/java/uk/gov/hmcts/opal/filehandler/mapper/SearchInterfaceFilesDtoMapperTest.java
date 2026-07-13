package uk.gov.hmcts.opal.filehandler.mapper;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.time.Month;
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
        InterfaceFileTypeEnumInterfaceFile type = InterfaceFileTypeEnumInterfaceFile.SOURCE;
        DomainEnumTypes domain = DomainEnumTypes.FINES;
        StatusEnumInterfaceFile status = StatusEnumInterfaceFile.SUCCESS;
        LocalDateTime fromDate = LocalDateTime.of(2026, Month.APRIL, 1, 9, 0);
        LocalDateTime toDate = LocalDateTime.of(2026, Month.MAY, 1, 0, 00);

        SearchInterfaceFilesDto searchDto = mapper.toSearchInterfaceFilesDto(
            source, target, type, domain, status, fromDate, toDate
        );

        assertAll(
            () -> assertEquals(Interface.BTECKOH_REPORT, searchDto.getSource()),
            () -> assertEquals(Interface.OPAL, searchDto.getTarget()),
            () -> assertEquals(Type.SOURCE, searchDto.getType()),
            () -> assertEquals(Domain.FINES, searchDto.getDomain()),
            () -> assertEquals(Status.SUCCESS, searchDto.getStatus()),
            () -> assertEquals(fromDate, searchDto.getFromDate()),
            () -> assertEquals(toDate, searchDto.getToDate())
        );
    }

    @Test
    void toSearchInterfaceFilesDto_allFieldsNull() {
        SearchInterfaceFilesDto searchDto = mapper.toSearchInterfaceFilesDto(
            null, null, null, null, null, null, null
        );

        assertNotNull(searchDto);
        assertAll(
            () -> assertNull(searchDto.getSource()),
            () -> assertNull(searchDto.getTarget()),
            () -> assertNull(searchDto.getType()),
            () -> assertNull(searchDto.getDomain()),
            () -> assertNull(searchDto.getStatus()),
            () -> assertNull(searchDto.getFromDate()),
            () -> assertNull(searchDto.getToDate())
        );
    }
}
