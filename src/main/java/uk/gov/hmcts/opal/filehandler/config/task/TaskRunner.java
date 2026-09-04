package uk.gov.hmcts.opal.filehandler.config.task;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "opal.automated-task")
@Slf4j
public class TaskRunner implements ApplicationRunner {

    public TaskRunner(List<TaskConfiguration> configs, @Value("${opal.automated-task}") String task) {
        configuration = configs.stream()
            .filter(c -> c.getClass().toString().toLowerCase().contains(task.toLowerCase()))
            .findFirst().orElseThrow();
    }

    private final TaskConfiguration configuration;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        configuration.run();
    }
}
