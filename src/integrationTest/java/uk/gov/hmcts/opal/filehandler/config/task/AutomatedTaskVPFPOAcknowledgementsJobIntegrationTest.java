package uk.gov.hmcts.opal.filehandler.config.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.servlet.DispatcherServlet;
import uk.gov.hmcts.opal.filehandler.support.AbstractIntegrationTest;

@ActiveProfiles("integration")
@SpringBootTest(properties = {
    "opal.automated-task=VPFPOAcknowledgementsJob",
    "spring.main.web-application-type=none",
    "opal.testing-support-endpoints.enabled=false"
})
public class AutomatedTaskVPFPOAcknowledgementsJobIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void shouldNotCreateWebLayer() {
        assertThat(applicationContext.containsBean("dispatcherServlet")).isFalse();
        assertThat(applicationContext.getBeansOfType(DispatcherServlet.class).isEmpty()).isTrue();
    }
}
