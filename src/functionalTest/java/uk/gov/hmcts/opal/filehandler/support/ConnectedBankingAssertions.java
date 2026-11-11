package uk.gov.hmcts.opal.filehandler.support;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Map;
import org.apache.commons.codec.digest.DigestUtils;
import uk.gov.hmcts.opal.filehandler.blob.BlobStorageClient;
import uk.gov.hmcts.opal.filehandler.db.ConnectedFinesTestDatabaseClient;
import uk.gov.hmcts.opal.filehandler.sftp.SftpClient;

/** Checks that malformed inputs are retained without starting downstream processing. */
public class ConnectedBankingAssertions {
    private final BaisInterfaceFileClient files;

    public ConnectedBankingAssertions(BaisInterfaceFileClient files) {
        this.files = files;
    }

    public void rejectedInput(ConnectedBankingFixture fixture, String expectedError) throws IOException {
        var config = fixture.config();
        Map<String, Object> failed = files.awaitInterfaceFile(config, "SOURCE", "FAILED");
        assertTrue(failed.get("errors").toString().contains(expectedError), "Unexpected rejection: " + failed);
        files.content(failed, 422);
        byte[] retained = new BlobStorageClient(config.blobContainerName())
            .download(failed.get("filestore_uuid").toString());
        assertEquals(failed.get("checksum"), DigestUtils.md5Hex(retained));
        assertArrayEquals(Resources.toByteArray(Resources.getResource(config.resourcePath())), retained);
        assertEquals(1, files.records(config).size(), "A rejected input must not create extracted/transformed output");
        try (var fines = new ConnectedFinesTestDatabaseClient()) {
            assertEquals(0, fines.findByFileName(fixture.businessUnit(), config.fileName()).size(),
                "Rejected input must not create a Fines job/file");
        }
        try (SftpClient sftp = new SftpClient(config.sftpUsername())) {
            assertTrue(sftp.exists(config.fileName()), "Rejected inputs must remain available for recovery");
        }
    }

}
