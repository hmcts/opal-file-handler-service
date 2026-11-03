package uk.gov.hmcts.opal.filehandler.config;

import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import uk.gov.hmcts.opal.filehandler.entity.Interface;

@Getter
@Setter
public abstract class AbstractFileProcessorConfiguration
    implements FileProcessorConfiguration {

    private String featureFlag;

    private Interface source;

    private Interface target;

    private Pattern fileNameRegex;

    private String containerName;
}