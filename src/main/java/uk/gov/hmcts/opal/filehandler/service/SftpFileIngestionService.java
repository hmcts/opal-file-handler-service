package uk.gov.hmcts.opal.filehandler.service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.opal.filehandler.config.BaisFileProcessorConfiguration;
import uk.gov.hmcts.opal.filehandler.util.BaisSftpClient;

@RequiredArgsConstructor
@Slf4j
public class SftpFileIngestionService implements FileIngestionService<String> {

    private final BaisFileProcessorConfiguration config;
    private final BaisSftpClient baisSftpClient;

    @Override
    public List<String> getFiles() {
        log.info("Selecting files to process from BAIS for user '{}' and source '{}'",
            config.getSftpUsername(), config.getSource());
        List<String> baisFiles = baisSftpClient.listRegularFiles(config.getSftpUsername());

        if (baisFiles.isEmpty()) {
            log.info("No files found in BAIS for user '{}' when processing source '{}'", config.getSftpUsername(),
                config.getSource());
            return List.of();
        }

        Map<Boolean, List<String>> filesByMatch = baisFiles.stream()
            .collect(Collectors.partitioningBy(fileName ->
                config.getFileNameRegex().matcher(fileName).matches()));

        List<String> matchingFiles = filesByMatch.get(true);
        List<String> ignoringFiles = filesByMatch.get(false);

        if (!ignoringFiles.isEmpty()) {
            log.error("Found {} additional files in BAIS for user '{}' that did not match the regex for source '{}' "
                    + "and were ignored: {}",
                ignoringFiles.size(), config.getSftpUsername(), config.getSource(), String.join(", ", ignoringFiles));
        }

        return matchingFiles;
    }

    @Override
    public ByteArrayOutputStream getFile(String fileName) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        baisSftpClient.downloadFile(config.getSftpUsername(), fileName, stream);
        return stream;
    }

    @Override
    public boolean deleteFile(String fileName) {
        return baisSftpClient.deleteFile(config.getSftpUsername(), fileName);
    }
}
