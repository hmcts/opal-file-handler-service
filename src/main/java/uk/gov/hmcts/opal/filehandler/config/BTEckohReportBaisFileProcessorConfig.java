package uk.gov.hmcts.opal.filehandler.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class BTEckohReportBaisFileProcessorConfig implements BaisFileProcessorConfig {

    @Value("${opal.file-handling-service.file-types.BTEckoh-report.storage-container-name}")
    private String containerName;

}
