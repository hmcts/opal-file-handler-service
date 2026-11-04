package uk.gov.hmcts.opal.filehandler.config.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.BarclaycardBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.BarclaycardInterfaceFileProcessorService;

@Component
@ConditionalOnExpression(
    "'${opal.automated-task}'.equals('BarclaycardFileTransferJob') or ${opal.testing-support-endpoints.enabled}"
)
@Slf4j
@RequiredArgsConstructor
public class AutomatedBarclaycardFileTransferJob implements TaskConfiguration {

    private final BarclaycardInterfaceFileProcessorService service;
    private final BarclaycardBaisFileProcessorConfiguration configuration;

    @Override
    public void run() {
        log.info("Starting automated Barclaycard file transfer job");

        service.run(configuration);

        log.info("Completed automated Barclaycard file transfer job");
    }

}
