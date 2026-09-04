package uk.gov.hmcts.opal.filehandler.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.opal.filehandler.util.TaskRunnerUtil;

@ExtendWith(MockitoExtension.class)
public class TestSupportControllerTest {
    @Mock
    private TaskRunnerUtil taskRunnerUtil;

    @InjectMocks
    private TestSupportController controller;

    @Test
    void testSupportAutomatedJobsNamePost_returns200() {
        ResponseEntity<Void> response = controller.testingSupportAutomatedJobsNamePost("BTEckohReport");
        verify(taskRunnerUtil).runAutomatedTask(eq("automatedBTEckohReport"));
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }
}
