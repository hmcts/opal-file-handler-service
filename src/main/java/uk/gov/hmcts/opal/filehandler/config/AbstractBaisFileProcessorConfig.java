package uk.gov.hmcts.opal.filehandler.config;

import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.opal.filehandler.entity.Interface;

@Configuration
@Getter
@Setter
public class AbstractBaisFileProcessorConfig implements BaisFileProcessorConfiguration {

    private String containerName;
    private String featureFlag;
    private Pattern fileNameRegex;
    private Interface source;
    private Interface target;
    private String sftpUsername;

}
