package uk.gov.hmcts.opal.filehandler.config.task;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.BarclaycardBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.BarclaycardBaisFileProcessorService;

@Component
@ConditionalOnExpression(
    "'${opal.automated-task}'.equals('BarclaycardFileTransferJob') or ${opal.testing-support-endpoints.enabled}"
)
@Slf4j
@RequiredArgsConstructor
public class AutomatedBarclaycardFileTransferJob implements TaskConfiguration {

    private final BarclaycardBaisFileProcessorService service;
    private final BarclaycardBaisFileProcessorConfiguration configuration;

    @Override
    public void run() {
        log.info("Starting automated Barclaycard file transfer job");

        service.run(configuration);

        log.info("Completed automated Barclaycard file transfer job");
    }

}
