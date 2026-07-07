package uk.gov.hmcts.opal.filehandler.entity;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class InterfaceFileSourceTest {
    @Test
    void getByLabel_shouldReturnEnum_forKnownLabel() {
        assertAll(
            () -> assertEquals(InterfaceFileSource.NATWEST, InterfaceFileSource.fromCode("NATWEST")),
            () -> assertEquals(InterfaceFileSource.ALLPAY, InterfaceFileSource.fromCode("ALLPAY")),
            () -> assertEquals(InterfaceFileSource.ALLPAY_DD, InterfaceFileSource.fromCode("ALLPAY_DD")),
            () -> assertEquals(InterfaceFileSource.BARCLAYCARD, InterfaceFileSource.fromCode("BARCLAYCARD")),
            () -> assertEquals(InterfaceFileSource.BTECKOH, InterfaceFileSource.fromCode("BTECKOH")),
            () -> assertEquals(InterfaceFileSource.DWP, InterfaceFileSource.fromCode("DWP")),
            () -> assertEquals(InterfaceFileSource.CDER, InterfaceFileSource.fromCode("CDER")),
            () -> assertEquals(InterfaceFileSource.JACOBS, InterfaceFileSource.fromCode("JACOBS")),
            () -> assertEquals(InterfaceFileSource.MARSTON, InterfaceFileSource.fromCode("MARSTON")),
            () -> assertEquals(InterfaceFileSource.OTHER, InterfaceFileSource.fromCode("OTHER"))
        );
    }

    @Test
    void getByLabel_shouldThrow_forUnknownLabel() {
        assertThrows(IllegalArgumentException.class, () -> InterfaceFileSource.fromCode("Unknown"));
    }
}
