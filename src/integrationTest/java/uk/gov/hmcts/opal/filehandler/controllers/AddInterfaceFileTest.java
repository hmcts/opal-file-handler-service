package uk.gov.hmcts.opal.filehandler.controllers;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.opal.common.dto.ToJsonString.toJsonString;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.opal.filehandler.IntegrationSecurityConfiguration;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.queue.FinesInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.support.AbstractControllerIntegrationTest;
import uk.gov.hmcts.opal.filehandler.support.UtilBlobStoreService;
import uk.gov.hmcts.opal.filehandler.testdata.BusinessUnitBankAccountEntityTestData;
import uk.gov.hmcts.opal.generated.model.AddInterfaceFileRequestMetadata;
import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileTypeEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.PaymentTypeEnumTypes;
import uk.gov.hmcts.opal.generated.model.StatusEnumInterfaceFile;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraDefect;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraEpic;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraStory;

@Slf4j
@DisplayName("Interface Files Controller Integration Tests - Add Interface File")
@ActiveProfiles(profiles = {"integration"})
@Import(IntegrationSecurityConfiguration.class)
public class AddInterfaceFileTest extends AbstractControllerIntegrationTest {

    private static final String URI = "/interface-files";

    private static UtilBlobStoreService utilBlobStoreService;
    @Autowired
    private InterfaceFilesRepository repository;

    @Autowired
    private BusinessUnitBankAccountEntityTestData buBankAccountTestData;

    @MockitoBean
    private FinesInterfaceFilePreprocessQueueService finesQueueService;

    private static String BASE_RESOURCE_PATH = "azure/data/";
    private static final String bteckohResourcePath = BASE_RESOURCE_PATH +
        "/bteckoh-report/2498-MCPLDB-MOJ-Payments-Report-Daily-2026-07-06-06-00-18.xlsx";
    private static final String jacobsResourcePath = BASE_RESOURCE_PATH +
        "/jacobs/0000031712_dat_0000098475_20260408_103500.txt";

    private static byte[] bteckohFileContents;
    private static byte[] jacobsFileContents;
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

    private AddInterfaceFileRequestMetadata buildMetaData(InterfaceFileEnumInterfaceFile source, Long relatedId,
        boolean shouldPreProcessFile) {
        return AddInterfaceFileRequestMetadata.builder()
            .fileName("file-name.dat")
            .businessUnitCode("010")
            .paymentType(PaymentTypeEnumTypes.CASH)
            .type(InterfaceFileTypeEnumInterfaceFile.SOURCE)
            .domain(DomainEnumTypes.FINES)
            .shouldPreProcessFile(shouldPreProcessFile)
            .source(source)
            .target(InterfaceFileEnumInterfaceFile.OPAL)
            .relatedInterfaceFileId(relatedId)
            .build();
    }

    private AddInterfaceFileRequestMetadata buildMetaData(InterfaceFileEnumInterfaceFile source, Long relatedId) {
        return buildMetaData(source, relatedId, false);
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
            utilBlobStoreService.createContainerIfNotExists("jacobs");

            URL bteckohUrl = Resources.getResource(bteckohResourcePath);
            URL jacobsUrl = Resources.getResource(jacobsResourcePath);
            bteckohFileContents = Resources.toByteArray(bteckohUrl);
            jacobsFileContents = Resources.toByteArray(jacobsUrl);
        }

        @BeforeEach
        void beforeEach() {
            buBankAccountTestData.clear();
            buBankAccountTestData.saveTypicalBusinessUnitBankAccount(1L, "JA01", "0000031714");
        }

        @Test
        @JiraStory("PO-6453")
        @JiraEpic("PO-3497")
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
                    "file", "some-file-name", "application/json", bteckohFileContents)
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
        @JiraStory("PO-8745")
        @JiraEpic("PO-3952")
        @DisplayName("Correctly processes file after ingestion when type=SOURCE and shouldPreProcessFile=true")
        void addNewInterfaceFileAndPreProcess() {
            String originalFileName = "jacobs-file-name";
            String metadata = toJsonString(buildMetaData(InterfaceFileEnumInterfaceFile.JACOBS, null, true));

            InterfaceFileObjectInterfaceFile response =  setupFileUploadApiTest(HttpMethod.POST, URI)
                .includeMultipartBody(
                    "file", originalFileName, "application/json", jacobsFileContents)
                .includeMultipartBody(
                    "metadata", "metadata.json", "application/json", metadata.getBytes())
                .includeContentDigest()
                .clearPermissions()
                .addPermission((short) 1, FileHandlerPermission.VIEW_INTERFACE_FILES)
                .execute()
                .assertSuccess(HttpStatus.CREATED)
                .getResponseBodyAsObject(InterfaceFileObjectInterfaceFile.class);


            assertExistsInDatabase(response.getInterfaceFileId());
            Optional<InterfaceFileEntity> sourceJsonFile = repository
                .findByRelatedInterfaceFileInterfaceFileIdAndTypeAndFileName(
                    response.getInterfaceFileId(), Type.SOURCE_JSON, originalFileName, Status.SUCCESS);
            assertThat(sourceJsonFile.isPresent()).isTrue();
            assertAddedToBlobStorage("jacobs", response.getFilestoreUuid().toString());
            assertAddedToBlobStorage("jacobs", sourceJsonFile.get().getFilestoreUuid().toString());
            verify(finesQueueService, times(1)).send(sourceJsonFile.get().getInterfaceFileId());
        }

        @Test
        @JiraStory("PO-6453")
        @JiraEpic("PO-3497")
        @DisplayName("Fails when user has no permissions")
        void failsWhenUserHasNoPermission() {
            String metadata = toJsonString(buildMetaData(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, null));

            setupFileUploadApiTest(HttpMethod.POST, URI)
                .includeMultipartBody(
                    "file", "some-file-name", "application/json", bteckohFileContents)
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
        @JiraEpic("PO-3497")
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
                .includeMultipartBody("file", "some-other-file-name", "application/json", bteckohFileContents)
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
        @JiraEpic("PO-3497")
        @DisplayName("Correctly adds a duplicate file")
        void rejectsDuplicateCorrectly() {
            String metadata = toJsonString(buildMetaData(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, null));

            InterfaceFileEntity originalEntity = buildEntity(
                "some-file-name", "d553f8f289bd08e5c513de5c000c0374", Status.SUCCESS, null, null);
            insertInterfaceFileEntity(originalEntity);
            utilBlobStoreService.storeBlob(bteckohFileContents, "bteckoh-report", uuid.toString());
            final String originalVersion = utilBlobStoreService.getBlobVersion(
                "bteckoh-report", originalEntity.getFilestoreUuid().toString());

            InterfaceFileObjectInterfaceFile expectedResponse = buildExpectedResponse(
                buildEntity("some-file-name", "d553f8f289bd08e5c513de5c000c0374", Status.DUPLICATE,
                    "{\"message\":\"File with name 'some-file-name' and checksum 'd553f8f289bd08e5c513de5c000c0374'"
                        + " for source 'BTECKOH_REPORT' is a duplicate of " + originalEntity.getInterfaceFileId()
                        + "\"}", null)
            );

            InterfaceFileObjectInterfaceFile response =  setupFileUploadApiTest(HttpMethod.POST, URI)
                .includeMultipartBody("file", "some-file-name", "application/json", bteckohFileContents)
                .includeMultipartBody("metadata", "metadata.json", "application/json", metadata.getBytes())
                .includeContentDigest()
                .clearPermissions()
                .addPermission((short) 1, FileHandlerPermission.VIEW_INTERFACE_FILES)
                .execute()
                .assertSuccess(HttpStatus.CREATED)
                .getResponseBodyAsObject(InterfaceFileObjectInterfaceFile.class);


            assertResponse(expectedResponse, response);
            assertDuplicateExistsInDatabase("some-file-name", "d553f8f289bd08e5c513de5c000c0374");
            assertEquals(uuid, response.getFilestoreUuid());
            assertAddedToBlobStorage("bteckoh-report", uuid.toString());
            assertEquals(
                originalVersion,
                utilBlobStoreService.getBlobVersion("bteckoh-report", originalEntity.getFilestoreUuid().toString())
            );
        }

        @Test
        @JiraStory("PO-6453")
        @JiraEpic("PO-3497")
        @DisplayName("AC4 - Repeating an INGESTED file creates a new record and blob")
        void ingestedFileIsNotDuplicate() {
            InterfaceFileEntity original = buildEntity("some-file-name",
                "d553f8f289bd08e5c513de5c000c0374", Status.INGESTED, null, null);
            insertInterfaceFileEntity(original);
            utilBlobStoreService.storeBlob(bteckohFileContents, "bteckoh-report", uuid.toString());

            InterfaceFileObjectInterfaceFile response = uploadContent(bteckohFileContents,
                buildMetaData(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, null));

            assertEquals(StatusEnumInterfaceFile.INGESTED, response.getStatus());
            assertNotEquals(original.getInterfaceFileId(), response.getInterfaceFileId());
            assertNotEquals(uuid, response.getFilestoreUuid());
            assertEquals(2, repository.count());
            assertEquals(Status.INGESTED, repository.findById(original.getInterfaceFileId()).orElseThrow().getStatus());
            assertAddedToBlobStorage("bteckoh-report", response.getFilestoreUuid().toString());
            assertAddedToBlobStorage("bteckoh-report", uuid.toString());
        }

        @Test
        @JiraStory("PO-6453")
        @JiraEpic("PO-3497")
        @DisplayName("AC2 - Invalid BTEckoh JSON is recorded as FAILED without a blob reference")
        void invalidContentIsRecordedAsFailed() {
            InterfaceFileObjectInterfaceFile response = uploadContent(
                "{}".getBytes(StandardCharsets.UTF_8),
                buildMetaData(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, null));

            assertEquals(StatusEnumInterfaceFile.FAILED, response.getStatus());
            assertNull(response.getFilestoreUuid());
            assertTrue(response.getErrors() != null
                && response.getErrors().contains("BTEckoh report was not a valid XLSX workbook"));
            assertEquals(1, repository.count());
            InterfaceFileEntity stored = repository.findById(response.getInterfaceFileId()).orElseThrow();
            assertEquals(Status.FAILED, stored.getStatus());
            assertNull(stored.getFilestoreUuid());
        }

        @Test
        @JiraStory("PO-6453")
        @JiraEpic("PO-3497")
        @DisplayName("AC2 - Different metadata and multipart filenames are accepted")
        void differentMetadataFilenameIsAccepted() {
            AddInterfaceFileRequestMetadata metadata = buildMetaData(
                InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, null);
            metadata.setFileName("different-metadata-name.xlsx");
            InterfaceFileObjectInterfaceFile response = uploadContent(bteckohFileContents, metadata);

            assertEquals(StatusEnumInterfaceFile.INGESTED, response.getStatus());
            assertEquals("some-file-name", response.getFileName());
            assertEquals(response.getFileName(), repository.findById(response.getInterfaceFileId())
                .orElseThrow().getFileName());
            assertAddedToBlobStorage("bteckoh-report", response.getFilestoreUuid().toString());
        }

        @Test
        @JiraStory("PO-6453")
        @JiraEpic("PO-3497")
        @DisplayName("AC2 - Reject a multipart upload without the required file")
        @JiraDefect("PO-10908")
        @Disabled("PO-10908: missing multipart parts return 500; re-enable when fixed to return 400")
        void rejectsMissingFilePart() {
            String metadata = toJsonString(buildMetaData(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, null));
            setupFileUploadApiTest(HttpMethod.POST, URI)
                .includeMultipartBody("metadata", "metadata.json", "application/json",
                    metadata.getBytes(StandardCharsets.UTF_8))
                .includeContentDigest()
                .clearPermissions()
                .addPermission((short) 1, FileHandlerPermission.VIEW_INTERFACE_FILES)
                .execute()
                .assertStatus(HttpStatus.BAD_REQUEST);
            assertDatabaseUnchanged();
        }

        @Test
        @JiraStory("PO-6453")
        @JiraEpic("PO-3497")
        @DisplayName("AC2 - Reject a multipart upload without the required metadata")
        @JiraDefect("PO-10908")
        @Disabled("PO-10908: missing multipart parts return 500; re-enable when fixed to return 400")
        void rejectsMissingMetadataPart() {
            setupFileUploadApiTest(HttpMethod.POST, URI)
                .includeMultipartBody("file", "some-file-name", "application/octet-stream", bteckohFileContents)
                .includeContentDigest()
                .clearPermissions()
                .addPermission((short) 1, FileHandlerPermission.VIEW_INTERFACE_FILES)
                .execute()
                .assertStatus(HttpStatus.BAD_REQUEST);
            assertDatabaseUnchanged();
        }

        private InterfaceFileObjectInterfaceFile uploadContent(
            byte[] content, AddInterfaceFileRequestMetadata metadata) {
            return setupFileUploadApiTest(HttpMethod.POST, URI)
                .includeMultipartBody("file", "some-file-name", "application/octet-stream", content)
                .includeMultipartBody("metadata", "metadata.json", "application/json",
                    toJsonString(metadata).getBytes(StandardCharsets.UTF_8))
                .includeContentDigest()
                .clearPermissions()
                .addPermission((short) 1, FileHandlerPermission.VIEW_INTERFACE_FILES)
                .execute()
                .assertSuccess(HttpStatus.CREATED)
                .getResponseBodyAsObject(InterfaceFileObjectInterfaceFile.class);
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
        @JiraEpic("PO-3497")
        @DisplayName("API should return 404 when feature flag is off")
        void addInterfaceFile_shouldReturn404_whenFeatureFlagIsOff() throws Exception {
            String metadata = toJsonString(buildMetaData(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, null));
            setupFileUploadApiTest(HttpMethod.POST, URI)
                .includeMultipartBody("file", "some-file-name", "application/json", bteckohFileContents)
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
            bteckohFileContents = Resources.toByteArray(url);
        }

        @Test
        @JiraStory("PO-6453")
        @JiraEpic("PO-3497")
        @DisplayName("Correctly handles a blob store upload failure")
        void blobstoreUploadfailure() {
            String metadata = toJsonString(buildMetaData(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, null));
            String problemDetail = "Blob upload failed for file 'some-file-name': Status code 404, "
                + "\"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<Error>\n  <Code>ContainerNotFound"
                + "</Code>\n  <Message>The specified container does not exist.\n";

            utilBlobStoreService = new UtilBlobStoreService();
            utilBlobStoreService.deleteContainer("bteckoh-report");

            setupFileUploadApiTest(HttpMethod.POST, URI)
                .includeMultipartBody("file", "some-file-name", "application/json", bteckohFileContents)
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
