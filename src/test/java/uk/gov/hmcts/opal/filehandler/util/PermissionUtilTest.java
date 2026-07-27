package uk.gov.hmcts.opal.filehandler.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.util.SecurityUtil;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;

@ExtendWith(MockitoExtension.class)
public class PermissionUtilTest {

    @Mock
    private OpalJwtAuthenticationToken authToken;

    private MockedStatic<SecurityUtil> securityUtil;

    @BeforeEach
    public void setup() {
        securityUtil = mockStatic(SecurityUtil.class);
        securityUtil.when(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser).thenReturn(authToken);
    }

    @AfterEach
    public void teardown() {
        securityUtil.close();
    }

    @Test
    public void checkPermissionReturnsTrue() {
        when(authToken.hasPermission(FileHandlerPermission.ViewInterfacesFile)).thenReturn(true);

        PermissionUtil.checkPermission(FileHandlerPermission.ViewInterfacesFile);

        securityUtil.verify(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser);
    }

    @Test
    public void checkPermissionFailedThrowsError() {
        when(authToken.hasPermission(FileHandlerPermission.ViewInterfacesFile)).thenReturn(false);

        Exception e = assertThrows(
            PermissionNotAllowedException.class,
            () -> PermissionUtil.checkPermission(FileHandlerPermission.ViewInterfacesFile)
        );
        assertEquals("[ViewInterfacesFile] permission(s) are not enabled for the user.", e.getMessage());

        securityUtil.verify(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser);
    }
}
