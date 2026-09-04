package uk.gov.hmcts.opal.filehandler.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.opal.filehandler.repository.InterfaceFilesRepository;
import uk.gov.hmcts.opal.filehandler.testdata.InterfaceFileEntityTestData;

public class AbstractControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    public MockMvc mockMvc;
    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected InterfaceFileEntityTestData interfaceFileEntityTestData;

    @Autowired
    private InterfaceFilesRepository interfaceFilesRepository;

    @BeforeEach
    void setUp() {
        interfaceFilesRepository.deleteAll();
    }

    protected ApiTest setupApiTest(HttpMethod method, String uriTemplate) {
        return new ApiTest(objectMapper, mockMvc, method, uriTemplate);
    }
}
