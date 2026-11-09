package uk.gov.hmcts.opal.filehandler.service.queue;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;
import static uk.gov.hmcts.opal.common.dto.ToJsonString.toJsonString;

import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import java.util.Optional;
import org.apache.qpid.jms.provider.amqp.message.AmqpJmsTextMessageFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationEntity;
import uk.gov.hmcts.opal.filehandler.entity.VpfpoRegistrationStatus;
import uk.gov.hmcts.opal.filehandler.repository.VpfpoRegistrationsRepository;
import uk.gov.hmcts.opal.filehandler.service.queue.AcknowledgementsQueueMessage.Status;
import uk.gov.hmcts.opal.filehandler.support.AbstractIntegrationTest;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraEpic;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraStory;

@Sql(scripts = "classpath:db/insertData/insert_into_vpfpo_registrations.sql",
    executionPhase = BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:db/deleteData/delete_from_vpfpo_registrations.sql",
    executionPhase = AFTER_TEST_METHOD)
@TestPropertySource(properties = {
    "opal.common.service-bus.acknowledgements=VPFPO-acknowledgements-queue"
})
public class AcknowledgementsQueueListenerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private AcknowledgementsQueueListener listener;

    @Autowired
    private VpfpoRegistrationsRepository repository;

    private static final long VALID_REGISTRATION_ID = 1000L;

    @Test
    @DisplayName("AC1: A valid success message will be processed correctly")
    @JiraStory("PO-10667")
    @JiraEpic("PO-7462")
    void service_ConsumesSuccessfulMessage() throws JMSException {
        AcknowledgementsQueueMessage message = new AcknowledgementsQueueMessage(
            VALID_REGISTRATION_ID, 20L, Status.PUBLISHED);
        queueMessage(message);

        assertVpfpoRegistration(vpfpoRegistrationEntity(20L, VpfpoRegistrationStatus.PUBLISHED));
    }

    @Test
    @DisplayName("AC2: A valid error message will be processed correctly")
    @JiraStory("PO-10667")
    @JiraEpic("PO-7462")
    void service_ConsumesErrorMessage() throws JMSException {
        AcknowledgementsQueueMessage message = new AcknowledgementsQueueMessage(
            VALID_REGISTRATION_ID, null, Status.PUBLISHING_FAILED);
        queueMessage(message);

        assertVpfpoRegistration(vpfpoRegistrationEntity(null, VpfpoRegistrationStatus.PUBLISHING_FAILED));
    }

    private void queueMessage(AcknowledgementsQueueMessage message) throws JMSException {
        String payload = toJsonString(message);
        TextMessage textMessage = new AmqpJmsTextMessageFacade().asJmsMessage();
        textMessage.setText(payload);
        listener.onMessage(textMessage);
    }

    private void assertVpfpoRegistration(VpfpoRegistrationEntity expectedEntity) {
        Optional<VpfpoRegistrationEntity> maybeFoundEntity =
            repository.findByVpfpoRegistrationId(expectedEntity.getVpfpoRegistrationId());

        assertThat(maybeFoundEntity).isPresent();
        VpfpoRegistrationEntity foundEntity = maybeFoundEntity.get();

        assertAll(
            () -> assertEquals(expectedEntity.getVpfpoRegistrationId(), foundEntity.getVpfpoRegistrationId()),
            () -> assertEquals(expectedEntity.getDefendantAccountId(), foundEntity.getDefendantAccountId()),
            () -> assertEquals(expectedEntity.getRegistrationStatus(), foundEntity.getRegistrationStatus()),
            () -> assertEquals(expectedEntity.getInterfaceFileId(), foundEntity.getInterfaceFileId())
        );
    }

    private static VpfpoRegistrationEntity vpfpoRegistrationEntity(Long defendantId, VpfpoRegistrationStatus status) {
        return VpfpoRegistrationEntity.builder()
            .vpfpoRegistrationId(VALID_REGISTRATION_ID)
            .interfaceFileId(100L)
            .defendantAccountId(defendantId)
            .registrationStatus(status)
            .build();
    }

}
