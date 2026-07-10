package uk.gov.hmcts.opal.filehandler.service.request;

import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SearchInterfaceFilesDto {
    private Interface source;
    private Interface target;
    private Type type;
    private Domain domain;
    private Status status;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
