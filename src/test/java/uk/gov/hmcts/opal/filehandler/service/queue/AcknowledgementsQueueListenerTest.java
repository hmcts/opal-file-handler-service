package uk.gov.hmcts.opal.filehandler.service.queue;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.jms.JMSException;
import jakarta.jms.StreamMessage;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.filehandler.service.AcknowledgementsProcessingService;

@ExtendWith(MockitoExtension.class)
public class AcknowledgementsQueueListenerTest {

    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private AcknowledgementsProcessingService service;
    @Mock
    private TextMessage message;
    @Mock
    private AcknowledgementsQueueMessage payload;

    @InjectMocks
    private AcknowledgementsQueueListener listener;

    private static final String PUBLISHED_JSON = """
        {"vpfpo_registration_id": 1, "defendant_id": 2, "status": "PUBLISHED"}""";
    private static final String FAILED_JSON = """
        {"vpfpo_registration_id": 1, "status": "PUBLISHING_FAILED"}""";

    @Test
    void onMessage_PassesSuccessfulPayloadToConsumerService() throws JMSException, JsonProcessingException {
        when(message.getText()).thenReturn(PUBLISHED_JSON);
        when(objectMapper.readValue(PUBLISHED_JSON, AcknowledgementsQueueMessage.class)).thenReturn(payload);

        listener.onMessage(message);

        verify(service, times(1)).consume(payload);
    }

    @Test
    void onMessage_PassesFailedPayloadToConsumerService() throws JMSException, JsonProcessingException {
        when(message.getText()).thenReturn(FAILED_JSON);
        when(objectMapper.readValue(FAILED_JSON, AcknowledgementsQueueMessage.class)).thenReturn(payload);

        listener.onMessage(message);

        verify(service, times(1)).consume(payload);
    }

    @Test
    void onMessage_HandlesMessageGetTextException() throws JMSException {
        when(message.getText()).thenThrow(new JMSException("failed to get text"));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> listener.onMessage(message));

        verify(service, never()).consume(any());
        assertThat(e).hasMessageContaining("Unable to parse Acknowledgements message payload");
    }

    @Test
    void onMessage_HandlesJsonDecodingException() throws JMSException, JsonProcessingException {
        when(message.getText()).thenReturn(PUBLISHED_JSON);
        when(objectMapper.readValue(PUBLISHED_JSON, AcknowledgementsQueueMessage.class))
            .thenThrow(mock(JsonProcessingException.class));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> listener.onMessage(message));

        verify(service, never()).consume(any());
        assertThat(e).hasMessageContaining("Unable to parse Acknowledgements message payload");
    }

    @Test
    void onMessage_HandlesNonTextMessageExceptions() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> listener.onMessage(mock(StreamMessage.class))
        );

        verify(service, never()).consume(any());
        assertThat(e).hasMessageContaining("Message must be of type TextMessage");
    }
}
