package uk.gov.hmcts.opal.filehandler.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_CLASS;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_CLASS;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.filehandler.support.AbstractIntegrationTest;
import uk.gov.hmcts.opal.generated.model.DomainEnumTypes;
import uk.gov.hmcts.opal.generated.model.GetInterfaceFiles200Response;
import uk.gov.hmcts.opal.generated.model.InterfaceFileEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;
import uk.gov.hmcts.opal.generated.model.InterfaceFileTypeEnumInterfaceFile;
import uk.gov.hmcts.opal.generated.model.StatusEnumInterfaceFile;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraEpic;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraStory;

@Slf4j(topic = "opal.GetInterfaceFilesTest")
@DisplayName("Get Interface Files Integration Tests")
public class GetInterfaceFilesTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    private static final String URL = "/interface-files";

    @TestPropertySource(properties = {
        "launchdarkly.enabled=false",
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=true"
    })
    @Sql(scripts = "classpath:db/insertData/insert_into_interface_files.sql", executionPhase = BEFORE_TEST_CLASS)
    @Sql(scripts = "classpath:db/deleteData/delete_from_interface_files.sql", executionPhase = AFTER_TEST_CLASS)
    @Nested
    class FeatureOn {

        @Test
        @DisplayName("PO-3947 - Returns interface files correctly")
        @JiraStory("PO-3947")
        @JiraEpic("PO-3495")
        void returnsAllInterfaceFiles_200() throws Exception {
            setupAuthorisedUser();
            ResultActions result = mockMvc.perform(
                get(URL)
                    .with(userStateStub.getAuthenticaitonRequestPostProcessor())
                    .header(HttpHeaders.AUTHORIZATION, userStateStub.getBearerToken())
            );

            String body = result.andReturn().getResponse().getContentAsString();
            result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            GetInterfaceFiles200Response response = objectMapper.readValue(body, new TypeReference<>() {});
            List<InterfaceFileObjectInterfaceFile> interfaceFiles = response.getInterfaceFiles();
            UUID failedFilestoreUuid = UUID.fromString("a5695e1e-bd9f-4a5b-ae15-9deeed2d1384");
            LocalDateTime failedFileCreatedDate = LocalDateTime.of(
                2026, 1, 4, 12, 30, 0
            );
            // Just picking one row we know is there and checking that all fields are correct
            // THe other tests will test the filtering on the endpoint
            InterfaceFileObjectInterfaceFile failedFile = interfaceFiles.stream()
                .filter(f -> f.getFilestoreUuid().equals(failedFilestoreUuid))
                    .findAny()
                        .orElse(null);

            assertThat(interfaceFiles).hasSizeGreaterThanOrEqualTo(4);
            assertAll(
                () -> assertNotNull(failedFile),
                () -> assertEquals(InterfaceFileEnumInterfaceFile.OPAL, failedFile.getTarget()),
                () -> assertEquals(InterfaceFileEnumInterfaceFile.BTECKOH_REPORT, failedFile.getSource()),
                () -> assertEquals(InterfaceFileTypeEnumInterfaceFile.SOURCE, failedFile.getType()),
                () -> assertEquals(DomainEnumTypes.FILE_HANDLER, failedFile.getDomain()),
                () -> assertEquals(StatusEnumInterfaceFile.FAILED, failedFile.getStatus()),
                () -> assertEquals("2500-Payments-Report-Daily.xlsx", failedFile.getFileName()),
                () -> assertEquals("{\"error\":\"malformed xlsx\"}", failedFile.getErrors()),
                () -> assertEquals(failedFileCreatedDate, failedFile.getCreatedDatetime()),
                () -> assertNull(failedFile.getChecksum())
            );
        }

        @Test
        @DisplayName("PO-3947 - Filters interface files correctly by status and source")
        @JiraStory("PO-3947")
        @JiraEpic("PO-3495")
        void filtersInterfaceFilesCorrectlyBySourceAndStatus_200() throws Exception {
            setupAuthorisedUser();
            ResultActions result = mockMvc.perform(
                get(URL)
                    .with(userStateStub.getAuthenticaitonRequestPostProcessor())
                    .header(HttpHeaders.AUTHORIZATION, userStateStub.getBearerToken())
                    .param("source", InterfaceFileEnumInterfaceFile.CAPS_REPORT.getValue())
                    .param("status", StatusEnumInterfaceFile.SUCCESS.getValue())
            );

            String body = result.andReturn().getResponse().getContentAsString();
            result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            GetInterfaceFiles200Response response = objectMapper.readValue(body, new TypeReference<>() {});

            assertThat(response.getInterfaceFiles()).hasSizeGreaterThanOrEqualTo(1);
            assertThat(response.getInterfaceFiles()).allMatch(
                i -> i.getStatus() == StatusEnumInterfaceFile.SUCCESS &&
                    i.getSource() == InterfaceFileEnumInterfaceFile.CAPS_REPORT
            );
        }

        @Test
        @DisplayName("PO-3947 - Filters interface files correctly by target and type")
        @JiraStory("PO-3947")
        @JiraEpic("PO-3495")
        void filtersInterfaceFilesCorrectlyByTargetAndType_200() throws Exception {
            setupAuthorisedUser();
            ResultActions result = mockMvc.perform(
                get(URL)
                    .with(userStateStub.getAuthenticaitonRequestPostProcessor())
                    .header(HttpHeaders.AUTHORIZATION, userStateStub.getBearerToken())
                    .param("target", InterfaceFileEnumInterfaceFile.OPAL.getValue())
                    .param("type", InterfaceFileTypeEnumInterfaceFile.SOURCE.getValue())
            );

            String body = result.andReturn().getResponse().getContentAsString();
            result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            GetInterfaceFiles200Response response = objectMapper.readValue(body, new TypeReference<>() {});

            assertThat(response.getInterfaceFiles()).hasSizeGreaterThanOrEqualTo(1);
            assertThat(response.getInterfaceFiles()).allMatch(
                i -> i.getTarget() == InterfaceFileEnumInterfaceFile.OPAL &&
                    i.getType() == InterfaceFileTypeEnumInterfaceFile.SOURCE);
        }


        @Test
        @DisplayName("PO-3947 - Filters interface files correctly by domain")
        @JiraStory("PO-3947")
        @JiraEpic("PO-3495")
        void filtersInterfaceFilesCorrectlyByDomain_200() throws Exception {
            setupAuthorisedUser();
            ResultActions result = mockMvc.perform(
                get(URL)
                    .with(userStateStub.getAuthenticaitonRequestPostProcessor())
                    .header(HttpHeaders.AUTHORIZATION, userStateStub.getBearerToken())
                    .param("domain", DomainEnumTypes.FINES.getValue())
            );

            String body = result.andReturn().getResponse().getContentAsString();
            result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            GetInterfaceFiles200Response response = objectMapper.readValue(body, new TypeReference<>() {});

            assertThat(response.getInterfaceFiles()).hasSizeGreaterThanOrEqualTo(1);
            assertThat(response.getInterfaceFiles()).allMatch(i -> i.getDomain() == DomainEnumTypes.FINES);
        }

        @Test
        @DisplayName("PO-3947 - Filters interface files correctly by to and from dates")
        @JiraStory("PO-3947")
        @JiraEpic("PO-3495")
        void filtersInterfaceFilesCorrectlyByDates_200() throws Exception {
            LocalDateTime fromDate = LocalDateTime.of(2025, Month.DECEMBER, 30, 0, 0);
            LocalDateTime toDate = LocalDateTime.of(2026, Month.JANUARY, 4, 12, 30);

            setupAuthorisedUser();
            ResultActions result = mockMvc.perform(
                get(URL)
                    .with(userStateStub.getAuthenticaitonRequestPostProcessor())
                    .header(HttpHeaders.AUTHORIZATION, userStateStub.getBearerToken())
                    .param("from_date", fromDate.toString())
                    .param("to_date", toDate.toString())
            );

            String body = result.andReturn().getResponse().getContentAsString();
            result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            GetInterfaceFiles200Response response = objectMapper.readValue(body, new TypeReference<>() {});

            assertThat(response.getInterfaceFiles()).hasSizeGreaterThanOrEqualTo(1);
            assertThat(response.getInterfaceFiles()).allMatch(i -> {
                    LocalDateTime createdDate = i.getCreatedDatetime();
                    return (createdDate.equals(fromDate) || createdDate.isAfter(fromDate)) &&
                        (createdDate.equals(toDate) || createdDate.isBefore(toDate));
            });
        }

        @Test
        @DisplayName("PO-3947 – Forbidden without View Interface Files permission")
        @JiraStory("PO-3947")
        @JiraEpic("PO-3495")
        void forbiddenWithoutAutoEnforcementPermission() throws Exception {
            userStateStub.setupWithNoPermissions();
            ResultActions result = mockMvc.perform(
                get(URL)
                    .with(userStateStub.getAuthenticaitonRequestPostProcessor())
                    .header(HttpHeaders.AUTHORIZATION, userStateStub.getBearerToken())
            );

            result.andExpect(status().isForbidden());
        }
    }

    @TestPropertySource(properties = {
        "launchdarkly.enabled=false",
        "launchdarkly.default-flag-values.release-1c-banking-interfaces=false"
    })
    @Nested
    class FeatureOff {

        @Test
        @DisplayName("PO-3947 - Feature flag off test")
        @JiraStory("PO-3947")
        @JiraEpic("PO-3495")
        void getInterfaceFiles_FeatureOff_404() throws Exception {
            setupAuthorisedUser();
            ResultActions result = mockMvc.perform(
                get(URL)
                    .with(userStateStub.getAuthenticaitonRequestPostProcessor())
                    .header(HttpHeaders.AUTHORIZATION, userStateStub.getBearerToken())
            );

            result.andExpect(status().isNotFound());
        }
    }

    private void setupAuthorisedUser() {
        userStateStub.setupWithNoPermissions();
        userStateStub.addPermissions((short) 1, FileHandlerPermission.ViewInterfacesFile);
    }
}
