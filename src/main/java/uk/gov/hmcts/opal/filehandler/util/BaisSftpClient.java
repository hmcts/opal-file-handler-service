package uk.gov.hmcts.opal.filehandler.util;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.sshd.sftp.client.SftpClient.DirEntry;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.exception.BaisSftpFileDownloadException;

@Component
@RequiredArgsConstructor
public class BaisSftpClient {

    private static final String ROOT_DIRECTORY = ".";

    private final BaisSftpSessionFactory sessionFactory;

    public List<String> listRegularFiles(String username) {
        DirEntry[] entries = sessionFactory.connect(username).list(ROOT_DIRECTORY);

        return Arrays.stream(entries)
            .filter(entry -> entry.getAttributes().isRegularFile())
            .map(DirEntry::getFilename)
            .toList();
    }

    public void downloadFile(String username, String fileName, OutputStream outputStream) {
        boolean downloaded = sessionFactory.connect(username)
            .get(fileName, inputStream -> inputStream.transferTo(outputStream));

        if (!downloaded) {
            throw new BaisSftpFileDownloadException("Unable to download BAIS file " + fileName);
        }
    }

    public boolean deleteFile(String username, String fileName) {
        return sessionFactory.connect(username).remove(fileName);
    }

}
