package uk.gov.hmcts.opal.filehandler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("opal.file-handler-service.vpfpo-acknowledgements")
@Getter
@Setter
public class VpfpoAcknowledgementsConfiguration {

    private String containerName;
}
