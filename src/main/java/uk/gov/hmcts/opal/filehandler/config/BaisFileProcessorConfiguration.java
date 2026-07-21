package uk.gov.hmcts.opal.filehandler.config;

import java.util.regex.Pattern;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;

public interface BaisFileProcessorConfiguration {

    String getContainerName();

    String getFeatureFlag();

    Pattern getFileNameRegex();

    Interface getSource();

    Interface getTarget();

    Domain getDomain();

    String getSftpUsername();

}
