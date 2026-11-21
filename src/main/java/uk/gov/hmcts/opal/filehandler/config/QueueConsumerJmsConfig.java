package uk.gov.hmcts.opal.filehandler.config;

import jakarta.jms.ConnectionFactory;
import lombok.RequiredArgsConstructor;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import uk.gov.hmcts.opal.common.config.ServiceBusConnectionStringParser;
import uk.gov.hmcts.opal.common.config.ServiceBusProperties;

@EnableJms
@Configuration
@ConditionalOnExpression("'${opal.automated-task:}' == ''")
@EnableConfigurationProperties(ServiceBusProperties.class)
@RequiredArgsConstructor
public class QueueConsumerJmsConfig {

    private final ServiceBusConnectionStringParser serviceBusConnectionStringParser;

    @Bean
    public ConnectionFactory consumerConnectionFactory(ServiceBusProperties properties) {
        ServiceBusConnectionStringParser.ConnectionDetails details =
            serviceBusConnectionStringParser.parse(properties.getConnectionString());

        String remoteUri = "%s://%s?amqp.idleTimeout=%d".formatted(
            properties.getProtocol(),
            details.fullyQualifiedNamespace(),
            properties.getIdleTimeoutMs()
        );

        JmsConnectionFactory qpidFactory = new JmsConnectionFactory(remoteUri);
        qpidFactory.setUsername(details.sharedAccessKeyName());
        qpidFactory.setPassword(details.sharedAccessKey());
        return new CachingConnectionFactory(qpidFactory);
    }

    @Bean
    public DefaultJmsListenerContainerFactory opalFileHandlerConsumerConnectionFactory(
        ConnectionFactory consumerConnectionFactory) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(consumerConnectionFactory);
        factory.setSessionTransacted(true);
        return factory;
    }

}
