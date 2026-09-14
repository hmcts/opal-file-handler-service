package uk.gov.hmcts.opal.filehandler.service.queue;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class FinesInterfaceFilePreprocessQueueServiceTest {

    private final Logger logger = (Logger) LoggerFactory.getLogger(FinesInterfaceFilePreprocessQueueService.class);
    private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();
    @Mock
    private JmsTemplate jmsTemplate;
    @Mock
    private ObjectMapper objectMapper;
    private FinesInterfaceFilePreprocessQueueService service;

    @BeforeEach
    void setUp() {
        logAppender.start();
        logger.addAppender(logAppender);
        service = new FinesInterfaceFilePreprocessQueueService(
            jmsTemplate,
            "banking-interfaces-preprocess-interface-file-fines",
            objectMapper
        );
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Test
    void shouldLogFinesQueueSend() {
        service.send(123L);

        assertThat(logAppender.list)
            .filteredOn(event -> event.getLevel() == Level.INFO)
            .extracting(ILoggingEvent::getFormattedMessage)
            .containsExactly(
                "Adding interfaceFileId=123 to queue banking-interfaces-preprocess-interface-file-fines"
            );
    }
}
