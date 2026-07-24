package uk.gov.hmcts.opal.filehandler.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlobStorageBeanConfiguration {

    @Bean
    public BlobServiceClient blobServiceClient(BlobStorageConfiguration configuration) {
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(
            configuration.getStorageAccountName(), configuration.getStorageKey());

        return new BlobServiceClientBuilder()
            .endpoint(configuration.getStorageUrl())
            .credential(credential)
            .buildClient();
    }

}
