package uk.gov.hmcts.opal.filehandler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component("VariantBankingFileProcessorConfiguration")
@ConfigurationProperties("opal.file-handler-service.file-types.variant-banking")
public class VariantBankingFileProcessorConfig  extends AbstractBaisFileProcessorConfiguration
    implements BaisFileProcessorConfiguration {

}