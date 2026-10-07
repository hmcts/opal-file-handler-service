package uk.gov.hmcts.opal.filehandler.config.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.NatWestBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.NatWestBaisFileProcessorService;

@Component
@ConditionalOnExpression(
    "'${opal.automated-task}'.equals('NatWestFileTransferJob') or ${opal.testing-support-endpoints.enabled}"
)
@Slf4j
@RequiredArgsConstructor
public class AutomatedNatWestFileTransferJob implements TaskConfiguration {

    private final NatWestBaisFileProcessorService processorService;
    private final NatWestBaisFileProcessorConfiguration processorConfiguration;

    @Override
    public void run() {
        log.info("Starting automated NatWest file transfer job");

        processorService.run(processorConfiguration);

        log.info("Completed automated NatWest file transfer job");
    }
}
