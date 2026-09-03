package uk.gov.hmcts.opal.filehandler.service;

import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.opal.filehandler.config.BTEckohBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.PaymentType;
import uk.gov.hmcts.opal.filehandler.service.queue.InterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.support.AbstractBacsStandard18BaisFileProcessorServiceIntegrationTest;

@ActiveProfiles("integration")
@TestPropertySource(properties = {
    "opal.file-handler-service.file-types.bteckoh.sftp-username=BTEckoh",
    "opal.file-handler-service.file-types.bteckoh.container-name=bteckoh"
})
@DisplayName("BTEckoh BACS Standard 18 File Processor Integration Tests")
class BTEckohBaisFileProcessorServiceIntegrationTest
    extends AbstractBacsStandard18BaisFileProcessorServiceIntegrationTest {

    private static final BacsStandard18Fixture FIXTURE = new BacsStandard18Fixture(
        "a121_00350005_300000.dat",
        "bais-emulator/a121_00350005_300000.dat",
        "bbecbed9c565374b110b7113ecceae03",
        Interface.BTECKOH,
        Interface.OPAL,
        "AB01",
        Domain.MAINTENANCE,
        PaymentType.CASH,
        "560033",
        "27048527"
    );

    @Autowired
    private BTEckohBaisFileProcessorService processor;

    @Autowired
    private BTEckohBaisFileProcessorConfiguration configuration;

    @MockitoSpyBean
    private MaintenanceInterfaceFilePreprocessQueueService maintenanceQueueService;

    @Override
    protected AbstractInterfaceFileProcessorService processor() {
        return processor;
    }

    @Override
    protected BaisFileProcessorConfiguration processorConfiguration() {
        return configuration;
    }

    @Override
    protected InterfaceFilePreprocessQueueService queueService() {
        return maintenanceQueueService;
    }

    @Override
    protected BacsStandard18Fixture validFixture() {
        return FIXTURE;
    }

    @Override
    protected String unsupportedFileName() {
        return "a121_00350005_300000.txt";
    }
}
