package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.filehandler.config.BarclaycardBaisFileBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.queue.FinesInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.support.AbstractBaisFileProcessorServiceIntegrationTest;
import uk.gov.hmcts.opal.filehandler.testdata.BusinessUnitBankAccountEntityTestData;

@ActiveProfiles("integration")
@TestPropertySource(properties = {
    "opal.file-handler-service.file-types.natwest.sftp-username=BARCLAYCARD",
    "launchdarkly.default-flag-values.barclaycard-file-transfer-Job=true",
})
public class BarclaycardBaisFileProcessorServiceTest extends AbstractBaisFileProcessorServiceIntegrationTest {

    private static final String BARCLAYCARD_FILE = "a121_00010065_317608.dat";
    private static final String BARCLAYCARD_FILE_CHECKSUM = "3e7eb40eae410fee9a8d999bdcd7c302";
    private static final String BARCLAYCARD_FILE_RESOURCE = "bais-emulator/" + BARCLAYCARD_FILE;
    private static final String BARCLAYCARD_FILE_CONTAINER = "/home/AllPay/" + BARCLAYCARD_FILE;
    private static final String BUSINESS_UNIT_CODE = "BC12";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private BarclaycardBaisFileProcessorService service;

    @Autowired
    private BarclaycardBaisFileBaisFileProcessorConfiguration configuration;

    @Autowired
    private BusinessUnitBankAccountEntityTestData businessUnitBankAccountEntityTestData;

    @MockitoBean
    private FinesInterfaceFilePreprocessQueueService finesQueueService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        businessUnitBankAccountEntityTestData.clear();
        businessUnitBankAccountEntityTestData.saveTypicalBusinessUnitBankAccount(1L, BUSINESS_UNIT_CODE);
        blobServiceClient.createBlobContainerIfNotExists(configuration.getContainerName());
    }


    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true",
        "launchdarkly.default-flag-values.barclaycard-file-transfer-Job=false"
    })
    public class NatWestFileTransferJobDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'barclaycard-file-transfer-Job' is false")
        void barclaycardFileTransferJobIsDisabled() {
            FeatureDisabledException exception = assertThrows(
                FeatureDisabledException.class, () -> service.run(configuration)
            );
            assertThat(exception).hasMessage("barclaycard-file-transfer-Job is not enabled");
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.barclays-file-transfer-Job=false"
    })
    public class BothFeatureFlagsDisabled {

        @Test
        @DisplayName("AC1: Both feature flags are false")
        void bothFeatureFlagsAreDisabled() {
            FeatureDisabledException exception = assertThrows(FeatureDisabledException.class, () ->
                service.run(configuration)
            );

            assertThat(exception).hasMessage(FeatureFlags.RELEASE_1C_BANKING_INTERFACES + " is not enabled");
        }
    }

}
