package uk.gov.hmcts.opal.filehandler.sftp;

import com.google.common.io.Resources;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.xfer.InMemorySourceFile;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;

/**
 * Reusable SSHJ-based SFTP helper for smoke and functional tests.
 */
public class SftpClient implements AutoCloseable {

    private final SSHClient sshClient;
    private final SFTPClient sftpClient;
    private final String sftpUsername;

    /**
     * Opens an authenticated SFTP connection using the configured functional-test environment
     * settings.
     */
    public SftpClient() {
        this(TestEnvironment.getSftpUsername());
    }

    /**
     * Opens an authenticated SFTP connection for the supplied BAIS report user.
     *
     * @param sftpUsername SFTP user to authenticate as.
     */
    public SftpClient(String sftpUsername) {
        this.sftpUsername = sftpUsername;
        try {
            sshClient = new SSHClient();
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            sshClient.connect(TestEnvironment.getSftpHost(), TestEnvironment.getSftpPort());
            authenticate();
            sftpClient = sshClient.newSFTPClient();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to connect to SFTP server", e);
        }
    }

    /**
     * Returns whether the underlying SSH session is connected and authenticated.
     *
     * @return {@code true} when the SFTP session is ready for use.
     */
    public boolean canConnect() {
        return sshClient.isConnected() && sshClient.isAuthenticated();
    }

    /**
     * Lists the contents of a remote SFTP directory.
     *
     * @param path remote directory path to list.
     * @return remote directory entries.
     */
    public List<RemoteResourceInfo> listDirectory(String path) {
        try {
            return sftpClient.ls(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to list SFTP directory: " + path, e);
        }
    }

    /**
     * Uploads a classpath resource to the SFTP user's home directory, replacing an existing file.
     *
     * @param resourcePath classpath resource containing the file content.
     * @param remoteFileName filename to create in the SFTP user's home directory.
     */
    public void uploadResource(String resourcePath, String remoteFileName) {
        try {
            URL resource = Resources.getResource(resourcePath);
            byte[] content = Resources.toByteArray(resource);
            sftpClient.put(new ByteArraySourceFile(remoteFileName, content), remoteFileName);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to upload SFTP fixture: " + remoteFileName, e);
        }
    }

    /**
     * Returns whether a regular file exists in the SFTP user's home directory.
     *
     * @param remoteFileName filename to inspect.
     * @return {@code true} when the file exists.
     */
    public boolean exists(String remoteFileName) {
        try {
            return sftpClient.statExistence(remoteFileName) != null;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to inspect SFTP fixture: " + remoteFileName, e);
        }
    }

    /**
     * Deletes a file from the SFTP user's home directory when present.
     *
     * @param remoteFileName filename to remove.
     */
    public void deleteIfExists(String remoteFileName) {
        try {
            if (sftpClient.statExistence(remoteFileName) != null) {
                sftpClient.rm(remoteFileName);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete SFTP fixture: " + remoteFileName, e);
        }
    }

    private void authenticate() throws IOException {
        if (TestEnvironment.getSftpPrivateKey().isPresent()) {
            String privateKey = TestEnvironment.getSftpPrivateKey().orElseThrow();
            sshClient.authPublickey(sftpUsername, sshClient.loadKeys(privateKey, null, null));
            return;
        }

        if (TestEnvironment.getSftpPrivateKeyPath(sftpUsername).isPresent()) {
            Path privateKeyPath = TestEnvironment.getSftpPrivateKeyPath(sftpUsername).orElseThrow();
            sshClient.authPublickey(sftpUsername, privateKeyPath.toString());
            return;
        }

        String password = TestEnvironment.getSftpPassword().orElseThrow(() -> new IllegalStateException(
            "SFTP authentication requires FUNCTIONAL_TEST_SFTP_PRIVATE_KEY, "
                + "FUNCTIONAL_TEST_SFTP_PRIVATE_KEY_PATH or FUNCTIONAL_TEST_SFTP_PASSWORD"));
        sshClient.authPassword(sftpUsername, password);
    }

    private static final class ByteArraySourceFile extends InMemorySourceFile {

        private final String fileName;
        private final byte[] content;

        private ByteArraySourceFile(String fileName, byte[] content) {
            this.fileName = fileName;
            this.content = content;
        }

        @Override
        public String getName() {
            return fileName;
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

    /**
     * Closes the underlying SFTP and SSH sessions.
     */
    @Override
    public void close() {
        try {
            sftpClient.close();
            sshClient.close();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to close SFTP connection", e);
        }
    }
}
