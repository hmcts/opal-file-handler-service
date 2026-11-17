package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.filehandler.config.JacobsBaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.exception.BaisSftpFileDownloadException;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;

@ExtendWith(MockitoExtension.class)
class SftpFileIngestionServiceTest {

    private static final String SFTP_USERNAME = "test-sftp-user";
    private static final String FILE_NAME = "matching-file.dat";

    @Mock
    private BaisSftpClient baisSftpClient;

    private SftpFileIngestionService service;

    @BeforeEach
    void setUp() {
        JacobsBaisFileProcessorConfiguration config = new JacobsBaisFileProcessorConfiguration();
        config.setSftpUsername(SFTP_USERNAME);
        config.setSource(Interface.JACOBS);
        config.setFileNameRegex(Pattern.compile("matching-.*\\.dat"));
        service = new SftpFileIngestionService(config, baisSftpClient);
    }

    @Nested
    @DisplayName("getFiles()")
    class GetFiles {

        @Test
        @DisplayName("Returns an empty list when no files exist")
        void returnsEmptyWhenNoFilesExist() {
            when(baisSftpClient.listRegularFiles(SFTP_USERNAME)).thenReturn(List.of());

            assertThat(service.getFiles()).isEmpty();
        }

        @Test
        @DisplayName("Filters filenames and preserves their order")
        void filtersFilesAndPreservesOrder() {
            String secondFile = "matching-second.dat";
            when(baisSftpClient.listRegularFiles(SFTP_USERNAME))
                .thenReturn(List.of("ignored.txt", secondFile, "ignored.dat", FILE_NAME));

            assertThat(service.getFiles()).containsExactly(secondFile, FILE_NAME);
        }

        @Test
        @DisplayName("Returns all files when every filename matches")
        void returnsAllMatchingFiles() {
            String secondFile = "matching-second.dat";
            when(baisSftpClient.listRegularFiles(SFTP_USERNAME)).thenReturn(List.of(FILE_NAME, secondFile));

            assertThat(service.getFiles()).containsExactly(FILE_NAME, secondFile);
        }

        @Test
        @DisplayName("Requires the entire filename to match the configured pattern")
        void requiresFullFilenameMatch() {
            when(baisSftpClient.listRegularFiles(SFTP_USERNAME))
                .thenReturn(List.of("prefix-" + FILE_NAME, FILE_NAME + ".bak", "ignored.txt"));

            assertThat(service.getFiles()).isEmpty();
        }

        @Test
        @DisplayName("Propagates SFTP listing failures")
        void propagatesListingFailure() {
            IllegalStateException failure = new IllegalStateException("SFTP unavailable");
            when(baisSftpClient.listRegularFiles(SFTP_USERNAME)).thenThrow(failure);

            assertThatThrownBy(service::getFiles).isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("getFile()")
    class GetFile {

        @Test
        @DisplayName("Returns the downloaded file bytes unchanged")
        void returnsDownloadedBytes() throws Exception {
            byte[] content = {0, 1, 13, 10, (byte) 255};
            doAnswer(invocation -> {
                OutputStream stream = invocation.getArgument(2);
                stream.write(content);
                return null;
            }).when(baisSftpClient).downloadFile(eq(SFTP_USERNAME), eq(FILE_NAME), any(OutputStream.class));

            try (ByteArrayOutputStream result = service.getFile(FILE_NAME)) {
                assertThat(result.toByteArray()).containsExactly(content);
            }
        }

        @Test
        @DisplayName("Returns an empty stream for an empty file")
        void returnsEmptyStreamForEmptyFile() throws Exception {
            try (ByteArrayOutputStream result = service.getFile(FILE_NAME)) {
                assertThat(result.toByteArray()).isEmpty();
            }

            verify(baisSftpClient).downloadFile(eq(SFTP_USERNAME), eq(FILE_NAME), any(OutputStream.class));
        }

        @Test
        @DisplayName("Propagates SFTP download failures")
        void propagatesDownloadFailure() {
            BaisSftpFileDownloadException failure = new BaisSftpFileDownloadException("Download failed");
            doThrow(failure).when(baisSftpClient)
                .downloadFile(eq(SFTP_USERNAME), eq(FILE_NAME), any(OutputStream.class));

            assertThatThrownBy(() -> service.getFile(FILE_NAME)).isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("deleteFile()")
    class DeleteFile {

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        @DisplayName("Returns the SFTP client deletion result")
        void returnsClientResult(boolean deleted) {
            when(baisSftpClient.deleteFile(SFTP_USERNAME, FILE_NAME)).thenReturn(deleted);

            assertThat(service.deleteFile(FILE_NAME)).isEqualTo(deleted);
        }
    }
}
