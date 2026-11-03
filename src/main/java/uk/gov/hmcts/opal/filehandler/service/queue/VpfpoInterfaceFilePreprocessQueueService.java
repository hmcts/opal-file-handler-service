package uk.gov.hmcts.opal.filehandler.service.queue;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.common.queue.AbstractQueueService;
import uk.gov.hmcts.opal.filehandler.dto.FileHandlerDraftAccountPayloadDTO;

@Service
@Slf4j
public class VpfpoInterfaceFilePreprocessQueueService
    extends AbstractQueueService<FileHandlerDraftAccountPayloadDTO>
    implements InterfaceFilePreprocessQueueService<FileHandlerDraftAccountPayloadDTO> {

    public VpfpoInterfaceFilePreprocessQueueService(
        @Qualifier("commonServiceBusJmsTemplate")
        JmsTemplate jmsTemplate,
        @Value("${opal.common.service-bus.queues.vpfpo-interface-file-preprocess}")
        String queueName,
        ObjectMapper objectMapper
    ) {
        super(jmsTemplate, objectMapper, queueName);
    }

    @Override
    public void send(FileHandlerDraftAccountPayloadDTO payload) {
        log.info("Adding payload to queue {}", queueName);
        super.send(payload);
    }
}