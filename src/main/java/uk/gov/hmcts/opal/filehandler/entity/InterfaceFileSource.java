package uk.gov.hmcts.opal.filehandler.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum InterfaceFileSource {
    NATWEST("NATWEST"),
    ALLPAY("ALLPAY"),
    ALLPAY_DD("ALLPAY_DD"),
    BARCLAYCARD("BARCLAYCARD"),
    BTECKOH("BTECKOH"),
    DWP("DWP"),
    CDER("CDER"),
    JACOBS("JACOBS"),
    MARSTON("MARSTON"),
    OTHER("OTHER");

    @Getter
    private final String code;

    public static InterfaceFileSource fromCode(String code) {
        for (InterfaceFileSource value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown code: " + code);
    }
}
