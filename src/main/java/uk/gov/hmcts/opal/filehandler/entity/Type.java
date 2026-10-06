package uk.gov.hmcts.opal.filehandler.entity;

import uk.gov.hmcts.opal.generated.model.InterfaceFileTypeEnumInterfaceFile;

public enum Type {
    SOURCE,
    SOURCE_JSON,
    TRANSFORMED_JSON;

    public static Type valueOf(InterfaceFileTypeEnumInterfaceFile type) {
        return Type.valueOf(type.name());
    }
}
