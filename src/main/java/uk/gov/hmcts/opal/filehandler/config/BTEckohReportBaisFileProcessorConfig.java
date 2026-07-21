package uk.gov.hmcts.opal.filehandler.config;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "opal.file-handling-service.file-types.bteckoh-report")
public class BTEckohReportBaisFileProcessorConfig implements BaisFileProcessorConfig {

    @Setter
    private String storageContainerName;

    public String getContainerName() {
        return storageContainerName;
    }

}
