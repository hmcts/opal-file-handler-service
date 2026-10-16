package uk.gov.hmcts.opal.filehandler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("MarstonBaisFileProcessorConfig")
@ConfigurationProperties(prefix = "opal.file-handler-service.file-types.bailiffs.marston")
public class MarstonBaisFileProcessorConfig extends AbstractBaisFileProcessorConfiguration {

}