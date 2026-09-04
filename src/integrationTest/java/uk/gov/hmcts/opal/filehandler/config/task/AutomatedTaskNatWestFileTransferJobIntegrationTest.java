package uk.gov.hmcts.opal.filehandler.config.task;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.DispatcherServlet;
import uk.gov.hmcts.opal.filehandler.config.NatWestBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.NatWestBaisFileProcessorService;
import uk.gov.hmcts.opal.filehandler.support.AbstractIntegrationTest;

@ActiveProfiles("integration")
@SpringBootTest(properties = {
    "opal.automated-task=NatWestFileTransferJob",
    "spring.main.web-application-type=none"
})
public class AutomatedTaskNatWestFileTransferJobIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean(enforceOverride = true, reset = MockReset.NONE)
    private NatWestBaisFileProcessorService service;

    @Test
    void shouldNotCreateWebLayer() {
        assertThat(applicationContext.containsBean("dispatcherServlet")).isFalse();
        assertThat(applicationContext.getBeansOfType(DispatcherServlet.class).isEmpty()).isTrue();
    }

    @Test
    void shouldCallAutomatedTaskRun() {
        verify(service, times(1)).run(any(NatWestBaisFileProcessorConfiguration.class));
    }

}
