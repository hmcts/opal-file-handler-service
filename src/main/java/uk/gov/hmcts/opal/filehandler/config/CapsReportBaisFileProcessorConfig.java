package uk.gov.hmcts.opal.filehandler.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class CapsReportBaisFileProcessorConfig implements BaisFileProcessorConfig {

    @Value("${opal.file-handling-service.file-types.CAPS-report.storage-container-name}")
    private String containerName;

}
