package uk.gov.hmcts.opal.filehandler;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class ApplicationTest {
    @Test
    void isAutomatedTaskReturnsFalseWhenNoAutomatedTaskArguments() {
        assertThat(Application.isAutomatedTask(new String[] { "someRandomArgument" })).isFalse();
    }

    @Test
    void getAutomatedTaskNameReturnsTaskName() {
        assertThat(Application.getAutomatedTaskName(new String[] { "AutomatedTask:CAPSReport"  }))
            .isEqualTo("CAPSReport");
    }

    @Test
    void isAutomatedTaskShouldThrowExceptionWhenMultipleTasksFound() {
        var exception = assertThrows(
            IllegalArgumentException.class,
            () -> Application.isAutomatedTask(new String[] {
                "AutomatedTask:BTEckohReport",
                "someRandomArgument",
                "AutomatedTask:CAPSReport"
            })
        );

        assertThat(exception.getMessage()).isEqualTo("Multiple automated tasks found");
    }

    @Test
    void mainShouldRunNormallyWhenAutomatedTaskIsMissing() {
        var context = mock(ConfigurableApplicationContext.class);
        String[] args = new String[0];

        try (MockedStatic<SpringApplication> app = mockStatic(SpringApplication.class)) {
            app.when(() -> SpringApplication.run(Application.class, args)).thenReturn(context);
            Application.main(args);
            app.verify(() -> SpringApplication.run(Application.class, args));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "BTEckohReport", "CAPSReport" })
    void runAutomatedTaskShouldDisableWebLayerAndExit(final String task) {
        var context = mock(ConfigurableApplicationContext.class);
        String[] args = new String[] { "AutomatedTask:" + task };

        try (MockedConstruction<SpringApplicationBuilder> construction =
                mockConstruction(SpringApplicationBuilder.class, (mock, mockContext) -> {
                    when(mock.web(WebApplicationType.NONE)).thenReturn(mock);
                    when(mock.properties(anyMap())).thenReturn(mock);
                    when(mock.run(any(String[].class))).thenReturn(context);
                });

            MockedStatic<SpringApplication> app = mockStatic(SpringApplication.class)) {

            app.when(() -> SpringApplication.exit(context)).thenReturn(0);

            int exitCode = Application.runAutomatedTask(args);

            assertThat(exitCode).isZero();

            var builder = construction.constructed().get(0);

            verify(builder).web(WebApplicationType.NONE);
            verify(builder).properties(Map.of(Application.AUTOMATED_TASK_PROPERTY, task));
            verify(builder).run(args);

            app.verify(() -> SpringApplication.exit(context));
        }
    }
}
