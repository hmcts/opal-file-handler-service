package uk.gov.hmcts.opal.filehandler.entity;

import lombok.Getter;
import uk.gov.hmcts.opal.filehandler.service.AllpayBaisFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.BTEckohReportBaisFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.BarclaycardBaisFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.CapsReportBaisFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.CderBaisFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.DWPBaisFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.InterfaceFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.JacobsBaisFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.NatWestBaisFileProcessorService;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;

@Getter
public enum Interface {
    NATWEST("NatWestBaisFileBaisFileProcessorConfig", NatWestBaisFileProcessorService.class),
    ALLPAY("AllpayBaisFileBaisFileProcessorConfig", AllpayBaisFileProcessorService.class),
    ALLPAY_DD(null, AllpayBaisFileProcessorService.class),
    BARCLAYCARD("BarclaycardBaisFileProcessorConfig", BarclaycardBaisFileProcessorService.class),
    BTECKOH("BTEckohBaisFileProcessorConfig", BTEckohReportBaisFileProcessorService.class),
    DWP("dwpBaisFileProcessorConfig", DWPBaisFileProcessorService.class),
    CDER("cderBaisFileProcessorConfig", CderBaisFileProcessorService.class),
    JACOBS("JacobsBaisFileBaisFileProcessorConfig", JacobsBaisFileProcessorService.class),
    MARSTON(null, null),
    BTECKOH_REPORT("BTEckohReportBaisFileProcessorConfig", BTEckohReportBaisFileProcessorService.class),
    CAPS_REPORT("capsReportBaisFileProcessorConfig", CapsReportBaisFileProcessorService.class),
    OPAL(null, InterfaceFileProcessorService.class);

    private final String configComponentName;
    private final Class<? extends InterfaceFileProcessorService> processorServiceClass;

    Interface(String configComponentName, Class<? extends InterfaceFileProcessorService> processorServiceClass) {
        this.configComponentName = configComponentName;
        this.processorServiceClass = processorServiceClass;
    }

    public static Interface valueOf(InterfaceFileEnumInterfaceFile interfaceFileEnumInterfaceFile) {
        return Interface.valueOf(interfaceFileEnumInterfaceFile.name());
    }
}
