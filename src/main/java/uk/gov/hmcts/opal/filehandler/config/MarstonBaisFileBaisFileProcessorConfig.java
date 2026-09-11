package uk.gov.hmcts.opal.filehandler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("marstonBaisFileBaisFileProcessorConfig")
@ConfigurationProperties(prefix = "opal.file-handler-service.bailiffs.marston")
public class MarstonBaisFileBaisFileProcessorConfig  extends AbstractBaisFileProcessorConfiguration {

}