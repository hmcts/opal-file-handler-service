package uk.gov.hmcts.opal.filehandler.support;

import java.io.File;
import java.io.IOException;
import net.thucydides.core.reports.html.HtmlAggregateStoryReporter;
import net.thucydides.model.reports.ResultChecker;
import net.thucydides.model.reports.TestOutcomes;
import net.thucydides.model.requirements.DefaultRequirements;

public final class SerenityReportGenerator {

    private static final int EXPECTED_ARGUMENT_COUNT = 4;

    private SerenityReportGenerator() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != EXPECTED_ARGUMENT_COUNT) {
            throw new IllegalArgumentException(
                "Expected report directory, test root, project directory and project name"
            );
        }

        File reportDirectory = new File(args[0]);
        String testRoot = args[1];
        String projectDirectory = args[2];
        String projectName = args[3];

        System.setProperty("serenity.outputDirectory", reportDirectory.getAbsolutePath());
        System.setProperty("serenity.test.root", testRoot);
        System.setProperty("serenity.project.directory", projectDirectory);

        HtmlAggregateStoryReporter reporter = new HtmlAggregateStoryReporter(
            projectName,
            new DefaultRequirements(testRoot)
        );
        reporter.setOutputDirectory(reportDirectory);
        reporter.setTestRoot(testRoot);
        reporter.setProjectDirectory(projectDirectory);
        reporter.setGenerateTestOutcomeReports();

        TestOutcomes outcomes = reporter.generateReportsForTestResultsFrom(reportDirectory);
        new ResultChecker(reportDirectory).checkTestResults(outcomes);
    }
}
