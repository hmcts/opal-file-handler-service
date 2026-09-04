package uk.gov.hmcts.opal.filehandler.config.task;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.DWPBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.DWPBaisFileProcessorService;

@Component
@ConditionalOnProperty(name = "opal.automated-task", havingValue = "DWPFileTransferJob")
@Slf4j
@RequiredArgsConstructor
public class AutomatedDWPFileTransferJob implements ApplicationRunner {

    private final DWPBaisFileProcessorService processorService;
    private final DWPBaisFileProcessorConfiguration processorConfiguration;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        log.info("Starting automated DWP file transfer job");

        processorService.run(processorConfiguration);

        log.info("Completed automated DWP file transfer job");
    }
}
