package uk.gov.hmcts.opal.filehandler.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.apache.sshd.sftp.client.SftpClient.Attributes;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.apache.sshd.sftp.common.SftpConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.file.remote.InputStreamCallback;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

@ExtendWith(MockitoExtension.class)
class BaisSftpClientTest {

    private static final String USERNAME = "CAPS-user";
    private static final String FILE_NAME = "CapFa.GB.20260723.120000.xml";
    private static final byte[] FILE_CONTENT = "unaltered file content".getBytes(StandardCharsets.UTF_8);

    @Mock
    private BaisSftpSessionFactory sessionFactory;

    @Mock
    private SftpRemoteFileTemplate remoteFileTemplate;

    private BaisSftpClient client;

    @BeforeEach
    void setUp() {
        when(sessionFactory.connect(USERNAME)).thenReturn(remoteFileTemplate);
        client = new BaisSftpClient(sessionFactory);
    }

    @Test
    void listRegularFilesExcludesDirectoriesAndLinks() {
        when(remoteFileTemplate.list(".")).thenReturn(new DirEntry[] {
            entry(FILE_NAME, SftpConstants.S_IFREG),
            entry("archive", SftpConstants.S_IFDIR),
            entry("latest", SftpConstants.S_IFLNK)
        });

        List<String> files = client.listRegularFiles(USERNAME);

        assertThat(files).containsExactly(FILE_NAME);
    }

    @Test
    void downloadFileCopiesOriginalContent() {
        when(remoteFileTemplate.get(eq(FILE_NAME), any(InputStreamCallback.class))).thenAnswer(invocation -> {
            InputStreamCallback callback = invocation.getArgument(1);
            callback.doWithInputStream(new ByteArrayInputStream(FILE_CONTENT));
            return true;
        });
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        client.downloadFile(USERNAME, FILE_NAME, outputStream);

        assertThat(outputStream.toByteArray()).containsExactly(FILE_CONTENT);
    }

    @Test
    void downloadFileFailsWhenRemoteFileCannotBeFinalised() {
        when(remoteFileTemplate.get(eq(FILE_NAME), any(InputStreamCallback.class))).thenReturn(false);

        assertThatThrownBy(() -> client.downloadFile(USERNAME, FILE_NAME, new ByteArrayOutputStream()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Unable to download BAIS file " + FILE_NAME);
    }

    @Test
    void deleteFileReturnsRemoteResult() {
        when(remoteFileTemplate.remove(FILE_NAME)).thenReturn(true);

        assertThat(client.deleteFile(USERNAME, FILE_NAME)).isTrue();
        verify(sessionFactory).connect(USERNAME);
    }

    private DirEntry entry(String name, int permissions) {
        return new DirEntry(name, name, new Attributes().perms(permissions));
    }
}
