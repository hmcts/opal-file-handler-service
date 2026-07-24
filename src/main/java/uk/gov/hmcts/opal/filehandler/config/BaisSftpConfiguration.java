package uk.gov.hmcts.opal.filehandler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("opal.file-handler-service.sftp.bais")
public class BaisSftpConfiguration {

    private String host;

    private int port;

    private String privateKey;

}
