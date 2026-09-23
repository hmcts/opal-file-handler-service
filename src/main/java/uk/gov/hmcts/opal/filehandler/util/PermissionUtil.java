package uk.gov.hmcts.opal.filehandler.util;

import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.user.authorisation.model.Domain;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;
import uk.gov.hmcts.opal.common.util.SecurityUtil;

public class PermissionUtil {

    public static void checkPermission(PermissionDescriptor permission) {
        if (!SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser().hasPermission(permission)) {
            throw new PermissionNotAllowedException(permission);
        }
    }

    public static void checkPermissionDomainAgnostic(PermissionDescriptor permission) {
        boolean hasPermission = SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser()
            .getUserState()
            .getDomains()
            .values()
            .stream()
            .flatMap(domain -> domain.getBusinessUnitUsers().stream())
            .flatMap(buUser -> buUser.getPermissions().stream())
            //TODO update to code checks once PO-10759 is played
            .filter(perm -> perm.getDescription().equalsIgnoreCase(permission.getDescription()))
            .findFirst().isPresent();
        if (!hasPermission) {
            throw new PermissionNotAllowedException(permission);
        }
    }


    public static void checkPermissionInDomain(PermissionDescriptor permission, Domain domain) {
        boolean hasPermission = SecurityUtil.getOpalJwtAuthenticationTokenForCurrentUser()
            .getUserState()
            .getDomainBusinessUnitUsers(domain)
            .getBusinessUnitUsers()
            .stream()
            .flatMap(buUser -> buUser.getPermissions().stream())
            //TODO update to code checks once PO-10759 is played
            .filter(perm -> perm.getDescription().equalsIgnoreCase(permission.getDescription()))
            .findFirst().isPresent();
        if (!hasPermission) {
            throw new PermissionNotAllowedException(permission);
        }
    }
}
