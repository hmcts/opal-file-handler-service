package uk.gov.hmcts.opal.filehandler.entity;

import lombok.Getter;
import uk.gov.hmcts.opal.filehandler.service.AllpayInterfaceFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.BTEckohReportInterfaceFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.BarclaycardInterfaceFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.CapsReportInterfaceFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.CderInterfaceFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.DWPInterfaceFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.InterfaceFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.JacobsInterfaceFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.MarstonInterfaceFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.NatWestInterfaceFileProcessorService;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;

@Getter
public enum Interface {
    NATWEST("NatWestBaisFileBaisFileProcessorConfig", NatWestInterfaceFileProcessorService.class),
    ALLPAY("AllpayBaisFileBaisFileProcessorConfig", AllpayInterfaceFileProcessorService.class),
    ALLPAY_DD(null, AllpayInterfaceFileProcessorService.class),
    BARCLAYCARD("BarclaycardBaisFileProcessorConfig", BarclaycardInterfaceFileProcessorService.class),
    BTECKOH("BTEckohBaisFileProcessorConfig", BTEckohReportInterfaceFileProcessorService.class),
    DWP("dwpBaisFileProcessorConfig", DWPInterfaceFileProcessorService.class),
    CDER("cderBaisFileProcessorConfig", CderInterfaceFileProcessorService.class),
    JACOBS("JacobsBaisFileProcessorConfig", JacobsInterfaceFileProcessorService.class),
    MARSTON("marstonBaisFileBaisFileProcessorConfig", MarstonInterfaceFileProcessorService.class),
    BTECKOH_REPORT("BTEckohReportBaisFileProcessorConfig", BTEckohReportInterfaceFileProcessorService.class),
    CAPS_REPORT("capsReportBaisFileProcessorConfig", CapsReportInterfaceFileProcessorService.class),
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
