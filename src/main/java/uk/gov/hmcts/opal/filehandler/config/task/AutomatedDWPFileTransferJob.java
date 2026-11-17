package uk.gov.hmcts.opal.filehandler.config.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.service.DWPInterfaceFileProcessorService;

@Component
@ConditionalOnExpression(
    "'${opal.automated-task}'.equals('DWPFileTransferJob') or ${opal.testing-support-endpoints.enabled}"
)
@Slf4j
@RequiredArgsConstructor
public class AutomatedDWPFileTransferJob implements TaskConfiguration {

    private final DWPInterfaceFileProcessorService processorService;

    @Override
    public void run() {
        log.info("Starting automated DWP file transfer job");

        processorService.run();

        log.info("Completed automated DWP file transfer job");
    }
}
