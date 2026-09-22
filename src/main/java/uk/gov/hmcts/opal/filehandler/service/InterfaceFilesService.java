package uk.gov.hmcts.opal.filehandler.service;

import com.azure.core.util.BinaryData;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;
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
import uk.gov.hmcts.opal.filehandler.util.StringUtil;
import uk.gov.hmcts.opal.filehandler.utils.StreamUtil;
import uk.gov.hmcts.opal.generated.model.AddInterfaceFileRequestMetadata;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;

@Service
@AllArgsConstructor
@Slf4j
public class InterfaceFilesService {

    private final InterfaceFilesRepository repository;
    private final InterfaceFileSpecsFactory specsFactory;
    private final InterfaceFileMapper mapper;
    private final InterfaceFileBlobStoreService blobStoreService;
    private final Map<String, BaisFileProcessorConfiguration> configs;
    private final ObjectMapper objectMapper;
    protected final InterfaceFileBlobStoreService interfaceFileBlobStoreService;

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

    @Transactional
    public InterfaceFileObjectInterfaceFile addInterfaceFile(MultipartFile file,
        AddInterfaceFileRequestMetadata metadata) {
        String checksum;

        try (InputStream stream = file.getInputStream()) {
            checksum = StreamUtil.calculateChecksum(stream);
        } catch (IOException e) {
            throw new InternalServerErrorException("Internal Server Error",
                "Failed to read file content for checksum calculation ", e);
        }
        List<InterfaceFileEntity> locatedDuplicates = repository.findByTypeAndChecksumAndFileName(
            Type.valueOf(metadata.getType().name()),
            checksum,
            metadata.getFileName()
        );

        InterfaceFileEntity relatedInterfaceFile = null;
        if (metadata.getRelatedInterfaceFileId() != null) {
            relatedInterfaceFile = getInterfaceFileEntity(metadata.getRelatedInterfaceFileId());
        }

        InterfaceFileEntity interfaceFileEntity = InterfaceFileEntity.builder()
            .relatedInterfaceFile(relatedInterfaceFile)
            .source(Interface.valueOf(metadata.getSource()))
            .target(Interface.valueOf(metadata.getTarget()))
            .type(Type.valueOf(metadata.getType().name()))
            .opalDomain(Domain.valueOf(metadata.getDomain()))
            .fileName(metadata.getFileName())
            .businessUnitCode(new String[] {metadata.getBusinessUnitCode()})
            .paymentType(PaymentType.valueOf(metadata.getPaymentType()))
            .status(Status.SUCCESS)
            .checksum(checksum)
            .build();

        if (locatedDuplicates.isEmpty()) {
            try (InputStream stream = file.getInputStream()) {
                storeInterfaceFile(interfaceFileEntity, stream, checksum);
            } catch (IOException e) {
                throw new InternalServerErrorException("Internal Server Error",
                    "Failed to read file content for checksum calculation ", e);
            }
        } else {
            log.info("Duplicate file detected: {} with checksum: {} and type: {}", metadata.getFileName(), checksum,
                metadata.getType().name());
            interfaceFileEntity.setStatus(Status.DUPLICATE);
            interfaceFileEntity.setErrors(StringUtil.toMessageJson(objectMapper,
                "Duplicate file detected duplicate ids: " + locatedDuplicates.stream()
                    .map(InterfaceFileEntity::getInterfaceFileId)
                    .toList()));
        }
        interfaceFileEntity = repository.save(interfaceFileEntity);

        //TODO replace with actual call once PO-7205 is implemented
        return new InterfaceFileObjectInterfaceFile();


    }

    private boolean storeInterfaceFile(InterfaceFileEntity interfaceFileEntity, InputStream inputStream,
        String fileChecksum) {
        UUID fileStoreUuid = UUID.randomUUID();
        try {
            interfaceFileBlobStoreService.uploadBaisFile(
                fileStoreUuid,
                getConfig(interfaceFileEntity.getSource()).getContainerName(),
                inputStream,
                fileChecksum);
        } catch (Exception e) {
            log.error("Failed to upload file to blob store for interface file with id: {}",
                interfaceFileEntity.getInterfaceFileId(), e);
            interfaceFileEntity.setStatus(Status.FAILED);
            interfaceFileEntity.setErrors(
                StringUtil.toMessageJson(objectMapper, "Failed to upload file to blob store"));
            return false;
        }
        interfaceFileEntity.setFilestoreUuid(fileStoreUuid);
        return true;
    }
}
