package uk.gov.hmcts.opal.filehandler.controllers;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;
import uk.gov.hmcts.opal.filehandler.service.InterfaceFilesService;
import uk.gov.hmcts.opal.filehandler.util.TaskRunnerUtil;
import uk.gov.hmcts.opal.generated.http.api.TestSupportApi;
import uk.gov.hmcts.opal.generated.model.DeleteInterfaceFilesResultTestSupport;

@Slf4j(topic = "opal.TestSupportController")
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "opal.testing-support-endpoints", name = "enabled", havingValue = "true")
public class TestSupportController implements TestSupportApi {

    private final TaskRunnerUtil taskRunnerUtil;
    private final InterfaceFilesService interfaceFilesService;

    @Override
    public ResponseEntity<Void> testingSupportAutomatedJobsNamePost(String name) {
        taskRunnerUtil.runAutomatedTask("automated" + name);

        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<DeleteInterfaceFilesResultTestSupport> testingSupportInterfaceFilesDelete(List<Long> ids) {
        log.warn("TEST ENDPOINT: Request to delete interface files with ids: {}", ids);
        DeleteInterfaceFilesResultTestSupport result = interfaceFilesService.deleteInterfaceFiles(ids);
        if (result.getFailedIds().size() == 0) {
            return ResponseEntity.ok(result);
        } else if (result.getSuccessfullyDeletedIds().size() > 0) {
            return ResponseEntity.status(HttpStatus.MULTI_STATUS).body(result);
        } else {
            throw new InternalServerErrorException("Failed to delete interface files",
                "Process exited cleanly but errors were encountered. See logs for details");
        }
    }
}
