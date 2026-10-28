package uk.gov.hmcts.opal.filehandler.controllers;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.opal.filehandler.service.InterfaceFilesService;
import uk.gov.hmcts.opal.filehandler.service.response.DeleteInterfaceFilesResult;
import uk.gov.hmcts.opal.filehandler.util.TaskRunnerUtil;
import uk.gov.hmcts.opal.generated.http.api.TestSupportApi;

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

    @DeleteMapping("/testing-support/interface-files")
    @Operation(summary = "Deletes a list of Interface jobs. FOR TESTING ONLY!")
    public ResponseEntity<DeleteInterfaceFilesResult> deleteInterfaceFiles(@RequestParam(value = "ids") List<Long> ids) {
        log.warn("TEST ENDPOINT: Request to delete interface files with ids: {}", ids);
        DeleteInterfaceFilesResult result = interfaceFilesService.deleteInterfaceFiles(Set.of(ids.toArray(Long[]::new)));
        return ResponseEntity.ok(result);
    }
}
