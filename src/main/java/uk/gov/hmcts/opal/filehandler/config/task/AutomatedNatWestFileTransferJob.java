package uk.gov.hmcts.opal.filehandler.config.task;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.NatWestBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.NatWestBaisFileProcessorService;

@Component
@ConditionalOnProperty(name = "opal.automated-task", havingValue = "NatWestFileTransferJob")
@Slf4j
@RequiredArgsConstructor
public class AutomatedNatWestFileTransferJob implements ApplicationRunner {

    private final NatWestBaisFileProcessorService processorService;
    private final NatWestBaisFileProcessorConfiguration processorConfiguration;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        log.info("Starting automated NatWest file transfer job");

        processorService.run(processorConfiguration);

        log.info("Completed automated NatWest file transfer job");
    }
}
