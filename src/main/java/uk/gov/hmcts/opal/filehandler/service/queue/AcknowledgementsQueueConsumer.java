package uk.gov.hmcts.opal.filehandler.service.queue;

public interface AcknowledgementsQueueConsumer {
    void consume(AcknowledgementsQueueMessage message);
}
