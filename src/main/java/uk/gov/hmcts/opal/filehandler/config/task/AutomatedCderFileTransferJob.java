package uk.gov.hmcts.opal.filehandler.config.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.CderBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.CderBaisFileProcessorService;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression(
    "'${opal.automated-task}'.equals('CderFileTransferJob') or ${opal.testing-support-endpoints.enabled}"
)
@Slf4j
public class AutomatedCderFileTransferJob implements TaskConfiguration {

    private final CderBaisFileProcessorService service;
    private final CderBaisFileProcessorConfiguration configuration;

    @Override
    public void run() {
        log.info("Starting CDER File Transfer Job");

        service.run(configuration);

        log.info("Completed CDER File Transfer Job");
    }

}
