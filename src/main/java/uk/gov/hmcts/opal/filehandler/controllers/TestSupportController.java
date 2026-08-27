package uk.gov.hmcts.opal.filehandler.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.opal.filehandler.util.TaskRunnerUtil;
import uk.gov.hmcts.opal.generated.http.api.TestSupportApi;

@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "opal.testing-support-endpoints", name = "enabled", havingValue = "true")
@Slf4j
public class TestSupportController implements TestSupportApi {

    private final TaskRunnerUtil taskRunnerUtil;

    @Override
    public ResponseEntity<Void> testingSupportAutomatedJobsNamePost(String name) {
        taskRunnerUtil.runAutomatedTask("automated" + name);

        return ResponseEntity.ok().build();
    }
}
