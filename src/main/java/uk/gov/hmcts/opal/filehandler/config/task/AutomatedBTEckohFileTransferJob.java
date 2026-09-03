package uk.gov.hmcts.opal.filehandler.config.task;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.BTEckohBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.BTEckohBaisFileProcessorService;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "opal.automated-task", havingValue = "BTEckohFileTransferJob")
@Slf4j
public class AutomatedBTEckohFileTransferJob implements ApplicationRunner {

    private final BTEckohBaisFileProcessorService fileProcessorService;
    private final BTEckohBaisFileProcessorConfiguration configuration;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        log.info("Starting BTEckoh File Transfer Job");

        fileProcessorService.run(configuration);

        log.info("Completed BTEckoh File Transfer Job");
    }
}
