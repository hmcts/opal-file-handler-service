package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.common.exceptions.standard.BadRequestException;
import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
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
import uk.gov.hmcts.opal.filehandler.mapper.InterfaceFileMapper;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.repository.specs.InterfaceFileSpecsFactory;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.pdpl.InterfaceFilesPdplLoggingService;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;
import uk.gov.hmcts.opal.filehandler.util.PermissionUtil;
import uk.gov.hmcts.opal.generated.model.AddInterfaceFileRequestMetadata;
import uk.gov.hmcts.opal.generated.model.DeleteInterfaceFilesResultTestSupport;
import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileTypeEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.PaymentTypeEnumTypes;

@ExtendWith(MockitoExtension.class)
public class InterfaceFilesServiceTest {

    @Mock
    private InterfaceFileMapper mapper;

    @Mock
    private InterfaceFilesRepository repository;

    @Mock
    private InterfaceFileSpecsFactory specsFactory;

    @Mock
    private InterfaceFileBlobStoreService blobStoreService;

    @Mock
    private InterfaceFilesPdplLoggingService loggingService;

    @Mock
    private OpalJwtAuthenticationToken authToken;

    @Mock
    private Specification<InterfaceFileEntity> specification;

    @Mock
    private List<InterfaceFileProcessorService> processorServicesList;

    @InjectMocks
    private InterfaceFilesService service;


    @Test
    public void getInterfaceFiles_shouldOrchestrateCallsCorrectly() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class);
             MockedStatic<PermissionUtil> permissionUtil = mockStatic(PermissionUtil.class)) {
            securityUtil.when(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser).thenReturn(authToken);
            UserStateV2 userState = mock(UserStateV2.class);
            DomainBusinessUnitUsers domainUsers = mock(DomainBusinessUnitUsers.class);
            when(authToken.getUserState()).thenReturn(userState);
            when(userState.getDomains()).thenReturn(Map.of(
                uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FINES, domainUsers));
            when(domainUsers.getBusinessUnitUsers()).thenReturn(List.of(BusinessUnitUser.builder()
                .businessUnitUserId("FINES1")
                .businessUnitId((short) 1)
                .permissions(Set.of(new Permission(18L, "View Interface Files")))
                .build()));
            List<InterfaceFileEntity> interfaceFiles = List.of(
                mock(InterfaceFileEntity.class)
            );
            SearchInterfaceFilesDto searchDto = new SearchInterfaceFilesDto();
            when(specsFactory.createSearchSpecs(searchDto, Set.of(Domain.FINES))).thenReturn(specification);
            when(repository.findAll(
                specification, Sort.by(Direction.ASC, TypedPropertyPath.of(InterfaceFileEntity::getCreatedDatetime)))
            ).thenReturn(interfaceFiles);

            service.searchInterfaceFiles(searchDto);

            permissionUtil.verify(() ->
                PermissionUtil.checkPermissionDomainAgnostic(FileHandlerPermission.VIEW_INTERFACE_FILES));
            verify(mapper).toInterfaceFileObjects(interfaceFiles);
        }
    }

    @Test
    void searchInterfaceFiles_selectsDomainsByPermissionDescription() {
        DomainBusinessUnitUsers finesUsers = DomainBusinessUnitUsers.builder()
            .businessUnitUsers(List.of(BusinessUnitUser.builder()
                .businessUnitUserId("FINES1")
                .businessUnitId((short) 1)
                .permissions(Set.of(new Permission(1L, "Create and Manage Draft Accounts")))
                .build()))
            .build();
        DomainBusinessUnitUsers fileHandlerUsers = DomainBusinessUnitUsers.builder()
            .businessUnitUsers(List.of(BusinessUnitUser.builder()
                .businessUnitUserId("FHAB01")
                .businessUnitId((short) 2)
                .permissions(Set.of(new Permission(18L, "View Interface Files")))
                .build()))
            .build();
        UserStateV2 userState = UserStateV2.builder()
            .userId(1L)
            .username("test")
            .domains(Map.of(
                uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FINES, finesUsers,
                uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FILE_HANDLING, fileHandlerUsers))
            .build();

        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser).thenReturn(authToken);
            when(authToken.getUserState()).thenReturn(userState);

            SearchInterfaceFilesDto request = new SearchInterfaceFilesDto();
            service.searchInterfaceFiles(request);

            verify(specsFactory).createSearchSpecs(request, Set.of(Domain.FILE_HANDLER));
        }
    }

    @Test
    void getInterfaceFile_shouldCheckDomainAgnosticPermissionWhenEntityHasNoDomain() {
        try (MockedStatic<PermissionUtil> permissionUtil = mockStatic(PermissionUtil.class)) {
            Long id = 103L;
            InterfaceFileEntity entity = mock(InterfaceFileEntity.class);
            InterfaceFileObjectInterfaceFile mapped = mock(InterfaceFileObjectInterfaceFile.class);
            InterfaceFilesService spyService = spy(service);

            doReturn(entity).when(spyService).getInterfaceFileEntity(id);
            when(mapper.toInterfaceFileObject(entity)).thenReturn(mapped);

            InterfaceFileObjectInterfaceFile result = spyService.getInterfaceFile(id);

            assertEquals(mapped, result);
            permissionUtil.verify(() ->
                PermissionUtil.checkPermissionDomainAgnostic(FileHandlerPermission.VIEW_INTERFACE_FILES));
            verify(spyService).getInterfaceFileEntity(id);
            verify(mapper).toInterfaceFileObject(entity);
        }
    }

    @Test
    void getInterfaceFile_shouldCheckPermissionInEntityDomain() {
        try (MockedStatic<PermissionUtil> permissionUtil = mockStatic(PermissionUtil.class)) {
            Long id = 104L;
            InterfaceFileEntity entity = mock(InterfaceFileEntity.class);
            InterfaceFileObjectInterfaceFile mapped = mock(InterfaceFileObjectInterfaceFile.class);
            InterfaceFilesService spyService = spy(service);

            doReturn(entity).when(spyService).getInterfaceFileEntity(id);
            when(entity.getOpalDomain()).thenReturn(Domain.FINES);
            when(mapper.toInterfaceFileObject(entity)).thenReturn(mapped);

            InterfaceFileObjectInterfaceFile result = spyService.getInterfaceFile(id);

            assertEquals(mapped, result);
            permissionUtil.verify(() -> PermissionUtil.checkPermissionInDomain(
                FileHandlerPermission.VIEW_INTERFACE_FILES,
                uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FINES
            ));
            verify(spyService).getInterfaceFileEntity(id);
            verify(mapper).toInterfaceFileObject(entity);
        }
    }

    @Test
    void getInterfaceFile_shouldCheckPermissionInFileHandlingDomain() {
        try (MockedStatic<PermissionUtil> permissionUtil = mockStatic(PermissionUtil.class)) {
            Long id = 105L;
            InterfaceFileEntity entity = mock(InterfaceFileEntity.class);
            InterfaceFilesService spyService = spy(service);

            doReturn(entity).when(spyService).getInterfaceFileEntity(id);
            when(entity.getOpalDomain()).thenReturn(Domain.FILE_HANDLER);

            spyService.getInterfaceFile(id);

            permissionUtil.verify(() -> PermissionUtil.checkPermissionInDomain(
                FileHandlerPermission.VIEW_INTERFACE_FILES,
                uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FILE_HANDLING
            ));
        }
    }

    @Test
    void getInterfaceFileEntity_shouldReturnEntityWhenFound() {
        Long id = 101L;
        InterfaceFileEntity entity = mock(InterfaceFileEntity.class);
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        InterfaceFileEntity result = service.getInterfaceFileEntity(id);

        assertEquals(entity, result);
        verify(repository).findById(id);
    }

    @Test
    void getInterfaceFileEntity_shouldThrowWhenEntityNotFound() {
        Long id = 102L;
        when(repository.findById(id)).thenReturn(Optional.empty());

        InterfaceFileNotFoundException exception = assertThrows(
            InterfaceFileNotFoundException.class,
            () -> service.getInterfaceFileEntity(id)
        );
        assertEquals("404 NOT_FOUND \"Interface file with id 102 could not be located.\"",exception.getMessage());
        verify(repository).findById(id);
    }

    @Test
    void addInterfaceFile_savesEnrichedEntityAndReturnsMappedFile() throws IOException {
        DWPBaisFileProcessorService processorService = mock(DWPBaisFileProcessorService.class);
        BaisFileProcessorConfiguration config = mock(BaisFileProcessorConfiguration.class);
        InterfaceFilesService uploadService = serviceWithProcessor(processorService, config);
        MultipartFile file = mock(MultipartFile.class);
        byte[] fileBytes = "file-content".getBytes();
        InterfaceFileEntity relatedInterfaceFile = InterfaceFileEntity.builder()
            .interfaceFileId(15L)
            .build();
        InterfaceFileEntity ingestedEntity = InterfaceFileEntity.builder()
            .interfaceFileId(24L)
            .build();
        InterfaceFileObjectInterfaceFile mapped = mock(InterfaceFileObjectInterfaceFile.class);
        AddInterfaceFileRequestMetadata metadata = addInterfaceFileMetadata()
            .relatedInterfaceFileId(15L)
            .paymentType(PaymentTypeEnumTypes.CHEQUE)
            .businessUnitCode("1234");

        when(repository.findById(15L)).thenReturn(Optional.of(relatedInterfaceFile));
        when(config.getContainerName()).thenReturn("dwp-container");
        when(file.getOriginalFilename()).thenReturn("payments.dat");
        when(file.getBytes()).thenReturn(fileBytes);

        when(authToken.getUserId()).thenReturn(1L);
        when(processorService.ingestFile(
            "payments.dat",
            fileBytes,
            Interface.DWP,
            Interface.OPAL,
            Type.TRANSFORMED_JSON,
            Domain.FINES,
            "dwp-container",
            1L
        )).thenReturn(ingestedEntity);
        when(repository.save(ingestedEntity)).thenReturn(ingestedEntity);
        when(repository.findById(24L)).thenReturn(Optional.of(ingestedEntity));
        when(mapper.toInterfaceFileObject(ingestedEntity)).thenReturn(mapped);

        try (MockedStatic<SecurityUtil> ignored = authenticateWithPermission(
            uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FINES,
            FileHandlerPermission.CREATE_INTERFACE_FILES)) {
            InterfaceFileObjectInterfaceFile result = uploadService.addInterfaceFile(file, metadata);

            assertEquals(mapped, result);
            assertEquals(relatedInterfaceFile, ingestedEntity.getRelatedInterfaceFile());
            assertEquals(PaymentType.CHEQUE, ingestedEntity.getPaymentType());
            assertArrayEquals(new String[] {"1234"}, ingestedEntity.getBusinessUnitCode());

            verify(repository).findById(15L);
            verify(repository).save(ingestedEntity);
            verify(repository).findById(24L);
            verify(mapper).toInterfaceFileObject(ingestedEntity);
        }
    }

    @Test
    void addInterfaceFile_savesEntityWithoutRelatedFileWhenNoRelatedIdProvided() throws IOException {
        DWPBaisFileProcessorService processorService = mock(DWPBaisFileProcessorService.class);
        BaisFileProcessorConfiguration config = mock(BaisFileProcessorConfiguration.class);
        InterfaceFilesService uploadService = serviceWithProcessor(processorService, config);
        MultipartFile file = mock(MultipartFile.class);
        byte[] fileBytes = "source-file".getBytes();
        InterfaceFileEntity ingestedEntity = InterfaceFileEntity.builder()
            .interfaceFileId(25L)
            .build();
        InterfaceFileObjectInterfaceFile mapped = mock(InterfaceFileObjectInterfaceFile.class);
        AddInterfaceFileRequestMetadata metadata = addInterfaceFileMetadata()
            .paymentType(PaymentTypeEnumTypes.CASH)
            .businessUnitCode("9876");

        when(config.getContainerName()).thenReturn("dwp-container");
        when(file.getOriginalFilename()).thenReturn("source.dat");
        when(file.getBytes()).thenReturn(fileBytes);

        when(authToken.getUserId()).thenReturn(1L);
        when(processorService.ingestFile(
            "source.dat",
            fileBytes,
            Interface.DWP,
            Interface.OPAL,
            Type.TRANSFORMED_JSON,
            Domain.FINES,
            "dwp-container",
            1L
        )).thenReturn(ingestedEntity);
        when(repository.save(ingestedEntity)).thenReturn(ingestedEntity);
        when(repository.findById(25L)).thenReturn(Optional.of(ingestedEntity));
        when(mapper.toInterfaceFileObject(ingestedEntity)).thenReturn(mapped);

        try (MockedStatic<SecurityUtil> ignored = authenticateWithPermission(
            uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FINES,
            FileHandlerPermission.CREATE_INTERFACE_FILES)) {
            InterfaceFileObjectInterfaceFile result = uploadService.addInterfaceFile(file, metadata);

            assertEquals(mapped, result);
            assertNull(ingestedEntity.getRelatedInterfaceFile());
            assertEquals(PaymentType.CASH, ingestedEntity.getPaymentType());
            assertArrayEquals(new String[] {"9876"}, ingestedEntity.getBusinessUnitCode());

            verify(repository).save(ingestedEntity);
            verify(repository).findById(25L);
            verify(mapper).toInterfaceFileObject(ingestedEntity);
        }
    }

    @Test
    void addInterfaceFile_wrapsFileReadFailure() throws IOException {
        InterfaceFilesService service = serviceWithProcessor(
            mock(DWPBaisFileProcessorService.class),
            mock(BaisFileProcessorConfiguration.class)
        );
        MultipartFile file = mock(MultipartFile.class);
        IOException ioException = new IOException("cannot read");

        when(file.getBytes()).thenThrow(ioException);

        try (MockedStatic<SecurityUtil> ignored = authenticateWithPermission(
            uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FINES,
            FileHandlerPermission.CREATE_INTERFACE_FILES)) {
            InternalServerErrorException exception = assertThrows(
                InternalServerErrorException.class,
                () -> service.addInterfaceFile(file, addInterfaceFileMetadata()));

            assertEquals(ioException, exception.getCause());
            assertEquals(
                "Failed to read file content for interface file creation ",
                exception.getMessage());
        }
    }

    @Test
    void addInterfaceFile_rejectsViewPermissionWithoutCreatePermission() {
        MultipartFile file = mock(MultipartFile.class);

        try (MockedStatic<SecurityUtil> ignored = authenticateWithPermission(
            uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FINES,
            FileHandlerPermission.VIEW_INTERFACE_FILES)) {
            PermissionNotAllowedException exception = assertThrows(PermissionNotAllowedException.class,
                () -> service.addInterfaceFile(file, addInterfaceFileMetadata()));
            assertEquals("[CREATE_INTERFACE_FILES] permission(s) are not enabled for the user.",
                exception.getMessage());
        }

        verifyNoInteractions(file, repository);
    }

    @Test
    void addInterfaceFile_rejectsCreatePermissionInDifferentDomain() {
        MultipartFile file = mock(MultipartFile.class);

        try (MockedStatic<SecurityUtil> ignored = authenticateWithPermission(
            uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FILE_HANDLING,
            FileHandlerPermission.CREATE_INTERFACE_FILES)) {
            PermissionNotAllowedException exception = assertThrows(PermissionNotAllowedException.class,
                () -> service.addInterfaceFile(file, addInterfaceFileMetadata()));
            assertEquals("[CREATE_INTERFACE_FILES] permission(s) are not enabled for the user.",
                exception.getMessage());
        }

        verifyNoInteractions(file, repository);
    }

    private MockedStatic<SecurityUtil> authenticateWithPermission(
        uk.gov.hmcts.opal.common.user.authorisation.model.Domain domain,
        FileHandlerPermission permission) {
        long userServicePermissionId = switch (permission) {
            case VIEW_INTERFACE_FILES -> 18L;
            case CREATE_INTERFACE_FILES -> 19L;
        };

        UserStateV2 userState = UserStateV2.builder()
            .userId(1L)
            .username("test")
            .domains(Map.of(domain, DomainBusinessUnitUsers.builder()
                .businessUnitUsers(List.of(BusinessUnitUser.builder()
                    .businessUnitUserId("BU1")
                    .businessUnitId((short) 1)
                    .permissions(Set.of(new Permission(userServicePermissionId, permission.getDescription())))
                    .build()))
                .build()))
            .build();

        MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class);
        securityUtil.when(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser).thenReturn(authToken);
        when(authToken.getUserState()).thenReturn(userState);

        return securityUtil;
    }

    private InterfaceFilesService serviceWithProcessor(
        InterfaceFileProcessorService processorService,
        BaisFileProcessorConfiguration config
    ) {
        return new InterfaceFilesService(
            repository,
            specsFactory,
            mapper,
            null,
            loggingService,
            Map.of("dwpBaisFileProcessorConfig", config),
            List.of(processorService));
    }

    private AddInterfaceFileRequestMetadata addInterfaceFileMetadata() {
        return new AddInterfaceFileRequestMetadata()
            .source(InterfaceFileEnumInterfaceFile.DWP)
            .target(InterfaceFileEnumInterfaceFile.OPAL)
            .type(InterfaceFileTypeEnumInterfaceFile.TRANSFORMED_JSON)
            .domain(DomainEnumTypes.FINES)
            .fileName("metadata-file-name.dat");
    }

    @Test
    public void getInterfaceFiles_unauthorisedUser_shouldThrowPermissionsException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            securityUtil.when(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser).thenReturn(authToken);
            when(authToken.getUserState()).thenReturn(UserStateV2.builder()
                .userId(1L)
                .username("test")
                .domains(Map.of())
                .build());

            assertThrows(PermissionNotAllowedException.class, () ->
                service.searchInterfaceFiles(new SearchInterfaceFilesDto()));

            verifyNoInteractions(specsFactory);
            verifyNoInteractions(repository);
            verifyNoInteractions(mapper);
        }
    }

    @Test
    void deleteInterfaceFiles_shouldRecursivelyDeleteRelatedFilesAndRemoveBlobs() {
        // Arrange
        final Long sourceId = 100L;
        final Long sourceId2 = 101L;
        final UUID sourceFilestoreUuid = UUID.fromString("aeda6410-a641-4b10-b422-d400bf7dd433");
        final UUID failedSourceJsonFilestoreUuid = UUID.fromString("0fb294db-a695-4df5-b324-f0e92b51b1d3");
        final UUID sourceJsonFilestoreUuid = UUID.fromString("33bae435-f0d6-4870-a005-afc4fae162c4");
        final UUID transformedJsonFilestoreUuid = UUID.fromString("515d3e37-ed95-4966-af98-e225412ef08c");
        final UUID sourceFilestoreUuid2 = UUID.fromString("a8027f50-6743-4ed9-bc16-4301a3db3a6f");
        final String natwestContainer = "natwest";
        final String barclaycardContainer = "barclaycard";

        InterfaceFileEntity sourceFile = InterfaceFileEntity.builder()
            .interfaceFileId(sourceId)
            .filestoreUuid(sourceFilestoreUuid)
            .type(Type.SOURCE)
            .source(Interface.NATWEST)
            .childRelatedInterfaceFiles(new HashSet<>())
            .build();

        InterfaceFileEntity failedSourceJsonFile = InterfaceFileEntity.builder()
            .interfaceFileId(200L)
            .filestoreUuid(failedSourceJsonFilestoreUuid)
            .type(Type.SOURCE_JSON)
            .source(Interface.NATWEST)
            .status(Status.FAILED)
            .relatedInterfaceFile(sourceFile)
            .childRelatedInterfaceFiles(new HashSet<>())
            .build();

        InterfaceFileEntity sourceJsonFile = InterfaceFileEntity.builder()
            .interfaceFileId(300L)
            .filestoreUuid(sourceJsonFilestoreUuid)
            .type(Type.SOURCE_JSON)
            .source(Interface.NATWEST)
            .relatedInterfaceFile(sourceFile)
            .childRelatedInterfaceFiles(new HashSet<>())
            .build();


        InterfaceFileEntity transformedJsonFile = InterfaceFileEntity.builder()
            .interfaceFileId(400L)
            .filestoreUuid(transformedJsonFilestoreUuid)
            .type(Type.TRANSFORMED_JSON)
            .source(Interface.NATWEST)
            .relatedInterfaceFile(sourceJsonFile)
            .childRelatedInterfaceFiles(new HashSet<>())
            .build();

        InterfaceFileEntity sourceFile2 = InterfaceFileEntity.builder()
            .interfaceFileId(sourceId2)
            .filestoreUuid(sourceFilestoreUuid2)
            .type(Type.SOURCE)
            .source(Interface.BARCLAYCARD)
            .childRelatedInterfaceFiles(new HashSet<>())
            .build();

        sourceFile.getChildRelatedInterfaceFiles().add(failedSourceJsonFile);
        sourceFile.getChildRelatedInterfaceFiles().add(sourceJsonFile);
        sourceJsonFile.getChildRelatedInterfaceFiles().add(transformedJsonFile);

        when(repository.findById(sourceId)).thenReturn(Optional.of(sourceFile));
        when(repository.findById(sourceId2)).thenReturn(Optional.of(sourceFile2));

        // Act
        Set<Long> ids = Set.of(sourceId, sourceId2);
        DeleteInterfaceFilesResultTestSupport result = service.deleteInterfaceFiles(ids);

        // Assert
        assertThat(result.getSuccessfullyDeletedIds()).containsExactlyInAnyOrder(sourceId, sourceId2);
        assertThat(result.getFailedIds()).isEmpty();

        ArgumentCaptor<UUID> filestoreUuidCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> containerCaptor = ArgumentCaptor.forClass(String.class);
        verify(blobStoreService, times(5)).deleteBlob(filestoreUuidCaptor.capture(), containerCaptor.capture());
        List<UUID> filestoreArgs = filestoreUuidCaptor.getAllValues();
        assertThat(filestoreArgs).containsExactlyInAnyOrder(
            transformedJsonFilestoreUuid, sourceJsonFilestoreUuid, failedSourceJsonFilestoreUuid,
            sourceFilestoreUuid, sourceFilestoreUuid2
        );
        assertThat(filestoreArgs.indexOf(transformedJsonFilestoreUuid))
            .isLessThan(filestoreArgs.indexOf(sourceJsonFilestoreUuid));
        assertThat(filestoreArgs.indexOf(sourceJsonFilestoreUuid))
            .isLessThan(filestoreArgs.indexOf(sourceFilestoreUuid));

        assertThat(containerCaptor.getAllValues()).containsExactlyInAnyOrder(
            natwestContainer, natwestContainer, natwestContainer, natwestContainer, barclaycardContainer
        );
        assertThat(containerCaptor.getAllValues().indexOf(barclaycardContainer))
            .isEqualTo(filestoreArgs.indexOf(sourceFilestoreUuid2));

        ArgumentCaptor<InterfaceFileEntity> interfaceFileCaptor = ArgumentCaptor.forClass(InterfaceFileEntity.class);
        verify(repository, times(5)).delete(interfaceFileCaptor.capture());
        List<InterfaceFileEntity> repositoryDeleteArgs = interfaceFileCaptor.getAllValues();
        assertThat(repositoryDeleteArgs).containsExactlyInAnyOrder(
            failedSourceJsonFile, transformedJsonFile, sourceJsonFile, sourceFile, sourceFile2
        );
        assertThat(repositoryDeleteArgs.indexOf(transformedJsonFile))
            .isLessThan(repositoryDeleteArgs.indexOf(sourceJsonFile));
        assertThat(repositoryDeleteArgs.indexOf(sourceJsonFile))
            .isLessThan(repositoryDeleteArgs.indexOf(sourceFile));
    }

    @Test
    void deleteInterfaceFiles_shouldDeleteFilesWithSameNameAndChecksumAndRemoveBlobs() {
        // Arrange
        final Long sourceId = 100L;
        final Long supersededSourceId = 101L;
        final UUID sourceFilestoreUuid = UUID.fromString("92383ff4-14b7-4174-a0c3-b8165fcd687b");
        final UUID supersededFilestoreUuid = UUID.fromString("0d6663dc-a174-4107-9a69-746c0ef203cf");
        final String fileName = "test-file";
        final String checksum = "check";
        final String natwestContainer = "natwest";

        InterfaceFileEntity sourceFile = InterfaceFileEntity.builder()
            .interfaceFileId(sourceId)
            .filestoreUuid(sourceFilestoreUuid)
            .type(Type.SOURCE)
            .fileName(fileName)
            .checksum(checksum)
            .source(Interface.NATWEST)
            .childRelatedInterfaceFiles(new HashSet<>())
            .build();

        InterfaceFileEntity supersededSourceFile = InterfaceFileEntity.builder()
            .interfaceFileId(supersededSourceId)
            .filestoreUuid(supersededFilestoreUuid)
            .type(Type.SOURCE)
            .source(Interface.NATWEST)
            .status(Status.FAILED_SUPERSEDED)
            .build();

        when(repository.findById(sourceId)).thenReturn(Optional.of(sourceFile));
        when(repository.findByTypeAndChecksumAndFileName(Type.SOURCE, checksum, fileName)).thenReturn(
            new ArrayList<>(List.of(sourceFile, supersededSourceFile)));

        // Act
        Set<Long> ids = Set.of(sourceId);
        DeleteInterfaceFilesResultTestSupport result = service.deleteInterfaceFiles(ids);

        // Assert
        assertThat(result.getSuccessfullyDeletedIds()).containsExactly(sourceId);
        assertThat(result.getFailedIds()).isEmpty();

        ArgumentCaptor<UUID> filestoreUuidCaptor = ArgumentCaptor.forClass(UUID.class);
        ArgumentCaptor<String> containerCaptor = ArgumentCaptor.forClass(String.class);
        verify(blobStoreService, times(2)).deleteBlob(filestoreUuidCaptor.capture(), containerCaptor.capture());
        assertThat(filestoreUuidCaptor.getAllValues()).containsExactly(supersededFilestoreUuid, sourceFilestoreUuid);
        assertThat(containerCaptor.getAllValues()).containsExactly(natwestContainer, natwestContainer);

        ArgumentCaptor<InterfaceFileEntity> interfaceFileCaptor = ArgumentCaptor.forClass(InterfaceFileEntity.class);
        verify(repository, times(2)).delete(interfaceFileCaptor.capture());
        List<InterfaceFileEntity> repositoryDeleteArgs = interfaceFileCaptor.getAllValues();
        assertThat(interfaceFileCaptor.getAllValues()).containsExactly(supersededSourceFile, sourceFile);

        ArgumentCaptor<Long> findByIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(repository, times(1)).findById(findByIdCaptor.capture());
        assertThat(findByIdCaptor.getValue()).isNotEqualTo(supersededSourceId);
    }

    @Test
    void deleteInterfaceFiles_shouldCorrectlyHandleFailedDeletions() {
        // Arrange
        final Long sourceId = 100L;
        final UUID sourceFilestoreUuid = UUID.fromString("d095de29-ba2b-4421-8b00-2dc003f85f12");
        final UUID sourceJsonFilestoreUuid = UUID.fromString("c59a81b5-5f53-4dec-9366-283690875706");
        final String barclaycardContainer = "barclaycard";

        InterfaceFileEntity sourceFile = InterfaceFileEntity.builder()
            .interfaceFileId(sourceId)
            .filestoreUuid(sourceFilestoreUuid)
            .type(Type.SOURCE)
            .source(Interface.BARCLAYCARD)
            .childRelatedInterfaceFiles(new HashSet<>())
            .build();

        InterfaceFileEntity sourceJsonFile = InterfaceFileEntity.builder()
            .interfaceFileId(200L)
            .filestoreUuid(sourceJsonFilestoreUuid)
            .type(Type.SOURCE_JSON)
            .source(Interface.BARCLAYCARD)
            .status(Status.SUCCESS)
            .relatedInterfaceFile(sourceFile)
            .childRelatedInterfaceFiles(new HashSet<>())
            .build();

        sourceFile.getChildRelatedInterfaceFiles().add(sourceJsonFile);
        when(repository.findById(sourceId)).thenReturn(Optional.of(sourceFile));
        when(blobStoreService.deleteBlob(sourceJsonFilestoreUuid, barclaycardContainer))
            .thenThrow(new BlobDeleteException(sourceFilestoreUuid, barclaycardContainer, new RuntimeException("")));

        // Act
        Set<Long> ids = Set.of(sourceId);
        DeleteInterfaceFilesResultTestSupport result = service.deleteInterfaceFiles(ids);

        // Assert
        assertThat(result.getFailedIds()).containsExactly(sourceId);
        assertThat(result.getSuccessfullyDeletedIds()).isEmpty();
        verify(repository, never()).delete(sourceFile);
        verify(repository, never()).delete(sourceJsonFile);
        verify(blobStoreService, never()).deleteBlob(sourceFilestoreUuid, barclaycardContainer);

    }

    @Test
    void deleteInterfaceFiles_notFoundIds_shouldThrowBadRequestException() {
        // Arrange
        Long validId = 10L;
        Long invalidId1 = 20L;
        Long invalidId2 = 30L;
        Set<Long> ids = Set.of(validId, invalidId1, invalidId2);
        when(repository.findById(validId)).thenReturn(Optional.of(mock(InterfaceFileEntity.class)));
        when(repository.findById(invalidId1)).thenReturn(Optional.empty());
        when(repository.findById(invalidId2)).thenReturn(Optional.empty());

        // Act + Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.deleteInterfaceFiles(ids));
        assertThat(ex.getTitle()).isEqualTo("Request to delete interface files failed validation");
        assertThat(ex.getDetail()).contains("The following interface files do not exist: [20, 30]");
    }

    @Test
    void deleteInterfaceFiles_sourceJson_transformedJson_shouldThrowBadRequestException() {
        // Arrange
        Long sourceId = 10L;
        Long sourceJsonId = 20L;
        Long transformedJsonId = 30L;
        Set<Long> ids = Set.of(sourceId, sourceJsonId, transformedJsonId);

        InterfaceFileEntity source = mock(InterfaceFileEntity.class);
        when(source.getType()).thenReturn(Type.SOURCE);
        InterfaceFileEntity sourceJson = mock(InterfaceFileEntity.class);
        when(sourceJson.getInterfaceFileId()).thenReturn(sourceJsonId);
        when(sourceJson.getType()).thenReturn(Type.SOURCE_JSON);
        InterfaceFileEntity transformedJson = mock(InterfaceFileEntity.class);
        when(transformedJson.getInterfaceFileId()).thenReturn(transformedJsonId);
        when(transformedJson.getType()).thenReturn(Type.TRANSFORMED_JSON);

        when(repository.findById(sourceId)).thenReturn(Optional.of(source));
        when(repository.findById(sourceJsonId)).thenReturn(Optional.of(sourceJson));
        when(repository.findById(transformedJsonId)).thenReturn(Optional.of(transformedJson));

        // Act + Assert
        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.deleteInterfaceFiles(ids));
        assertThat(ex.getTitle()).isEqualTo("Request to delete interface files failed validation");
        assertThat(ex.getDetail()).contains("The following interface files are not of type SOURCE: [20, 30]");
    }

}
