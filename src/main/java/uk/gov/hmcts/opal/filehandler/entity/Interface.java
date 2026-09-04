package uk.gov.hmcts.opal.filehandler.entity;

import lombok.Getter;

@Getter
public enum Interface {
    NATWEST(null),
    ALLPAY("AllpayBaisFileBaisFileProcessorConfig"),
    ALLPAY_DD(null),
    BARCLAYCARD("BarclaycardBaisFileBaisFileProcessorConfig"),
    BTECKOH(null),
    DWP(null),
    CDER(null),
    JACOBS(null),
    MARSTON(null),
    BTECKOH_REPORT("BTEckohReportBaisFileProcessorConfig"),
    CAPS_REPORT("capsReportBaisFileProcessorConfig"),
    OPAL(null);

    private final String configComponentName;

    Interface(String configComponentName) {
        this.configComponentName = configComponentName;
    }
}
