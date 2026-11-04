package uk.gov.hmcts.opal.filehandler.service.queue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.azure.storage.common.implementation.connectionstring.StorageAuthenticationSettings.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.dto.FileHandlerDraftAccountPayloadDTO;

@ExtendWith(MockitoExtension.class)
class VpfpoInterfaceFilePreprocessQueueServiceTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private ObjectMapper objectMapper;

    private VpfpoInterfaceFilePreprocessQueueService service;

    @BeforeEach
    void setUp() {

        service = new VpfpoInterfaceFilePreprocessQueueService(jmsTemplate,"vpfpo-queue", objectMapper);

    }

    @Test
    void shouldSendPayloadToQueue() throws Exception {

        Account account = mock(Account.class);

        FileHandlerDraftAccountPayloadDTO payload =
            FileHandlerDraftAccountPayloadDTO.builder()
                .account(account)
                .prosecutorCode("ABC")
                .build();

        String serialisedPayload =
            "{\"accountJson\":\"{\\\"accountNumber\\\":\\\"12345\\\"}\",\"prosecutorCode\":\"ABC\"}";

        when(objectMapper.writeValueAsString(payload)).thenReturn(serialisedPayload);
        service.send(payload);
        verify(jmsTemplate).convertAndSend("vpfpo-queue", serialisedPayload);
    }

}