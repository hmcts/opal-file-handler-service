package uk.gov.hmcts.opal.filehandler.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.config.LaunchDarklyProperties;
import uk.gov.hmcts.opal.common.launchdarkly.service.FeatureToggleApi;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagUtil {

    private final FeatureToggleApi featureToggleApi;
    private final LaunchDarklyProperties properties;

    public void requireEnabledFeature(String feature) {
        log.info("TMP: properties.isEnabled() '{}'", properties.isEnabled());
        log.info("TMP: properties.getEnv() '{}'", properties.getEnv());
        if (!featureToggleApi.isFeatureEnabled(feature)) {
            throw new FeatureDisabledException("'" + feature + "' is not enabled");
        }
    }

}
