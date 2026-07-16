package uk.gov.hmcts.opal.filehandler.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureToggle;
import uk.gov.hmcts.opal.filehandler.service.InterfaceFileService;
import uk.gov.hmcts.opal.generated.http.api.InterfaceFilesApi;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags;

@RestController
@Slf4j(topic = "opal.InterfaceFilesController")
@RequiredArgsConstructor
@Tag(name = "Interface Files Controller")
public class InterfaceFilesController implements InterfaceFilesApi {

    private final InterfaceFileService interfaceFilesService;

    @FeatureToggle(feature = FeatureFlags.RELEASE_1C_BANKING_INTERFACES,
        defaultValueProperty = FeatureFlags.RELEASE_1C_BANKING_INTERFACES_ENABLED_PROPERTY)
    @Override
    public ResponseEntity<Resource> getInterfaceFileContent(Long id) {
        InputStream stream = interfaceFilesService.GetInterfaceFilesContent(id);

        return ResponseEntity.ok(new InputStreamResource(stream));
    }
}
