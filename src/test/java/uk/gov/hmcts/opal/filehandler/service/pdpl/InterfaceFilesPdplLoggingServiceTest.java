package uk.gov.hmcts.opal.filehandler.service.pdpl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.opal.common.logging.LogUtil;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.opal.filehandler.entity.PdplIdentifierType;
import uk.gov.hmcts.opal.logging.integration.dto.ParticipantIdentifier;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingCategory;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;
import uk.gov.hmcts.opal.logging.integration.service.LoggingService;

@ExtendWith(MockitoExtension.class)
class InterfaceFilesPdplLoggingServiceTest {

    private static final long INTERFACE_FILE_ID = 123L;
    private static final long USER_ID = 456L;
    private static final String IP_ADDRESS = "192.0.2.10";
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-09-29T10:15:30Z");

    @Mock
    private LoggingService loggingService;

    @Mock
    private UserStateV2 userState;

    @Test
    void logPdpl_createsExpectedAsynchronousDisclosureLog() {
        Clock clock = Clock.fixed(CREATED_AT.toInstant(), ZoneOffset.UTC);
        InterfaceFilesPdplLoggingService service = new InterfaceFilesPdplLoggingService(clock, loggingService);
        when(userState.getUserId()).thenReturn(USER_ID);
        when(loggingService.personalDataAccessLogAsync(any())).thenReturn(true);

        try (MockedStatic<LogUtil> logUtil = mockStatic(LogUtil.class)) {
            logUtil.when(LogUtil::getIpAddress).thenReturn(IP_ADDRESS);
            service.logPdpl(INTERFACE_FILE_ID, userState);
        }

        ArgumentCaptor<PersonalDataProcessingLogDetails> captor =
            ArgumentCaptor.forClass(PersonalDataProcessingLogDetails.class);
        verify(loggingService).personalDataAccessLogAsync(captor.capture());

        PersonalDataProcessingLogDetails logDetails = captor.getValue();
        assertEquals("Interface file - download", logDetails.getBusinessIdentifier());
        assertEquals(PersonalDataProcessingCategory.DISCLOSURE, logDetails.getCategory());
        assertEquals(IP_ADDRESS, logDetails.getIpAddress());
        assertEquals(CREATED_AT, logDetails.getCreatedAt());
        assertEquals(Long.toString(USER_ID), logDetails.getCreatedBy().getIdentifier());
        assertEquals(PdplIdentifierType.OPAL_USER_ID, logDetails.getCreatedBy().getType());
        assertNull(logDetails.getRecipient().getIdentifier());
        assertNull(logDetails.getRecipient().getType());
        assertEquals(1, logDetails.getIndividuals().size());

        ParticipantIdentifier individual = logDetails.getIndividuals().getFirst();
        assertEquals(Long.toString(INTERFACE_FILE_ID), individual.getIdentifier());
        assertEquals(PdplIdentifierType.FILE_HANDLER_INTERFACE_FILE, individual.getType());
    }
}
