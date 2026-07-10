package uk.gov.hmcts.opal.filehandler.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
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
import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.util.SecurityUtil;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.mapper.InterfaceFileMapper;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.repository.specs.InterfaceFileSpecs;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;

@ExtendWith(MockitoExtension.class)
public class InterfaceFilesServiceTest {

    @Mock
    private InterfaceFileMapper mapper;

    @Mock
    private InterfaceFilesRepository repository;

    @Mock
    private InterfaceFileSpecs specBuilder;

    @Mock
    private OpalJwtAuthenticationToken authToken;

    @Mock
    private Specification<InterfaceFileEntity> specification;

    @InjectMocks
    private InterfaceFilesService service;


    @Test
    public void getInterfaceFiles_shouldOrchestrateCallsCorrectly() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            when(authToken.hasPermission(FileHandlerPermission.ViewInterfacesFile)).thenReturn(true);
            securityUtil.when(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser).thenReturn(authToken);
            List<InterfaceFileEntity> interfaceFiles = List.of(
                mock(InterfaceFileEntity.class)
            );
            SearchInterfaceFilesDto searchDto = new SearchInterfaceFilesDto();
            when(specBuilder.findBySearchCriteria(searchDto)).thenReturn(specification);
            when(repository.findAll(
                specification, Sort.by(Direction.ASC, TypedPropertyPath.of(InterfaceFileEntity::getCreatedDatetime)))
            ).thenReturn(interfaceFiles);

            service.searchInterfaceFiles(searchDto);

            verify(mapper).toInterfaceFileObjects(interfaceFiles);
        }
    }

    @Test
    public void getInterfaceFiles_unauthorisedUser_shouldThrowPermissionsException() {
        try (MockedStatic<SecurityUtil> securityUtil = mockStatic(SecurityUtil.class)) {
            when(authToken.hasPermission(FileHandlerPermission.ViewInterfacesFile)).thenReturn(false);
            securityUtil.when(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser).thenReturn(authToken);

            assertThrows(PermissionNotAllowedException.class, () -> service.searchInterfaceFiles(new SearchInterfaceFilesDto()));
            verifyNoInteractions(specBuilder);
            verifyNoInteractions(repository);
            verifyNoInteractions(mapper);
        }
    }

}
