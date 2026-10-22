package uk.gov.hmcts.opal.filehandler.service;

import java.io.InputStream;
import java.util.List;

public interface FileIngestionService<T> {
    List<T> getFiles();

    InputStream getFile(T file);

    void deleteFile(T file);
}
