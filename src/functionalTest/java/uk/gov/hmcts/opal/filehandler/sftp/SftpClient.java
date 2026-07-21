package uk.gov.hmcts.opal.filehandler.sftp;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.sftp.RemoteResourceInfo;
import net.schmizz.sshj.sftp.SFTPClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import uk.gov.hmcts.opal.filehandler.config.TestEnvironment;

import java.io.IOException;
import java.util.List;

/**
 * Reusable SSHJ-based SFTP helper for smoke and functional tests.
 */
public class SftpClient implements AutoCloseable {

    private final SSHClient sshClient;
    private final SFTPClient sftpClient;

    /**
     * Opens an authenticated SFTP connection using the configured functional-test environment
     * settings.
     */
    public SftpClient() {
        try {
            sshClient = new SSHClient();
            sshClient.addHostKeyVerifier(new PromiscuousVerifier());
            sshClient.connect(TestEnvironment.getSftpHost(), TestEnvironment.getSftpPort());
            sshClient.authPassword(TestEnvironment.getSftpUsername(), TestEnvironment.getSftpPassword());
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
