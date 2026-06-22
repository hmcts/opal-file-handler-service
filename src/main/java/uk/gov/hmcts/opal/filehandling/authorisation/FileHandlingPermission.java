package uk.gov.hmcts.opal.filehandling.authorisation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.opal.common.user.authorisation.model.PermissionDescriptor;

@AllArgsConstructor
@Getter
public enum FileHandlingPermission implements PermissionDescriptor {
    ;


    private final long id;
    private final String description;
}
