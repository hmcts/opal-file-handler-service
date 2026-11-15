package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.opal.filehandler.config.VpfpoConfiguration;
import uk.gov.hmcts.opal.filehandler.support.AbstractIntegrationTest;

@ActiveProfiles("integration")
class VpfpoingestionserviceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Vpfpoingestionservice service;

    @Autowired
    private BlobServiceClient blobServiceClient;

    @Autowired
    private VpfpoConfiguration config;

    private BlobContainerClient container;

    @BeforeEach
    void setUp() {
        container = blobServiceClient.getBlobContainerClient(
            config.getContainerName()
        );

        container.createIfNotExists();

        container.listBlobs()
            .forEach(blob -> container.getBlobClient(blob.getName()).deleteIfExists());
    }

    @Test
    @DisplayName("AC2: Returns all files from the VPFPO blob container")
    void returnsAllFiles() {

        container.getBlobClient("file1.dat")
            .upload(BinaryData.fromString("content1"));

        container.getBlobClient("file2.dat")
            .upload(BinaryData.fromString("content2"));

        List<String> files = service.getFiles();

        assertThat(files)
            .containsExactlyInAnyOrder(
                "file1.dat",
                "file2.dat"
            );
    }

    @Test
    @DisplayName("AC3: Returns contents of selected file")
    void returnsFileContents() {

        container.getBlobClient("file1.dat")
            .upload(BinaryData.fromString("hello world"));

        ByteArrayOutputStream result =
            service.getFile("file1.dat");

        assertThat(result.toString(StandardCharsets.UTF_8))
            .isEqualTo("hello world");
    }

    @Test
    @DisplayName("AC4: Deletes selected file")
    void deletesFile() {

        container.getBlobClient("file1.dat")
            .upload(BinaryData.fromString("hello world"));

        assertThat(
            container.getBlobClient("file1.dat").exists()
        ).isTrue();

        boolean deleted = service.deleteFile("file1.dat");

        assertThat(deleted).isTrue();

        assertThat(
            container.getBlobClient("file1.dat").exists()
        ).isFalse();
    }
}