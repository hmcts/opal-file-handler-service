package uk.gov.hmcts.opal.filehandler.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.azure.core.util.BinaryData;
import jakarta.persistence.EntityNotFoundException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.common.spring.security.OpalJwtAuthenticationToken;
import uk.gov.hmcts.opal.common.user.authorisation.exception.PermissionNotAllowedException;
import uk.gov.hmcts.opal.common.util.SecurityUtil;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.config.BTEckohReportBaisFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.config.CapsReportBaisFileProcessorConfig;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.exception.BlobNotFoundException;
import uk.gov.hmcts.opal.filehandler.exception.InvalidInterfaceFileStatusException;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;

@ExtendWith(MockitoExtension.class)
public class InterfaceFileServiceTest {

    @Mock
    private InterfaceFilesRepository repository;

    @Mock
    private InterfaceFileBlobStoreService blobStoreService;

    @Mock
    private BTEckohReportBaisFileProcessorConfig bteckohConfig;

    @Mock
    private CapsReportBaisFileProcessorConfig capsConfig;

    @Mock
    private OpalJwtAuthenticationToken authToken;

    @Mock
    private Map<String, BaisFileProcessorConfig> configs;

    @InjectMocks
    private InterfaceFileService interfaceFileService;

    private UUID uuid;

    private BinaryData mockData;
    private MockedStatic<SecurityUtil> securityUtil;

    @BeforeEach
    public void setup() {
        uuid = UUID.randomUUID();
        mockData = mock(BinaryData.class);
        securityUtil = mockStatic(SecurityUtil.class);
    }

    public void withPermissions() {
        securityUtil.when(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser).thenReturn(authToken);
        when(authToken.hasPermission(FileHandlerPermission.ViewInterfacesFile)).thenReturn(true);
    }

    public void withoutPermissions() {
        securityUtil.when(SecurityUtil::getOpalJwtAuthenticationTokenForCurrentUser).thenReturn(authToken);
        when(authToken.hasPermission(FileHandlerPermission.ViewInterfacesFile)).thenReturn(false);
    }

    @AfterEach
    public void teardown() {
        securityUtil.close();
    }

    @Test
    public void getInterfaceFileContent_bteckohSourceReturnsData() {
        withPermissions();
        when(configs.get(eq("BTECKOH_REPORT"))).thenReturn(bteckohConfig);
        when(bteckohConfig.getContainerName()).thenReturn("bteckoh-report");

        when(repository.findById(eq(1L))).thenReturn(
            Optional.of(buildEntity(1L, uuid, Interface.BTECKOH_REPORT, Status.SUCCESS))
        );
        when(blobStoreService.fetchInterfaceFile(eq(1L), eq(uuid), eq("bteckoh-report"))).thenReturn(mockData);

        InputStream response = interfaceFileService.getInterfaceFilesContent(1L);

        verify(repository).findById(eq(1L));
        verify(blobStoreService).fetchInterfaceFile(eq(1L), eq(uuid), eq("bteckoh-report"));
        verify(bteckohConfig).getContainerName();
    }

    @Test
    public void getInterfaceFileContent_capsSourceReturnsData() {
        withPermissions();
        when(configs.get(eq("CAPS_REPORT"))).thenReturn(capsConfig);
        when(capsConfig.getContainerName()).thenReturn("caps-report");

        when(repository.findById(eq(1L))).thenReturn(
            Optional.of(buildEntity(1L, uuid, Interface.CAPS_REPORT, Status.SUCCESS))
        );
        when(blobStoreService.fetchInterfaceFile(eq(1L), eq(uuid), eq("caps-report"))).thenReturn(mockData);

        InputStream response = interfaceFileService.getInterfaceFilesContent(1L);

        verify(repository).findById(eq(1L));
        verify(blobStoreService).fetchInterfaceFile(eq(1L), eq(uuid), eq("caps-report"));
        verify(capsConfig).getContainerName();
    }

    @Test
    public void getInterfaceFileContent_missingPermissionsThrowsError() {
        withoutPermissions();

        assertThrows(
            PermissionNotAllowedException.class,
            () -> interfaceFileService.getInterfaceFilesContent(1L)
        );
    }

    @Test
    public void getInterfaceFileContent_EntityNotFoundThrowsError() {
        withPermissions();

        when(repository.findById(eq(1L))).thenReturn(
            Optional.ofNullable(null)
        );

        assertThrows(EntityNotFoundException.class, () -> interfaceFileService.getInterfaceFilesContent(1L));

        verify(repository).findById(1L);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(blobStoreService);
    }


    @Test
    public void getInterfaceFileContent_invalidStatusThrowsError() {
        withPermissions();

        when(repository.findById(eq(1L))).thenReturn(
            Optional.of(buildEntity(1L, uuid, Interface.BTECKOH_REPORT, Status.FAILED))
        );

        assertThrows(
            InvalidInterfaceFileStatusException.class,
            () -> interfaceFileService.getInterfaceFilesContent(1L)
        );

        verify(repository).findById(1L);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(blobStoreService);
    }

    @Test
    public void getInterfaceFileContent_missingBlobThrowsError() {
        withPermissions();
        when(configs.get(eq("BTECKOH_REPORT"))).thenReturn(bteckohConfig);
        when(bteckohConfig.getContainerName()).thenReturn("bteckoh-report");

        when(repository.findById(eq(1L))).thenReturn(
            Optional.of(buildEntity(1L, uuid, Interface.BTECKOH_REPORT, Status.SUCCESS))
        );
        when(blobStoreService.fetchInterfaceFile(eq(1L), eq(uuid), eq("bteckoh-report")))
            .thenThrow(BlobNotFoundException.class);

        assertThrows(BlobNotFoundException.class, () -> interfaceFileService.getInterfaceFilesContent(1L));

        verify(repository).findById(1L);
        verifyNoMoreInteractions(repository);
        verify(bteckohConfig).getContainerName();

    }

    private InterfaceFileEntity buildEntity(long id, UUID fsuuid, Interface source, Status status) {
        return InterfaceFileEntity.builder()
            .interfaceFileId(id)
            .filestoreUuid(fsuuid)
            .source(source)
            .target(Interface.BTECKOH_REPORT)
            .type(Type.SOURCE)
            .opalDomain(Domain.FILE_HANDLER)
            .fileName("fileName")
            .status(status)
            .createdDatetime(Date.from(Instant.now()))
            .build();
    }
}
