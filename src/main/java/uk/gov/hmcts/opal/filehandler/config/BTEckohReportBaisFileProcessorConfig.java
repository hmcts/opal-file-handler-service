package uk.gov.hmcts.opal.filehandler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "opal.file-handler-service.file-types.bteckoh-report")
@Component
public class BTEckohReportBaisFileProcessorConfig extends AbstractBaisFileProcessorConfig {
}
