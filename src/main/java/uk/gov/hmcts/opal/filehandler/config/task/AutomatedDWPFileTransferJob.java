package uk.gov.hmcts.opal.filehandler.config.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.DWPBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.DWPBaisFileProcessorService;

@Component
@ConditionalOnExpression(
    "'${opal.automated-task}'.equals('DWPFileTransferJob') or ${opal.testing-support-endpoints.enabled}"
)
@Slf4j
@RequiredArgsConstructor
public class AutomatedDWPFileTransferJob implements TaskConfiguration {

    private final DWPBaisFileProcessorService processorService;
    private final DWPBaisFileProcessorConfiguration processorConfiguration;

    @Override
    public void run() {
        log.info("Starting automated DWP file transfer job");

        processorService.run(processorConfiguration);

        log.info("Completed automated DWP file transfer job");
    }
}
