package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.filehandler.config.VariantBankingFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.support.AbstractBaisFileProcessorServiceIntegrationTest;

@ActiveProfiles("integration")
@TestPropertySource(properties = {
    "launchdarkly.default-flag-values.variant-banking=true"
})
@DisplayName("Variant Banking File Processor Service Integration Tests")
class VariantBankingFileProcessorServiceIntegrationTest
    extends AbstractBaisFileProcessorServiceIntegrationTest {

    @Autowired
    private VariantBankingFileProcessorService service;

    @Autowired
    private VariantBankingFileProcessorConfig config;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        blobServiceClient.createBlobContainerIfNotExists(config.getContainerName());
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.variant-banking=true"
    })
    class BankingInterfacesDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'release-1c-banking-interfaces' is false")
        void bankingInterfacesIsDisabled() {

            FeatureDisabledException exception =
                assertThrows(
                    FeatureDisabledException.class,
                    () -> service.run(config)
                );

            assertThat(exception)
                .hasMessage(
                    FeatureFlags.RELEASE_1C_BANKING_INTERFACES
                        + " is not enabled"
                );
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true",
        "launchdarkly.default-flag-values.variant-banking=false"
    })
    class VariantBankingDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'variant-banking' is false")
        void variantBankingIsDisabled() {

            FeatureDisabledException exception =
                assertThrows(
                    FeatureDisabledException.class,
                    () -> service.run(config)
                );

            assertThat(exception)
                .hasMessage("variant-banking is not enabled");
        }
    }

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.variant-banking=false"
    })
    class BothFeatureFlagsDisabled {

        @Test
        @DisplayName("AC1: Both feature flags are false")
        void bothFeatureFlagsAreDisabled() {

            FeatureDisabledException exception =
                assertThrows(
                    FeatureDisabledException.class,
                    () -> service.run(config)
                );

            assertThat(exception)
                .hasMessage(
                    FeatureFlags.RELEASE_1C_BANKING_INTERFACES
                        + " is not enabled"
                );
        }
    }

    @Test
    @DisplayName("Should not select files from BAIS SFTP")
    void shouldNotSelectFilesFromSftp() {

        List<String> files = service.selectFilesToProcess(config);

        assertThat(files).isEmpty();
    }


    @Test
    @DisplayName("AC2: Uploaded file is accepted for processing")
    void shouldProcessUploadedFile() throws Exception {

        byte[] fileBytes = "variant-banking-test-content".getBytes();
        service.processUploadedFile(config,"VB001.dat", fileBytes);
        assertThat(repository.findAll()).isNotEmpty();
    }

    @Test
    @DisplayName("AC3: Duplicate detection uses filename only when checksum differs")
    void shouldMarkDuplicateWhenFilenameMatchesAndChecksumDiffers() throws Exception {

        InterfaceFileEntity existingFile = InterfaceFileEntity.builder()
            .source(Interface.VARIANT_BANKING)
            .target(Interface.OPAL)
            .type(Type.SOURCE)
            .opalDomain(Domain.MAINTENANCE)
            .fileName("VB001.dat")
            .checksum("existing-checksum")
            .status(Status.SUCCESS)
            .createdDatetime(LocalDateTime.now())
            .build();

        repository.save(existingFile);

        byte[] fileBytes = "different-content".getBytes();

        service.processUploadedFile(config, "VB001.dat", fileBytes);

        assertThat(repository.findAll())
            .anyMatch(file -> file.getStatus().equals(Status.DUPLICATE));
    }
}
