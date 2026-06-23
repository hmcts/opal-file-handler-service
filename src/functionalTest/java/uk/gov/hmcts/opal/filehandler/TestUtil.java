package uk.gov.hmcts.opal.filehandler;

public class TestUtil {

    public static String getTestUrl() {
        String testUrl = System.getenv("TEST_URL");
        if (testUrl == null || testUrl.isBlank()) {
            testUrl = "http://localhost:4075";
        }
        return testUrl;
    }
}
