package uk.gov.hmcts.opal.filehandler.config.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.service.AllpayInterfaceFileProcessorService;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression(
    "'${opal.automated-task}'.equals('AllpayFileTransferJob') or ${opal.testing-support-endpoints.enabled}"
)
@Slf4j
public class AutomatedAllpayFileTransferJob implements TaskConfiguration {

    private final AllpayInterfaceFileProcessorService fileProcessorService;

    @Override
    public void run() {
        log.info("Starting Allpay File Transfer Job");

        fileProcessorService.run();

        log.info("Completed Allpay File Transfer Job");
    }

}
