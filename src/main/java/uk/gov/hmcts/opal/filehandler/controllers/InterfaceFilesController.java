package uk.gov.hmcts.opal.filehandler.controllers;

import static uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags.RELEASE_1C_AUTO_ENFORCEMENT_CONFIG;
import static uk.gov.hmcts.opal.common.launchdarkly.FeatureFlags.RELEASE_1C_AUTO_ENFORCEMENT_CONFIG_ENABLED_PROPERTY;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.opal.common.launchdarkly.FeatureToggle;
import uk.gov.hmcts.opal.filehandler.mapper.SearchInterfaceFilesDtoMapper;
import uk.gov.hmcts.opal.filehandler.service.InterfaceFilesService;
import uk.gov.hmcts.opal.filehandler.service.request.SearchInterfaceFilesDto;
import uk.gov.hmcts.opal.generated.http.api.InterfaceFilesApi;
import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;
import uk.gov.hmcts.opal.generated.model.GetInterfaceFiles200Response;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileTypeEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.StatusEnumInterfaceFile;

@RestController
@Slf4j(topic = "opal.InterfaceFilesController")
@RequiredArgsConstructor
@Tag(name = "Interface Files Controller")
public class InterfaceFilesController implements InterfaceFilesApi {
    private final SearchInterfaceFilesDtoMapper searchMapper;
    private final InterfaceFilesService service;

    @FeatureToggle(
        feature = RELEASE_1C_AUTO_ENFORCEMENT_CONFIG,
        defaultValueProperty = RELEASE_1C_AUTO_ENFORCEMENT_CONFIG_ENABLED_PROPERTY
    )
    @Override
    public ResponseEntity<GetInterfaceFiles200Response> getInterfaceFiles(
        @Nullable InterfaceFileEnumInterfaceFile source,
        @Nullable InterfaceFileEnumInterfaceFile target,
        @Nullable InterfaceFileTypeEnumInterfaceFile type,
        @Nullable DomainEnumTypes domain,
        @Nullable StatusEnumInterfaceFile status,
        @Nullable LocalDateTime fromDate,
        @Nullable LocalDateTime toDate) {

        SearchInterfaceFilesDto searchDto = searchMapper.toSearchInterfaceFilesDto(
            source, target, type, domain, status, fromDate, toDate
        );
        List<InterfaceFileObjectInterfaceFile> interfaceFileObjects = service.searchInterfaceFiles(searchDto);
        GetInterfaceFiles200Response response = GetInterfaceFiles200Response.builder()
            .interfaceFiles(interfaceFileObjects)
            .numberOfResults(interfaceFileObjects.size())
            .build();

        return ResponseEntity.ok(response);
    }
}
