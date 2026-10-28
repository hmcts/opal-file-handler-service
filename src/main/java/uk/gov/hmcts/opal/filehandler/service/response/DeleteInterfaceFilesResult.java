package uk.gov.hmcts.opal.filehandler.service.response;

import java.util.Collection;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Builder
@RequiredArgsConstructor
public class DeleteInterfaceFilesResult {
    @NonNull
    private final Collection<Long> successfullyDeletedIds;
    @NonNull
    private final Collection<Long> ignoredIds;
    @NonNull
    private final Collection<Long> failedIds;
}
