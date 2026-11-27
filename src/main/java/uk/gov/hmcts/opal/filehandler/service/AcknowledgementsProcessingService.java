package uk.gov.hmcts.opal.filehandler.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationEntity;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationStatus;
import uk.gov.hmcts.opal.filehandler.repository.VpfpoRegistrationsRepository;
import uk.gov.hmcts.opal.filehandler.service.queue.AcknowledgementsQueueConsumer;
import uk.gov.hmcts.opal.filehandler.service.queue.AcknowledgementsQueueMessage;

@Service
@AllArgsConstructor
public class AcknowledgementsProcessingService implements AcknowledgementsQueueConsumer {

    private final VpfpoRegistrationsRepository repository;

    @Transactional
    public void consume(AcknowledgementsQueueMessage message) {
        VpfpoRegistrationEntity registration = repository
            .findByVpfpoRegistrationId(message.registrationId())
            .orElseThrow(() -> new EntityNotFoundException(
                "could not find vpfpo registration with id %s".formatted(message.registrationId())
            ));
        if (message.isSuccessful()) {
            registration.setDefendantAccountId(message.defendant_id());
            registration.setRegistrationStatus(VpfpoRegistrationStatus.PUBLISHED);
        } else {
            registration.setRegistrationStatus(VpfpoRegistrationStatus.PUBLISHING_FAILED);
        }
    }
}
