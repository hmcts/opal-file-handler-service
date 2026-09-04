package uk.gov.hmcts.opal.filehandler.support;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.authorisation.FileHandlerPermission;
import uk.gov.hmcts.opal.generated.model.InterfaceFileObjectInterfaceFile;

@RequiredArgsConstructor
public class ApiTest {

    private final ObjectMapper objectMapper;
    private final MockMvc mockMvc;
    private final HttpMethod method;
    private final String uriTemplate;
    private final UserStateStub userStateStub = new UserStateStub();
    private MockHttpServletRequestBuilder requestBuilder;
    private boolean addAuthorisationHeader = true;

    public ApiTest clearPermissions() {
        userStateStub.setupWithNoPermissions();
        return this;
    }

    public ApiTest addPermission(short businessUnit, FileHandlerPermission permission) {
        userStateStub.addPermissions(businessUnit, permission);
        return this;
    }

    public ApiTest excludeAuthorisationHeader() {
        addAuthorisationHeader = false;
        return this;
    }

    public ApiTest build(Object... uriVariables) {
        requestBuilder = MockMvcRequestBuilders.request(
            method,
            uriTemplate,
            uriVariables
        );
        if (addAuthorisationHeader) {
            requestBuilder.with(userStateStub.getAuthenticaitonRequestPostProcessor())
                .header("Authorization", userStateStub.getBearerToken());
        }
        return this;
    }

    @SneakyThrows
    public ApiTest.Response execute(Object... uriVariables) {
        if (requestBuilder == null) {
            build(uriVariables);
        }
        return new Response(mockMvc.perform(requestBuilder));
    }

    @RequiredArgsConstructor
    public class Response {

        private final ResultActions resultActions;

        public Response assertFeatureFlagDisabledResponse() {
            assertResponse(status().isNotFound());
            assertResponse(jsonPath("$.detail")
                .value("The requested feature is not currently available"));
            assertResponse(jsonPath("$.title")
                .value("Feature Disabled"));
            assertResponse(jsonPath("$.operation_id").exists());
            return this;
        }

        public Response assertSuccess(HttpStatus httpStatus) {
            assertResponse(status().is(httpStatus.value()));
            assertResponse(content().contentType(MediaType.APPLICATION_JSON));
            return this;
        }

        public Response assertError(HttpStatus httpStatus) {
            assertResponse(status().is(httpStatus.value()));
            assertResponse(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
            return this;
        }

        public Response assertStatus(HttpStatus httpStatus) {
            return assertResponse(status().is(httpStatus.value()));
        }

        @SneakyThrows
        public Response assertResponse(ResultMatcher resultMatcher) {
            resultActions.andExpect(resultMatcher);
            return this;
        }

        //Only to be used for debugging purposes, as it will print the response to the console
        //Do not commit this in any integration tests
        public Response logResponse() throws Exception {
            resultActions.andDo(result -> {
                System.out.println("Response status: " + result.getResponse().getStatus());
                System.out.println("Response body: " + result.getResponse().getContentAsString());
            });
            return this;
        }

        @SneakyThrows
        public <T> T getResponseBodyAsObject(Class<T> interfaceFileObjectInterfaceFileClass) {
            return objectMapper.convertValue(resultActions.andReturn().getResponse()
                    .getContentAsString(),
                interfaceFileObjectInterfaceFileClass);
        }

        public Response assertBody(InterfaceFileObjectInterfaceFile expectedResponse) {
            String expectedJson = objectMapper.writeValueAsString(expectedResponse);
            assertResponse(content().json(expectedJson, JsonCompareMode.STRICT));
            return this;
        }

        public Response assertForbidden() {
            assertError(HttpStatus.FORBIDDEN);
            assertResponse(jsonPath("$.detail")
                .value("You do not have permission to access this resource"));
            assertResponse(jsonPath("$.title")
                .value("Forbidden"));
            assertResponse(jsonPath("$.operation_id").exists());

            assertResponse(jsonPath("$.retriable").value(false));
            return this;
        }


        public Response assertNotFound(String detail) {
            assertError(HttpStatus.NOT_FOUND);
            assertResponse(jsonPath("$.detail")
                .value(detail));
            assertResponse(jsonPath("$.title")
                .value("Not Found"));
            assertResponse(jsonPath("$.operation_id").exists());

            assertResponse(jsonPath("$.retriable").value(false));
            return this;
        }
    }
}
