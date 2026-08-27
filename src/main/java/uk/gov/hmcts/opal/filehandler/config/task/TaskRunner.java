package uk.gov.hmcts.opal.filehandler.config.task;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "opal.automated-task")
@Slf4j
@RequiredArgsConstructor
public class TaskRunner implements ApplicationRunner {


    private final TaskConfiguration configuration;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        configuration.run();
    }
}
