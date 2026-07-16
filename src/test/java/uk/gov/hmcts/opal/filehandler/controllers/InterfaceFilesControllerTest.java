package uk.gov.hmcts.opal.filehandler.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.opal.filehandler.service.InterfaceFileService;

@ExtendWith(MockitoExtension.class)
public class InterfaceFilesControllerTest {

    @Mock
    private InterfaceFileService interfaceFileService;

    @InjectMocks
    private InterfaceFilesController interfaceFilesController;

    @Test
    public void getInterfaceFileContent_returns200() {
        when(interfaceFileService.GetInterfaceFilesContent(eq(1L))).thenReturn(
            mock(InputStream.class)
        );

        ResponseEntity<Resource> response = interfaceFilesController.getInterfaceFileContent(1L);

        verify(interfaceFileService).GetInterfaceFilesContent(eq(1L));
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
    }

    // TODO: unit tests for the other statuses
}
