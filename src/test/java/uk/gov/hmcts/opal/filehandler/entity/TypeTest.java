package uk.gov.hmcts.opal.filehandler.entity;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.opal.generated.model.InterfaceFileTypeEnumInterfaceFile;

class TypeTest {

    @Test
    void valueOf_mapsAllKnownTypes() {
        assertAll(
            () -> assertEquals(Type.SOURCE, Type.valueOf(InterfaceFileTypeEnumInterfaceFile.SOURCE)),
            () -> assertEquals(Type.SOURCE_JSON, Type.valueOf(InterfaceFileTypeEnumInterfaceFile.SOURCE_JSON)),
            () -> assertEquals(Type.TRANSFORMED_JSON,
                Type.valueOf(InterfaceFileTypeEnumInterfaceFile.TRANSFORMED_JSON))
        );
    }

    @Test
    void valueOf_throwsWhenTypeIsNull() {
        assertThrows(NullPointerException.class, () -> Type.valueOf((InterfaceFileTypeEnumInterfaceFile) null));
    }
}

