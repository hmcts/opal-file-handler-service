package uk.gov.hmcts.opal.filehandler.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;
import uk.gov.hmcts.opal.filehandler.service.InterfaceFilesService;
import uk.gov.hmcts.opal.filehandler.util.TaskRunnerUtil;
import uk.gov.hmcts.opal.generated.model.DeleteInterfaceFilesResultTestSupport;

@ExtendWith(MockitoExtension.class)
public class TestSupportControllerTest {
    @Mock
    private TaskRunnerUtil taskRunnerUtil;

    @Mock
    private InterfaceFilesService interfaceFilesService;

    @Captor
    private ArgumentCaptor<Set<Long>> idsArgCaptor;

    @InjectMocks
    private TestSupportController controller;

    @Test
    void testSupportAutomatedJobsNamePost_returns200() {
        ResponseEntity<Void> response = controller.testingSupportAutomatedJobsNamePost("BTEckohReport");
        verify(taskRunnerUtil).runAutomatedTask(eq("automatedBTEckohReport"));
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    void testSupport_deleteInterfaceFiles_shouldOrchestrateCallToServiceCorrectly() {
        // Arrange
        Long id1 = 100L;
        Long id2 = 200L;
        Long id3 = 300L;
        List<Long> ids = List.of(id1, id2, id3, id3, id2);
        DeleteInterfaceFilesResultTestSupport result = mock(DeleteInterfaceFilesResultTestSupport.class);
        when(interfaceFilesService.deleteInterfaceFiles(anySet())).thenReturn(result);

        // Act
        ResponseEntity<DeleteInterfaceFilesResultTestSupport> response =
            controller.testingSupportInterfaceFilesDelete(ids);

        // Assert
        verify(interfaceFilesService, times(1)).deleteInterfaceFiles(idsArgCaptor.capture());
        assertThat(idsArgCaptor.getValue()).containsExactlyInAnyOrder(id1, id2, id3);
        assertThat(response.getBody()).isSameAs(result);
    }

    @Test
    void testSupport_deleteInterfaceFiles_noErrors_shouldReturn200() {
        // Arrange
        List<Long> ids = List.of(1L);
        DeleteInterfaceFilesResultTestSupport result = DeleteInterfaceFilesResultTestSupport.builder()
            .successfullyDeletedIds(ids)
            .failedIds(Collections.emptyList())
            .build();
        when(interfaceFilesService.deleteInterfaceFiles(anySet())).thenReturn(result);

        // Act
        ResponseEntity<DeleteInterfaceFilesResultTestSupport> response = controller
            .testingSupportInterfaceFilesDelete(ids);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(result);
    }

    @Test
    void testSupport_deleteInterfaceFiles_allErrors_shouldThrow500() {
        // Arrange
        List<Long> ids = List.of(1L);
        DeleteInterfaceFilesResultTestSupport result = DeleteInterfaceFilesResultTestSupport.builder()
            .successfullyDeletedIds(Collections.emptyList())
            .failedIds(ids)
            .build();
        when(interfaceFilesService.deleteInterfaceFiles(anySet())).thenReturn(result);

        // Act + Assert
        InternalServerErrorException ex = assertThrows(InternalServerErrorException.class, () ->
            controller.testingSupportInterfaceFilesDelete(ids));

        // Assert
        assertThat(ex.getTitle()).isEqualTo("Failed to delete interface files");
        assertThat(ex.getDetail()).isEqualTo("Process exited cleanly but errors were encountered. "
            + "See logs for details");
    }

    @Test
    void testSupport_deleteInterfaceFiles_someSuccessAndSomeFailures_shouldReturn207() {
        // Arrange
        List<Long> ids = List.of(1L, 2L);
        DeleteInterfaceFilesResultTestSupport result = DeleteInterfaceFilesResultTestSupport.builder()
            .successfullyDeletedIds(List.of(1L))
            .failedIds(List.of(2L))
            .build();
        when(interfaceFilesService.deleteInterfaceFiles(anySet())).thenReturn(result);

        // Act
        ResponseEntity<DeleteInterfaceFilesResultTestSupport> response = controller
            .testingSupportInterfaceFilesDelete(ids);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.MULTI_STATUS);
        assertThat(response.getBody()).isSameAs(result);
    }
}
