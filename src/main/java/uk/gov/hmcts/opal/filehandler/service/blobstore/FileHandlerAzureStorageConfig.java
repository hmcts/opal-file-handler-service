package uk.gov.hmcts.opal.filehandler.service.blobstore;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
public class FileHandlerAzureStorageConfig {

    private final String connectionString;
    private final String accountName;
    private final String endpoint;
    private final String accountKey;

    public FileHandlerAzureStorageConfig(
        @Value("${opal.file-handler-service.file-store.connection-string}") String connectionString,
        @Value("${opal.file-handler-service.file-store.account-name}") String accountName,
        @Value("${opal.file-handler-service.file-store.endpoint}") String endpoint,
        @Value("${opal.file-handler-service.file-store.account-key}") String accountKey
    ) {
        this.connectionString = connectionString;
        this.accountName = accountName;
        this.endpoint = endpoint;
        this.accountKey = accountKey;
    }

    @Bean
    public BlobServiceClient blobServiceClient() {
        boolean hasAccountName = StringUtils.hasText(accountName);
        boolean hasEndpoint = StringUtils.hasText(endpoint);
        boolean hasAccountKey = StringUtils.hasText(accountKey);

        if (hasAccountName || hasEndpoint || hasAccountKey) {
            if (!hasAccountName || !hasEndpoint || !hasAccountKey) {
                throw new IllegalStateException(
                    "File store shared-key configuration requires an account name, endpoint and account key"
                );
            }

            return new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .credential(new StorageSharedKeyCredential(accountName, accountKey))
                .buildClient();
        }

        return new BlobServiceClientBuilder()
            .connectionString(connectionString)
            .buildClient();
    }
}
