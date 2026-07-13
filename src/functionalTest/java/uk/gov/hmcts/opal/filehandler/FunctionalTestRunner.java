package uk.gov.hmcts.opal.filehandler;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * JUnit Platform suite entry point for the functional Cucumber feature set.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
public class FunctionalTestRunner {
}
