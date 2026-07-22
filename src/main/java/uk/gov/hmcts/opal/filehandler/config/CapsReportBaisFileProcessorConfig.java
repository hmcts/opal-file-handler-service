package uk.gov.hmcts.opal.filehandler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "opal.file-handling-service.file-types.caps-report")
@Component("CAPS_REPORT")
public class CapsReportBaisFileProcessorConfig extends AbstractBaisFileProcessorConfig {
}
