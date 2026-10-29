package uk.gov.hmcts.opal.filehandler.config;

import java.util.regex.Pattern;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.opal.filehandler.entity.Interface;

@Getter
@Setter
@Component("VariantBankingFileProcessorConfiguration")
@ConfigurationProperties("opal.file-handler-service.file-types.variant-banking")
public class VariantBankingFileProcessorConfig implements BaisFileProcessorConfiguration {

    private String featureFlag;

    private Interface source;

    private Interface target;

    private String containerName;

    private Pattern fileNameRegex;

    private String sftpUsername;

}