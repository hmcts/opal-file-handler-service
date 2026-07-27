package uk.gov.hmcts.opal.filehandler.entity;

import lombok.Getter;

@Getter
public enum Interface {
    BTECKOH_REPORT("BTEckohReportBaisFileProcessorConfig"),
    CAPS_REPORT("capsReportBaisFileProcessorConfig"),
    OPAL(null);

    private final String configComponentName;

    Interface(String configComponentName) {
        this.configComponentName = configComponentName;
    }
}
