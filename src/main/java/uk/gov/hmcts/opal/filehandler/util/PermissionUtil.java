package uk.gov.hmcts.opal.filehandler.util;

import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;

public class PermissionUtil {

    public static void checkPermission(PermissionDescriptor permission) {
        if (!uk.gov.hmcts.opal.common.util.SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser()
            .hasPermission(permission)) {
            throw new PermissionNotAllowedException(permission);
        }
    }
}
