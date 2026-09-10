package uk.gov.hmcts.opal.filehandler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("AllpayBaisFileBaisFileProcessorConfig")
@ConfigurationProperties("opal.file-handler-service.file-types.allpay")
public class AllpayBaisFileProcessorConfiguration extends AbstractBaisFileProcessorConfiguration {
}
