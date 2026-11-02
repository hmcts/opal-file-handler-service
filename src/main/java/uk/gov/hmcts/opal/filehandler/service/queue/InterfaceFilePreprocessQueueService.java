package uk.gov.hmcts.opal.filehandler.service.queue;

public interface InterfaceFilePreprocessQueueService<T> {

    void send(T obj);
}
