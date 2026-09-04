package uk.gov.hmcts.opal.filehandler.controllers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.entity.InterfaceFileEntity;
import uk.gov.hmcts.opal.filehandler.support.AbstractControllerIntegrationTest;
import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileTypeEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.StatusEnumInterfaceFile;

@Slf4j
@DisplayName("Get Interface File")
public class GetInterfaceFileTest extends AbstractControllerIntegrationTest {


    private static final String URI = "/interface-files/{id}";


    @TestPropertySource(properties = {
        "launchdarkly.enabled=false",
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true"
    })
    @Nested
    @DisplayName("Feature Flag On")
    class FeatureOn {

        private InterfaceFileObjectInterfaceFile buildExpectedResponse(InterfaceFileEntity interfaceFileEntity) {
            return InterfaceFileObjectInterfaceFile.builder()
                .interfaceFileId(interfaceFileEntity.getInterfaceFileId())
                .source(InterfaceFileEnumInterfaceFile.valueOf(interfaceFileEntity.getSource().name()))
                .target(InterfaceFileEnumInterfaceFile.valueOf(interfaceFileEntity.getTarget().name()))
                .type(InterfaceFileTypeEnumInterfaceFile.valueOf(interfaceFileEntity.getType().name()))
                .domain(DomainEnumTypes.valueOf(interfaceFileEntity.getOpalDomain().name()))
                .fileName(interfaceFileEntity.getFileName())
                .filestoreUuid(interfaceFileEntity.getFilestoreUuid())
                .status(StatusEnumInterfaceFile.valueOf(interfaceFileEntity.getStatus().name()))
                .createdDatetime(interfaceFileEntity.getCreatedDatetime())
                .build();
        }

        @Test
        @DisplayName("Given valid id When getInterfaceFile is called Then returns interface file 200")
        void givenValidDataAndCredentials_shouldReturnCorrectInformation() {
            InterfaceFileEntity interfaceFileEntity =
                interfaceFileEntityTestData.getTypicalInterfaceFile("some-file-name");
            InterfaceFileObjectInterfaceFile expectedResponse = buildExpectedResponse(interfaceFileEntity);

            setupApiTest(HttpMethod.GET, URI)
                .clearPermissions()
                .addPermission((short) 1, FileHandlerPermission.ViewInterfacesFile)
                .execute(interfaceFileEntity.getInterfaceFileId())
                .assertSuccess(HttpStatus.OK)
                .assertBody(expectedResponse);
        }

        @Test
        @DisplayName("Given valid id When getInterfaceFile is called but the user does not have the correct permission"
            + "Should reutrn 401")
        void givenValidDataButUserDoesNotHaveCorrectPermission_shouldReturnError() {
            InterfaceFileEntity interfaceFileEntity =
                interfaceFileEntityTestData.getTypicalInterfaceFile("some-file-name");
            setupApiTest(HttpMethod.GET, URI)
                .clearPermissions()
                .execute(interfaceFileEntity.getInterfaceFileId())
                .assertForbidden();
        }

        @Test
        @DisplayName("Given id is provided which does not exist then I should get a 404")
        void givenIdIsProvidedWhichDoesNotExistOnDb_shouldReturnError() {
            setupApiTest(HttpMethod.GET, URI)
                .clearPermissions()
                .addPermission((short) 1, FileHandlerPermission.ViewInterfacesFile)
                .execute(512)
                .assertNotFound("Interface file with id 512 could not be located.");
        }
    }

    @TestPropertySource(properties = {
        "launchdarkly.enabled=false",
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false"
    })
    @Nested
    class FeatureOff {

        @Test
        @DisplayName("API should return 404 when feature flag is off")
        void getInterfaceFile_shouldReturn404_whenFeatureFlagIsOff() throws Exception {
            setupApiTest(HttpMethod.GET, URI)
                .addPermission((short) 1, FileHandlerPermission.ViewInterfacesFile)
                .execute(1L)
                .assertFeatureFlagDisabledResponse();
        }
    }
}
