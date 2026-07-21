package uk.gov.hmcts.opal.filehandler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("opal.file-handling-service.sftp.bais")
@Getter
@Setter
public class BaisSftpConfiguration {

    private String host;

    private int port;

    private String privateKey;

}
