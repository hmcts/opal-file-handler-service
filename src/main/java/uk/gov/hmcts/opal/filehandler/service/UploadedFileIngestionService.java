package uk.gov.hmcts.opal.filehandler.service;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * API uploads supply their content directly and have no remote source file to remove.
 */
public class UploadedFileIngestionService implements FileIngestionService<String> {

    @Override
    public List<String> getFiles() {
        return List.of();
    }

    @Override
    public ByteArrayOutputStream getFile(String fileName) {
        throw new UnsupportedOperationException("Uploaded file content must be supplied by the API");
    }

    @Override
    public boolean deleteFile(String fileName) {
        return true;
    }
}
