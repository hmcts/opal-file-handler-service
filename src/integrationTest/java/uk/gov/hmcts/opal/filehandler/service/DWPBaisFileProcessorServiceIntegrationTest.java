package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.filehandler.config.DWPBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.PaymentType;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.support.AbstractBaisFileProcessorServiceIntegrationTest;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraEpic;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraStory;

@ActiveProfiles("integration")
@Slf4j
public class DWPBaisFileProcessorServiceIntegrationTest
    extends AbstractBaisFileProcessorServiceIntegrationTest {

    private static final String CHECKSUM = "82e82ecf86bf04017d6a7a754e3eeb95";
    private static final String CHECKSUM_2 = "d13e942a704fc368fa0742e49845bdf3";
    private static final String FILE = "0000015232_dat_0000000612_08011008_111355.txt";
    private static final String RESOURCE = "bais-emulator/" + FILE;
    private static final String CONTAINER = "/home/DWP/" + FILE;

    @Autowired
    private DWPBaisFileProcessorService service;

    @Autowired
    private DWPBaisFileProcessorConfiguration config;

    @MockitoSpyBean
    private MaintenanceInterfaceFilePreprocessQueueService maintenanceQueueService;

    private final Logger logger = (Logger) LoggerFactory.getLogger(AbstractInterfaceFileProcessorService.class);
    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        blobServiceClient.createBlobContainerIfNotExists(config.getContainerName());

        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.BTEckoh-Report-file-transfer-job=true"
    })
    public class BankingInterfacesDisabled {

        @Test
        @DisplayName("Feature flag 'release-1c-banking-interfaces' is false")
        @JiraStory("PO-6436")
        @JiraEpic("PO-3497")
        void bankingInterfacesIsDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                service.run(config));

            assertThat(exception).hasMessage(FeatureFlags.RELEASE_1C_BANKING_INTERFACES + " is not enabled");
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true",
        "launchdarkly.default-flag-values.dwp-file-transfer-job=false"
    })
    public class BTEckohReportFileTransferJobDisabled {

        @Test
        @DisplayName("Feature flag 'dwp-file-transfer-job' is false")
        @JiraStory("PO-6436")
        @JiraEpic("PO-3497")
        void bankingInterfacesIsDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                service.run(config));

            assertThat(exception).hasMessage("dwp-file-transfer-job is not enabled");
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.dwp-file-transfer-job=false"
    })
    public class BothFeatureFlagsDisabled {

        @Test
        @DisplayName("Both feature flags are false")
        @JiraStory("PO-6436")
        @JiraEpic("PO-3497")
        void bankingInterfacesIsDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                service.run(config));

            assertThat(exception).hasMessage(FeatureFlags.RELEASE_1C_BANKING_INTERFACES + " is not enabled");
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "opal.file-handler-service.file-types.dwp.sftp-username=DWP",
        "launchdarkly.default-flag-values.dwp-file-transfer-job=true",
    })
    @Sql(
        scripts = "classpath:db/insertData/insert_into_business_unit_bank_account.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
    )
    @Sql(
        scripts = "classpath:db/deleteData/delete_from_business_unit_bank_account.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
    )
    public class FeatureOnTests {

        @Test
        @DisplayName("DWP file is present, read and stored correctly")
        @JiraStory("PO-6436")
        @JiraEpic("PO-3497")
        void dwpBaisFileProcessorServiceShouldRunSuccesfully() {
            uploadResourceToSftp(RESOURCE, CONTAINER);
            service.run(config);

            InterfaceFileEntity parentEntity = assertNthEntity(0, FILE, CHECKSUM, Interface.DWP,
                Status.SUCCESS, Type.SOURCE, null, null);
            InterfaceFileEntity childEntity = assertNthEntity(1, FILE, CHECKSUM_2, Interface.DWP,
                Status.SUCCESS, Type.SOURCE_JSON, PaymentType.CASH, parentEntity.getInterfaceFileId());
            assertBlobChecksum(FILE, CHECKSUM, config.getContainerName());
            assertNumberOfSftpFiles(config.getSftpUsername(), 0);

            verify(maintenanceQueueService, times(1))
                .send(eq(childEntity.getInterfaceFileId()));
        }
    }
}
