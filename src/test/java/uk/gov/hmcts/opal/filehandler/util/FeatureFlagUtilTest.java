package uk.gov.hmcts.opal.filehandler.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.service.FeatureToggleApi;

@ExtendWith(MockitoExtension.class)
public class FeatureFlagUtilTest {

    private static final String TEST_FEATURE_FLAG = "test-feature-flag";

    @Mock
    private FeatureToggleApi featureToggleApi;

    private FeatureFlagUtil featureFlagUtil;

    @BeforeEach
    void setUp() {
        featureFlagUtil = new FeatureFlagUtil(featureToggleApi);
    }

    @Test
    void whenFeatureIsDisabledExceptionIsThrown() {
        when(featureToggleApi.isFeatureEnabled(TEST_FEATURE_FLAG)).thenReturn(false);

        FeatureDisabledException exc = assertThrows(FeatureDisabledException.class, () ->
            featureFlagUtil.requireEnabledFeature(TEST_FEATURE_FLAG));

        assertThat(exc).hasMessage(TEST_FEATURE_FLAG + " is not enabled");
    }

}
