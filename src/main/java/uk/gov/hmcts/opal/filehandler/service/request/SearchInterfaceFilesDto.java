package uk.gov.hmcts.opal.filehandler.service.request;

import java.time.LocalDateTime;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.opal.filehandler.entity.Domain;
import uk.gov.hmcts.opal.filehandler.entity.Interface;
import uk.gov.hmcts.opal.filehandler.entity.Status;
import uk.gov.hmcts.opal.filehandler.entity.Type;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class SearchInterfaceFilesDto {
    private Interface source;
    private Interface target;
    private Interface notTarget;
    private Set<Type> types;
    private Domain domain;
    private Status status;
    private Set<Status> notStatuses;
    private String businessUnitCode;
    private LocalDateTime fromDate;
    private LocalDateTime toDate;
}
