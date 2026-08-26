package uk.gov.hmcts.opal.filehandler.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.opal.filehandler.util.TaskRunnerUtil;

@ExtendWith(MockitoExtension.class)
public class TestSupportControllerTest {
    @InjectMocks
    private TestSupportController controller;

    @Test
    void testSupportAutomatedJobsNamePost_returns200() {
        try (MockedStatic<TaskRunnerUtil> taskRunnerUtil = mockStatic(TaskRunnerUtil.class)) {
            taskRunnerUtil.when(() -> TaskRunnerUtil.runAutomatedTask(eq("AutomatedTask:BTEckohReport"))).thenReturn(0);

            ResponseEntity<Void> response = controller.testSupportAutomatedJobsNamePost("BTEckohReport");

            taskRunnerUtil.verify(() -> TaskRunnerUtil.runAutomatedTask(eq("AutomatedTask:BTEckohReport")));
            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }
}
