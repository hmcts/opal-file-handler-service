package uk.gov.hmcts.opal.filehandler.service.response;

import java.util.Collection;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
@Getter
public class DeleteInterfaceFilesResult { @Getter // TODO delete as now replaced by file generated from open api spec
    @NonNull
    private final Collection<Long> successfullyDeletedIds;
    @NonNull
    private final Collection<Long> failedIds;
}
