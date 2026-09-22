package uk.gov.hmcts.opal.filehandler.entity;

import lombok.Getter;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;

@Getter
public enum Interface {
    NATWEST("NatWestBaisFileBaisFileProcessorConfig"),
    ALLPAY("AllpayBaisFileBaisFileProcessorConfig"),
    ALLPAY_DD(null),
    BARCLAYCARD("BarclaycardBaisFileProcessorConfig"),
    BTECKOH("BTEckohBaisFileProcessorConfig"),
    DWP("dwpBaisFileProcessorConfig"),
    CDER("cderBaisFileProcessorConfig"),
    JACOBS("JacobsBaisFileBaisFileProcessorConfig"),
    MARSTON(null),
    BTECKOH_REPORT("BTEckohReportBaisFileProcessorConfig"),
    CAPS_REPORT("capsReportBaisFileProcessorConfig"),
    OPAL(null);

    private final String configComponentName;

    Interface(String configComponentName) {
        this.configComponentName = configComponentName;
    }

    public static Interface valueOf(InterfaceFileEnumInterfaceFile interfaceFileEnumInterfaceFile){
        return Interface.valueOf(interfaceFileEnumInterfaceFile.name());
    }
}
