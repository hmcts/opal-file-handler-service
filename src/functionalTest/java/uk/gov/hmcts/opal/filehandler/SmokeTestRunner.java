package uk.gov.hmcts.opal.filehandler;

import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * JUnit Platform suite entry point for tagged smoke scenarios that live under the functional-test
 * feature tree.
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features.smoke")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@Smoke and not @Ignore")
public class SmokeTestRunner {
}
