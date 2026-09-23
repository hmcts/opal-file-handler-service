package uk.gov.hmcts.opal.filehandler.service;

import com.azure.core.util.BinaryData;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.PaymentType;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.exception.InterfaceFileNotFoundException;
import uk.gov.hmcts.opal.filehandler.exception.InvalidInterfaceFileStatusException;
import uk.gov.hmcts.opal.filehandler.mapper.InterfaceFileMapper;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.repository.specs.InterfaceFileSpecsFactory;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;
import uk.gov.hmcts.opal.generated.model.AddInterfaceFileRequestMetadata;
import uk.gov.hmcts.opal.filehandler.util.PermissionUtil;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;

@Service
//@AllArgsConstructor
@Slf4j
public class InterfaceFilesService {

    private final InterfaceFilesRepository repository;
    private final InterfaceFileSpecsFactory specsFactory;
    private final InterfaceFileMapper mapper;
    private final InterfaceFileBlobStoreService blobStoreService;
    private final Map<String, BaisFileProcessorConfiguration> configs;
    private final Map<Class<? extends InterfaceFileProcessorService>, InterfaceFileProcessorService> processorServices;

    public InterfaceFilesService(InterfaceFilesRepository repository,
        InterfaceFileSpecsFactory specsFactory,
        InterfaceFileMapper mapper,
        InterfaceFileBlobStoreService blobStoreService,
        Map<String, BaisFileProcessorConfiguration> configs,
        List<InterfaceFileProcessorService> processorServicesList) {
        this.repository = repository;
        this.specsFactory = specsFactory;
        this.mapper = mapper;
        this.blobStoreService = blobStoreService;
        this.configs = configs;

        this.processorServices = processorServicesList.stream()
            .collect(Collectors.toMap(
                InterfaceFilesService::getProcessorClass,
                Function.identity()
            ));
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends InterfaceFileProcessorService> getProcessorClass(
        InterfaceFileProcessorService processor) {

        return (Class<? extends InterfaceFileProcessorService>)
            ClassUtils.getUserClass(processor);
    }


    private BaisFileProcessorConfiguration getConfig(Interface source) {
        return configs.get(source.getConfigComponentName());
    }


    @Transactional(readOnly = true)
    public List<InterfaceFileObjectInterfaceFile> searchInterfaceFiles(SearchInterfaceFilesDto request) {
        // Permissions to be dealt with by: https://tools.hmcts.net/jira/browse/PO-8686
        // PermissionUtil.checkPermissions(FileHandlerPermission.VIEW_INTERFACE_FILES);

        Specification<InterfaceFileEntity> specs = specsFactory.createSearchSpecs(request);
        Sort sort = Sort.by(Direction.ASC, TypedPropertyPath.of(InterfaceFileEntity::getCreatedDatetime));
        List<InterfaceFileEntity> interfacesFiles = repository.findAll(specs, sort);
        return mapper.toInterfaceFileObjects(interfacesFiles);
    }

    private InterfaceFileEntity getInterfaceFileEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(
                () -> new InterfaceFileNotFoundException(
                    String.format("Interface file with id %d could not be located.", id)
                )
            );
    }

    public InputStream getInterfaceFilesContent(Long id) {
        // TODO: permission check is removed from this api, to be re-added in PO-8686
        // PermissionUtil.checkPermission(FileHandlerPermission.ViewInterfacesFile);

        InterfaceFileEntity entity = getInterfaceFileEntity(id);

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

    public InterfaceFileObjectInterfaceFile getInterfaceFile(Long id) {
        InterfaceFileEntity entity = getInterfaceFileEntity(id);
        checkAccessPermission(entity);
        return mapper.toInterfaceFileObject(entity);
    }

    private void checkAccessPermission(InterfaceFileEntity entity) {
        if (entity.getOpalDomain() != null && !Domain.FILE_HANDLING.equals(entity.getOpalDomain().toCommonDomain())) {
            PermissionUtil.checkPermissionInDomain(FileHandlerPermission.VIEW_INTERFACE_FILES,
                entity.getOpalDomain().toCommonDomain());
        } else {
            PermissionUtil.checkPermissionDomainAgnostic(FileHandlerPermission.VIEW_INTERFACE_FILES);
        }
    }

    public InterfaceFileEntity getInterfaceFileEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new InterfaceFileNotFoundException(id));
    }

    private InterfaceFileProcessorService getProcessorService(Interface sourceType) {
        return processorServices.get(sourceType.getProcessorServiceClass());
    }

    @Transactional
    public InterfaceFileObjectInterfaceFile addInterfaceFile(MultipartFile file,
        AddInterfaceFileRequestMetadata metadata) {
        try {
            //Ensure the related interface file exists if provided
            InterfaceFileEntity relatedInterfaceFile = null;
            if (metadata.getRelatedInterfaceFileId() != null) {
                relatedInterfaceFile = getInterfaceFileEntity(metadata.getRelatedInterfaceFileId());
            }
            InterfaceFileProcessorService processorService =
                getProcessorService(Interface.valueOf(metadata.getSource()));
            InterfaceFileEntity entity = processorService.ingestFile(
                file.getOriginalFilename(),
                file.getBytes(),
                Interface.valueOf(metadata.getSource()),
                Interface.valueOf(metadata.getTarget()),
                Type.valueOf(metadata.getType()),
                Domain.valueOf(metadata.getDomain()),
                null
            );
            entity.setRelatedInterfaceFile(relatedInterfaceFile);
            entity.setPaymentType(PaymentType.valueOf(metadata.getPaymentType()));
            entity.setBusinessUnitCode(new String[] {metadata.getBusinessUnitCode()});
            entity = repository.save(entity);
            //TODO replace with actual call once PO-7205 is implemented
            return new InterfaceFileObjectInterfaceFile();
        } catch (IOException e) {
            throw new InternalServerErrorException(
                "Internal Server Error",
                "Failed to read file content for interface file creation ", e);
        }
    }
}
