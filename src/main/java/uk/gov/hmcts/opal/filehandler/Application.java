package uk.gov.hmcts.opal.filehandler;

import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.azure.servicebus.jms.ServiceBusJmsConnectionFactory;
import jakarta.annotation.PostConstruct;
import jakarta.jms.ConnectionFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
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

    public static void main(final String[] args) {
        if (TaskRunnerUtil.isAutomatedTask(args)) {
            System.exit(TaskRunnerUtil.runAutomatedTaskWithSpring(args));
        }

        SpringApplication.run(Application.class, args);
    }

//    @PostConstruct
//    public void init() {
//        try {
//            log.info("Application started");
//            JmsTemplate jmsTemplate = commonServiceBusJmsTemplate(commonServiceBusConnectionFactory());
//            jmsTemplate.convertAndSend("opal-common-servicebus-jms-template", "Test message");
//        } catch (Throwable e) {
//            log.info(e.getMessage());
//        }
//    }
//
//    private ConnectionFactory commonServiceBusConnectionFactory() {
//        ManagedIdentityCredential credential =
//            new ManagedIdentityCredentialBuilder()
//                .build();
//
//        String host = "opal-servicebus-stg.servicebus.windows.net";
//
//        return new ServiceBusJmsConnectionFactory(
//            credential,
//            host,
//            null
//        );
//    }
//
//    private JmsTemplate commonServiceBusJmsTemplate(ConnectionFactory connectionFactory) {
//        JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
//        jmsTemplate.setDeliveryPersistent(true);
//        jmsTemplate.setExplicitQosEnabled(true);
//        jmsTemplate.setSessionTransacted(true);
//        return jmsTemplate;
//    }
}
