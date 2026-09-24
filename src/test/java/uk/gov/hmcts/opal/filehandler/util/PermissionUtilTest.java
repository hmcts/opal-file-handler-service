package uk.gov.hmcts.opal.filehandler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.user.authorisation.model.BusinessUnitUser;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.DomainBusinessUnitUsers;
import uk.gov.hmcts.opal.common.user.authorisation.model.Permission;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.opal.common.util.SecurityUtil;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;

@ExtendWith(MockitoExtension.class)
class PermissionUtilTest {

    @Mock
    private OpalJwtAuthenticationToken authToken;

    private MockedStatic<SecurityUtil> securityUtil;

    @BeforeEach
    void setup() {
        securityUtil = mockStatic(SecurityUtil.class);
        securityUtil.when(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser).thenReturn(authToken);
    }

    @AfterEach
    void teardown() {
        securityUtil.close();
    }

    @Test
    void checkPermissionReturnsTrue() {
        when(authToken.hasPermission(FileHandlerPermission.VIEW_INTERFACE_FILES)).thenReturn(true);

        PermissionUtil.checkPermission(FileHandlerPermission.VIEW_INTERFACE_FILES);

        securityUtil.verify(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser);
    }

    @Test
    void checkPermissionFailedThrowsError() {
        when(authToken.hasPermission(FileHandlerPermission.VIEW_INTERFACE_FILES)).thenReturn(false);

        Exception e = assertThrows(
            PermissionNotAllowedException.class,
            () -> PermissionUtil.checkPermission(FileHandlerPermission.VIEW_INTERFACE_FILES)
        );
        assertEquals("[VIEW_INTERFACE_FILES] permission(s) are not enabled for the user.", e.getMessage());

        securityUtil.verify(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser);
    }

    @Test
    void checkPermissionDomainAgnosticReturnsWhenAnyDomainHasPermission() {
        when(authToken.getUserState()).thenReturn(userStateWithPermissions(Map.of(
            Domain.FINES, permission("view interface files")
        )));

        PermissionUtil.checkPermissionDomainAgnostic(FileHandlerPermission.VIEW_INTERFACE_FILES);

        securityUtil.verify(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser);
    }

    @Test
    void checkPermissionDomainAgnosticThrowsWhenNoDomainHasPermission() {
        when(authToken.getUserState()).thenReturn(userStateWithPermissions(Map.of(
            Domain.FINES, permission("Manage Fines")
        )));

        Exception e = assertThrows(
            PermissionNotAllowedException.class,
            () -> PermissionUtil.checkPermissionDomainAgnostic(FileHandlerPermission.VIEW_INTERFACE_FILES)
        );
        assertEquals("[VIEW_INTERFACE_FILES] permission(s) are not enabled for the user.", e.getMessage());

        securityUtil.verify(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser);
    }

    @Test
    void checkPermissionInDomainReturnsWhenSelectedDomainHasPermission() {
        when(authToken.getUserState()).thenReturn(userStateWithPermissions(Map.of(
            Domain.FILE_HANDLING, permission("view interface files")
        )));

        PermissionUtil.checkPermissionInDomain(FileHandlerPermission.VIEW_INTERFACE_FILES, Domain.FILE_HANDLING);

        securityUtil.verify(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser);
    }

    @Test
    void checkPermissionInDomainThrowsWhenOnlyDifferentDomainHasPermission() {
        when(authToken.getUserState()).thenReturn(userStateWithPermissions(Map.of(
            Domain.FINES, permission("View Interface Files")
        )));

        Exception e = assertThrows(
            PermissionNotAllowedException.class,
            () -> PermissionUtil.checkPermissionInDomain(
                FileHandlerPermission.VIEW_INTERFACE_FILES,
                Domain.FILE_HANDLING
            )
        );
        assertEquals("[VIEW_INTERFACE_FILES] permission(s) are not enabled for the user.", e.getMessage());

        securityUtil.verify(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser);
    }

    private UserStateV2 userStateWithPermissions(Map<Domain, Permission> domainPermissions) {
        return UserStateV2.builder()
            .userId(1L)
            .username("test-user")
            .domains(domainPermissions.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> DomainBusinessUnitUsers.builder()
                    .businessUnitUsers(List.of(BusinessUnitUser.builder()
                        .businessUnitUserId("BU_USER")
                        .businessUnitId((short) 1)
                        .permissions(Set.of(entry.getValue()))
                        .build()))
                    .build()
            )))
            .build();
    }

    private Permission permission(String description) {
        return Permission.builder()
            .permissionId(99L)
            .permissionName(description)
            .build();
    }
}
