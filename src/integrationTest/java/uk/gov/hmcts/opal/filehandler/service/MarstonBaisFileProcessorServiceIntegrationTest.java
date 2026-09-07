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
import uk.gov.hmcts.opal.filehandler.config.MarstonBaisFileBaisFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.support.AbstractBaisFileProcessorServiceIntegrationTest;

@ActiveProfiles("integration")
@TestPropertySource(properties = {
    "opal.file-handler-service.bailiffs.marston.sftp-username=MARSTON",
    "launchdarkly.default-flag-values.marston-file-transfer-job=true",
})

@Slf4j
public class MarstonBaisFileProcessorServiceIntegrationTest
    extends AbstractBaisFileProcessorServiceIntegrationTest {

    private static final String MARSTON_FILE =
        "Marston.GB.20260701.173024.xml";

    private static final String MARSTON_FILE_2 =
        "Marston.GB.20260701.173024.xml";

    private static final String MARSTON_FILE_CHECKSUM =
        "REPLACE_WITH_REAL_CHECKSUM";

    private static final String MARSTON_FILE_CHECKSUM_2 =
        "REPLACE_WITH_REAL_CHECKSUM_2";

    private static final String MARSTON_FILE_RESOURCE =
        "bais-emulator/" + MARSTON_FILE;

    private static final String MARSTON_FILE_RESOURCE_2 =
        "bais-emulator/" + MARSTON_FILE_2;

    private static final String MARSTON_FILE_CONTAINER =
        "/home/MARSTON/" + MARSTON_FILE;

    private static final String MARSTON_FILE_CONTAINER_2 =
        "/home/MARSTON/" + MARSTON_FILE_2;

    @Autowired
    private MarstonBaisFileProcessorService service;

    @Autowired
    private MarstonBaisFileBaisFileProcessorConfig config;

    private final Logger logger =
        (Logger) LoggerFactory.getLogger(AbstractInterfaceFileProcessorService.class);

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        System.out.println("username=[" + config.getSftpUsername() + "]");
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
        "launchdarkly.default-flag-values.marston-file-transfer-job=false"
    })
    public class MarstonFileTransferJobDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'marston-file-transfer-job' is false")
        void marstonFileTransferJobIsDisabled() {

            FeatureDisabledException exception = assertThrows(
                FeatureDisabledException.class,
                () -> service.run(config));

            assertThat(exception)
                .hasMessage("marston-file-transfer-job is not enabled");
        }
    }

    @Test
    @DisplayName("AC2: Marston file is present, read and stored correctly")
    void marstonBaisFileProcessorServiceShouldRunSuccessfully() {

        uploadResourceToSftp(MARSTON_FILE_RESOURCE, MARSTON_FILE_CONTAINER);
        service.run(config);
        assertMostRecentEntityHasStatus(MARSTON_FILE, MARSTON_FILE_CHECKSUM,Interface.MARSTON,Status.SUCCESS);
        assertBlobChecksum(MARSTON_FILE,MARSTON_FILE_CHECKSUM,config.getContainerName());
        assertNumberOfSftpFiles(config.getSftpUsername(),0);
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
                String.format(
                    "No files found in BAIS for user '%s' when processing source 'MARSTON'",
                    config.getSftpUsername()));
    }

    @Test
    @DisplayName("AC4: Duplicate file with previous success should reject")
    void duplicateFileShouldReject() {

        final InterfaceFileEntity success =
            createSuccessfulInterfaceFile(MARSTON_FILE, MARSTON_FILE_CHECKSUM);
        uploadResourceToSftp(MARSTON_FILE_RESOURCE, MARSTON_FILE_CONTAINER);

        uploadResourceToSftp(MARSTON_FILE_RESOURCE_2, MARSTON_FILE_CONTAINER_2);
        service.run(config);
        assertEntitiesWithStatus(MARSTON_FILE_2,MARSTON_FILE_CHECKSUM_2,Status.SUCCESS);

        assertEntitiesWithStatus(MARSTON_FILE,MARSTON_FILE_CHECKSUM,Status.DUPLICATE);

        assertNumberOfSftpFiles(config.getSftpUsername(), 0);

        assertThat(logAppender.list)
            .filteredOn(event -> event.getLevel() == Level.ERROR)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly(
                String.format(
                    "File with name '%s' and checksum '%s' for source 'MARSTON' is a duplicate of %s",
                    MARSTON_FILE,
                    MARSTON_FILE_CHECKSUM,
                    success.getInterfaceFileId()));
    }

    @Test
    @DisplayName("AC5: Duplicate file with no previous success should process")
    void processDuplicateWithoutPreviousSuccess() {

        createFailedInterfaceFile(MARSTON_FILE,MARSTON_FILE_CHECKSUM,Interface.MARSTON);
        uploadResourceToSftp(MARSTON_FILE_RESOURCE,MARSTON_FILE_CONTAINER);
        service.run(config);
        assertNumberOfSftpFiles(config.getSftpUsername(), 0);
        assertEntitiesWithStatus( MARSTON_FILE,MARSTON_FILE_CHECKSUM, Status.FAILED_SUPERSEDED);

        assertMostRecentEntityHasStatus(MARSTON_FILE, MARSTON_FILE_CHECKSUM,Interface.MARSTON,Status.SUCCESS);

        assertBlobChecksum(MARSTON_FILE,MARSTON_FILE_CHECKSUM, config.getContainerName());
    }
}