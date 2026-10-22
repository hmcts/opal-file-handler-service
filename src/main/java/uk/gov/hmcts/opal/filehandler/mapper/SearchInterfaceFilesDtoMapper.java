package uk.gov.hmcts.opal.filehandler.mapper;

import java.time.LocalDateTime;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.NullValueMappingStrategy;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;
import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileTypeEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.StatusEnumInterfaceFile;

@Mapper(componentModel = "spring",
    nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT,
    nullValueIterableMappingStrategy = NullValueMappingStrategy.RETURN_NULL)
public interface SearchInterfaceFilesDtoMapper {
    SearchInterfaceFilesDto toSearchInterfaceFilesDto(InterfaceFileEnumInterfaceFile source,
        InterfaceFileEnumInterfaceFile target,
        InterfaceFileEnumInterfaceFile notTarget,
        List<InterfaceFileTypeEnumInterfaceFile> types,
        DomainEnumTypes domain,
        StatusEnumInterfaceFile status,
        List<StatusEnumInterfaceFile> notStatuses,
        String businessUnitCode,
        LocalDateTime fromDate,
        LocalDateTime toDate);
}
