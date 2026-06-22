package uk.gov.hmcts.opal.filehandling.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.opal.filehandling.support.AbstractIntegrationTest;

@Slf4j(topic = "opal.GetWelcomeTest")
@DisplayName("Get Welcome Test Integration Tests")
class GetWelcomeTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("Should welcome upon root request with 200 response code")
    @Test
    void welcomeRootEndpoint() throws Exception {
        MvcResult response = mockMvc.perform(get("/")
                .with(userStateStub.getAuthenticaitonRequestPostProcessor())
                .header("authorization", userStateStub.getBearerToken())
            )
            .andExpect(status().isOk())
            .andReturn();

        assertThat(response.getResponse().getContentAsString()).startsWith("Welcome");
    }
}
