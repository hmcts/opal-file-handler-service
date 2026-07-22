package uk.gov.hmcts.opal.filehandler.service;

import com.azure.core.util.BinaryData;
import jakarta.persistence.EntityNotFoundException;
import java.io.InputStream;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.filehandler.util.PermissionUtil;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.exception.InvalidInterfaceFileStatusException;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;

@Service
@AllArgsConstructor
public class InterfaceFileService {

    private InterfaceFilesRepository repository;
    private InterfaceFileBlobStoreService blobStoreService;

    @Autowired
    private Map<String, BaisFileProcessorConfig> configs;

    public InputStream getInterfaceFilesContent(Long id) {
        PermissionUtil.checkPermission(FileHandlerPermission.ViewInterfacesFile);

        InterfaceFileEntity entity = repository.findById(id)
            .orElseThrow(
                () -> new EntityNotFoundException(String.format("Interface file with id %d could not be located.", id))
            );

        if (entity.getStatus() != Status.SUCCESS) {
            throw new InvalidInterfaceFileStatusException(
                String.format("Interface file with id %d could not be retrieved as it has an invalid status of:"
                        + " \"%s\" only files with status: \"SUCCESS\" can be returned.",
                id, entity.getStatus()));
        }

        BaisFileProcessorConfig config = configs.get(entity.getSource().toString());
        String containerName = config.getContainerName();

        BinaryData file = blobStoreService.fetchInterfaceFile(id, entity.getFilestoreUuid(), containerName);

        return file.toStream();
    }

}
