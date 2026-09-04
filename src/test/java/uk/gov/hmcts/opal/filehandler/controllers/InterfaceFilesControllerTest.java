package uk.gov.hmcts.opal.filehandler.controllers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.opal.filehandler.mapper.SearchInterfaceFilesDtoMapper;
import uk.gov.hmcts.opal.filehandler.service.InterfaceFilesService;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;
import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;
import uk.gov.hmcts.opal.generated.model.GetInterfaceFiles200Response;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;
import uk.gov.hmcts.opal.generated.model.StatusEnumInterfaceFile;

@ExtendWith(MockitoExtension.class)
public class InterfaceFilesControllerTest {

    @Mock
    private InterfaceFilesService service;

    @Mock
    private SearchInterfaceFilesDtoMapper mapper;

    @InjectMocks
    private InterfaceFilesController controller;

    @Test
    void getEnforcementAccountTypes_Success() {
        InterfaceFileEnumInterfaceFile source = InterfaceFileEnumInterfaceFile.BTECKOH_REPORT;
        InterfaceFileEnumInterfaceFile target = InterfaceFileEnumInterfaceFile.OPAL;
        DomainEnumTypes domain = DomainEnumTypes.FINES;
        StatusEnumInterfaceFile status = StatusEnumInterfaceFile.SUCCESS;
        LocalDateTime toDate = LocalDateTime.of(2026, Month.APRIL, 1, 9, 0);
        SearchInterfaceFilesDto searchDto = new SearchInterfaceFilesDto();
        List<InterfaceFileObjectInterfaceFile> interfaceFiles = List.of(
            mock(InterfaceFileObjectInterfaceFile.class),
            mock(InterfaceFileObjectInterfaceFile.class)
        );

        when(mapper.toSearchInterfaceFilesDto(
            source, target, null, domain, status, null, toDate)
        ).thenReturn(searchDto);
        when(service.searchInterfaceFiles(searchDto)).thenReturn(interfaceFiles);

        ResponseEntity<GetInterfaceFiles200Response> response = controller.getInterfaceFiles(
            source, target, null, domain, status, null, toDate
        );

        assertAll(
            () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
            () -> assertSame(interfaceFiles, response.getBody().getInterfaceFiles()),
            () -> assertEquals(2, response.getBody().getNumberOfResults())
        );
    }

    @Test
    void getEnforcementAccountTypes_ReturnsOkWithEmptyCollection() {
        StatusEnumInterfaceFile status = StatusEnumInterfaceFile.FAILED_SUPERSEDED;
        SearchInterfaceFilesDto searchDto = new SearchInterfaceFilesDto();
        List<InterfaceFileObjectInterfaceFile> interfaceFiles = Collections.emptyList();

        when(mapper.toSearchInterfaceFilesDto(
            null, null, null, null, status, null, null)
        ).thenReturn(searchDto);
        when(service.searchInterfaceFiles(searchDto)).thenReturn(interfaceFiles);

        ResponseEntity<GetInterfaceFiles200Response> response = controller.getInterfaceFiles(
            null, null, null, null, status, null, null
        );

        assertAll(
            () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
            () -> assertSame(interfaceFiles, response.getBody().getInterfaceFiles()),
            () -> assertEquals(0, response.getBody().getNumberOfResults())
        );
    }

    @Test
    void getInterfaceFile_returns200WithResponseBody() {
        InterfaceFileObjectInterfaceFile expected = mock(InterfaceFileObjectInterfaceFile.class);
        when(service.getInterfaceFile(1L)).thenReturn(expected);

        ResponseEntity<InterfaceFileObjectInterfaceFile> response = controller.getInterfaceFile(1L);

        verify(service).getInterfaceFile(1L);
        verifyNoMoreInteractions(service);
        assertAll(
            () -> assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode()),
            () -> assertSame(expected, response.getBody())
        );
    }

    @Test
    void getInterfaceFile_propagatesServiceException() {
        RuntimeException expected = new RuntimeException("not found");
        when(service.getInterfaceFile(1L)).thenThrow(expected);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> controller.getInterfaceFile(1L));

        assertSame(expected, exception);
        verify(service).getInterfaceFile(1L);
        verifyNoMoreInteractions(service);
    }

    @Test
    void getInterfaceFileContent_returns200WithBodyWrappingReturnedStream() {
        InputStream expectedStream = new ByteArrayInputStream("payload".getBytes(StandardCharsets.UTF_8));
        when(service.getInterfaceFilesContent(eq(1L))).thenReturn(expectedStream);

        ResponseEntity<Resource> response = controller.getInterfaceFileContent(1L);

        verify(service).getInterfaceFilesContent(eq(1L));
        verifyNoMoreInteractions(service);
        assertAll(
            () -> assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode()),
            () -> assertEquals(InputStreamResource.class, response.getBody().getClass()),
            () -> assertEquals("payload",
                new String(response.getBody().getInputStream().readAllBytes(), StandardCharsets.UTF_8))
        );
    }
}
