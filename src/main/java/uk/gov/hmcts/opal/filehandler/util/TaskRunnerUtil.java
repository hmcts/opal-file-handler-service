package uk.gov.hmcts.opal.filehandler.util;

import java.util.Arrays;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.filehandler.Application;
import uk.gov.hmcts.opal.filehandler.config.task.TaskConfiguration;

@Service
@RequiredArgsConstructor
public class TaskRunnerUtil {

    public static final String AUTOMATED_TASK_PREFIX = "AutomatedTask:";
    public static final String AUTOMATED_TASK_PROPERTY = "opal.automated-task";


    private final Map<String, TaskConfiguration> tasks;

    public static int runAutomatedTaskWithSpring(final String... args) {
        var ctx = new SpringApplicationBuilder(Application.class)
            .web(WebApplicationType.NONE)
            .properties(Map.of(AUTOMATED_TASK_PROPERTY, getAutomatedTaskName(args)))
            .run(args);

        return SpringApplication.exit(ctx);
    }

    public void runAutomatedTask(String name) {
        tasks.get(name).run();
    }

    public static boolean isAutomatedTask(final String[] args) {
        var tasks = Arrays.stream(args)
            .filter(arg -> arg.startsWith(AUTOMATED_TASK_PREFIX))
            .toList();

        if (tasks.isEmpty()) {
            return false;
        }

        if (tasks.size() >= 2) {
            throw new IllegalArgumentException("Multiple automated tasks found");
        }

        return true;
    }

    public static String getAutomatedTaskName(final String[] args) {
        return Arrays.stream(args)
            .filter(arg -> arg.startsWith(AUTOMATED_TASK_PREFIX))
            .findFirst()
            .orElse("")
            .substring(AUTOMATED_TASK_PREFIX.length());
    }
}
