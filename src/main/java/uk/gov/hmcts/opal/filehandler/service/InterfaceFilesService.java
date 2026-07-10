package uk.gov.hmcts.opal.filehandler.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.util.SecurityUtil;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.mapper.InterfaceFileMapper;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.repository.specs.InterfaceFileSpecs;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;

@RequiredArgsConstructor
@Service
public class InterfaceFilesService {
    private final InterfaceFilesRepository repository;
    private final InterfaceFileSpecs specBuilder;
    private final InterfaceFileMapper mapper;

    @Transactional(readOnly = true)
    public List<InterfaceFileObjectInterfaceFile> searchInterfaceFiles(SearchInterfaceFilesDto request) {
        checkPermissions();

        List<InterfaceFileEntity> interfacesFiles = repository.findAll(
            specBuilder.findBySearchCriteria(request),
            Sort.by(Direction.ASC, TypedPropertyPath.of(InterfaceFileEntity::getCreatedDatetime))
        );
        return mapper.toInterfaceFileObjects(interfacesFiles);
    }

    private void checkPermissions() {
        // Needs the "View Interface File" permission
        if (!SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser()
            .hasPermission(FileHandlerPermission.ViewInterfacesFile)) {
            throw new PermissionNotAllowedException(FileHandlerPermission.ViewInterfacesFile);
        }
    }
}
