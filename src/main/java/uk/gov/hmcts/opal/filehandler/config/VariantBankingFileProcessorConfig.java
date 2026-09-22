package uk.gov.hmcts.opal.filehandler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("variantBankingFileProcessorConfig")
@ConfigurationProperties( prefix = "opal.file-handler-service.file-types.variant-banking")
public class VariantBankingFileProcessorConfig extends AbstractBaisFileProcessorConfiguration {
}
