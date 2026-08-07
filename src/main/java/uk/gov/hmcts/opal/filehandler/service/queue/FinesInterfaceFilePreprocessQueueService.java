package uk.gov.hmcts.opal.filehandler.service.queue;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FinesInterfaceFilePreprocessQueueService implements InterfaceFilePreprocessQueueService {

    static final String QUEUE_NAME = "banking-interfaces-preprocess-interface-file-fines";

    @Override
    public void send(Long interfaceFileId) {
        log.info("Adding interfaceFileId={} to queue {}", interfaceFileId, QUEUE_NAME);
    }
}
