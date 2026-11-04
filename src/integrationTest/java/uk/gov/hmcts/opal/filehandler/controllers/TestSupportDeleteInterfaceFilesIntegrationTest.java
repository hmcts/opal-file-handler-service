package uk.gov.hmcts.opal.filehandler.controllers;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.opal.filehandler.IntegrationSecurityConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.support.AbstractControllerIntegrationTest;
import uk.gov.hmcts.opal.filehandler.support.TestContainerConfig;
import uk.gov.hmcts.opal.filehandler.support.UtilBlobStoreService;
import uk.gov.hmcts.opal.generated.model.DeleteInterfaceFilesResultTestSupport;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraEpic;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraStory;

@Slf4j(topic = "opal.TestSupportControllerIntegrationTest")
@ActiveProfiles(profiles = {"integration"})
@Import(IntegrationSecurityConfiguration.class)
public class TestSupportDeleteInterfaceFilesIntegrationTest extends AbstractControllerIntegrationTest {
    private static final String URI = "/testing-support/interface-files/";
    private static final String CONTAINER_NAME = "natwest";
    private static final String FILE_NAME = "file-to-delete.dat";
    private static final UUID FILESTORE_UUID = UUID.fromString("86f86146-08fe-4520-b6db-68bf65e635b4");
    private static UtilBlobStoreService utilBlobStoreService;

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    private InterfaceFilesRepository repository;


    @TestPropertySource(properties = {
        "opal.testing-support-endpoints.enabled=true"
    })
    @Nested
    class FeatureOn {

        @BeforeEach
        void setup() {
            utilBlobStoreService = new UtilBlobStoreService();
        }

        @Test
        @DisplayName("PO-11123 - Delete interface files - successfully deletes 2 files in same group")
        @JiraStory("PO-11123")
        @JiraEpic("PO-3497")
        void successfullyDeletesInterfaceFiles() throws Exception {
            // Arrange
            utilBlobStoreService.createContainerIfNotExists(CONTAINER_NAME);
            utilBlobStoreService.storeBlob(new byte[] { 0, 1, 2, 3}, CONTAINER_NAME, FILESTORE_UUID);
            InterfaceFileEntity file1 = createSourceInterfaceFile();
            InterfaceFileEntity file2 = createSourceInterfaceFile();
            file1.setStatus(Status.FAILED_SUPERSEDED);
            interfaceFileEntityTestData.saveAndFlushInterfaceFile(file1);

            List<Long> ids = List.of(file2.getInterfaceFileId());
            DeleteInterfaceFilesResultTestSupport expectedResponse = DeleteInterfaceFilesResultTestSupport.builder()
                .successfullyDeletedIds(ids)
                .failedIds(Collections.emptyList())
                .build();

            // Act + Assert
            setupApiTest(HttpMethod.DELETE, URI)
                .param("ids", buildParams(ids))
                .clearPermissions()
                .execute()
                .assertSuccess(HttpStatus.OK)
                .assertBody(expectedResponse);

            assertThat(repository.existsById(file1.getInterfaceFileId())).isFalse();
            assertThat(repository.existsById(file2.getInterfaceFileId())).isFalse();
            assertThat(utilBlobStoreService.getBlobExists(CONTAINER_NAME, FILESTORE_UUID)).isFalse();
        }

        @Disabled("This test passes but runs very slowly due to the retries on connecting to azure")
        @Test
        @DisplayName("PO-11123 - Delete interface files - records failure if error occurs deleting blob")
        @JiraStory("PO-11123")
        @JiraEpic("PO-3497")
        void recordsFailureIfAzureContainerNotAvailable() throws Exception {
            // Arrange
            TestContainerConfig.AZURITE_CONTAINER.stop();
            InterfaceFileEntity interfaceFile = createSourceInterfaceFile();

            List<Long> ids = List.of(interfaceFile.getInterfaceFileId());
            DeleteInterfaceFilesResultTestSupport expectedResponse = DeleteInterfaceFilesResultTestSupport.builder()
                .successfullyDeletedIds(Collections.emptyList())
                .failedIds(ids)
                .build();

            // Act + Assert
            setupApiTest(HttpMethod.DELETE, URI)
                .param("ids", buildParams(ids))
                .clearPermissions()
                .execute()
                .assertSuccess(HttpStatus.OK)
                .assertBody(expectedResponse);

            assertThat(repository.existsById(interfaceFile.getInterfaceFileId())).isTrue();
            TestContainerConfig.AZURITE_CONTAINER.start();
        }

        @Test
        @DisplayName("PO-11123 - Delete interface files - returns 400 if IDs provided are not SOURCE files")
        @JiraStory("PO-11123")
        @JiraEpic("PO-3497")
        void returnsBadRequestIfIdsAreNotForSourceFiles_400() throws Exception {
            // Arrange
            InterfaceFileEntity file = interfaceFileEntityTestData.getTypicalInterfaceFile(FILE_NAME, FILESTORE_UUID,
                "0123456789");
            List<Long> ids = List.of(file.getInterfaceFileId());
            utilBlobStoreService.createContainerIfNotExists(CONTAINER_NAME);
            utilBlobStoreService.storeBlob(new byte[] { 0, 1, 2, 3}, CONTAINER_NAME, FILESTORE_UUID);

            // Act + Assert
            setupApiTest(HttpMethod.DELETE, URI)
                .param("ids", buildParams(ids))
                .clearPermissions()
                .execute()
                .assertStatus(HttpStatus.BAD_REQUEST);

            assertThat(repository.existsById(file.getInterfaceFileId())).isTrue();
            assertThat(utilBlobStoreService.getBlobExists(CONTAINER_NAME, FILESTORE_UUID)).isTrue();
        }
    }

    private InterfaceFileEntity createSourceInterfaceFile() {
        InterfaceFileEntity file = interfaceFileEntityTestData.getTypicalInterfaceFile(FILE_NAME, FILESTORE_UUID,
            "0123456789");
        file.setType(Type.SOURCE);
        interfaceFileEntityTestData.saveAndFlushInterfaceFile(file);
        return file;
    }

    private static String buildParams(Collection<Long> ids) {
        return String.join(",", ids.stream().map(id -> id.toString()).toList());
    }



    @TestPropertySource(properties = {
        "opal.testing-support-endpoints.enabled=false"
    })
    @Nested
    class FeatureOff {
        @Test
        @DisplayName("PO-6454 - Feature flag off test")
        @JiraStory("PO-11123")
        @JiraEpic("PO-3497")
        void returns404WhenFeatureIsOff() throws Exception {
            setupApiTest(HttpMethod.DELETE, URI)
                .param("ids", buildParams(List.of(1L)))
                .execute()
                .assertNotFound("The requested resource could not be found");
        }
    }
}
