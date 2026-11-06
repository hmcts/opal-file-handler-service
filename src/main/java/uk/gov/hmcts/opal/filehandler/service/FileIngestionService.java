package uk.gov.hmcts.opal.filehandler.service;

import java.io.ByteArrayOutputStream;
import java.util.List;

public interface FileIngestionService<T> {

    List<T> getFiles();

    ByteArrayOutputStream getFile(T fileName);

    boolean deleteFile(T file);
}
