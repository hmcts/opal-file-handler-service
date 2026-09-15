package uk.gov.hmcts.opal.filehandler.config.task;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.filehandler.config.BTEckohBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.service.BTEckohBaisFileProcessorService;

@ExtendWith(MockitoExtension.class)
@DisplayName("Automated BTEckoh file transfer job")
class AutomatedTaskBTEckohTest {

    @Mock
    private BTEckohBaisFileProcessorService processorService;

    @Mock
    private BTEckohBaisFileProcessorConfiguration processorConfiguration;

    @InjectMocks
    private AutomatedBTEckohFileTransferJob job;

    @Test
    @DisplayName("Runs the BTEckoh processor with its configuration")
    void shouldRunBteckohProcessorWithItsConfiguration() {
        job.run();

        verify(processorService).run(processorConfiguration);
    }
}
