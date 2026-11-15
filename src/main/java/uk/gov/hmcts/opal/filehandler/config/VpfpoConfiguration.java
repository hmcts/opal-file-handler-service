package uk.gov.hmcts.opal.filehandler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component("vpfpoConfiguration")
@ConfigurationProperties(
    prefix = "opal.file-handler-service.file-types.vpfpo"
)
public class VpfpoConfiguration extends AbstractFileProcessorConfiguration {
}