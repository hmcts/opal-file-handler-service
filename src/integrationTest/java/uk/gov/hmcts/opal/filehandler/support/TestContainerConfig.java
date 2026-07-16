package uk.gov.hmcts.opal.filehandler.support;

import com.redis.testcontainers.RedisContainer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.azure.AzuriteContainer;
import org.testcontainers.containers.BindMode;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestContainerConfig {

    private static final String DEFAULT_POSTGRES_IMAGE = "postgres:17.5";
    private static final String DEFAULT_AZURITE_IMAGE = "mcr.microsoft.com/azure-storage/azurite:latest";
    private static final String POSTGRES_IMAGE =
        System.getenv().getOrDefault("OPAL_POSTGRES_IMAGE", DEFAULT_POSTGRES_IMAGE);
    public static final PostgreSQLContainer POSTGRES_CONTAINER;
    public static final RedisContainer REDIS_CONTAINER;
    public static final AzuriteContainer AZURITE_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withCommand("postgres -c max_connections=200 -c log_connections=on -c log_disconnections=on");

        // Uncomment the following to enable connection to the Test Containers DB whilst debugging.
        //POSTGRES_CONTAINER.setPortBindings(List.of("5432:5432"));

        POSTGRES_CONTAINER.start();

        REDIS_CONTAINER = new RedisContainer(DockerImageName.parse("redis:6.2.6"))
            .withExposedPorts(6379);
        REDIS_CONTAINER.start();

        AZURITE_CONTAINER = new AzuriteContainer(DockerImageName.parse(DEFAULT_AZURITE_IMAGE))
//            .withClasspathResourceMapping("azure/data", "/data", BindMode.READ_ONLY)
            .withCommand("azurite --loose --blobHost 0.0.0.0 --blobPort 10000 --location /data --skipApiVersionCheck");
        AZURITE_CONTAINER.start();
    }
}
