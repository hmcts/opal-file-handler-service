package uk.gov.hmcts.opal.filehandler.config.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.CapsReportBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.CapsReportBaisFileProcessorService;

@Component("automatedCAPSReport")
@ConditionalOnExpression("'${opal.automated-task}'.equals('CAPSReport') or ${opal.testing-support-endpoints.enabled}")
@Slf4j
@RequiredArgsConstructor
public class AutomatedCapsReport implements TaskConfiguration {

    private final CapsReportBaisFileProcessorService processorService;
    private final CapsReportBaisFileProcessorConfiguration processorConfiguration;

    @Override
    public void run() {
        log.info("Starting automated CAPS report");

        processorService.run(processorConfiguration);

        log.info("Completed automated CAPS report");
    }
}
