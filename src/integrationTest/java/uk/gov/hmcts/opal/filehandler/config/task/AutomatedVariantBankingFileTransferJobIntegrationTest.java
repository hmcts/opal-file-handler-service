package uk.gov.hmcts.opal.filehandler.config.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.servlet.DispatcherServlet;

import uk.gov.hmcts.opal.filehandler.service.VariantBankingFileProcessorService;
import uk.gov.hmcts.opal.filehandler.support.AbstractIntegrationTest;

@ActiveProfiles("integration")
@SpringBootTest(properties = {
    "opal.automated-task=VariantBankingFileTransferJob",
    "spring.main.web-application-type=none"
})
class AutomatedVariantBankingFileTransferJobIntegrationTest
    extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private AutomatedVariantBankingFileTransferJob automatedVariantBankingFileTransferJob;

    @MockitoBean
    private VariantBankingFileProcessorService processorService;

    @Test
    void shouldNotCreateWebLayer() {
        assertThat(applicationContext.containsBean("dispatcherServlet")).isFalse();
        assertThat(applicationContext.getBeansOfType(DispatcherServlet.class).isEmpty()).isTrue();
    }

    @Test
    void shouldCallAutomatedTaskRun() {
        assertThatCode(() -> automatedVariantBankingFileTransferJob.run())
            .doesNotThrowAnyException();

        verify(processorService).run(any());
    }
}
