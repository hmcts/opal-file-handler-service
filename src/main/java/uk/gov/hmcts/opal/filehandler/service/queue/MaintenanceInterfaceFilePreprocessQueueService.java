package uk.gov.hmcts.opal.filehandler.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.common.queue.AbstractQueueService;

@Slf4j
@Service
public class MaintenanceInterfaceFilePreprocessQueueService
    extends AbstractQueueService<Long>
    implements InterfaceFilePreprocessQueueService {

    public MaintenanceInterfaceFilePreprocessQueueService(
        @Qualifier("commonServiceBusJmsTemplate")
        JmsTemplate jmsTemplate,
        @Value("${opal.common.service-bus.queues.maintenance-interface-file-preprocess}")
        String queueName,
        ObjectMapper objectMapper) {
        super(jmsTemplate, objectMapper, queueName);
    }

    @Override
    public void send(Long interfaceFileId) {
        log.info("Adding interfaceFileId={} to queue {}", interfaceFileId, queueName);
        super.send(interfaceFileId);
    }
}
