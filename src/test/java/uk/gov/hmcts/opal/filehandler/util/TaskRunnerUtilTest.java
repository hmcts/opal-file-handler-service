package uk.gov.hmcts.opal.filehandler.util;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TaskRunnerUtilTest {
    @Test
    void isAutomatedTaskReturnsFalseWhenNoAutomatedTaskArguments() {
        assertThat(TaskRunnerUtil.isAutomatedTask(new String[] { "someRandomArgument" })).isFalse();
    }

    @Test
    void getAutomatedTaskNameReturnsTaskName() {
        assertThat(TaskRunnerUtil.getAutomatedTaskName(new String[] { "AutomatedTask:CAPSReport"  }))
            .isEqualTo("CAPSReport");
    }

    @Test
    void isAutomatedTaskShouldThrowExceptionWhenMultipleTasksFound() {
        var exception = assertThrows(
            IllegalArgumentException.class,
            () -> TaskRunnerUtil.isAutomatedTask(new String[] {
                "AutomatedTask:BTEckohReport",
                "someRandomArgument",
                "AutomatedTask:CAPSReport"
            })
        );

        assertThat(exception.getMessage()).isEqualTo("Multiple automated tasks found");
    }
}
