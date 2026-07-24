package uk.gov.hmcts.opal.filehandler.mapper;

import java.util.Collection;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;

@Mapper(componentModel = "spring")
public interface InterfaceFileMapper {

    @Mapping(target = "domain", source = "opalDomain")
    InterfaceFileObjectInterfaceFile toInterfaceFileObject(InterfaceFileEntity interfaceFile);

    default List<InterfaceFileObjectInterfaceFile> toInterfaceFileObjects(
        Collection<InterfaceFileEntity> interfaceFiles) {
        return interfaceFiles.stream()
            .map(this::toInterfaceFileObject)
            .toList();
    }
}
