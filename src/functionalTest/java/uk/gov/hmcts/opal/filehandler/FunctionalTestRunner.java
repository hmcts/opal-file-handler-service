package uk.gov.hmcts.opal.filehandler;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * JUnit Platform suite entry point for the functional Cucumber feature set.
 */
@Suite
@IncludeEngines("cucumber")
@SelectPackages("features")
public class FunctionalTestRunner {
}
