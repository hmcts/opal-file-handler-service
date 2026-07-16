package uk.gov.hmcts.opal.filehandler.service;

import com.azure.core.util.BinaryData;
import jakarta.persistence.EntityNotFoundException;
import java.io.InputStream;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.util.SecurityUtil;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
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

    public InputStream getInterfaceFilesContent(Long id) {
        checkPermissions();

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

        Interface source = entity.getSource();
        String containerName = "";
        switch (source) {
            case BTECKOH_REPORT -> containerName = BaisFileProcessorConfig.getBTEckohContainerName();
            case CAPS_REPORT -> containerName = BaisFileProcessorConfig.getCAPSContainerName();
            case OPAL -> containerName = BaisFileProcessorConfig.getOpalContainerName();
        }

        BinaryData file = blobStoreService.fetchInterfaceFile(id, entity.getFilestoreUuid(), containerName);

        return file.toStream();
    }

    private void checkPermissions() {
        if (!SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser()
            .hasPermission(FileHandlerPermission.ViewInterfacesFile)) {
            throw new PermissionNotAllowedException(FileHandlerPermission.ViewInterfacesFile);
        }
    }

}
