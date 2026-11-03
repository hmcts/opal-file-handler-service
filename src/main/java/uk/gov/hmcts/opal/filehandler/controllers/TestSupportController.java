package uk.gov.hmcts.opal.filehandler.controllers;

import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
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
        DeleteInterfaceFilesResultTestSupport result = interfaceFilesService.deleteInterfaceFiles(new HashSet<>(ids));
        return ResponseEntity.ok(result);
    }
}
