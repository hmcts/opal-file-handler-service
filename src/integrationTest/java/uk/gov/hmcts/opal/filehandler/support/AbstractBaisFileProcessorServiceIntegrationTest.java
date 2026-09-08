package uk.gov.hmcts.opal.filehandler.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.DigestUtils;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureDisabledException;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;
import uk.gov.hmcts.opal.common.launchdarkly.config.LaunchDarklyProperties;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.service.AbstractInterfaceFileProcessorService;
import uk.gov.hmcts.opal.filehandler.service.CapsReportBaisFileProcessorServiceIntegrationTest;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;

@SpringBootTest(properties = "spring.main.web-application-type=none")
@Slf4j
@Testcontainers
public abstract class AbstractBaisFileProcessorServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Clock clock;

    @Autowired
    protected InterfaceFilesRepository repository;

    @Autowired
    protected BaisSftpClient sftpClient;

    @Autowired
    protected BlobServiceClient blobServiceClient;

    @Autowired
    private LaunchDarklyProperties launchDarklyProperties;

    protected abstract AbstractInterfaceFileProcessorService processor();

    protected abstract BaisFileProcessorConfiguration processorConfiguration();

    protected abstract BaisTestFile validFile();

    protected abstract String unsupportedFileName();

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
    }

    @BeforeEach
    void setUpBaisContract() {
        repository.deleteAll();

        BlobContainerClient container = blobContainer();
        container.createIfNotExists();
        container.listBlobs().forEach(blob -> container.getBlobClient(blob.getName()).deleteIfExists());

        deleteSftpFiles();

        setFeatureFlag(FeatureFlags.RELEASE_1C_BANKING_INTERFACES, true);
        setFeatureFlag(processorConfiguration().getFeatureFlag(), true);
    }

    @AfterEach
    void tearDownBaisContract() {
        deleteSftpFiles();
    }

    @Test
    @DisplayName("BAIS processor feature flag has an offline default")
    void shouldConfigureProcessorFeatureFlagDefault() {
        assertThat(launchDarklyProperties.getDefaultFlagValues())
            .containsKey(processorConfiguration().getFeatureFlag());
    }

    @ParameterizedTest(name = "banking interfaces enabled={0}, feature enabled={1}")
    @CsvSource({
        "false, true, release-1c-banking-interfaces",
        "true, false, feature",
        "false, false, release-1c-banking-interfaces"
    })
    @DisplayName("AC1: processing requires both feature flags")
    void shouldNotProcessWhenARequiredFeatureIsDisabled(
        boolean bankingInterfacesEnabled,
        boolean featureEnabled,
        String disabledFeature
    ) {
        BaisTestFile fixture = validFile();
        // A real file proves disabled feature flags prevent ingestion and leave SFTP contents untouched.
        uploadFixture(fixture.fileName());

        setFeatureFlag(FeatureFlags.RELEASE_1C_BANKING_INTERFACES, bankingInterfacesEnabled);
        setFeatureFlag(processorConfiguration().getFeatureFlag(), featureEnabled);

        String expectedDisabledFeature = "feature".equals(disabledFeature)
            ? processorConfiguration().getFeatureFlag()
            : FeatureFlags.RELEASE_1C_BANKING_INTERFACES;

        assertThatThrownBy(() -> processor().run(processorConfiguration()))
            .isInstanceOf(FeatureDisabledException.class)
            .hasMessage(expectedDisabledFeature + " is not enabled");

        assertThat(repository.findAll()).isEmpty();
        assertThat(blobContainer().listBlobs()).isEmpty();
        assertThat(sftpClient.listRegularFiles(processorConfiguration().getSftpUsername()))
            .containsExactly(fixture.fileName());
    }

    @Test
    @DisplayName("Unsupported BAIS filenames remain on SFTP without processing")
    void shouldIgnoreUnsupportedFileAndLeaveItOnSftp() {
        uploadFixture(unsupportedFileName());

        processor().run(processorConfiguration());

        assertThat(repository.findAll()).isEmpty();
        assertThat(blobContainer().listBlobs()).isEmpty();
        assertThat(sftpClient.listRegularFiles(processorConfiguration().getSftpUsername()))
            .containsExactly(unsupportedFileName());
    }

    @Test
    @DisplayName("An empty BAIS SFTP directory completes without side effects")
    void shouldSucceedWhenSftpDirectoryIsEmpty() {
        assertThatCode(() -> processor().run(processorConfiguration())).doesNotThrowAnyException();

        assertThat(repository.findAll()).isEmpty();
        assertThat(blobContainer().listBlobs()).isEmpty();
    }

    public final void uploadResourceToSftp(String resourcePath, String containerPath) {
        TestContainerConfig.SFTP_CONTAINER.copyFileToContainer(
            MountableFile.forClasspathResource(resourcePath), containerPath);
    }

    protected final void uploadFixture(String destinationFileName) {
        uploadResourceToSftp(validFile().classpathResource(), sftpPath(destinationFileName));
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

    protected final BlobContainerClient blobContainer() {
        return blobServiceClient.getBlobContainerClient(processorConfiguration().getContainerName());
    }

    private String sftpPath(String fileName) {
        return "/home/%s/%s".formatted(processorConfiguration().getSftpUsername(), fileName);
    }

    private void deleteSftpFiles() {
        String username = processorConfiguration().getSftpUsername();
        sftpClient.listRegularFiles(username).forEach(file -> sftpClient.deleteFile(username, file));
    }

    private void setFeatureFlag(String featureFlag, boolean enabled) {
        assertThat(launchDarklyProperties.getDefaultFlagValues()).containsKey(featureFlag);
        launchDarklyProperties.getDefaultFlagValues().put(featureFlag, enabled);
    }

    public record BaisTestFile(String fileName, String classpathResource) {
    }

}
