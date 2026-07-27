package uk.gov.hmcts.opal.filehandler.util;

import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;
import uk.gov.hmcts.opal.common.util.SecurityUtil;

public class PermissionUtil {

    public static void checkPermission(PermissionDescriptor permission) {
        if (!SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser().hasPermission(permission)) {
            throw new PermissionNotAllowedException(permission);
        }
    }
}
