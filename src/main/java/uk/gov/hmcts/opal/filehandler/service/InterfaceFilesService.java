package uk.gov.hmcts.opal.filehandler.service;

import com.azure.core.util.BinaryData;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.exception.InterfaceFileNotFoundException;
import uk.gov.hmcts.opal.filehandler.exception.InvalidInterfaceFileStatusException;
import uk.gov.hmcts.opal.filehandler.mapper.InterfaceFileMapper;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.repository.specs.InterfaceFileSpecsFactory;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;

@Service
@AllArgsConstructor
public class InterfaceFilesService {

    private final InterfaceFilesRepository repository;
    private final InterfaceFileSpecsFactory specsFactory;
    private final InterfaceFileMapper mapper;
    private final InterfaceFileBlobStoreService blobStoreService;

    @Autowired
    private final Map<String, BaisFileProcessorConfiguration> configs;

    private BaisFileProcessorConfiguration getConfig(Interface source) {
        return configs.get(source.getConfigComponentName());
    }

    @Transactional(readOnly = true)
    public List<InterfaceFileObjectInterfaceFile> searchInterfaceFiles(SearchInterfaceFilesDto request) {
        // Permissions to be dealt with by: https://tools.hmcts.net/jira/browse/PO-8686
        // PermissionUtil.checkPermissions(FileHandlerPermission.ViewInterfacesFile);

        Specification<InterfaceFileEntity> specs = specsFactory.createSearchSpecs(request);
        Sort sort = Sort.by(Direction.ASC, TypedPropertyPath.of(InterfaceFileEntity::getCreatedDatetime));
        List<InterfaceFileEntity> interfacesFiles = repository.findAll(specs, sort);
        return mapper.toInterfaceFileObjects(interfacesFiles);
    }

    public InputStream getInterfaceFilesContent(Long id) {
        // TODO: permission check is removed from this api, to be re-added in PO-8686
        // PermissionUtil.checkPermission(FileHandlerPermission.ViewInterfacesFile);

        InterfaceFileEntity entity = repository.findById(id)
            .orElseThrow(
                () -> new InterfaceFileNotFoundException(
                    String.format("Interface file with id %d could not be located.", id)
                )
            );

        if (entity.getStatus() != Status.SUCCESS) {
            throw new InvalidInterfaceFileStatusException(
                String.format("Interface file with id %d could not be retrieved as it has an invalid status of:"
                        + " \"%s\" only files with status: \"SUCCESS\" can be returned.",
                    id, entity.getStatus()));
        }

        BaisFileProcessorConfiguration config = getConfig(entity.getSource());
        String containerName = config.getContainerName();

        BinaryData file = blobStoreService.fetchInterfaceFile(id, entity.getFilestoreUuid(), containerName);

        return file.toStream();
    }

}
