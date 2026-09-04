package uk.gov.hmcts.opal.filehandler.config.task;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.JacobsBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.JacobsBaisFileProcessorService;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "opal.automated-task", havingValue = "JacobsFileTransferJob")
@Slf4j
public class AutomatedJacobsFileTransfer implements ApplicationRunner {

    private final JacobsBaisFileProcessorService fileProcessorService;
    private final JacobsBaisFileProcessorConfiguration configuration;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        log.info("Starting Jacobs File Transfer Job");

        fileProcessorService.run(configuration);

        log.info("Completed Jacobs File Transfer Job");
    }

}
