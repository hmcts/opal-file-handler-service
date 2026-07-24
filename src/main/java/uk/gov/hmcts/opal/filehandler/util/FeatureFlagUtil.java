package uk.gov.hmcts.opal.filehandler.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.service.FeatureToggleApi;

@Component
@RequiredArgsConstructor
public class FeatureFlagUtil {

    private final FeatureToggleApi featureToggleApi;

    public void requireEnabledFeature(String feature) {
        if (!featureToggleApi.isFeatureEnabled(feature)) {
            throw new FeatureDisabledException(feature + " is not enabled");
        }
    }

}
