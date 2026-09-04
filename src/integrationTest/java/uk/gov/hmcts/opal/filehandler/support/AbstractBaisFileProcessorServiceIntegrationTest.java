package uk.gov.hmcts.opal.filehandler.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.DigestUtils;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.CapsReportBaisFileProcessorServiceIntegrationTest;
import uk.gov.hmcts.opal.filehandler.service.InterfaceFilesService;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;

@SpringBootTest(properties = {
    "spring.main.web-application-type=none",
    "launchdarkly.default-flag-values.release-1c-banking-interfaces=true"
})
@Slf4j
@Testcontainers
public class AbstractBaisFileProcessorServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Clock clock;

    @Autowired
    protected InterfaceFilesRepository repository;

    @Autowired
    protected BaisSftpClient sftpClient;

    @Autowired
    protected BlobServiceClient blobServiceClient;

    @Autowired
    private InterfaceFilesService interfaceFilesService;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) throws IOException {
        registry.add("opal.file-handler-service.file-store.connection-string",
            TestContainerConfig::azuriteConnectionString);

        ByteArrayOutputStream privateKeyStreamOut;

        try (InputStream privateKeyStream = CapsReportBaisFileProcessorServiceIntegrationTest.class.getClassLoader()
            .getResourceAsStream("bais-emulator/keys/bais-sftp-key")) {

            privateKeyStreamOut = new ByteArrayOutputStream();
            privateKeyStream.transferTo(privateKeyStreamOut);
        }

        String privateKey = privateKeyStreamOut.toString();

        registry.add("opal.file-handler-service.sftp.bais.private-key", () -> privateKey);
        registry.add("opal.file-handler-service.sftp.bais.host", TestContainerConfig.SFTP_CONTAINER::getHost);
        registry.add("opal.file-handler-service.sftp.bais.port",
            () -> TestContainerConfig.SFTP_CONTAINER.getMappedPort(22));
    }

    public final void uploadResourceToSftp(String resourcePath, String containerPath) {
        TestContainerConfig.SFTP_CONTAINER.copyFileToContainer(
            MountableFile.forClasspathResource(resourcePath), containerPath);
    }

    protected final Map<String, String> storedBlobs(String containerName) {
        Map<String, String> blobs = new TreeMap<>();
        blobServiceClient.getBlobContainerClient(containerName).listBlobs()
            .forEach(blob -> blobs.put(blob.getName(), blob.getProperties().getETag()));
        return blobs;
    }

    protected final void clearReportFiles(String username, String containerName) {
        sftpClient.listRegularFiles(username).forEach(file -> sftpClient.deleteFile(username, file));
        var container = blobServiceClient.createBlobContainerIfNotExists(containerName);
        container.listBlobs().forEach(blob -> container.getBlobClient(blob.getName()).delete());
    }

    public final void assertNumberOfSftpFiles(String username, int expected) {
        assertThat(sftpClient.listRegularFiles(username)).hasSize(expected);
    }

    public final void assertEntitiesWithStatus(String fileName, String checksum, Status status) {
        List<InterfaceFileEntity> entities = repository.findAllByFileNameAndChecksumAndStatus(
            fileName, checksum, status);

        assertThat(entities.size()).isGreaterThan(0);
    }

    public final void assertNumberOfEntitiesWithStatus(String fileName, String checksum, Status status,
        int numExpected) {
        List<InterfaceFileEntity> entities = repository.findAllByFileNameAndChecksumAndStatus(
            fileName, checksum, status);

        assertThat(entities).hasSize(numExpected);
    }

    public final InterfaceFileEntity assertSuccessfulInterfaceFile(String fileName, String checksum,
        Interface source, Type type, Domain domain) {

        List<InterfaceFileEntity> entities = repository.findAllByFileNameAndChecksumAndStatus(
            fileName, checksum, Status.SUCCESS);

        assertThat(entities)
            .singleElement()
            .satisfies(entity -> {
                assertThat(entity)
                    .extracting(
                        InterfaceFileEntity::getFileName,
                        InterfaceFileEntity::getChecksum,
                        InterfaceFileEntity::getSource,
                        InterfaceFileEntity::getTarget,
                        InterfaceFileEntity::getType,
                        InterfaceFileEntity::getOpalDomain,
                        InterfaceFileEntity::getStatus)
                    .containsExactly(fileName, checksum, source, Interface.OPAL, type, domain, Status.SUCCESS);
                assertThat(entity.getErrors()).isNull();
            });

        return entities.getFirst();
    }

    public final InterfaceFileEntity assertSuccessfulSourceJsonInterfaceFile(String fileName, Interface source,
        Domain domain, Long relatedInterfaceFileId) {

        List<InterfaceFileEntity> entities = repository.findAll().stream()
            .filter(entity -> entity.getType() == Type.SOURCE_JSON)
            .filter(entity -> fileName.equals(entity.getFileName()))
            .filter(entity -> entity.getRelatedInterfaceFile() != null)
            .filter(entity -> relatedInterfaceFileId.equals(
                entity.getRelatedInterfaceFile().getInterfaceFileId()))
            .toList();

        assertThat(entities)
            .singleElement()
            .satisfies(entity -> {
                assertThat(entity)
                    .extracting(
                        InterfaceFileEntity::getFileName,
                        InterfaceFileEntity::getSource,
                        InterfaceFileEntity::getTarget,
                        InterfaceFileEntity::getType,
                        InterfaceFileEntity::getOpalDomain,
                        InterfaceFileEntity::getStatus,
                        related -> related.getRelatedInterfaceFile().getInterfaceFileId())
                    .containsExactly(fileName, source, Interface.OPAL, Type.SOURCE_JSON, domain, Status.SUCCESS,
                        relatedInterfaceFileId);
                assertThat(entity.getChecksum()).isNotBlank();
                assertThat(entity.getFilestoreUuid()).isNotNull();
                assertThat(entity.getErrors()).isNull();
            });

        return entities.getFirst();
    }

    public final void assertBlobChecksum(String fileName, String fileChecksum, String containerName) {
        List<InterfaceFileEntity> entities = repository.findAllByFileNameAndChecksumAndStatus(
            fileName, fileChecksum, Status.SUCCESS);

        assertThat(entities).hasSize(1);

        InterfaceFileEntity entity = entities.getFirst();

        BlobClient client = blobServiceClient
            .getBlobContainerClient(containerName)
            .getBlobClient(entity.getFilestoreUuid().toString());

        assertThat(client.exists()).isTrue();

        final byte[] content = client.downloadContent().toBytes();
        BlobProperties properties = client.getProperties();

        assertThat(DigestUtils.md5DigestAsHex(content)).isEqualTo(fileChecksum);
        assertThat(HexFormat.of().formatHex(properties.getContentMd5())).isEqualTo(fileChecksum);
        assertThat(properties.getBlobSize()).isEqualTo(content.length);
    }

    public final void assertReportCanBeListedAndDownloaded(String fileName, String checksum, String resourcePath)
        throws IOException {
        InterfaceFileEntity entity = repository.findByFileNameAndChecksumAndStatus(fileName, checksum, Status.SUCCESS)
            .orElseThrow();
        assertThat(entity.getBusinessUnitCode()).isNullOrEmpty();
        assertThat(entity.getPaymentType()).isNull();
        assertThat(entity.getErrors()).isNull();
        var listed = interfaceFilesService.searchInterfaceFiles(SearchInterfaceFilesDto.builder()
            .source(entity.getSource()).status(Status.SUCCESS).build());
        assertThat(listed).filteredOn(file -> file.getInterfaceFileId().equals(entity.getInterfaceFileId()))
            .singleElement().satisfies(file -> {
                assertThat(file.getFileName()).isEqualTo(fileName);
                assertThat(file.getFilestoreUuid()).isEqualTo(entity.getFilestoreUuid());
                assertThat(file.getChecksum()).isEqualTo(checksum);
                assertThat(file.getSource().getValue()).isEqualTo(entity.getSource().name());
                assertThat(file.getCreatedDatetime()).isEqualTo(entity.getCreatedDatetime());
                assertThat(file.getErrors()).isNull();
            });
        try (InputStream expected = new ClassPathResource(resourcePath).getInputStream();
             InputStream actual = interfaceFilesService.getInterfaceFilesContent(entity.getInterfaceFileId())) {
            assertThat(actual.readAllBytes()).isEqualTo(expected.readAllBytes());
        }
    }

    public final InterfaceFileEntity createFailedInterfaceFile(String fileName, String checksum, Interface source) {
        InterfaceFileEntity entity = InterfaceFileEntity.builder()
            .fileName(fileName)
            .checksum(checksum)
            .type(Type.SOURCE)
            .source(source)
            .target(Interface.OPAL)
            .opalDomain(Domain.MAINTENANCE)
            .createdDatetime(LocalDateTime.now(clock))
            .status(Status.FAILED)
            .errors("{\"message\": \"something went wrong\"}")
            .build();

        return repository.save(entity);
    }

    public final InterfaceFileEntity createSuccessfulInterfaceFile(String fileName, String checksum) {
        InterfaceFileEntity entity = InterfaceFileEntity.builder()
            .fileName(fileName)
            .checksum(checksum)
            .type(Type.SOURCE)
            .source(Interface.CAPS_REPORT)
            .target(Interface.OPAL)
            .opalDomain(Domain.MAINTENANCE)
            .createdDatetime(LocalDateTime.now(clock))
            .status(Status.SUCCESS)
            .build();

        return repository.save(entity);
    }

}
