package uk.gov.hmcts.opal.filehandler.config.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockReset;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.DispatcherServlet;
import uk.gov.hmcts.opal.filehandler.config.BTEckohBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.BTEckohBaisFileProcessorService;
import uk.gov.hmcts.opal.filehandler.support.AbstractIntegrationTest;

@ActiveProfiles("integration")
@SpringBootTest(properties = {
    "opal.automated-task=BTEckohFileTransferJob",
    "spring.main.web-application-type=none"
})
@DisplayName("BTEckoh Automated Task Integration Tests")
class AutomatedTaskBTEckohIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @MockitoBean(enforceOverride = true, reset = MockReset.NONE)
    private BTEckohBaisFileProcessorService processorService;

    @Autowired
    private BTEckohBaisFileProcessorConfiguration processorConfiguration;

    @Test
    @DisplayName("BTEckoh file transfer starts without the web layer")
    void shouldNotCreateWebLayer() {
        assertThat(applicationContext.containsBean("dispatcherServlet")).isFalse();
        assertThat(applicationContext.getBeansOfType(DispatcherServlet.class)).isEmpty();
    }

    @Test
    @DisplayName("BTEckoh file transfer invokes the configured processor")
    void shouldRunBTEckohFileTransferJob() {
        verify(processorService, times(1)).run(processorConfiguration);
    }
}
