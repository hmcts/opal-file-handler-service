package uk.gov.hmcts.opal.filehandler.config.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.BTEckohReportBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.BTEckohReportBaisFileProcessorService;

@Component
@ConditionalOnExpression(
    "'${opal.automated-task}'.equals('BTEckohReport') or ${opal.testing-support-endpoints.enabled}"
)
@Slf4j
@RequiredArgsConstructor
public class AutomatedBTEckohReport implements TaskConfiguration {

    private final BTEckohReportBaisFileProcessorService service;
    private final BTEckohReportBaisFileProcessorConfiguration config;

    @Override
    public void run() {
        log.info("Starting automated BTEckoh report");

        service.run(config);

        log.info("Completed automated BTEckoh report");
    }
}
