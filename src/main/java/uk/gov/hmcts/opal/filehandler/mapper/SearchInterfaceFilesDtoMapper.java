package uk.gov.hmcts.opal.filehandler.mapper;

import java.time.LocalDateTime;
import org.mapstruct.Mapper;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;
import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileTypeEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.StatusEnumInterfaceFile;

@Mapper(componentModel = "spring")
public interface SearchInterfaceFilesDtoMapper {
    SearchInterfaceFilesDto searchInterfaceFilesDto(InterfaceFileEnumInterfaceFile source,
        InterfaceFileEnumInterfaceFile target,
        InterfaceFileTypeEnumInterfaceFile type,
        DomainEnumTypes domain,
        StatusEnumInterfaceFile status,
        LocalDateTime fromDate,
        LocalDateTime toDate);
}
