package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import ch.qos.logback.classic.Level;
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
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.filehandler.config.BTEckohReportBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.support.AbstractBaisFileProcessorServiceIntegrationTest;

@ActiveProfiles("integration")
@TestPropertySource(properties = {
    "opal.file-handler-service.file-types.bteckoh-report.sftp-username=BTEckoh-report",
    "launchdarkly.default-flag-values.BTEckoh-Report-file-transfer-Job=true",
})
@Slf4j
public class BTEckohReportBaisFileProcessorServiceIntegrationTest
    extends AbstractBaisFileProcessorServiceIntegrationTest {

    private static final String BTECKOH_FILE = "2498-MCPLDB-MOJ-Payments-Report-Daily-2026-07-06-06-00-18.xlsx";
    private static final String BTECKOH_FILE_2 = "2498-MCPLDB-MOJ-Payments-Report-Daily-2026-07-07-06-00-18.xlsx";
    private static final String BTECKOH_FILE_CHECKSUM = "d553f8f289bd08e5c513de5c000c0374";
    private static final String BTECKOH_FILE_CHECKSUM_2 = "30276e2620c23e98d2c227efca8b220c";
    private static final String BTECKOH_FILE_RESOURCE = "bais-emulator/" + BTECKOH_FILE;
    private static final String BTECKOH_FILE_RESOURCE_2 = "bais-emulator/" + BTECKOH_FILE_2;
    private static final String BTECKOH_FILE_CONTAINER = "/home/BTEckoh-report/" + BTECKOH_FILE;
    private static final String BTECKOH_FILE_CONTAINER_2 = "/home/BTEckoh-report/" + BTECKOH_FILE_2;

    @Autowired
    private BTEckohReportBaisFileProcessorService service;

    @Autowired
    private BTEckohReportBaisFileProcessorConfiguration config;

    private final Logger logger = (Logger) LoggerFactory.getLogger(AbstractBaisFileProcessorService.class);
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
        "launchdarkly.default-flag-values.BTEckoh-Report-file-transfer-Job=true"
    })
    public class BankingInterfacesDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'release-1c-banking-interfaces' is false")
        void bankingInterfacesIsDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                service.run(config));

            assertThat(exception).hasMessage(FeatureFlags.RELEASE_1C_BANKING_INTERFACES + " is not enabled");
        }

    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true",
        "launchdarkly.default-flag-values.BTEckoh-Report-file-transfer-Job=false"
    })
    public class BTEckohReportFileTransferJobDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'BTEckoh-Report-file-transfer-Job' is false")
        void bankingInterfacesIsDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                service.run(config));

            assertThat(exception).hasMessage("BTEckoh-Report-file-transfer-Job is not enabled");
        }

    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.BTEckoh-Report-file-transfer-Job=false"
    })
    public class BothFeatureFlagsDisabled {

        @Test
        @DisplayName("AC1: Both feature flags are false")
        void bankingInterfacesIsDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                service.run(config));

            assertThat(exception).hasMessage(FeatureFlags.RELEASE_1C_BANKING_INTERFACES + " is not enabled");
        }

    }

    @Test
    @DisplayName("AC2: BTEckoh-Report file is present, read and stored correctly")
    void btEckohReportBaisFileProcessorServiceShouldRunSuccesfully() {
        uploadResourceToSftp(BTECKOH_FILE_RESOURCE, BTECKOH_FILE_CONTAINER);
        service.run(config);

        assertMostRecentEntityHasStatus(BTECKOH_FILE, BTECKOH_FILE_CHECKSUM, Interface.BTECKOH_REPORT, Status.SUCCESS);
        assertBlobChecksum(BTECKOH_FILE, BTECKOH_FILE_CHECKSUM, config.getContainerName());
        assertNumberOfSftpFiles(config.getSftpUsername(), 0);
    }

    @Test
    @DisplayName("AC3: When no files are present the service should not fail")
    void whenNoFilesArePresentServiceSucceeds() {
        assertNumberOfSftpFiles(config.getSftpUsername(), 0);
        service.run(config);

        assertThat(repository.findAll()).isEmpty();
        assertThat(logAppender.list)
            .filteredOn(event -> event.getLevel() == Level.INFO)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly(
                String.format("No files found in BAIS for user '%s' when processing source 'BTECKOH_REPORT'",
                    config.getSftpUsername()));
    }

    @Test
    @DisplayName("AC4: Duplicate file with previous success should reject")
    void duplicateFileShouldReject() {
        final InterfaceFileEntity success = createSuccessfulInterfaceFile(BTECKOH_FILE, BTECKOH_FILE_CHECKSUM);
        uploadResourceToSftp(BTECKOH_FILE_RESOURCE, BTECKOH_FILE_CONTAINER);
        uploadResourceToSftp(BTECKOH_FILE_RESOURCE_2, BTECKOH_FILE_CONTAINER_2);

        service.run(config);

        assertNumberOfEntitiesWithStatus(BTECKOH_FILE, BTECKOH_FILE_CHECKSUM, Status.SUCCESS, 1);
        assertNumberOfEntitiesWithStatus(BTECKOH_FILE, BTECKOH_FILE_CHECKSUM, Status.DUPLICATE, 1);
        assertNumberOfEntitiesWithStatus(BTECKOH_FILE_2, BTECKOH_FILE_CHECKSUM_2, Status.SUCCESS, 1);
        assertNumberOfSftpFiles(config.getSftpUsername(), 0);

        assertBlobChecksum(BTECKOH_FILE_2, BTECKOH_FILE_CHECKSUM_2, config.getContainerName());

        assertThat(logAppender.list)
            .filteredOn(event -> event.getLevel() == Level.ERROR)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly(
                String.format("File with name '%s' and checksum '%s' for source 'BTECKOH_REPORT' is a duplicate of %s",
                    BTECKOH_FILE, BTECKOH_FILE_CHECKSUM, success.getInterfaceFileId()));
    }


    @Test
    @DisplayName("AC5: Duplicate file with no previous success should process")
    void processDuplicateWithoutPreviousSuccess() {
        createFailedInterfaceFile(BTECKOH_FILE, BTECKOH_FILE_CHECKSUM, Interface.BTECKOH_REPORT);
        uploadResourceToSftp(BTECKOH_FILE_RESOURCE, BTECKOH_FILE_CONTAINER);

        service.run(config);

        assertNumberOfSftpFiles(config.getSftpUsername(), 0);
        assertEntitiesWithStatus(BTECKOH_FILE, BTECKOH_FILE_CHECKSUM, Status.FAILED_SUPERSEDED);
        assertMostRecentEntityHasStatus(BTECKOH_FILE, BTECKOH_FILE_CHECKSUM, Interface.BTECKOH_REPORT, Status.SUCCESS);
        assertBlobChecksum(BTECKOH_FILE, BTECKOH_FILE_CHECKSUM, config.getContainerName());
    }

}
