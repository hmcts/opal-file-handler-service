package uk.gov.hmcts.opal.filehandler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AbstractBaisFileProcessorConfig implements BaisFileProcessorConfig {

    @Setter
    private String containerName;

}
