package uk.gov.hmcts.opal.filehandler;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.servicebus.jms.ServiceBusJmsConnectionFactory;
import com.azure.servicebus.jms.ServiceBusJmsConnectionFactorySettings;
import jakarta.annotation.PostConstruct;
import jakarta.jms.ConnectionFactory;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jms.core.JmsTemplate;
import uk.gov.hmcts.opal.common.config.ServiceBusProperties;
import uk.gov.hmcts.opal.filehandler.config.FeignConfiguration;
import uk.gov.hmcts.opal.filehandler.util.TaskRunnerUtil;

@SpringBootApplication(scanBasePackages = "uk.gov.hmcts.opal")
@EnableJpaRepositories("uk.gov.hmcts.opal.*")
@EntityScan("uk.gov.hmcts.opal.*")
@EnableFeignClients(basePackages = "uk.gov.hmcts.opal.*", defaultConfiguration = FeignConfiguration.class)
@EnableCaching
@Slf4j
@ConfigurationPropertiesScan
@EnableConfigurationProperties({ServiceBusProperties.class})
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
public class Application {

    private static final String DEFAULT_MANAGED_IDENTITY_CLIENT_ID = "2d883a38-dcc7-4cf2-8741-6ff5148f452e";
    private static final String DEFAULT_SERVICE_BUS_HOST = "opal-servicebus-stg.servicebus.windows.net";
    private static final String DEFAULT_TEST_QUEUE = "opal-test-queue";

    public static void main(final String[] args) {
        if (TaskRunnerUtil.isAutomatedTask(args)) {
            System.exit(TaskRunnerUtil.runAutomatedTaskWithSpring(args));
        }

        SpringApplication.run(Application.class, args);
    }

    @Autowired
    Environment env;

    @PostConstruct
    public void init() {
        if (List.of(env.getActiveProfiles()).contains("integration")) {
            return;
        }

        String queueName = env.getProperty("SERVICEBUS_MI_TEST_QUEUE_NAME", DEFAULT_TEST_QUEUE);

        try {
            log.info("Testing Service Bus JMS managed identity authentication");
            JmsTemplate jmsTemplate = commonServiceBusJmsTemplate(commonServiceBusConnectionFactory());
            jmsTemplate.convertAndSend(queueName, "Test message");
            log.info("Service Bus JMS managed identity authentication test message sent to {}", queueName);
        } catch (RuntimeException e) {
            log.error("Service Bus JMS managed identity authentication test failed for queue {}", queueName, e);
            throw e;
        }
    }

    private ConnectionFactory commonServiceBusConnectionFactory() {
        String managedIdentityClientId = env.getProperty(
            "SERVICEBUS_MI_CLIENT_ID",
            DEFAULT_MANAGED_IDENTITY_CLIENT_ID
        );
        String host = env.getProperty("SERVICEBUS_MI_HOST", DEFAULT_SERVICE_BUS_HOST);

        TokenCredential credential =
            new DefaultAzureCredentialBuilder()
                .managedIdentityClientId(managedIdentityClientId)
                .build();

        AccessToken token = credential
            .getToken(
                new TokenRequestContext()
                    .addScopes("https://servicebus.azure.net/.default")
            )
            .block();

        log.info(
            "Successfully obtained Service Bus token for host {}, expires at {}",
            host,
            token.getExpiresAt()
        );

        ServiceBusJmsConnectionFactorySettings settings = new ServiceBusJmsConnectionFactorySettings();
        settings.setStartupMaxReconnectAttempts(1);
        settings.setMaxReconnectAttempts(1);
        settings.setWarnAfterReconnectAttempts(1);

        ServiceBusJmsConnectionFactory connectionFactory = new ServiceBusJmsConnectionFactory(
            credential,
            host,
            settings
        );
        log.info("Created Service Bus JMS connection factory with remote URI {}", connectionFactory.getRemoteConnectionUri());

        return connectionFactory;
    }

    private JmsTemplate commonServiceBusJmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
        jmsTemplate.setDeliveryPersistent(true);
        jmsTemplate.setExplicitQosEnabled(true);
        jmsTemplate.setSessionTransacted(true);
        return jmsTemplate;
    }
}
