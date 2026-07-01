package uk.gov.hmcts.opal.filehandler;

import java.util.Arrays;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import uk.gov.hmcts.opal.filehandler.config.FeignConfiguration;

@SpringBootApplication(scanBasePackages = "uk.gov.hmcts.opal")
@EnableJpaRepositories("uk.gov.hmcts.opal.*")
@EntityScan("uk.gov.hmcts.opal.*")
@EnableFeignClients(basePackages = "uk.gov.hmcts.opal.*", defaultConfiguration = FeignConfiguration.class)
@EnableCaching
@Slf4j
@ConfigurationPropertiesScan
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {

    static final String AUTOMATED_TASK_PREFIX = "AutomatedTask:";
    static final String AUTOMATED_TASK_PROPERTY = "opal.automated-task";

    public static void main(final String[] args) {
        if (isAutomatedTask(args)) {
            System.exit(runAutomatedTask(args));
        }

        SpringApplication.run(Application.class, args);
    }

    static int runAutomatedTask(final String[] args) {
        var ctx = new SpringApplicationBuilder(Application.class)
            .web(WebApplicationType.NONE)
            .properties(Map.of(AUTOMATED_TASK_PROPERTY, getAutomatedTaskName(args)))
            .run(args);

        return SpringApplication.exit(ctx);
    }

    static boolean isAutomatedTask(final String[] args) {
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

    static String getAutomatedTaskName(final String[] args) {
        return Arrays.stream(args)
            .filter(arg -> arg.startsWith(AUTOMATED_TASK_PREFIX))
            .findFirst()
            .get()
            .substring(AUTOMATED_TASK_PREFIX.length());
    }
}
