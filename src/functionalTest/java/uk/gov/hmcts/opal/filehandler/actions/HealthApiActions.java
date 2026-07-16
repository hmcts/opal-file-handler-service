package uk.gov.hmcts.opal.filehandler.actions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.support.TestHttpClient;
import uk.gov.hmcts.opal.filehandler.support.TestHttpClient.TestHttpResponse;

import java.util.Map;

/**
 * Provides reusable calls for the service health endpoint.
 */
public class HealthApiActions {

    private static final Logger log = LoggerFactory.getLogger(HealthApiActions.class);

    /**
     * Calls the service health endpoint.
     *
     * @return response returned by the health endpoint.
     */
    public TestHttpResponse getHealth() {
        log.info("Checking file-handler API health endpoint at {}", TestEnvironment.getTestUrl());
        return TestHttpClient.get(TestEnvironment.getTestUrl() + "/health", Map.of());
    }
}
