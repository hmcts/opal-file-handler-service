package uk.gov.hmcts.opal.filehandler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties("opal.file-handler-service.file-store")
public class BlobStorageConfiguration {

    private String storageAccountName;

    private String storageUrl;

    private String storageKey;

}
