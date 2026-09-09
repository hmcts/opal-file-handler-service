package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.data.domain.Sort;
import uk.gov.hmcts.opal.filehandler.entity.BusinessUnitBankAccountEntity;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
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
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.support.AbstractBaisFileProcessorServiceIntegrationTest;
import uk.gov.hmcts.opal.filehandler.testdata.BusinessUnitBankAccountEntityTestData;

@ActiveProfiles("integration")
@TestPropertySource(properties = {
    "opal.file-handler-service.bailiffs.marston.sftp-username=MARSTON",
    "launchdarkly.default-flag-values.marston-file-transfer-job=true",
})

@Slf4j
public class MarstonBaisFileProcessorServiceIntegrationTest   extends AbstractBaisFileProcessorServiceIntegrationTest {

    private static final String MARSTON_FILE =
        "Marston.GB.20260701.173024.xml";

    private static final String MARSTON_FILE_2 =
        "Marston.GB.20260701.173024.xml";

    private static final String MARSTON_FILE_CHECKSUM =
        "ae51ad5900f1f99ac39c4f58bc6e9603";

    private static final String MARSTON_FILE_CHECKSUM_2 =
        "35e3ff0b0da86ee57a950c77ca0b1f7f";

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
    BusinessUnitBankAccountEntityTestData businessUnitBankAccountEntityTestData;
    @Autowired
    private MarstonBaisFileBaisFileProcessorConfig config;

    private final Logger logger =
        (Logger) LoggerFactory.getLogger(AbstractInterfaceFileProcessorService.class);

    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        blobServiceClient.createBlobContainerIfNotExists(config.getContainerName());
        businessUnitBankAccountExtractedData();
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

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false"
    })
    public class MarstonRelease1CBankingInterfacesDisabled {

        @Test
        @DisplayName("ACx: Feature flag 'release-1c-banking-interfaces' is false")
        void release1CBankingInterfacesIsDisabled() {

            FeatureDisabledException exception = assertThrows(
                FeatureDisabledException.class,
                () -> service.run(config));

            assertThat(exception)
                .hasMessage("release-1c-banking-interfaces is not enabled");
        }
    }


    @Test
    @DisplayName("AC2: Marston file is present, read and stored correctly")
    void marstonBaisFileProcessorServiceShouldRunSuccessfully() {

        uploadResourceToSftp(MARSTON_FILE_RESOURCE, MARSTON_FILE_CONTAINER);
        service.run(config);

        InterfaceFileEntity mostRecent = repository.findAll(
            Sort.by(Sort.Direction.ASC, "createdDatetime")
        ).getLast();
        assertThat(mostRecent.getFileName()).isEqualTo(MARSTON_FILE);
        assertThat(mostRecent.getChecksum()).isEqualTo(MARSTON_FILE_CHECKSUM);
        assertThat(mostRecent.getSource()).isEqualTo(Interface.MARSTON);
        assertThat(mostRecent.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(mostRecent.getType()).isEqualTo(Type.SOURCE_JSON);

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
    @DisplayName("AC5: Duplicate file with no previous success should process")
    void processDuplicateWithoutPreviousSuccess() {

        createFailedInterfaceFile(MARSTON_FILE,MARSTON_FILE_CHECKSUM,Interface.MARSTON);
        uploadResourceToSftp(MARSTON_FILE_RESOURCE,MARSTON_FILE_CONTAINER);
        service.run(config);
        assertNumberOfSftpFiles(config.getSftpUsername(), 0);
        assertEntitiesWithStatus(MARSTON_FILE,MARSTON_FILE_CHECKSUM,Status.FAILED);
        assertMarston();
        assertBlobChecksum(MARSTON_FILE,MARSTON_FILE_CHECKSUM, config.getContainerName());
    }

    private void assertMarston() {
        InterfaceFileEntity mostRecent = repository.findAll(
            Sort.by(Sort.Direction.ASC, "createdDatetime")
        ).getLast();
        assertThat(mostRecent.getFileName())
            .isEqualTo(MARSTON_FILE);
        assertThat(mostRecent.getStatus())
            .isEqualTo(Status.SUCCESS);
        assertThat(mostRecent.getChecksum())
            .isEqualTo(MARSTON_FILE_CHECKSUM);
        assertThat(mostRecent.getType())
            .isEqualTo(Type.SOURCE_JSON);
        assertThat(mostRecent.getSource())
            .isEqualTo(Interface.MARSTON);
        assertThat(mostRecent.getTarget())
            .isEqualTo(Interface.OPAL);
    }

    private void businessUnitBankAccountExtractedData() {
        businessUnitBankAccountEntityTestData.saveAndFlushBusinessUnitBankAccount(
            BusinessUnitBankAccountEntity.builder()
                .id(1L)
                .businessUnitCode("MR01")
                .domain(Domain.FINES)
                .bankSortCode("560033")
                .bankAccountNumber("27048527")
                .dwpCourtCode("DWP1234567")
                .build()
        );
    }
}