package uk.gov.hmcts.opal.filehandler.service.queue;

public interface InterfaceFilePreprocessQueueService {

    void send(Long interfaceFileId);
}
