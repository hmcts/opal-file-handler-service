package uk.gov.hmcts.opal.filehandler.config.task;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.common.user.authentication.service.SystemUserAuthenticationService;
import uk.gov.hmcts.opal.common.user.authentication.service.SystemUserEnum;
import uk.gov.hmcts.opal.common.util.SecurityUtil;

@Component
@ConditionalOnProperty(name = "opal.automated-task")
@Slf4j
public class TaskRunner implements ApplicationRunner {

    public TaskRunner(List<TaskConfiguration> configs, @Value("${opal.automated-task}") String task,
        SystemUserAuthenticationService systemUserAuthenticationService) {
        configuration = configs.stream()
            .filter(c -> c.getClass().toString().toLowerCase().contains(task.toLowerCase()))
            .findFirst().orElseThrow();
        this.systemUserAuthenticationService = systemUserAuthenticationService;
    }

    private final TaskConfiguration configuration;
    private final SystemUserAuthenticationService systemUserAuthenticationService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        systemUserAuthenticationService.setupAsSystemUser(SystemUserEnum.OPAL_SYSTEM_USER);
        configuration.run();
        SecurityUtil.clearSecurityContext();
    }
}
