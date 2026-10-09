package uk.gov.hmcts.opal.filehandler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.filehandler.UserStateService")
public class UserStateService {
    public UserStateV2 getUserStateFromSecurityContext() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OpalJwtAuthenticationToken authToken)) {
            throw new AccessDeniedException("Unexpected token type");
        }
        UserStateV2 userState = authToken.getUserState();
        if (userState == null) {
            throw new AccessDeniedException("User state not found in token");
        }
        return userState;
    }
}
