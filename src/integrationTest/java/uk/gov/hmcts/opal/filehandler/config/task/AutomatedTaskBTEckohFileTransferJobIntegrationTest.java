package uk.gov.hmcts.opal.filehandler.config.task;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.DispatcherServlet;
import uk.gov.hmcts.opal.filehandler.support.AbstractIntegrationTest;

@ActiveProfiles("integration")
@SpringBootTest(properties = {
    "opal.automated-task=BTEckohFileTransferJob",
    "spring.main.web-application-type=none"
})
public class AutomatedTaskBTEckohFileTransferJobIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldNotCreateWebLayer() {
        assertThat(context.containsBean("dispatcherServlet")).isFalse();
        assertThat(context.getBeansOfType(DispatcherServlet.class).isEmpty()).isTrue();
    }
}
