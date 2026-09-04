package uk.gov.hmcts.opal.filehandler.config.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.AllpayBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.AllpayBaisFileProcessorService;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression(
    "'${opal.automated-task}'.equals('AllpayFileTransferJob') or ${opal.testing-support-endpoints.enabled}"
)
@Slf4j
public class AutomatedAllpayFileTransferJob implements TaskConfiguration {

    private final AllpayBaisFileProcessorService fileProcessorService;
    private final AllpayBaisFileProcessorConfiguration configuration;

    @Override
    public void run() {
        log.info("Starting Allpay File Transfer Job");

        fileProcessorService.run(configuration);

        log.info("Completed Allpay File Transfer Job");
    }

}
