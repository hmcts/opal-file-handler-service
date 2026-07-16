package uk.gov.hmcts.opal.filehandler.service.blobstore;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureStorageConfig {

    @Value("${opal.blob-storage.connection-string}") //TODO: Update these configs
    private String connectionString;

    @Bean
    public BlobServiceClient blobServiceClient() {
        return new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();
    }
}
