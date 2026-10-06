package uk.gov.hmcts.opal.filehandler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.util.SecurityUtil;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.PaymentType;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.exception.InterfaceFileNotFoundException;
import uk.gov.hmcts.opal.filehandler.mapper.InterfaceFileMapper;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.repository.specs.InterfaceFileSpecsFactory;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;
import uk.gov.hmcts.opal.filehandler.util.PermissionUtil;
import uk.gov.hmcts.opal.generated.model.AddInterfaceFileRequestMetadata;
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
    private OpalJwtAuthenticationToken authToken;

    @Mock
    private Specification<InterfaceFileEntity> specification;

    @Mock
    private List<InterfaceFileProcessorService> processorServicesList;

    @InjectMocks
    private InterfaceFilesService service;


    @Test
    public void getInterfaceFiles_shouldOrchestrateCallsCorrectly() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            // Removed pending https://tools.hmcts.net/jira/browse/PO-8686
            //when(authToken.hasPermission(FileHandlerPermission.VIEW_INTERFACE_FILES)).thenReturn(true);
            securityUtil.when(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser).thenReturn(authToken);
            List<InterfaceFileEntity> interfaceFiles = List.of(
                mock(InterfaceFileEntity.class)
            );
            SearchInterfaceFilesDto searchDto = new SearchInterfaceFilesDto();
            when(specsFactory.createSearchSpecs(searchDto)).thenReturn(specification);
            when(repository.findAll(
                specification, Sort.by(Direction.ASC, TypedPropertyPath.of(InterfaceFileEntity::getCreatedDatetime)))
            ).thenReturn(interfaceFiles);

            service.searchInterfaceFiles(searchDto);

            verify(mapper).toInterfaceFileObjects(interfaceFiles);
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
        InterfaceFilesService spyService = spy(serviceWithProcessor(processorService, config));
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
        when(processorService.ingestFile(
            "payments.dat",
            fileBytes,
            Interface.DWP,
            Interface.OPAL,
            Type.TRANSFORMED_JSON,
            Domain.FINES,
            "dwp-container"
        )).thenReturn(ingestedEntity);
        when(repository.save(ingestedEntity)).thenReturn(ingestedEntity);
        doReturn(mapped).when(spyService).getInterfaceFile(24L);

        InterfaceFileObjectInterfaceFile result = spyService.addInterfaceFile(file, metadata);

        assertEquals(mapped, result);
        assertEquals(relatedInterfaceFile, ingestedEntity.getRelatedInterfaceFile());
        assertEquals(PaymentType.CHEQUE, ingestedEntity.getPaymentType());
        assertArrayEquals(new String[] {"1234"}, ingestedEntity.getBusinessUnitCode());
        verify(repository).findById(15L);
        verify(repository).save(ingestedEntity);
        verify(spyService).getInterfaceFile(24L);
    }

    @Test
    void addInterfaceFile_savesEntityWithoutRelatedFileWhenNoRelatedIdProvided() throws IOException {
        DWPBaisFileProcessorService processorService = mock(DWPBaisFileProcessorService.class);
        BaisFileProcessorConfiguration config = mock(BaisFileProcessorConfiguration.class);
        InterfaceFilesService spyService = spy(serviceWithProcessor(processorService, config));
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
        when(processorService.ingestFile(
            "source.dat",
            fileBytes,
            Interface.DWP,
            Interface.OPAL,
            Type.TRANSFORMED_JSON,
            Domain.FINES,
            "dwp-container"
        )).thenReturn(ingestedEntity);
        when(repository.save(ingestedEntity)).thenReturn(ingestedEntity);
        doReturn(mapped).when(spyService).getInterfaceFile(25L);

        InterfaceFileObjectInterfaceFile result = spyService.addInterfaceFile(file, metadata);

        assertEquals(mapped, result);
        assertNull(ingestedEntity.getRelatedInterfaceFile());
        assertEquals(PaymentType.CASH, ingestedEntity.getPaymentType());
        assertArrayEquals(new String[] {"9876"}, ingestedEntity.getBusinessUnitCode());
        verify(repository).save(ingestedEntity);
        verify(spyService).getInterfaceFile(25L);
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

        InternalServerErrorException exception = assertThrows(
            InternalServerErrorException.class,
            () -> service.addInterfaceFile(file, addInterfaceFileMetadata())
        );

        assertEquals(ioException, exception.getCause());
        assertEquals(
            "Failed to read file content for interface file creation ",
            exception.getMessage()
        );
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
            Map.of("dwpBaisFileProcessorConfig", config),
            List.of(processorService)
        );
    }

    private AddInterfaceFileRequestMetadata addInterfaceFileMetadata() {
        return new AddInterfaceFileRequestMetadata()
            .source(InterfaceFileEnumInterfaceFile.DWP)
            .target(InterfaceFileEnumInterfaceFile.OPAL)
            .type(InterfaceFileTypeEnumInterfaceFile.TRANSFORMED_JSON)
            .domain(DomainEnumTypes.FINES)
            .fileName("metadata-file-name.dat");
    }

    /* Removed pending https://tools.hmcts.net/jira/browse/PO-8686
    @Test
    public void getInterfaceFiles_unauthorisedUser_shouldThrowPermissionsException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            when(authToken.hasPermission(FileHandlerPermission.VIEW_INTERFACE_FILES)).thenReturn(false);
            securityUtil.when(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser).thenReturn(authToken);

            assertThrows(PermissionNotAllowedException.class, () ->
                service.searchInterfaceFiles(new SearchInterfaceFilesDto())
            );
            verifyNoInteractions(specsFactory);
            verifyNoInteractions(repository);
            verifyNoInteractions(mapper);
        }
    }
    */

}
