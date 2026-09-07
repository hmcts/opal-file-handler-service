package uk.gov.hmcts.opal.filehandler.config.task;

import java.io.IOException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import uk.gov.hmcts.opal.filehandler.config.MarstonBaisFileBaisFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.service.MarstonBaisFileProcessorService;

@Component
@ConditionalOnProperty(name = "opal.automated-task", havingValue = "Marston")
@Slf4j
@RequiredArgsConstructor
public class AutomatedMarston implements ApplicationRunner {

    private final MarstonBaisFileProcessorService processorService;
    private final MarstonBaisFileBaisFileProcessorConfig processorConfiguration;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        log.info("Starting automated Marston processing");

        processorService.run(processorConfiguration);

        log.info("Completed automated Marston processing");
    }
}