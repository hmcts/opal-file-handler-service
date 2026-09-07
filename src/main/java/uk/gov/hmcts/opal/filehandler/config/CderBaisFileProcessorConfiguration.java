package uk.gov.hmcts.opal.filehandler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("cderBaisFileProcessorConfig")
@ConfigurationProperties("opal.file-handling-service.file-types.bailiffs.cder")
public class CderBaisFileProcessorConfiguration extends AbstractBaisFileProcessorConfiguration {
}
