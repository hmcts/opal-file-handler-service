package uk.gov.hmcts.opal.filehandler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("NatWestBaisFileBaisFileProcessorConfig")
@ConfigurationProperties("opal.file-handler-service.file-types.natwest")
public class NatWestBaisFileProcessorConfiguration extends AbstractBaisFileProcessorConfiguration {
}
