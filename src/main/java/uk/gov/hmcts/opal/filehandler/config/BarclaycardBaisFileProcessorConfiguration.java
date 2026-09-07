package uk.gov.hmcts.opal.filehandler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("BarclaycardBaisFileProcessorConfig")
@ConfigurationProperties("opal.file-handler-service.file-types.barclaycard")
public class BarclaycardBaisFileProcessorConfiguration extends AbstractBaisFileProcessorConfiguration {
}
