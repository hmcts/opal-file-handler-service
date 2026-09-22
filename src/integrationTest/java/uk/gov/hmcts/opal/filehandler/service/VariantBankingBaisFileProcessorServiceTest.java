package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.azure.storage.blob.BlobClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.filehandler.config.VariantBankingFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.service.queue.FinesInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.support.AbstractBaisFileProcessorServiceIntegrationTest;
import uk.gov.hmcts.opal.filehandler.testdata.BusinessUnitBankAccountEntityTestData;

@ActiveProfiles("integration")
@TestPropertySource(properties = {
    "launchdarkly.default-flag-values.variant-banking-file-transfer-job=true",
})
class VariantBankingFileProcessorServiceTest
    extends AbstractBaisFileProcessorServiceIntegrationTest {

    private static final String VARIANT_BANKING_FILE =
        "a121_000100_VB001_01.dat";

    private static final String VARIANT_BANKING_FILE_CHECKSUM =
        "REPLACE_WITH_ACTUAL_CHECKSUM";

    private static final String VARIANT_BANKING_FILE_RESOURCE =
        "bais-emulator/" + VARIANT_BANKING_FILE;

    private static final String VARIANT_BANKING_FILE_CONTAINER =
        "/home/VARIANT_BANKING/" + VARIANT_BANKING_FILE;

    private static final String BUSINESS_UNIT_CODE = "VB001";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private VariantBankingFileProcessorService service;

    @Autowired
    private VariantBankingFileProcessorConfig configuration;

    @Autowired
    private BusinessUnitBankAccountEntityTestData businessUnitBankAccountEntityTestData;

    @MockitoBean
    private FinesInterfaceFilePreprocessQueueService finesQueueService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        businessUnitBankAccountEntityTestData.clear();

        businessUnitBankAccountEntityTestData
            .saveTypicalBusinessUnitBankAccount(
                1L,
                BUSINESS_UNIT_CODE
            );

        blobServiceClient.createBlobContainerIfNotExists(
            configuration.getContainerName()
        );
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true",
        "launchdarkly.default-flag-values.variant-banking-file-transfer-job=false"
    })
    class VariantBankingFileTransferJobDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'variant-banking-file-transfer-job' is false")
        void variantBankingFileTransferJobIsDisabled() {

            FeatureDisabledException exception =
                assertThrows(
                    FeatureDisabledException.class,
                    () -> service.run(configuration)
                );

            assertThat(exception)
                .hasMessage("variant-banking-file-transfer-job is not enabled");
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.variant-banking-file-transfer-job=false"
    })
    class BothFeatureFlagsDisabled {

        @Test
        @DisplayName("AC1: Both feature flags are false")
        void bothFeatureFlagsAreDisabled() {

            FeatureDisabledException exception =
                assertThrows(
                    FeatureDisabledException.class,
                    () -> service.run(configuration)
                );

            assertThat(exception)
                .hasMessage(
                    FeatureFlags.RELEASE_1C_BANKING_INTERFACES
                        + " is not enabled"
                );
        }
    }

    @Test
    @DisplayName("AC2: When Variant Banking file is present it should be read and stored correctly")
    void variantBankingFileProcessorServiceShouldRunSuccessfully()
        throws Exception {

        uploadResourceToSftp(
            VARIANT_BANKING_FILE_RESOURCE,
            VARIANT_BANKING_FILE_CONTAINER
        );

        service.run(configuration);

        InterfaceFileEntity sourceFile =
            assertSuccessfulInterfaceFile(
                VARIANT_BANKING_FILE,
                VARIANT_BANKING_FILE_CHECKSUM,
                Interface.VARIANT_BANKING,
                Type.SOURCE,
                Domain.FINES
            );

        InterfaceFileEntity sourceJsonFile =
            assertSuccessfulSourceJsonInterfaceFile(
                VARIANT_BANKING_FILE,
                Interface.VARIANT_BANKING,
                Domain.FINES,
                sourceFile.getInterfaceFileId()
            );

        assertBlobChecksum(
            VARIANT_BANKING_FILE,
            VARIANT_BANKING_FILE_CHECKSUM,
            configuration.getContainerName()
        );

        assertSourceJsonContents(sourceJsonFile);

        assertNumberOfSftpFiles(
            configuration.getSftpUsername(),
            0
        );

        verify(finesQueueService, times(1))
            .send(sourceJsonFile.getInterfaceFileId());
    }

    private void assertSourceJsonContents(
        InterfaceFileEntity sourceJson
    ) throws Exception {

        BlobClient client = blobServiceClient
            .getBlobContainerClient(configuration.getContainerName())
            .getBlobClient(sourceJson.getFilestoreUuid().toString());

        JsonNode json =
            objectMapper.readTree(client.downloadContent().toBytes());

        assertThat(json.get("file_name").asText())
            .isEqualTo(VARIANT_BANKING_FILE);

        // Important validation for Variant Banking:
        // account is derived from filename (VB001)

        assertThat(json.get("transactions").size())
            .isGreaterThan(0);
    }
}