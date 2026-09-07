package uk.gov.hmcts.opal.filehandler.config.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.JacobsBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.JacobsBaisFileProcessorService;

@Component("automatedJacobsFileTransfer")
@ConditionalOnExpression(
    "'${opal.automated-task}'.equals('JacobsFileTransferJob') or ${opal.testing-support-endpoints.enabled}"
)
@Slf4j
@RequiredArgsConstructor
public class AutomatedJacobsFileTransfer implements TaskConfiguration {

    private final JacobsBaisFileProcessorService processorService;
    private final JacobsBaisFileProcessorConfiguration configuration;

    @Override
    public void run() {
        log.info("Starting Jacobs File Transfer Job");

        processorService.run(configuration);

        log.info("Completed Jacobs File Transfer Job");
    }

}
