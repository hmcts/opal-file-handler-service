package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import uk.gov.hmcts.opal.filehandler.config.AllpayBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.PaymentType;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.service.queue.InterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.support.AbstractBacsStandard18BaisFileProcessorServiceIntegrationTest;

@ActiveProfiles("integration")
@TestPropertySource(properties = {
    "opal.file-handler-service.file-types.allpay.sftp-username=AllPay",
    "opal.file-handler-service.file-types.allpay.container-name=allpay"
})
@DisplayName("AllPay BACS Standard 18 File Processor Integration Tests")
public class AllpayBaisFileProcessorServiceIntegrationTest
    extends AbstractBacsStandard18BaisFileProcessorServiceIntegrationTest {

    private static final String FILE_STEM = "a121_00350005_300000";
    private static final BacsStandard18Fixture FIXTURE = new BacsStandard18Fixture(
        FILE_STEM + ".dat",
        "bais-emulator/" + FILE_STEM + ".dat",
        "bbecbed9c565374b110b7113ecceae03",
        Interface.ALLPAY,
        Interface.OPAL,
        "AB01",
        Domain.MAINTENANCE,
        PaymentType.CASH,
        "560033",
        "27048527"
    );

    @Autowired
    private AllpayBaisFileProcessorService processor;

    @Autowired
    private AllpayBaisFileProcessorConfiguration configuration;

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
        return FILE_STEM + ".txt";
    }

    @ParameterizedTest
    @ValueSource(strings = {".crf", ".dir", ".err", ".sta"})
    @DisplayName("Supported non-DAT AllPay files are stored without extraction or queueing")
    void shouldStoreSupportedNonDatFileWithoutExtracting(String fileExtension) {
        String fileName = FILE_STEM + fileExtension;

        uploadFixture(fileName);
        processor.run(configuration);

        assertSuccessfulInterfaceFile(
            fileName, FIXTURE.checksum(), FIXTURE.source(), Type.SOURCE, FIXTURE.domain());
        assertThat(repository.findAll())
            .filteredOn(interfaceFile -> interfaceFile.getType() == Type.SOURCE_JSON)
            .isEmpty();
        verify(maintenanceQueueService, never()).send(anyLong());
        assertBlobChecksum(fileName, FIXTURE.checksum(), configuration.getContainerName());
        assertNumberOfSftpFiles(configuration.getSftpUsername(), 0);
    }
}
