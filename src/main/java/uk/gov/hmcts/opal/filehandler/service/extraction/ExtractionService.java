package uk.gov.hmcts.opal.filehandler.service.extraction;

import java.io.InputStream;
import java.util.List;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;

public interface ExtractionService<T> {

    List<T> extractStandardData(InterfaceFileEntity sourceInterfaceFile, InputStream fileContents);
}



