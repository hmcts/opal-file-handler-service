package uk.gov.hmcts.opal.filehandler.config.task;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "opal.automated-task", havingValue = "CAPSReport")
@Slf4j
public class AutomatedCapsReport implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws IOException {
        log.info("Starting automated CAPS report");

        // TODO

        try {
            TimeUnit.MINUTES.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Completed automated CAPS report");
    }
}
