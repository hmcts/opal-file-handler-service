package uk.gov.hmcts.opal.filehandler.config;

import java.util.regex.Pattern;
import uk.gov.hmcts.opal.filehandler.entity.Interface;

public interface FileProcessorConfiguration {

    String getFeatureFlag();

    Interface getSource();

    Interface getTarget();

    Pattern getFileNameRegex();

}
