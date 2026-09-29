package uk.gov.hmcts.opal.filehandler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

@ExtendWith(MockitoExtension.class)
class UserStateServiceTest {

    private final UserStateService userStateService = new UserStateService();

    @Mock
    private OpalJwtAuthenticationToken authToken;

    @Mock
    private Authentication unexpectedAuthentication;

    @Mock
    private UserStateV2 userState;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserStateFromSecurityContext_returnsUserStateFromOpalToken() {
        SecurityContextHolder.getContext().setAuthentication(authToken);
        when(authToken.getUserState()).thenReturn(userState);

        UserStateV2 result = userStateService.getUserStateFromSecurityContext();

        assertSame(userState, result);
    }

    @Test
    void getUserStateFromSecurityContext_rejectsUnexpectedAuthenticationType() {
        SecurityContextHolder.getContext().setAuthentication(unexpectedAuthentication);

        AccessDeniedException exception = assertThrows(
            AccessDeniedException.class,
            userStateService::getUserStateFromSecurityContext
        );

        assertEquals("Unexpected token type", exception.getMessage());
    }

    @Test
    void getUserStateFromSecurityContext_rejectsOpalTokenWithoutUserState() {
        SecurityContextHolder.getContext().setAuthentication(authToken);
        when(authToken.getUserState()).thenReturn(null);

        AccessDeniedException exception = assertThrows(
            AccessDeniedException.class,
            userStateService::getUserStateFromSecurityContext
        );

        assertEquals("User state not found in token", exception.getMessage());
    }
}
