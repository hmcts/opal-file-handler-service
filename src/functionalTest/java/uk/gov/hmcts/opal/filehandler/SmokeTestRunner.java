package uk.gov.hmcts.opal.filehandler;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;

/**
 * JUnit Platform suite entry point for tagged smoke scenarios that live under the functional-test
 * feature tree.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/smoke")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@Smoke and not @Ignore")
public class SmokeTestRunner {
}
