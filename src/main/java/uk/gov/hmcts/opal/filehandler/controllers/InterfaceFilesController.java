package uk.gov.hmcts.opal.filehandler.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.opal.generated.http.api.InterfaceFilesApi;

@RestController
@Slf4j(topic = "opal.InterfaceFilesController")
@RequiredArgsConstructor
@Tag(name = "Interface Files Controller")
public class InterfaceFilesController implements InterfaceFilesApi {

}
