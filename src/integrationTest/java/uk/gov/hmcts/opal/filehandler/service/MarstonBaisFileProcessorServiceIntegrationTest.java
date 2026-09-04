package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.filehandler.config.MarstonBaisFileBaisFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.support.AbstractBaisFileProcessorServiceIntegrationTest;

@ActiveProfiles("integration")
@TestPropertySource(properties = {
    "opal.file-handler-service.file-types.bailiffs.marston.sftp-username=MARSTON",
    "launchdarkly.default-flag-values.bailiffs.marston-file-transfer-job=true"
})
public class MarstonBaisFileProcessorServiceIntegrationTest  extends AbstractBaisFileProcessorServiceIntegrationTest {

    @Autowired
    private MarstonBaisFileProcessorService marstonBaisFileProcessorService;

    @Autowired
    private MarstonBaisFileBaisFileProcessorConfig config;

    private static final String MARSTON_FILE = "1234567890dat1234567890.dat";

    private static final String MARSTON_FILE_CHECKSUM = "some-checksum-value";

    private static final String MARSTON_FILE_RESOURCE = "bais-emulator/" + MARSTON_FILE;

    private static final String MARSTON_FILE_CONTAINER = "/home/MARSTON/" + MARSTON_FILE;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        blobServiceClient.createBlobContainerIfNotExists(
            config.getContainerName()
        );
    }

    @Test
    void marstonBaisFileProcessorServiceShouldRunSuccessfully() {

        uploadResourceToSftp( MARSTON_FILE_RESOURCE, MARSTON_FILE_CONTAINER);
        marstonBaisFileProcessorService.run();
        super.assertMostRecentEntityHasStatus(MARSTON_FILE, MARSTON_FILE_CHECKSUM,Interface.MARSTON,Status.SUCCESS);
        assertNumberOfSftpFiles(config.getSftpUsername(),0 );
    }

    @Test
    @DisplayName("Feature flag 'bailiffs.marston-file-transfer-job' is false")
    void shouldThrowExceptionWhenMarstonFeatureFlagDisabled() {

        FeatureDisabledException exception = assertThrows(  FeatureDisabledException.class,
                () -> marstonBaisFileProcessorService.run() );

            assertThat(exception).hasMessage("bailiffs.marston-file-transfer-job is not enabled");
        }

}