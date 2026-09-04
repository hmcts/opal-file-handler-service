package uk.gov.hmcts.opal.filehandler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.util.SecurityUtil;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.exception.InterfaceFileNotFoundException;
import uk.gov.hmcts.opal.filehandler.mapper.InterfaceFileMapper;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.repository.specs.InterfaceFileSpecsFactory;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;
import uk.gov.hmcts.opal.filehandler.util.PermissionUtil;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;

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

    @InjectMocks
    private InterfaceFilesService service;


    @Test
    public void getInterfaceFiles_shouldOrchestrateCallsCorrectly() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            // Removed pending https://tools.hmcts.net/jira/browse/PO-8686
            //when(authToken.hasPermission(FileHandlerPermission.ViewInterfacesFile)).thenReturn(true);
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
    void getInterfaceFile_shouldFetchAndMapResult() {
        try (MockedStatic<PermissionUtil> permissionUtil = mockStatic(PermissionUtil.class)) {
            Long id = 103L;
            InterfaceFileEntity entity = mock(InterfaceFileEntity.class);
            InterfaceFileObjectInterfaceFile mapped = mock(InterfaceFileObjectInterfaceFile.class);
            InterfaceFilesService spyService = spy(service);

            doReturn(entity).when(spyService).getInterfaceFileEntity(id);
            when(mapper.toInterfaceFileObject(entity)).thenReturn(mapped);

            InterfaceFileObjectInterfaceFile result = spyService.getInterfaceFile(id);

            assertEquals(mapped, result);
            permissionUtil.verify(() -> PermissionUtil.checkPermission(FileHandlerPermission.ViewInterfacesFile));
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

    /* Removed pending https://tools.hmcts.net/jira/browse/PO-8686
    @Test
    public void getInterfaceFiles_unauthorisedUser_shouldThrowPermissionsException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            when(authToken.hasPermission(FileHandlerPermission.ViewInterfacesFile)).thenReturn(false);
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
