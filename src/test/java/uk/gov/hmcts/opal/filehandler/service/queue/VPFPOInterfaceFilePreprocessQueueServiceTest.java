package uk.gov.hmcts.opal.filehandler.service.queue;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.dto.FileHandlerDraftAccountPayloadDTO;

@ExtendWith(MockitoExtension.class)
class VPFPOInterfaceFilePreprocessQueueServiceTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private VPFPOInterfaceFilePreprocessQueueService service;

    @BeforeEach
    void setUp() {
        service = new VPFPOInterfaceFilePreprocessQueueService(
            jmsTemplate,
            "vpfpo-queue",
            objectMapper
        );
    }

    @Test
    void shouldSendPayloadToQueue() throws Exception {
        FileHandlerDraftAccountPayloadDTO payload =
            FileHandlerDraftAccountPayloadDTO.builder()
                .accountJson("{\"accountNumber\":\"12345\"}")
                .prosecutorCode("ABC")
                .build();

        String serialisedPayload =
            "{\"accountJson\":\"{\\\"accountNumber\\\":\\\"12345\\\"}\",\"prosecutorCode\":\"ABC\"}";

        when(objectMapper.writeValueAsString(payload)).thenReturn(serialisedPayload);
        service.send(payload);
        verify(jmsTemplate).convertAndSend("vpfpo-queue", serialisedPayload);
    }
}