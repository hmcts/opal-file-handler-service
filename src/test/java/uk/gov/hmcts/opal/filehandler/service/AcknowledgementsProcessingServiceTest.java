package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationEntity;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationStatus;
import uk.gov.hmcts.opal.filehandler.repository.VpfpoRegistrationsRepository;
import uk.gov.hmcts.opal.filehandler.service.queue.AcknowledgementsQueueMessage;
import uk.gov.hmcts.opal.filehandler.service.queue.AcknowledgementsQueueMessage.Status;

@ExtendWith(MockitoExtension.class)
public class AcknowledgementsProcessingServiceTest {

    @Mock
    private VpfpoRegistrationsRepository repository;

    @Mock
    private VpfpoRegistrationEntity entity;

    @InjectMocks
    private AcknowledgementsProcessingService service;

    private static final long VPFPO_REGISTRATION_ID = 1000L;
    private static final long DEFENDANT_ID = 2000L;

    @Test
    void consume_UpdatesDefendantIdAndStatusOnSuccess() {
        when(repository.findByVpfpoRegistrationId(VPFPO_REGISTRATION_ID))
            .thenReturn(Optional.of(entity));

        service.consume(successfulMessage());

        verify(entity, times(1)).setRegistrationStatus(VpfpoRegistrationStatus.PUBLISHED);
        verify(entity, times(1)).setDefendantAccountId(DEFENDANT_ID);
    }

    @Test
    void consume_UpdatesStatusOnFailure() {
        when(repository.findByVpfpoRegistrationId(VPFPO_REGISTRATION_ID))
            .thenReturn(Optional.of(entity));

        service.consume(failedMessage());

        verify(entity, times(1))
            .setRegistrationStatus(VpfpoRegistrationStatus.PUBLISHING_FAILED);
        verify(entity, never()).setDefendantAccountId(any());
    }

    @Test
    void consume_ThrowsExceptionOnMissingEntity() {
        when(repository.findByVpfpoRegistrationId(VPFPO_REGISTRATION_ID))
            .thenReturn(Optional.empty());

        EntityNotFoundException e = assertThrows(
            EntityNotFoundException.class,
            () -> service.consume(successfulMessage())
        );

        assertThat(e).hasMessageContaining("could not find vpfpo registration with id 1000");
        verify(entity, never()).setRegistrationStatus(any());
        verify(entity, never()).setDefendantAccountId(any());
    }


    private AcknowledgementsQueueMessage successfulMessage() {
        return new AcknowledgementsQueueMessage(
            VPFPO_REGISTRATION_ID,
            DEFENDANT_ID,
            Status.PUBLISHED
        );
    }

    private AcknowledgementsQueueMessage failedMessage() {
        return new AcknowledgementsQueueMessage(
            VPFPO_REGISTRATION_ID,
            null,
            Status.PUBLISHING_FAILED
        );
    }
}
