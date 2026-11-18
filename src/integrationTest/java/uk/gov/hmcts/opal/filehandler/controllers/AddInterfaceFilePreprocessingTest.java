package uk.gov.hmcts.opal.filehandler.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobServiceClient;
import com.google.common.io.Resources;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.util.DigestUtils;
import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;
import uk.gov.hmcts.opal.filehandler.IntegrationSecurityConfiguration;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.config.FileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.BusinessUnitBankAccountEntity;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.exception.BlobUploadException;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.ProcessingFailurePolicy;
import uk.gov.hmcts.opal.filehandler.service.blobstore.InterfaceFileBlobStoreService;
import uk.gov.hmcts.opal.filehandler.service.extraction.VariantBacsStandard18BaisExtractionService;
import uk.gov.hmcts.opal.filehandler.service.queue.FinesInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.service.queue.MaintenanceInterfaceFilePreprocessQueueService;
import uk.gov.hmcts.opal.filehandler.support.AbstractControllerIntegrationTest;
import uk.gov.hmcts.opal.filehandler.support.ApiTest;
import uk.gov.hmcts.opal.filehandler.testdata.BusinessUnitBankAccountEntityTestData;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;
import uk.gov.hmcts.opal.generated.model.AddInterfaceFileRequestMetadata;
import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileTypeEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.PaymentTypeEnumTypes;
import uk.gov.hmcts.opal.generated.model.StatusEnumInterfaceFile;

@Import(IntegrationSecurityConfiguration.class)
@TestPropertySource(properties = {
    "launchdarkly.enabled=false",
    "launchdarkly.default-flag-values.release-1c-banking-interfaces=true"
})
@DisplayName("Add interface file preprocessing")
class AddInterfaceFilePreprocessingTest extends AbstractControllerIntegrationTest {

    private static final short SUPPLIED_BUSINESS_UNIT_ID = 99;

    @Autowired
    private InterfaceFilesRepository repository;

    @Autowired
    private BusinessUnitBankAccountEntityTestData businessUnitTestData;

    @Autowired
    private Map<String, FileProcessorConfiguration> configs;

    @Autowired
    private BlobServiceClient blobServiceClient;

    @Autowired
    private InterfaceFileBlobStoreService blobStoreService;

    @MockitoBean
    private FinesInterfaceFilePreprocessQueueService finesQueue;

    @MockitoBean
    private MaintenanceInterfaceFilePreprocessQueueService maintenanceQueue;

    @MockitoBean
    private BaisSftpClient sftpClient;

    @BeforeEach
    void setUpBankAccounts() {
        businessUnitTestData.clear();
        businessUnitTestData.saveAndFlushBusinessUnitBankAccount(BusinessUnitBankAccountEntity.builder()
            .id((short) 123)
            .businessUnitCode("VB01")
            .domain(Domain.FINES)
            .bankSortCode("560033")
            .bankAccountNumber("27048527")
            .dwpCourtCode("VARIANT123")
            .build());
        businessUnitTestData.saveAndFlushBusinessUnitBankAccount(BusinessUnitBankAccountEntity.builder()
            .id((short) 1)
            .businessUnitCode("DW01")
            .domain(Domain.MAINTENANCE)
            .bankSortCode("010101")
            .bankAccountNumber("12341234")
            .dwpCourtCode("DWP1234567")
            .build());
        for (Interface source : List.of(Interface.CAPS_REPORT, Interface.DWP, Interface.VARIANT_BANKING)) {
            blobServiceClient.createBlobContainerIfNotExists(container(source));
        }
    }

    @AfterEach
    void clearBankAccounts() {
        repository.deleteAll();
        businessUnitTestData.clear();
    }

    @Nested
    @DisplayName("CAPS")
    class Caps {

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        @NullSource
        @DisplayName("Preprocessing should have no effect on CAPS report files, as they have no extraction service")
        void createsOnlySource(Boolean preProcess) throws IOException {
            InterfaceFileObjectInterfaceFile response = upload(Interface.CAPS_REPORT, preProcess);
            InterfaceFileEntity source = assertSource(response, Interface.CAPS_REPORT, Status.SUCCESS);

            assertThat(repository.findAll()).hasSize(1);
            assertThat(source.getType()).isEqualTo(Type.SOURCE);
            assertThat(source.getBusinessUnitId()).containsExactly(SUPPLIED_BUSINESS_UNIT_ID);
            verifyNoInteractions(finesQueue, maintenanceQueue);
        }
    }

    @Nested
    @DisplayName("DWP")
    class Dwp {

        @Test
        @DisplayName("Derives business units from DWP content for both SOURCE and queued SOURCE_JSON")
        void createsSourceJsonAndQueuesIt() throws IOException {
            assertBankingPreprocessing(Interface.DWP, Domain.MAINTENANCE, (short) 1);
        }
    }

    @Nested
    @DisplayName("Variant banking")
    class VariantBanking {

        @Test
        @DisplayName("Derives business units from the variant filename for both SOURCE and queued SOURCE_JSON")
        void createsSourceJsonAndQueuesIt() throws IOException {
            assertBankingPreprocessing(Interface.VARIANT_BANKING, Domain.FINES, (short) 123);
        }
    }

    @ParameterizedTest
    @EnumSource(value = Interface.class, names = {"CAPS_REPORT", "DWP", "VARIANT_BANKING"})
    @DisplayName("Preprocessing false creates only the source blob and preserves supplied business units")
    void skipsPreprocessingWhenFalse(Interface sourceType) throws IOException {
        final Map<String, Set<String>> blobsBefore = blobNamesByContainer();
        InterfaceFileObjectInterfaceFile response = upload(sourceType, false);
        InterfaceFileEntity source = assertSource(response, sourceType, Status.SUCCESS);

        assertThat(repository.findAll()).extracting(InterfaceFileEntity::getType).containsExactly(Type.SOURCE);
        assertThat(source.getBusinessUnitId()).containsExactly(SUPPLIED_BUSINESS_UNIT_ID);
        assertThat(source.getOpalDomain()).isEqualTo(Domain.FINES);
        Map<String, Set<String>> blobsAfter = blobNamesByContainer();

        blobsBefore.forEach((name, expectedBlobs) -> {
            if (name.equals(container(sourceType))) {
                expectedBlobs.add(source.getFilestoreUuid().toString());
            }
            assertThat(blobsAfter.get(name)).as("Blobs in container %s", name)
                .containsExactlyInAnyOrderElementsOf(expectedBlobs);
        });
        verifyNoInteractions(finesQueue, maintenanceQueue, sftpClient);
    }

    @Test
    @DisplayName("Duplicates wont reprocess when reprocessing")
    void duplicatesWontReprocessWhenPreprocessing() throws IOException {
        assertBankingPreprocessing(Interface.VARIANT_BANKING, Domain.FINES, (short) 123);

        Map<String, Set<String>> blobsBefore = blobNamesByContainer();
        InterfaceFileObjectInterfaceFile response = upload(Interface.VARIANT_BANKING, true);
        assertThat(response.getStatus()).isEqualTo(StatusEnumInterfaceFile.DUPLICATE);
        assertThat(blobNamesByContainer()).isEqualTo(blobsBefore);

        assertThat(repository.findAll())
            .extracting(InterfaceFileEntity::getType, InterfaceFileEntity::getStatus)
            .containsExactlyInAnyOrder(
                tuple(Type.SOURCE, Status.SUCCESS),
                tuple(Type.SOURCE_JSON, Status.SUCCESS),
                tuple(Type.SOURCE, Status.DUPLICATE));
        verifyNoMoreInteractions(finesQueue);
        verifyNoInteractions(maintenanceQueue, sftpClient);
    }

    @Nested
    @DisplayName("AC8 preprocessing rollback")
    class RollbackOnPreprocessingFailure {

        @MockitoSpyBean
        private InterfaceFileBlobStoreService rollbackBlobStore;

        @MockitoSpyBean
        private VariantBacsStandard18BaisExtractionService extractionService;

        @MockitoSpyBean
        private InterfaceFilesRepository rollbackRepository;

        private Map<String, Set<String>> originalBlobs;
        private UUID existingBlob;

        @BeforeEach
        void retainExistingFailure() throws IOException {
            originalBlobs = blobNamesByContainer();
            byte[] bytes = content(Interface.VARIANT_BANKING);
            existingBlob = UUID.randomUUID();
            blobServiceClient.getBlobContainerClient(container(Interface.VARIANT_BANKING))
                .getBlobClient(existingBlob.toString()).upload(BinaryData.fromBytes(bytes));
            repository.saveAndFlush(InterfaceFileEntity.builder()
                .source(Interface.VARIANT_BANKING).target(Interface.OPAL).type(Type.SOURCE)
                .opalDomain(Domain.FINES).fileName(filename(Interface.VARIANT_BANKING))
                .filestoreUuid(existingBlob).checksum(DigestUtils.md5DigestAsHex(bytes))
                .status(Status.FAILED).errors("{\"message\":\"previous failure\"}")
                .createdDatetime(LocalDateTime.of(2026, 1, 1, 0, 0)).createdBy(500000000L)
                .build());
        }

        @AfterEach
        void cleanOwnedBlobs() {
            blobNamesByContainer().forEach((name, blobs) -> {
                blobs.removeAll(originalBlobs.get(name));
                blobs.forEach(blob -> blobServiceClient.getBlobContainerClient(name)
                    .getBlobClient(blob).deleteIfExists());
            });
        }

        @Test
        @DisplayName("SOURCE upload failure leaves existing records and blobs unchanged")
        void rollsBackWhenSourceUploadFails() throws IOException {
            doAnswer(this::uploadThenFail).when(rollbackBlobStore)
                .uploadBaisFile(any(UUID.class), anyString(), any(InputStream.class), anyString(),
                    eq(ProcessingFailurePolicy.PROPAGATE));

            assertRollbackAfterUpload(HttpStatus.SERVICE_UNAVAILABLE);

            verifyNoInteractions(finesQueue, maintenanceQueue, sftpClient);
        }

        @Test
        @DisplayName("Extraction failure removes the new SOURCE blob and restores superseded records")
        void rollsBackWhenExtractionFails() throws IOException {
            doThrow(internalFailure("Extraction failed")).when(extractionService)
                .extractStandardData(any(InterfaceFileEntity.class), any(InputStream.class));

            assertRollbackAfterUpload(HttpStatus.INTERNAL_SERVER_ERROR);

            verify(extractionService).extractStandardData(any(InterfaceFileEntity.class), any(InputStream.class));
            verifyNoInteractions(finesQueue, maintenanceQueue, sftpClient);
        }

        @Test
        @DisplayName("SOURCE_JSON upload failure removes both new blobs and all new records")
        void rollsBackWhenSourceJsonUploadFails() throws IOException {
            doCallRealMethod().doAnswer(this::uploadThenFail).when(rollbackBlobStore)
                .uploadBaisFile(any(UUID.class), anyString(), any(InputStream.class), anyString(),
                    eq(ProcessingFailurePolicy.PROPAGATE));

            assertRollbackAfterUpload(HttpStatus.SERVICE_UNAVAILABLE);

            verify(rollbackBlobStore, times(2))
                .uploadBaisFile(any(UUID.class), anyString(), any(InputStream.class), anyString(),
                    eq(ProcessingFailurePolicy.PROPAGATE));
            verifyNoInteractions(finesQueue, maintenanceQueue, sftpClient);
        }

        @Test
        @DisplayName("Queue failure rolls back SOURCE and SOURCE_JSON while preserving existing data")
        void rollsBackWhenQueuePublicationFails() throws IOException {
            doThrow(internalFailure("Queue publication failed")).when(finesQueue).send(anyLong());

            assertRollbackAfterUpload(HttpStatus.INTERNAL_SERVER_ERROR);

            verify(finesQueue).send(anyLong());
            verifyNoMoreInteractions(finesQueue);
            verifyNoInteractions(maintenanceQueue, sftpClient);
        }

        @Test
        @DisplayName("Persistence failure before queue publication removes both new blobs")
        void rollsBackWhenPersistenceFails() throws IOException {
            doThrow(internalFailure("Persistence failed")).when(rollbackRepository).flush();

            assertRollbackAfterUpload(HttpStatus.INTERNAL_SERVER_ERROR);

            verifyNoInteractions(finesQueue, maintenanceQueue, sftpClient);
        }

        private InternalServerErrorException internalFailure(String detail) {
            return new InternalServerErrorException("Internal Server Error", detail, new IllegalStateException(detail));
        }

        private Void uploadThenFail(InvocationOnMock invocation) throws Throwable {
            // The blob exists even though the caller receives a failed upload result.
            invocation.callRealMethod();
            throw new BlobUploadException(invocation.getArgument(0), invocation.getArgument(1),
                new IllegalStateException("AC8 upload failed after writing the blob"));
        }

        private List<Tuple> records() {
            return repository.findAll().stream()
                .map(entity -> tuple(entity.getInterfaceFileId(), entity.getStatus(), entity.getType(),
                    entity.getFilestoreUuid(), entity.getChecksum(), entity.getErrors(), entity.getCreatedDatetime()))
                .toList();
        }

        private void assertRollbackAfterUpload(HttpStatus expectedStatus) throws IOException {
            List<Tuple> recordsBefore = records();
            Map<String, Set<String>> blobsBefore = blobNamesByContainer();
            String name = filename(Interface.VARIANT_BANKING);
            AddInterfaceFileRequestMetadata metadata = AddInterfaceFileRequestMetadata.builder()
                .source(InterfaceFileEnumInterfaceFile.VARIANT_BANKING).target(InterfaceFileEnumInterfaceFile.OPAL)
                .type(InterfaceFileTypeEnumInterfaceFile.SOURCE).domain(DomainEnumTypes.FINES)
                .paymentType(PaymentTypeEnumTypes.CASH).fileName(name).shouldPreProcessFile(true).build();

            ApiTest.Response response = setupFileUploadApiTest(HttpMethod.POST, "/interface-files")
                .includeMultipartBody("file", name, "application/octet-stream", content(Interface.VARIANT_BANKING))
                .includeMultipartBody("metadata", "metadata.json", "application/json",
                    objectMapper.writeValueAsString(metadata).getBytes(StandardCharsets.UTF_8))
                .includeContentDigest().clearPermissions()
                .addPermission((short) 2, FileHandlerPermission.CREATE_INTERFACE_FILES).execute();

            assertAll("AC8 rollback before teardown",
                () -> response.assertError(expectedStatus),
                () -> assertThat(records()).containsExactlyInAnyOrderElementsOf(recordsBefore),
                () -> assertThat(blobNamesByContainer()).isEqualTo(blobsBefore),
                () -> assertThat(blobServiceClient.getBlobContainerClient(container(Interface.VARIANT_BANKING))
                    .getBlobClient(existingBlob.toString()).downloadContent().toBytes())
                    .containsExactly(content(Interface.VARIANT_BANKING)));
        }
    }

    private void assertBankingPreprocessing(Interface sourceType, Domain domain, short businessUnitId)
        throws IOException {
        InterfaceFileObjectInterfaceFile response = upload(sourceType, true);
        InterfaceFileEntity source = assertSource(response, sourceType, Status.SUCCESS);
        assertThat(source.getBusinessUnitId()).containsExactly(businessUnitId);
        assertThat(source.getOpalDomain()).isEqualTo(domain);

        List<InterfaceFileEntity> children = repository.findAll().stream()
            .filter(entity -> entity.getType() == Type.SOURCE_JSON).toList();
        assertThat(repository.findAll()).hasSize(2);
        assertThat(children).hasSize(1);
        InterfaceFileEntity child = children.getFirst();
        assertThat(child.getRelatedInterfaceFile().getInterfaceFileId()).isEqualTo(source.getInterfaceFileId());
        assertThat(child.getSource()).isEqualTo(sourceType);
        assertThat(child.getTarget()).isEqualTo(Interface.OPAL);
        assertThat(child.getStatus()).isEqualTo(Status.SUCCESS);
        assertThat(child.getOpalDomain()).isEqualTo(domain);
        assertThat(child.getBusinessUnitId()).containsExactly(businessUnitId);
        assertThat(child.getCreatedBy()).isEqualTo(source.getCreatedBy());
        assertThat(child.getErrors()).isNull();

        byte[] json = blobStoreService.fetchInterfaceFile(child.getInterfaceFileId(), child.getFilestoreUuid(),
            container(sourceType)).toBytes();
        assertThat(DigestUtils.md5DigestAsHex(json)).isEqualTo(child.getChecksum());
        assertThat(objectMapper.readTree(json).get("transactions").size()).isGreaterThan(0);

        if (sourceType == Interface.DWP) {
            verify(maintenanceQueue).send(child.getInterfaceFileId());
            verifyNoMoreInteractions(maintenanceQueue);
            verifyNoInteractions(finesQueue);
        } else {
            verify(finesQueue).send(child.getInterfaceFileId());
            verifyNoMoreInteractions(finesQueue);
            verifyNoInteractions(maintenanceQueue);
            verifyNoInteractions(sftpClient);
        }
    }

    private InterfaceFileEntity assertSource(InterfaceFileObjectInterfaceFile response, Interface sourceType,
        Status status) throws IOException {
        assertThat(response.getStatus()).isEqualTo(StatusEnumInterfaceFile.valueOf(status.name()));
        InterfaceFileEntity source = repository.findById(response.getInterfaceFileId()).orElseThrow();
        assertThat(source.getType()).isEqualTo(Type.SOURCE);
        assertThat(source.getSource()).isEqualTo(sourceType);
        assertThat(source.getStatus()).isEqualTo(status);
        assertThat(source.getErrors()).isNull();
        assertThat(source.getCreatedBy()).isEqualTo(500000000L);
        byte[] stored = blobStoreService.fetchInterfaceFile(source.getInterfaceFileId(), source.getFilestoreUuid(),
            container(sourceType)).toBytes();
        assertThat(stored).containsExactly(content(sourceType));
        assertThat(source.getChecksum()).isEqualTo(DigestUtils.md5DigestAsHex(stored));
        return source;
    }

    private InterfaceFileObjectInterfaceFile upload(Interface source, Boolean preprocess) throws IOException {
        String name = filename(source);
        AddInterfaceFileRequestMetadata metadata = AddInterfaceFileRequestMetadata.builder()
            .source(InterfaceFileEnumInterfaceFile.valueOf(source.name()))
            .target(InterfaceFileEnumInterfaceFile.OPAL)
            .type(InterfaceFileTypeEnumInterfaceFile.SOURCE)
            .domain(DomainEnumTypes.FINES)
            .paymentType(PaymentTypeEnumTypes.CASH)
            .businessUnitId(Boolean.TRUE.equals(preprocess)
                && source != Interface.CAPS_REPORT ? null : SUPPLIED_BUSINESS_UNIT_ID)
            .fileName(name)
            .shouldPreProcessFile(preprocess)
            .build();

        return setupFileUploadApiTest(HttpMethod.POST, "/interface-files")
            .includeMultipartBody("file", name, "application/octet-stream", content(source))
            .includeMultipartBody("metadata", "metadata.json", "application/json",
                objectMapper.writeValueAsString(metadata).getBytes(StandardCharsets.UTF_8))
            .includeContentDigest()
            .clearPermissions()
            .addPermission((short) 2, FileHandlerPermission.CREATE_INTERFACE_FILES)
            .execute()
            .assertSuccess(HttpStatus.CREATED)
            .getResponseBodyAsObject(InterfaceFileObjectInterfaceFile.class);
    }

    private String container(Interface source) {
        return configs.get(source.getConfigComponentName()).getContainerName();
    }

    private Map<String, Set<String>> blobNamesByContainer() {
        return List.of(Interface.CAPS_REPORT, Interface.DWP, Interface.VARIANT_BANKING).stream()
            .map(this::container)
            .distinct()
            .collect(Collectors.toMap(name -> name, name -> {
                Set<String> names = new TreeSet<>();
                blobServiceClient.getBlobContainerClient(name).listBlobs()
                    .forEach(blob -> names.add(blob.getName()));
                return names;
            }));
    }

    private String filename(Interface source) {
        return switch (source) {
            case CAPS_REPORT -> "CapFa.GB.20260701.173024.xml";
            case DWP -> "0000015232_dat_0000000612_08011008_111355.txt";
            case VARIANT_BANKING -> "a121_003500_VB_123_13.dat";
            default -> throw new IllegalArgumentException("Unsupported test source " + source);
        };
    }

    private byte[] content(Interface source) throws IOException {
        String resource = source == Interface.VARIANT_BANKING ? "a121_00350005_300000.dat" : filename(source);
        return Resources.toByteArray(Resources.getResource("bais-emulator/" + resource));
    }
}
