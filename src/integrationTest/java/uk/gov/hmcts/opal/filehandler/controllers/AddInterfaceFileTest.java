package uk.gov.hmcts.opal.filehandler.controllers;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.opal.common.dto.ToJsonString.toJsonString;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.opal.filehandler.IntegrationSecurityConfiguration;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.support.AbstractControllerIntegrationTest;
import uk.gov.hmcts.opal.filehandler.support.UtilBlobStoreService;
import uk.gov.hmcts.opal.generated.model.AddInterfaceFileRequestMetadata;
import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileTypeEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.PaymentTypeEnumTypes;
import uk.gov.hmcts.opal.generated.model.StatusEnumInterfaceFile;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraEpic;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraStory;

@Slf4j
@DisplayName("Add Interface File")
@ActiveProfiles(profiles = {"integration"})
@Import(IntegrationSecurityConfiguration.class)
public class AddInterfaceFileTest extends AbstractControllerIntegrationTest {

    private static final String URI = "/interface-files";

    private static UtilBlobStoreService utilBlobStoreService;
    @Autowired
    private InterfaceFilesRepository repository;

    private static final String bteckohResourcePath = "azure/data/bteckoh-report/2498-MCPLDB-MOJ-Payments-Report-"
        + "Daily-2026-07-06-06-00-18.xlsx";

    private static byte[] fileContents;
    private static final UUID uuid = UUID.randomUUID();

    private InterfaceFileObjectInterfaceFile buildExpectedResponse(InterfaceFileEntity interfaceFileEntity) {
        return InterfaceFileObjectInterfaceFile.builder()
            .interfaceFileId(interfaceFileEntity.getInterfaceFileId())
            .source(InterfaceFileEnumInterfaceFile.valueOf(interfaceFileEntity.getSource().name()))
            .target(InterfaceFileEnumInterfaceFile.valueOf(interfaceFileEntity.getTarget().name()))
            .type(InterfaceFileTypeEnumInterfaceFile.valueOf(interfaceFileEntity.getType().name()))
            .domain(DomainEnumTypes.valueOf(interfaceFileEntity.getOpalDomain().name()))
            .fileName(interfaceFileEntity.getFileName())
            .filestoreUuid(interfaceFileEntity.getFilestoreUuid())
            .status(StatusEnumInterfaceFile.valueOf(interfaceFileEntity.getStatus().name()))
            .createdDatetime(interfaceFileEntity.getCreatedDatetime())
            .checksum(interfaceFileEntity.getChecksum())
            .errors(interfaceFileEntity.getErrors())
            .build();
    }

    private AddInterfaceFileRequestMetadata buildMetaData(InterfaceFileEnumInterfaceFile source, Long relatedId) {
        return AddInterfaceFileRequestMetadata.builder()
            .fileName("file-name.dat")
            .businessUnitCode("010")
            .paymentType(PaymentTypeEnumTypes.CASH)
            .type(InterfaceFileTypeEnumInterfaceFile.SOURCE)
            .domain(DomainEnumTypes.FINES)
            .shouldPreProcessFile(false)
            .source(source)
            .target(InterfaceFileEnumInterfaceFile.OPAL)
            .relatedInterfaceFileId(relatedId)
            .build();
    }

    private InterfaceFileEntity buildEntity(
        String filename, String checksum, Status status, String errors, Long relatedId) {
        InterfaceFileEntity.InterfaceFileEntityBuilder builder = InterfaceFileEntity.builder()
            .checksum(checksum)
            .fileName(filename)
            .filestoreUuid(uuid)
            .source(Interface.BTECKOH_REPORT)
            .type(Type.SOURCE)
            .target(Interface.OPAL)
            .opalDomain(Domain.FINES)
            .status(status)
            .errors(errors)
            .createdDatetime(LocalDateTime.now());
        if (relatedId != null) {
            builder.relatedInterfaceFile(InterfaceFileEntity.builder().interfaceFileId(relatedId).build());
        }
        return builder.build();
    }

    private void insertInterfaceFileEntity(InterfaceFileEntity entity) {
        repository.save(entity);
    }

    private void assertExistsInDatabase(Long id) {
        assertTrue(repository.findById(id).isPresent());
    }

    private void assertDatabaseUnchanged() {
        assertTrue(repository.findAll().isEmpty());
    }

    private void assertResponse(InterfaceFileObjectInterfaceFile expected, InterfaceFileObjectInterfaceFile response) {
        assertAll(
            () -> assertEquals(expected.getChecksum(), response.getChecksum()),
            () -> assertEquals(expected.getFileName(), response.getFileName()),
            () -> assertEquals(expected.getStatus(), response.getStatus()),
            () -> assertEquals(expected.getDomain(), response.getDomain()),
            () -> assertEquals(expected.getErrors(), response.getErrors())
        );
    }

    private void assertDuplicateExistsInDatabase(String filename, String checksum) {
        assertTrue(repository.findByFileNameAndChecksumAndStatus(filename, checksum, Status.DUPLICATE).isPresent());
    }

    private void assertAddedToBlobStorage(String container, String uuid) {
        assertTrue(utilBlobStoreService.getBlobExists(container, uuid));
    }

    @TestPropertySource(properties = {
        "launchdarkly.enabled=false",
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true"
    })
    @Nested
    @DisplayName("Feature Flag On")
    class FeatureOn {
        @BeforeAll
        static void setupData() throws IOException {
            utilBlobStoreService = new UtilBlobStoreService();
            utilBlobStoreService.createContainerIfNotExists("bteckoh-report");

            URL url = Resources.getResource(bteckohResourcePath);
            fileContents = Resources.toByteArray(url);
        }

        @Test
        @JiraStory("PO-6453")
        @JiraEpic("PO-3947")
        @DisplayName("Correctly adds a new interface file on the db and blob store")
        void addNewInterfaceFileToDBAndBlobStore() {
            InterfaceFileObjectInterfaceFile expectedResponse = buildExpectedResponse(
                buildEntity("some-file-name",
                    "d553f8f289bd08e5c513de5c000c0374",
                    Status.INGESTED,
                    null, null)
            );

            String metadata = toJsonString(buildMetaData(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, null));

            InterfaceFileObjectInterfaceFile response =  setupFileUploadApiTest(HttpMethod.POST, URI)
                .includeMultipartBody(
                    "file", "some-file-name", "application/json", fileContents)
                .includeMultipartBody(
                    "metadata", "metadata.json", "application/json", metadata.getBytes())
                .includeContentDigest()
                .clearPermissions()
                .addPermission((short) 1, FileHandlerPermission.VIEW_INTERFACE_FILES)
                .execute()
                .assertSuccess(HttpStatus.CREATED)
                .getResponseBodyAsObject(InterfaceFileObjectInterfaceFile.class);


            assertResponse(expectedResponse, response);
            assertExistsInDatabase(response.getInterfaceFileId());
            assertAddedToBlobStorage("bteckoh-report", response.getFilestoreUuid().toString());
        }

        @Test
        @JiraStory("PO-6453")
        @JiraEpic("PO-3947")
        @DisplayName("Fails when user has no permissions")
        void failsWhenUserHasNoPermission() {
            String metadata = toJsonString(buildMetaData(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, null));

            setupFileUploadApiTest(HttpMethod.POST, URI)
                .includeMultipartBody(
                    "file", "some-file-name", "application/json", fileContents)
                .includeMultipartBody(
                    "metadata", "metadata.json", "application/json", metadata.getBytes())
                .includeContentDigest()
                .clearPermissions()
                .execute()
                .assertForbidden();

            assertDatabaseUnchanged();
        }

        @Test
        @JiraStory("PO-6453")
        @JiraEpic("PO-3947")
        @DisplayName("Correctly sets related interface file")
        void relatedFileIsSetCorrectly() {
            InterfaceFileEntity originalEntity = buildEntity("some-file-name",
                "d553f8f289bd08e5c513de5c000c0374", Status.SUCCESS, null, null);
            insertInterfaceFileEntity(originalEntity);
            InterfaceFileObjectInterfaceFile expectedResponse = buildExpectedResponse(
                buildEntity("some-other-file-name",
                    "d553f8f289bd08e5c513de5c000c0374", Status.INGESTED,
                    null, originalEntity.getInterfaceFileId())
            );
            String metadata = toJsonString(buildMetaData(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT,
                originalEntity.getInterfaceFileId()));

            InterfaceFileObjectInterfaceFile response =  setupFileUploadApiTest(HttpMethod.POST, URI)
                .includeMultipartBody("file", "some-other-file-name", "application/json", fileContents)
                .includeMultipartBody("metadata", "metadata.json", "application/json", metadata.getBytes())
                .includeContentDigest()
                .clearPermissions()
                .addPermission((short) 1, FileHandlerPermission.VIEW_INTERFACE_FILES)
                .execute()
                .assertSuccess(HttpStatus.CREATED)
                .getResponseBodyAsObject(InterfaceFileObjectInterfaceFile.class);


            assertResponse(expectedResponse, response);
            assertExistsInDatabase(response.getInterfaceFileId());
        }

        @Test
        @JiraStory("PO-6453")
        @JiraEpic("PO-3947")
        @DisplayName("Correctly adds a duplicate file")
        void rejectsDuplicateCorrectly() {
            String metadata = toJsonString(buildMetaData(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, null));

            InterfaceFileEntity originalEntity = buildEntity(
                "some-file-name", "d553f8f289bd08e5c513de5c000c0374", Status.SUCCESS, null, null);
            insertInterfaceFileEntity(originalEntity);
            String originalVersion = utilBlobStoreService.getBlobVersion(
                "bteckoh-report", originalEntity.getFilestoreUuid().toString());

            InterfaceFileObjectInterfaceFile expectedResponse = buildExpectedResponse(
                buildEntity("some-file-name", "d553f8f289bd08e5c513de5c000c0374", Status.DUPLICATE,
                    "{\"message\":\"File with name 'some-file-name' and checksum 'd553f8f289bd08e5c513de5c000c0374'"
                        + " for source 'BTECKOH_REPORT' is a duplicate of " + originalEntity.getInterfaceFileId()
                        + "\"}", null)
            );

            InterfaceFileObjectInterfaceFile response =  setupFileUploadApiTest(HttpMethod.POST, URI)
                .includeMultipartBody("file", "some-file-name", "application/json", fileContents)
                .includeMultipartBody("metadata", "metadata.json", "application/json", metadata.getBytes())
                .includeContentDigest()
                .clearPermissions()
                .addPermission((short) 1, FileHandlerPermission.VIEW_INTERFACE_FILES)
                .execute()
                .assertSuccess(HttpStatus.CREATED)
                .getResponseBodyAsObject(InterfaceFileObjectInterfaceFile.class);


            assertResponse(expectedResponse, response);
            assertDuplicateExistsInDatabase("some-file-name", "d553f8f289bd08e5c513de5c000c0374");
            assertEquals(
                originalVersion,
                utilBlobStoreService.getBlobVersion("bteckoh-report", originalEntity.getFilestoreUuid().toString())
            );
        }

    }

    @TestPropertySource(properties = {
        "launchdarkly.enabled=false",
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false"
    })
    @Nested
    class FeatureOff {

        @Test
        @JiraStory("PO-6453")
        @JiraEpic("PO-3947")
        @DisplayName("API should return 404 when feature flag is off")
        void addInterfaceFile_shouldReturn404_whenFeatureFlagIsOff() throws Exception {
            String metadata = toJsonString(buildMetaData(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, null));
            setupFileUploadApiTest(HttpMethod.POST, URI)
                .includeMultipartBody("file", "some-file-name", "application/json", fileContents)
                .includeMultipartBody("metadata", "metadata.json", "application/json", metadata.getBytes())
                .addPermission((short) 1, FileHandlerPermission.VIEW_INTERFACE_FILES)
                .execute()
                .assertFeatureFlagDisabledResponse();
        }
    }

    @TestPropertySource(properties = {
        "launchdarkly.enabled=false",
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true",
    })
    @Nested
    class BlobStorageFailure {
        @BeforeAll
        static void setupData() throws IOException {
            URL url = Resources.getResource(bteckohResourcePath);
            fileContents = Resources.toByteArray(url);
        }

        @Test
        @JiraStory("PO-6453")
        @JiraEpic("PO-3947")
        @DisplayName("Correctly handles a blob store upload failure")
        void blobstoreUploadfailure() {
            String metadata = toJsonString(buildMetaData(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, null));
            String problemDetail = "Blob upload failed for file 'some-file-name': Status code 404, "
                + "\"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<Error>\n  <Code>ContainerNotFound"
                + "</Code>\n  <Message>The specified container does not exist.\n";

            utilBlobStoreService = new UtilBlobStoreService();
            utilBlobStoreService.deleteContainer("bteckoh-report");

            setupFileUploadApiTest(HttpMethod.POST, URI)
                .includeMultipartBody("file", "some-file-name", "application/json", fileContents)
                .includeMultipartBody("metadata", "metadata.json", "application/json", metadata.getBytes())
                .includeContentDigest()
                .clearPermissions()
                .addPermission((short) 1, FileHandlerPermission.VIEW_INTERFACE_FILES)
                .execute()
                .assertProblemDetails(HttpStatus.SERVICE_UNAVAILABLE, problemDetail, "Service Unavailable", false);

            assertDatabaseUnchanged();

        }
    }
}
