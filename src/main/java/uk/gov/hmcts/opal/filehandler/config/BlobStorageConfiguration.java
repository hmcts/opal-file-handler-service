package uk.gov.hmcts.opal.filehandler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("opal.file-handling-service.file-store")
@Getter
@Setter
public class BlobStorageConfiguration {

    private String storageAccountName;

    private String storageUrl;

    private String storageKey;

}
