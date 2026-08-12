package uk.gov.hmcts.opal.filehandler.service.blobstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FileHandlerAzureStorageConfigTest {

    private static final String DEVELOPMENT_STORAGE_CONNECTION_STRING = "UseDevelopmentStorage=true";

    @Test
    void shouldUseSharedKeyConfigurationWhenDeployedStorageValuesArePresent() {
        FileHandlerAzureStorageConfig config = new FileHandlerAzureStorageConfig(
            DEVELOPMENT_STORAGE_CONNECTION_STRING,
            "deployedstorageaccount",
            "https://deployedstorageaccount.blob.core.windows.net",
            "YWNjb3VudC1rZXk="
        );

        assertThat(config.blobServiceClient().getAccountUrl())
            .isEqualTo("https://deployedstorageaccount.blob.core.windows.net");
    }

    @Test
    void shouldUseConnectionStringWhenDeployedStorageValuesAreAbsent() {
        FileHandlerAzureStorageConfig config = new FileHandlerAzureStorageConfig(
            DEVELOPMENT_STORAGE_CONNECTION_STRING,
            "",
            "",
            ""
        );

        assertThat(config.blobServiceClient().getAccountUrl())
            .isEqualTo("http://127.0.0.1:10000/devstoreaccount1");
    }

    @Test
    void shouldRejectIncompleteSharedKeyConfiguration() {
        FileHandlerAzureStorageConfig config = new FileHandlerAzureStorageConfig(
            DEVELOPMENT_STORAGE_CONNECTION_STRING,
            "deployedstorageaccount",
            "https://deployedstorageaccount.blob.core.windows.net",
            ""
        );

        assertThatThrownBy(config::blobServiceClient)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("File store shared-key configuration requires an account name, endpoint and account key");
    }
}
