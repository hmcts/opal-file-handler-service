package uk.gov.hmcts.opal.filehandler.service.pdpl;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.opal.common.logging.LogUtil;
import uk.gov.hmcts.opal.common.user.authorisation.model.UserStateV2;
import uk.gov.hmcts.opal.filehandler.entity.PdplIdentifierType;
import uk.gov.hmcts.opal.logging.integration.dto.ParticipantIdentifier;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingCategory;
import uk.gov.hmcts.opal.logging.integration.dto.PersonalDataProcessingLogDetails;
import uk.gov.hmcts.opal.logging.integration.service.LoggingService;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "opal.filehandler.InterfaceFilesPdplLoggingService")
public class InterfaceFilesPdplLoggingService {
    private final Clock clock;
    private final LoggingService loggingService;
    private static final String BUSINESS_IDENTIFIER = "Interface file - download";

    public void logPdpl(Long interfaceFileId, UserStateV2 userState) {
        ParticipantIdentifier recipient = ParticipantIdentifier.builder()
            .identifier(null)
            .type(null)
            .build();

        ParticipantIdentifier createdBy = ParticipantIdentifier.builder()
            .identifier(userState.getUserId().toString())
            .type(PdplIdentifierType.OPAL_USER_ID)
            .build();

        List<ParticipantIdentifier> individuals = List.of(
            new ParticipantIdentifier(interfaceFileId.toString(), PdplIdentifierType.FILE_HANDLER_INTERFACE_FILE)
        );

        PersonalDataProcessingLogDetails logDetails = PersonalDataProcessingLogDetails.builder()
            .recipient(recipient)
            .businessIdentifier(BUSINESS_IDENTIFIER)
            .category(PersonalDataProcessingCategory.DISCLOSURE)
            .ipAddress(LogUtil.getIpAddress())
            .createdAt(OffsetDateTime.now(clock))
            .createdBy(createdBy)
            .individuals(individuals)
            .build();

        loggingService.personalDataAccessLogAsync(logDetails);
    }
}
