package uk.gov.hmcts.opal.filehandler.authorisation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;

@AllArgsConstructor
@Getter
public enum FileHandlerPermission implements PermissionDescriptor {
    ViewInterfacesFile(1L, "Can view interface files");

    private final long id;
    private final String description;
}
