package uk.gov.hmcts.opal.filehandler.mapper;

import java.time.LocalDateTime;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;
import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileTypeEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.StatusEnumInterfaceFile;

@Mapper(componentModel = "spring", nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface SearchInterfaceFilesDtoMapper {
    SearchInterfaceFilesDto toSearchInterfaceFilesDto(InterfaceFileEnumInterfaceFile source,
        InterfaceFileEnumInterfaceFile target,
        InterfaceFileTypeEnumInterfaceFile type,
        DomainEnumTypes domain,
        StatusEnumInterfaceFile status,
        LocalDateTime fromDate,
        LocalDateTime toDate);
}
