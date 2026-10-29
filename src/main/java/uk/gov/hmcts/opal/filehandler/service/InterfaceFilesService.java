package uk.gov.hmcts.opal.filehandler.service;

import com.azure.core.util.BinaryData;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.opal.common.util.SecurityUtil;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.PaymentType;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.exception.BlobDeleteException;
import uk.gov.hmcts.opal.filehandler.exception.InterfaceFileNotFoundException;
import uk.gov.hmcts.opal.filehandler.exception.InvalidInterfaceFileStatusException;
import uk.gov.hmcts.opal.filehandler.mapper.InterfaceFileMapper;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.repository.specs.InterfaceFileSpecsFactory;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.pdpl.InterfaceFilesPdplLoggingService;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;
import uk.gov.hmcts.opal.filehandler.service.response.DeleteInterfaceFilesResult;
import uk.gov.hmcts.opal.filehandler.util.PermissionUtil;
import uk.gov.hmcts.opal.generated.model.AddInterfaceFileRequestMetadata;
import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;

@Service
@Slf4j
public class InterfaceFilesService {

    private final InterfaceFilesRepository repository;
    private final InterfaceFileSpecsFactory specsFactory;
    private final InterfaceFileMapper mapper;
    private final InterfaceFileBlobStoreService blobStoreService;
    private final InterfaceFilesPdplLoggingService loggingService;
    private final Map<String, BaisFileProcessorConfiguration> configs;
    private final Map<Class<? extends InterfaceFileProcessorService>, InterfaceFileProcessorService> processorServices;

    public InterfaceFilesService(InterfaceFilesRepository repository,
        InterfaceFileSpecsFactory specsFactory,
        InterfaceFileMapper mapper,
        InterfaceFileBlobStoreService blobStoreService,
        InterfaceFilesPdplLoggingService loggingService,
        Map<String, BaisFileProcessorConfiguration> configs,
        List<InterfaceFileProcessorService> processorServicesList) {
        this.repository = repository;
        this.specsFactory = specsFactory;
        this.mapper = mapper;
        this.blobStoreService = blobStoreService;
        this.loggingService = loggingService;
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
        PermissionUtil.checkPermissionDomainAgnostic(FileHandlerPermission.VIEW_INTERFACE_FILES);

        Specification<InterfaceFileEntity> specs = specsFactory.createSearchSpecs(request, permittedDomains());

        Sort sort = Sort.by(Direction.ASC, TypedPropertyPath.of(InterfaceFileEntity::getCreatedDatetime));
        List<InterfaceFileEntity> interfacesFiles = repository.findAll(specs, sort);

        return mapper.toInterfaceFileObjects(interfacesFiles);
    }


    public InputStream getInterfaceFilesContent(Long id) {
        InterfaceFileEntity entity = getInterfaceFileEntity(id);
        checkAccessPermission(entity);

        if (entity.getStatus() != Status.SUCCESS) {
            throw new InvalidInterfaceFileStatusException(
                String.format("Interface file with id %d could not be retrieved as it has an invalid status of:"
                        + " \"%s\" only files with status: \"SUCCESS\" can be returned.",
                    id, entity.getStatus()));
        }

        BaisFileProcessorConfiguration config = getConfig(entity.getSource());
        String containerName = config.getContainerName();

        BinaryData file = blobStoreService.fetchInterfaceFile(id, entity.getFilestoreUuid(), containerName);

        UserStateV2 userState = SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser().getUserState();
        if (!userState.isSystemUser()) {
            loggingService.logPdpl(id, userState);
        }

        return file.toStream();
    }

    public InterfaceFileObjectInterfaceFile getInterfaceFile(Long id) {
        InterfaceFileEntity entity = getInterfaceFileEntity(id);
        checkAccessPermission(entity);

        return mapper.toInterfaceFileObject(entity);
    }

    private void checkAccessPermission(InterfaceFileEntity entity) {
        if (entity.getOpalDomain() != null) {
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

    private Set<uk.gov.hmcts.opal.filehandler.entity.Domain> permittedDomains() {
        return SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser()
            .getUserState()
            .getDomains()
            .entrySet()
            .stream()
            .filter(entry -> entry.getValue().getBusinessUnitUsers().stream()
                .flatMap(user -> user.getPermissions().stream())
                .anyMatch(permission -> permission.getDescription()
                    .equalsIgnoreCase(FileHandlerPermission.VIEW_INTERFACE_FILES.getDescription())))
            .flatMap(entry -> Arrays.stream(uk.gov.hmcts.opal.filehandler.entity.Domain.values())
                .filter(domain -> domain.toCommonDomain() == entry.getKey()))
            .collect(Collectors.toSet());
    }

    private InterfaceFileProcessorService getProcessorService(Interface sourceType) {
        return processorServices.get(sourceType.getProcessorServiceClass());
    }

    @Transactional
    public InterfaceFileObjectInterfaceFile addInterfaceFile(MultipartFile file,
        AddInterfaceFileRequestMetadata metadata) {

        DomainEnumTypes metadataDomain = metadata.getDomain();
        Domain fromMetadata = Domain.valueOf(metadataDomain);

        if (metadataDomain != null && fromMetadata != null) {
            PermissionUtil.checkPermissionInDomain(
                FileHandlerPermission.CREATE_INTERFACE_FILES, fromMetadata.toCommonDomain());
        } else {
            PermissionUtil.checkPermission(FileHandlerPermission.CREATE_INTERFACE_FILES);
        }

        try {
            //Ensure the related interface file exists if provided
            InterfaceFileEntity relatedInterfaceFile = null;
            if (metadata.getRelatedInterfaceFileId() != null) {
                relatedInterfaceFile = getInterfaceFileEntity(metadata.getRelatedInterfaceFileId());
            }
            Interface source = Interface.valueOf(metadata.getSource());
            InterfaceFileProcessorService processorService =
                getProcessorService(source);
            BaisFileProcessorConfiguration config = getConfig(source);

            InterfaceFileEntity entity = processorService.ingestFile(
                file.getOriginalFilename(),
                file.getBytes(),
                Interface.valueOf(metadata.getSource()),
                Interface.valueOf(metadata.getTarget()),
                Type.valueOf(metadata.getType()),
                Domain.valueOf(metadata.getDomain()),
                config.getContainerName(),
                SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser().getUserId()
            );
            entity.setRelatedInterfaceFile(relatedInterfaceFile);
            entity.setPaymentType(PaymentType.valueOf(metadata.getPaymentType()));
            entity.setBusinessUnitCode(new String[] {metadata.getBusinessUnitCode()});
            entity = repository.save(entity);

            return mapper.toInterfaceFileObject(getInterfaceFileEntity(entity.getInterfaceFileId()));
        } catch (IOException e) {
            throw new InternalServerErrorException(
                "Internal Server Error",
                "Failed to read file content for interface file creation ", e);
        }
    }

    @Transactional
    public DeleteInterfaceFilesResult deleteInterfaceFiles(Set<Long> ids) {
        ArrayList<Long> successIds = new ArrayList<>();
        ArrayList<Long> ignoredIds = new ArrayList<>();

        for(Long id: ids) {
            repository.findById(id).ifPresentOrElse(interfaceFile -> {
                if(interfaceFile.getType() != Type.SOURCE) {
                    log.warn("Interface file id: {} is of type {}, therefore skipping", id, interfaceFile.getType());
                    ignoredIds.add(id);
                    return;
                }

                try {
                    log.info("Processing delete for interface file id: {} and all related files...", id);
                    processDelete(interfaceFile);
                    successIds.add(id);
                } catch (Exception e) {
                    log.warn("Did not complete tree of deletes for interface file id: {}", id);
                }
            }, () -> {
                log.warn("Ignored request to delete interface file with id: {} as it does not exist", id);
                ignoredIds.add(id);
            });
        }

        log.info("Successfully processed {} out of {} requested. (ignored {})",
            successIds.size(), ids.size(), ignoredIds.size());

        ids.removeAll(successIds);
        ids.removeAll(ignoredIds);

        return DeleteInterfaceFilesResult.builder()
            .successfullyDeletedIds(successIds)
            .ignoredIds(ignoredIds)
            .failedIds(ids)
            .build();
    }

    private void processDelete(InterfaceFileEntity interfaceFile) {
        if(interfaceFile.hasChildren()) {
            interfaceFile.getChildRelatedInterfaceFiles().stream()
                .forEach(this::processDelete);
        } else {
            deleteInterfaceFile(interfaceFile);
            // See comment in deleteInterfaceFile(). We might need to remove the deleted file from its
            // parents collection (if it has a parent) to make the following code work, It depends if JPA does it
            // immediately on the execution of delete, or whether it does it at flush, or at all.
            if (interfaceFile.hasParent() && !interfaceFile.getRelatedInterfaceFile().hasChildren()) {
                // if we are at the bottom of the tree, and no other children exist at this level,
                // then we can delete the parent
                processDelete(interfaceFile.getRelatedInterfaceFile());
            }
        }
    }

    private void deleteInterfaceFile(InterfaceFileEntity interfaceFile) {
        Long id = interfaceFile.getInterfaceFileId();
        String containerName =  interfaceFile.getSource().name().toLowerCase();
        UUID filestoreUUID = interfaceFile.getFilestoreUuid();
        log.info("Deleting interface file id: {} type: {} from blob store and db", id, interfaceFile.getType().name());

        try {
            blobStoreService.deleteBlob(filestoreUUID, containerName);
            log.info("Successfully deleted Interface File id: {} filestoreUuid: {} container: {} from blob store",
                id, filestoreUUID, containerName);
        } catch (BlobDeleteException e) {
            log.warn("Failed to delete Interface File id: {} filestoreUuid: {} container: {} from blob store,"
                + "and will now skip deleting from db", id, filestoreUUID, containerName);
            throw e;
        }

        repository.delete(interfaceFile); // TODO make sure we don't need to remove from parent collection
        log.info("Successfully deleted interface file id: {} from db", id);
    }
}
