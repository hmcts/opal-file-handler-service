package uk.gov.hmcts.opal.filehandler.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.azure.core.util.BinaryData;
import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.opal.common.util.SecurityUtil;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.config.BTEckohReportBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.config.FileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.config.CapsReportBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.exception.BlobNotFoundException;
import uk.gov.hmcts.opal.filehandler.exception.InterfaceFileNotFoundException;
import uk.gov.hmcts.opal.filehandler.exception.InvalidInterfaceFileStatusException;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.pdpl.InterfaceFilesPdplLoggingService;
import uk.gov.hmcts.opal.filehandler.util.PermissionUtil;

@ExtendWith(MockitoExtension.class)
class InterfaceFileServiceTest {

    @Mock
    private InterfaceFilesPdplLoggingService loggingService;

    @Mock
    private UserStateV2 userState;

    @Mock
    private InterfaceFilesRepository repository;

    @Mock
    private InterfaceFileBlobStoreService blobStoreService;

    @Mock
    private BTEckohReportBaisFileProcessorConfiguration bteckohConfig;

    @Mock
    private CapsReportBaisFileProcessorConfiguration capsConfig;

    @Mock
    private OpalJwtAuthenticationToken authToken;

    @Mock
    private Map<String, FileProcessorConfiguration> configs;

    @Mock
    private List<InterfaceFileProcessorService> processorServicesList;

    @InjectMocks
    private InterfaceFilesService interfaceFileService;

    private UUID uuid;

    private BinaryData mockData;
    private MockedStatic<PermissionUtil> permissionUtil;
    private MockedStatic<SecurityUtil> securityUtil;

    @BeforeEach
    void setup() {
        uuid = UUID.randomUUID();
        mockData = mock(BinaryData.class);
        permissionUtil = mockStatic(PermissionUtil.class);
        securityUtil = mockStatic(SecurityUtil.class);
    }

    void withPermissions() {
        securityUtil.when(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser).thenReturn(authToken);
        // when(authToken.hasPermission(FileHandlerPermission.VIEW_INTERFACE_FILES)).thenReturn(true);
    }

    void withUserState() {
        when(authToken.getUserState()).thenReturn(userState);
    }

    void withoutPermissions() {
        securityUtil.when(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser).thenReturn(authToken);
        // when(authToken.hasPermission(FileHandlerPermission.VIEW_INTERFACE_FILES)).thenReturn(false);
    }

    @AfterEach
    void teardown() {
        permissionUtil.close();
        securityUtil.close();
    }

    @Test
    void getInterfaceFileContent_bteckohSourceReturnsData() {
        withPermissions();
        withUserState();
        when(configs.get(eq("BTEckohReportBaisFileProcessorConfig"))).thenReturn(bteckohConfig);
        when(bteckohConfig.getContainerName()).thenReturn("bteckoh-report");

        when(repository.findById(eq(1L))).thenReturn(
            Optional.of(buildEntity(1L, uuid, Interface.BTECKOH_REPORT, Status.SUCCESS)));
        when(blobStoreService.fetchInterfaceFile(eq(1L), eq(uuid), eq("bteckoh-report"))).thenReturn(mockData);
        when(userState.isSystemUser()).thenReturn(false);
        InputStream expectedStream = mock(InputStream.class);
        when(mockData.toStream()).thenReturn(expectedStream);

        InputStream response = interfaceFileService.getInterfaceFilesContent(1L);

        assertSame(expectedStream, response);
        verify(repository).findById(eq(1L));
        verify(blobStoreService).fetchInterfaceFile(eq(1L), eq(uuid), eq("bteckoh-report"));
        verify(bteckohConfig).getContainerName();
        securityUtil.verify(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser);
        verify(authToken).getUserState();
        verify(loggingService).logPdpl(1L, userState);
    }

    @Nested
    @DisplayName("Container selection")
    class ContainerSelection {

        @ParameterizedTest
        @CsvSource({
            "BTECKOH_REPORT, OPAL, BTEckohReportBaisFileProcessorConfig, bteckoh-report",
            "OPAL, BTECKOH_REPORT, BTEckohReportBaisFileProcessorConfig, bteckoh-report",
            "CAPS_REPORT, OPAL, capsReportBaisFileProcessorConfig, caps-report",
            "OPAL, CAPS_REPORT, capsReportBaisFileProcessorConfig, caps-report"
        })
        @DisplayName("Uses the external interface configuration for inbound and outbound files")
        void usesExternalInterfaceContainer(Interface source, Interface target, String configName, String container)
            throws IOException {
            withPermissions();
            withUserState();
            InterfaceFileEntity entity = buildEntity(1L, uuid, source, Status.SUCCESS);
            entity.setTarget(target);
            FileProcessorConfiguration config = source == Interface.CAPS_REPORT || target == Interface.CAPS_REPORT
                ? capsConfig : bteckohConfig;
            when(repository.findById(1L)).thenReturn(Optional.of(entity));
            when(configs.get(configName)).thenReturn(config);
            when(config.getContainerName()).thenReturn(container);
            when(blobStoreService.fetchInterfaceFile(1L, uuid, container))
                .thenReturn(BinaryData.fromString("file-content"));
            when(userState.isSystemUser()).thenReturn(true);

            InputStream result = interfaceFileService.getInterfaceFilesContent(1L);

            assertEquals("file-content", new String(result.readAllBytes(), StandardCharsets.UTF_8));
            verify(configs).get(configName);
            verifyNoMoreInteractions(configs);
            verify(blobStoreService).fetchInterfaceFile(1L, uuid, container);
        }
    }

    @Test
    void getInterfaceFileContent_capsSourceReturnsData() {
        withPermissions();
        withUserState();
        when(configs.get(eq("capsReportBaisFileProcessorConfig"))).thenReturn(capsConfig);
        when(capsConfig.getContainerName()).thenReturn("caps-report");

        when(repository.findById(eq(1L))).thenReturn(
            Optional.of(buildEntity(1L, uuid, Interface.CAPS_REPORT, Status.SUCCESS))
        );
        when(blobStoreService.fetchInterfaceFile(eq(1L), eq(uuid), eq("caps-report"))).thenReturn(mockData);
        when(userState.isSystemUser()).thenReturn(false);
        InputStream expectedStream = mock(InputStream.class);
        when(mockData.toStream()).thenReturn(expectedStream);

        InputStream response = interfaceFileService.getInterfaceFilesContent(1L);

        assertSame(expectedStream, response);
        verify(repository).findById(eq(1L));
        verify(blobStoreService).fetchInterfaceFile(eq(1L), eq(uuid), eq("caps-report"));
        verify(capsConfig).getContainerName();
        securityUtil.verify(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser);
        verify(authToken).getUserState();
        verify(loggingService).logPdpl(1L, userState);
    }

    @Test
    void getInterfaceFileContent_systemUserReturnsDataWithoutPdplLogging() {
        withPermissions();
        withUserState();
        when(configs.get(eq("BTEckohReportBaisFileProcessorConfig"))).thenReturn(bteckohConfig);
        when(bteckohConfig.getContainerName()).thenReturn("bteckoh-report");
        when(repository.findById(eq(1L))).thenReturn(
            Optional.of(buildEntity(1L, uuid, Interface.BTECKOH_REPORT, Status.SUCCESS))
        );
        when(blobStoreService.fetchInterfaceFile(eq(1L), eq(uuid), eq("bteckoh-report"))).thenReturn(mockData);
        when(userState.isSystemUser()).thenReturn(true);

        interfaceFileService.getInterfaceFilesContent(1L);

        verify(repository).findById(1L);
        verify(blobStoreService).fetchInterfaceFile(1L, uuid, "bteckoh-report");
        securityUtil.verify(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser);
        verify(authToken).getUserState();
        verifyNoInteractions(loggingService);
    }

    @Test
    void getInterfaceFileContent_missingPermissionsThrowsError() {
        when(repository.findById(1L)).thenReturn(
            Optional.of(buildEntity(1L, uuid, Interface.BTECKOH_REPORT, Status.SUCCESS)));
        permissionUtil.when(() ->
            PermissionUtil.checkPermissionInDomain(
                FileHandlerPermission.VIEW_INTERFACE_FILES,
                uk.gov.hmcts.opal.common.user.authorisation.model.Domain.FILE_HANDLING))
            .thenThrow(new PermissionNotAllowedException(FileHandlerPermission.VIEW_INTERFACE_FILES));

        Exception e = assertThrows(
            PermissionNotAllowedException.class,
            () -> interfaceFileService.getInterfaceFilesContent(1L)
        );
        assertEquals("[VIEW_INTERFACE_FILES] permission(s) are not enabled for the user.", e.getMessage());
    }

    @Test
    void getInterfaceFileContent_EntityNotFoundThrowsError() {
        withPermissions();

        when(repository.findById(eq(1L))).thenReturn(
            Optional.ofNullable(null)
        );

        Exception e = assertThrows(
            InterfaceFileNotFoundException.class,
            () -> interfaceFileService.getInterfaceFilesContent(1L)
        );
        assertEquals("404 NOT_FOUND \"Interface file with id 1 could not be located.\"", e.getMessage());

        verify(repository).findById(1L);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(blobStoreService);
        securityUtil.verifyNoInteractions();
        verifyNoInteractions(authToken, loggingService);
    }


    @Test
    void getInterfaceFileContent_invalidStatusThrowsError() {
        withPermissions();

        when(repository.findById(eq(1L))).thenReturn(
            Optional.of(buildEntity(1L, uuid, Interface.BTECKOH_REPORT, Status.FAILED))
        );

        Exception e = assertThrows(
            InvalidInterfaceFileStatusException.class,
            () -> interfaceFileService.getInterfaceFilesContent(1L)
        );
        assertEquals(
            "422 UNPROCESSABLE_CONTENT \"Interface file with id 1 could not be retrieved as it has "
            + "an invalid status of: \"FAILED\" only files with status: \"SUCCESS\" can be returned.\"",
            e.getMessage()
        );

        verify(repository).findById(1L);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(blobStoreService);
        securityUtil.verifyNoInteractions();
        verifyNoInteractions(authToken, loggingService);
    }

    @Test
    void getInterfaceFileContent_missingBlobThrowsError() {
        withPermissions();
        when(configs.get(eq("BTEckohReportBaisFileProcessorConfig"))).thenReturn(bteckohConfig);
        when(bteckohConfig.getContainerName()).thenReturn("bteckoh-report");

        when(repository.findById(eq(1L))).thenReturn(
            Optional.of(buildEntity(1L, uuid, Interface.BTECKOH_REPORT, Status.SUCCESS))
        );

        when(blobStoreService.fetchInterfaceFile(eq(1L), eq(uuid), eq("bteckoh-report")))
            .thenThrow(BlobNotFoundException.class);

        Exception e = assertThrows(
            BlobNotFoundException.class,
            () -> interfaceFileService.getInterfaceFilesContent(1L)
        );
        assertEquals("null", e.getMessage());

        verify(repository).findById(1L);
        verifyNoMoreInteractions(repository);
        verify(bteckohConfig).getContainerName();
        securityUtil.verifyNoInteractions();
        verifyNoInteractions(authToken, loggingService);

    }

    private InterfaceFileEntity buildEntity(long id, UUID fsuuid, Interface source, Status status) {
        return InterfaceFileEntity.builder()
            .interfaceFileId(id)
            .filestoreUuid(fsuuid)
            .source(source)
            .target(Interface.OPAL)
            .type(Type.SOURCE)
            .opalDomain(Domain.FILE_HANDLER)
            .fileName("fileName")
            .status(status)
            .createdDatetime(LocalDateTime.now())
            .createdBy(-1L)
            .build();
    }
}
