package uk.gov.hmcts.opal.filehandler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("dwpBaisFileProcessorConfig")
@ConfigurationProperties("opal.file-handler-service.file-types.dwp")
public class DWPBaisFileProcessorConfiguration extends AbstractBaisFileProcessorConfiguration {
}
