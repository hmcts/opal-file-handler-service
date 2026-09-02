package uk.gov.hmcts.opal.filehandler.config.task;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.BarclaycardBaisFileBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.BarclaycardBaisFileProcessorService;

@Component
@ConditionalOnProperty(name = "opal.automated-task", havingValue = "NatWestFileTransferJob")
@Slf4j
@RequiredArgsConstructor
public class AutomatedBarclaycardFileTransferJob implements ApplicationRunner {

    private final BarclaycardBaisFileProcessorService service;
    private final BarclaycardBaisFileBaisFileProcessorConfiguration configuration;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        log.info("Starting automated NatWest file transfer job");

        service.run(configuration);

        log.info("Completed automated NatWest file transfer job");
    }

}
