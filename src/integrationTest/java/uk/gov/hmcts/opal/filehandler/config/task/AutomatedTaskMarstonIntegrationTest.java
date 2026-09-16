package uk.gov.hmcts.opal.filehandler.config.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.DispatcherServlet;

import uk.gov.hmcts.opal.filehandler.support.AbstractIntegrationTest;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;

@ActiveProfiles("integration")
@SpringBootTest(properties = {
    "opal.automated-task=MarstonFileTransferJob",
    "spring.main.web-application-type=none",
    "launchdarkly.default-flag-values.release-1c-banking-interfaces=true",
    "launchdarkly.default-flag-values.marston-file-transfer-job=true"
})
class AutomatedTaskMarstonIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AutomatedMarstonFileTransferJob automatedMarstonFileTransferJob;

    @MockitoBean
    private BaisSftpClient baisSftpClient;

    @BeforeEach
    void setUp() {
        when(baisSftpClient.listRegularFiles(anyString())).thenReturn(List.of());
    }

    @Test
    void shouldNotCreateWebLayer() {
        assertThat(applicationContext.containsBean("dispatcherServlet")).isFalse();
        assertThat(applicationContext.getBeansOfType(DispatcherServlet.class).isEmpty()).isTrue();
    }

    @Test
    void shouldCallAutomatedTaskRun() {
        assertThatCode(() -> automatedMarstonFileTransferJob.run())
            .doesNotThrowAnyException();
    }
}