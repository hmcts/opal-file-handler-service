package uk.gov.hmcts.opal.filehandler.steps.natwest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;
import uk.gov.hmcts.opal.filehandler.support.SftpRetry;

/** Uploads a NatWest file built from feature data to the BAIS SFTP user. */
final class NatWestSftpUploader {

    private NatWestSftpUploader() {
    }

    static void upload(String username, String fileName, byte[] content) {
        SftpRetry.withRetry("upload NatWest file " + fileName, () -> uploadOnce(username, fileName, content));
    }

    private static void uploadOnce(String username, String fileName, byte[] content) {
        try (SSHClient ssh = new SSHClient()) {
            ssh.addHostKeyVerifier(new PromiscuousVerifier());
            ssh.connect(TestEnvironment.getSftpHost(), TestEnvironment.getSftpPort());
            authenticate(ssh, username);
            try (SFTPClient sftp = ssh.newSFTPClient()) {
                sftp.getFileTransfer().setPreserveAttributes(false);
                sftp.put(new NatWestSourceFile(fileName, content), fileName);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to upload feature-generated NatWest CARS file " + fileName,
                exception);
        }
    }

    private static void authenticate(SSHClient ssh, String username) throws IOException {
        if (TestEnvironment.getSftpPrivateKey().isPresent()) {
            ssh.authPublickey(username, ssh.loadKeys(TestEnvironment.getSftpPrivateKey().orElseThrow(), null, null));
            return;
        }
        if (TestEnvironment.getSftpPrivateKeyPath(username).isPresent()) {
            ssh.authPublickey(username, TestEnvironment.getSftpPrivateKeyPath(username).orElseThrow().toString());
            return;
        }
        String password = TestEnvironment.getSftpPassword().orElseThrow(() -> new IllegalStateException(
            "SFTP authentication requires FUNCTIONAL_TEST_SFTP_PRIVATE_KEY, "
                + "FUNCTIONAL_TEST_SFTP_PRIVATE_KEY_PATH or FUNCTIONAL_TEST_SFTP_PASSWORD"));
        ssh.authPassword(username, password);
    }

    private static final class NatWestSourceFile extends InMemorySourceFile {

        private final String name;
        private final byte[] content;

        private NatWestSourceFile(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public long getLength() {
            return content.length;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(content);
        }
    }
}
