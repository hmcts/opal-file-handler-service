package uk.gov.hmcts.opal.filehandler.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.io.Resources;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.opal.common.user.authentication.service.AccessTokenService;
import uk.gov.hmcts.opal.common.user.authorisation.client.service.UserStateClientService;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.support.AbstractIntegrationTest;
import uk.gov.hmcts.opal.filehandler.support.UtilBlobStoreService;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraEpic;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraStory;

@Slf4j(topic = "opal.GetInterfaceFilesContentIntegrationTest")
@ActiveProfiles(profiles = {"integration"})
public class GetInterfaceFilesContentTest extends AbstractIntegrationTest {

    private String urlWithID(long id) {
        return String.format("/interface-files/%d/content", id);
    }

    @MockitoBean
    UserStateClientService userStateClientService;

    @MockitoBean
    protected AccessTokenService accessTokenService;


    private static UtilBlobStoreService utilBlobStoreService;

    protected void authorizeWithPermission(short businessUnitId) {
        userStateStub.setupWithNoPermissions();
        userStateStub.addPermissions(businessUnitId, FileHandlerPermission.ViewInterfacesFile);
    }

    protected void authoriseNoPermissions() {
        userStateStub.setupWithNoPermissions();
    }

    protected void assertBlobStorageUnchanged() {
        assertEquals(
            bteckohReportOriginalVersion,
            utilBlobStoreService.getBlobVersion("bteckoh-report", "0f664b85-a5df-4600-9bb3-3b7092ab8718")
        );
        assertEquals(
            capsReportOriginalVersion,
            utilBlobStoreService.getBlobVersion("caps-report", "73c21773-6f49-438d-a760-78f0ffbedf0d")
        );
    }

    private static String bteckohReportOriginalVersion;
    private static String capsReportOriginalVersion;

    @BeforeAll
    public static void setupAzureData() throws IOException {
        utilBlobStoreService = new UtilBlobStoreService();

        URL url = Resources.getResource(
            "azure/data/bteckoh-report/2498-MCPLDB-MOJ-Payments-Report-Daily-2026-07-06-06-00-18.xlsx");
        String resource = Resources.toString(url, StandardCharsets.UTF_8);
        bteckohReportOriginalVersion = utilBlobStoreService.storeReport(resource, "bteckoh-report", "0f664b85-a5df-4600-9bb3-3b7092ab8718");

        url = Resources.getResource("azure/data/caps-report/CapFa.GB.20260701.173024.xml");
        resource = Resources.toString(url, StandardCharsets.UTF_8);
        capsReportOriginalVersion = utilBlobStoreService.storeReport(resource, "caps-report", "73c21773-6f49-438d-a760-78f0ffbedf0d");
    }

    @Sql(
        scripts = "classpath:db/insertData/insert_into_interface_files.sql",
        executionPhase = ExecutionPhase.BEFORE_TEST_CLASS
    )
    @Sql(
        scripts = "classpath:db/deleteData/delete_from_interface_files.sql",
        executionPhase = ExecutionPhase.AFTER_TEST_CLASS
    )
    @TestPropertySource(properties = {
        "launchdarkly.enabled=false",
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true"
    })
    @Nested
    class FeatureOn {

        @Test
        @DisplayName("OPAL: GET Interface File Content - Fetches file content")
        @JiraStory("PO-3948")
        @JiraEpic("PO-3495")
        void get_respondsWith200AndFileContents() throws Exception {
            authorizeWithPermission((short) 78); // Auto enforcement permission

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userStateStub.getBearerToken());
            headers.add("Business-Unit-Id", "78");

            ResultActions res = mockMvc.perform(
                get(urlWithID(1L))
                    .with(userStateStub.getAuthenticaitonRequestPostProcessor())
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
            );

            res.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM)); // TODO: Is this correct

            assertBlobStorageUnchanged();
        }

        @Test
        @DisplayName("OPAL: GET Interface File Content - Returns status 404 when IF cannot be found")
        @JiraStory("PO-3948")
        @JiraEpic("PO-3495")
        void get_respondsWith404WhenNotInDB() throws Exception {
            authorizeWithPermission((short) 78);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userStateStub.getBearerToken());
            headers.add("Business-Unit-Id", "78");

            ResultActions res = mockMvc.perform(
                get(urlWithID(1000L))
                    .with(userStateStub.getAuthenticaitonRequestPostProcessor())
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            );

            res.andExpect(status().isNotFound())
                .andExpect(jsonPath("$.reason")
                    .value("Interface file with id 1000 could not be located."));

            assertBlobStorageUnchanged();
        }

        @Test
        @DisplayName("OPAL: GET Interface File Content - Returns status 422 when IF status is invalid")
        @JiraStory("PO-3948")
        @JiraEpic("PO-3495")
        void get_respondsWith422WithInvalidStatus() throws Exception {
            authorizeWithPermission((short) 78);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userStateStub.getBearerToken());
            headers.add("Business-Unit-Id", "78");

            ResultActions res = mockMvc.perform(
                get(urlWithID(3L))
                    .with(userStateStub.getAuthenticaitonRequestPostProcessor())
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            );

            res.andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail")
                    .value("Interface file with id 3 could not be retrieved as it has an invalid status of:"
                        + " \"FAILED\" only files with status: \"SUCCESS\" can be returned."));

            assertBlobStorageUnchanged();
        }

        @Test
        @DisplayName("OPAL: GET Interface File Content - Returns status 500 when IF Blob is not found")
        @JiraStory("PO-3948")
        @JiraEpic("PO-3495")
        void get_respondsWith500WhenBlobNotFound() throws Exception {
            authorizeWithPermission((short) 78);

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userStateStub.getBearerToken());
            headers.add("Business-Unit-Id", "78");

            ResultActions res = mockMvc.perform(
                get(urlWithID(4L))
                    .with(userStateStub.getAuthenticaitonRequestPostProcessor())
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            );

            res.andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail")
                    .value("Expected interface file id: 4 to exist in blobstore "
                        + "container: \"bteckoh-report\" with name \"b5fed320-1ad1-47f5-8786-91ba31f1604d\" but this could "
                        + "not be located."));

            assertBlobStorageUnchanged();
        }

        @Test
        @DisplayName("OPAL: GET Interface File Content - Returns status 403 when permissions are missing")
        @JiraStory("PO-3948")
        @JiraEpic("PO-3495")
        void get_respondsWith403WhenPermissionsMissing() throws Exception {
            authoriseNoPermissions();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userStateStub.getBearerToken());
            headers.add("Business-Unit-Id", "78");

            ResultActions res = mockMvc.perform(
                get(urlWithID(4L))
                    .with(userStateStub.getAuthenticaitonRequestPostProcessor())
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
            );

            res.andExpect(status().isForbidden());

            assertBlobStorageUnchanged();
        }
    }

    @TestPropertySource(properties = {
        "launchdarkly.enabled=false",
        "launchdarkly.default-flag-values.release-1c-auto-enforcement-config=false"
    })
    @Nested
    class FeatureOff {
        @Test
        @DisplayName("PO-3948 - Feature flag off test")
        @JiraStory("PO-3948")
        @JiraEpic("PO-3495")
        void getAllEnforcementAccountTypes_FeatureOff_404() throws Exception {
            authorizeWithPermission((short) 78); // Auto enforcement permission

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(userStateStub.getBearerToken());
            headers.add("Business-Unit-Id", "78");

            ResultActions res = mockMvc.perform(
                get(urlWithID(1L))
                    .with(userStateStub.getAuthenticaitonRequestPostProcessor())
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_JSON)
            );

            res.andExpect(status().isNotFound());
        }
    }

}
