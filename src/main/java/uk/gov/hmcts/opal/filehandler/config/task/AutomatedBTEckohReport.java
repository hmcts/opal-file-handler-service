package uk.gov.hmcts.opal.filehandler.config.task;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.config.BTEckohReportBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.BTEckohReportBaisFileProcessorService;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "opal.automated-task", havingValue = "BTEckohReport")
@Slf4j
public class AutomatedBTEckohReport implements ApplicationRunner {

    private final BTEckohReportBaisFileProcessorService fileProcessorService;
    private final BTEckohReportBaisFileProcessorConfiguration configuration;

    @Override
    public void run(ApplicationArguments args) throws IOException {
        log.info("Starting automated BTEckoh report");

        fileProcessorService.run(configuration);

        log.info("Completed automated BTEckoh report");
    }
}
