package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.filehandler.config.VariantBankingFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.support.AbstractBaisFileProcessorServiceIntegrationTest;

@ActiveProfiles("integration")
@TestPropertySource(properties = {
    "launchdarkly.default-flag-values.release-1c-banking-interfaces=true",
    "launchdarkly.default-flag-values.variant-banking=true"
})
class VariantBankingFileProcessorServiceIntegrationTest
    extends AbstractBaisFileProcessorServiceIntegrationTest {

    @Autowired
    private VariantBankingFileProcessorService service;

    @Autowired
    private VariantBankingFileProcessorConfig config;

    @Nested
    @TestPropertySource(properties = {
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false",
        "launchdarkly.default-flag-values.variant-banking=true"
    })
    class BankingInterfacesDisabled {

        @Test
        @DisplayName("AC1: Feature flag 'release-1c-banking-interfaces' is false")
        void bankingInterfacesDisabled() {

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
        void variantBankingDisabled() {

            FeatureDisabledException exception =
                assertThrows(
                    FeatureDisabledException.class,
                    () -> service.run(config)
                );

            assertThat(exception)
                .hasMessage("variant-banking-file-transfer-job is not enabled");
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
        void bothFeatureFlagsDisabled() {

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
}
