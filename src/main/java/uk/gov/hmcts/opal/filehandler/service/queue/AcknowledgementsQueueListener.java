package uk.gov.hmcts.opal.filehandler.service.queue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;

@Component
@RequiredArgsConstructor
@ConditionalOnExpression("'${opal.automated-task:}' == ''")
@Slf4j(topic = "opal.AcknowledgementsQueueListener")
public class AcknowledgementsQueueListener {
    private final AcknowledgementsQueueConsumer consumerService;
    private final ObjectMapper objectMapper;

    @JmsListener(
        destination = "${opal.common.service-bus.queues.acknowledgements}",
        containerFactory = "opalFileHandlerConsumerConnectionFactory"
    )
    public void onMessage(Message message) {
        if (message instanceof TextMessage textMessage) {
            AcknowledgementsQueueMessage payload = parsePayload(textMessage);
            consumerService.consume(payload);
        } else {
            throw new IllegalArgumentException("Message must be of type TextMessage");
        }
    }

    private AcknowledgementsQueueMessage parsePayload(TextMessage textMessage) {
        try {
            return objectMapper.readValue(textMessage.getText(), AcknowledgementsQueueMessage.class);
        } catch (JacksonException | JsonProcessingException | JMSException ex) {
            log.error("Acknowledgements queue message parse failed", ex);
            throw new IllegalArgumentException("Unable to parse Acknowledgements message payload", ex);
        }
    }
}