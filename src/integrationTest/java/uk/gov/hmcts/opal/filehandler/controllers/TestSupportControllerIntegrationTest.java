package uk.gov.hmcts.opal.filehandler.controllers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import uk.gov.hmcts.opal.filehandler.IntegrationSecurityConfiguration;
import uk.gov.hmcts.opal.filehandler.support.AbstractIntegrationTest;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraEpic;
import uk.hmcts.zephyr.automation.junit5.annotations.JiraStory;

@Slf4j(topic = "opal.TestSupportControllerIntegrationTest")
@ActiveProfiles(profiles = {"integration"})
@Import(IntegrationSecurityConfiguration.class)
public class TestSupportControllerIntegrationTest extends AbstractIntegrationTest {
    private final String url = "/test-support/automated-jobs/";
    @Autowired
    protected MockMvc mockMvc;


    @TestPropertySource(properties = {
        "opal.testing-support-endpoints.enabled=true"
    })
    @Nested
    class FeatureOn {
        @Test
        @DisplayName("PO-6454 - Feature flag on test")
        @JiraStory("PO-6454")
        @JiraEpic("PO-3497")
        void returns200WhenFeatureIsOn() throws Exception {
            ResultActions res = mockMvc.perform(
                post(url + "some-task")
            );

            res.andExpect(status().isOk());
        }
    }

    @TestPropertySource(properties = {
        "opal.testing-support-endpoints.enabled=false"
    })
    @Nested
    class FeatureOff {
        @Test
        @DisplayName("PO-6454 - Feature flag off test")
        @JiraStory("PO-6454")
        @JiraEpic("PO-3497")
        void returns404WhenFeatureIsOff() throws Exception {
            ResultActions res = mockMvc.perform(
                post(url + "some-task")
            );

            res.andExpect(status().isNotFound());
        }
    }
}
