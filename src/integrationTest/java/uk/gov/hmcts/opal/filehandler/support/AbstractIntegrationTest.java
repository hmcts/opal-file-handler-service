package uk.gov.hmcts.opal.filehandler.support;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.opal.filehandler.Application;
import uk.hmcts.zephyr.automation.junit5.extension.ZephyrAutomationExtension;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("integration")
@ContextConfiguration(classes = {TestContainerConfig.class})
@AutoConfigureMockMvc(htmlUnit = @AutoConfigureMockMvc.HtmlUnit(webClient = false, webDriver = false))
@ExtendWith(ZephyrAutomationExtension.class)
@Slf4j
public class AbstractIntegrationTest {

    private static final int WIREMOCK_PORT = 4553;
    private static final WireMockServer WIREMOCK_SERVER = new WireMockServer(options().port(WIREMOCK_PORT));

    protected UserStateStub userStateStub;

    @Autowired
    protected MockMvc mockMvc;

    @BeforeAll
    static void startWireMock() {
        WIREMOCK_SERVER.start();
    }

    @AfterAll
    static void stopWireMock() {
        WIREMOCK_SERVER.stop();
    }

    @BeforeEach
    public void beforeEach() {
        resetWireMock();
        userStateStub = createUserStateStub();
    }

    @SneakyThrows
    private void resetWireMock() {
        WireMock.configureFor("localhost", WIREMOCK_PORT);
        try {
            WireMock.reset();
        } catch (RuntimeException ex) {
            log.error("Wiremock failed to reset", ex);
        }
    }

    protected UserStateStub createUserStateStub() {
        return new UserStateStub();
    }

    // Dynamically register properties to configure the datasource
    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestContainerConfig.POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", TestContainerConfig.POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", TestContainerConfig.POSTGRES_CONTAINER::getPassword);
        registry.add("spring.data.redis.url", TestContainerConfig.REDIS_CONTAINER::getRedisURI);
    }
}
