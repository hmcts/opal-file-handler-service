package uk.gov.hmcts.opal.filehandler.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Retrieves interface-file metadata and content using the scenario's authenticated requests. */
public class BaisInterfaceFileClient {
    private static final Duration INGESTION_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(500);
    private final Supplier<RequestSpecification> requests;
    private final String baseUrl;

    public BaisInterfaceFileClient(Supplier<RequestSpecification> requests, String baseUrl) {
        this.requests = requests;
        this.baseUrl = baseUrl;
    }

    public Response content(Map<String, Object> metadata) {
        Response response = requests.get().accept("application/octet-stream").when()
            .get(baseUrl + "/interface-files/" + metadata.get("interface_file_id") + "/content");
        assertEquals(200, response.statusCode(), "Extracted JSON could not be retrieved");
        return response;
    }

    public Map<String, Object> awaitInterfaceFile(
        BaisFileTestConfig config,
        String type,
        String status
    ) {
        long deadline = System.nanoTime() + INGESTION_TIMEOUT.toNanos();
        List<Map<String, Object>> matchingRecords = List.of();
        do {
            Response response = requests.get()
                .queryParam("source", config.source())
                .when()
                .get(baseUrl + "/interface-files");
            assertEquals(200, response.statusCode(), "Interface-file metadata could not be retrieved");

            matchingRecords = response.jsonPath()
                .<Map<String, Object>>getList("interface_files")
                .stream()
                .filter(record -> config.fileName().equals(record.get("file_name")))
                .toList();
            List<Map<String, Object>> successfulRecords = matchingRecords.stream()
                .filter(record -> type.equals(record.get("type")))
                .filter(record -> status.equals(record.get("status")))
                .toList();
            if (!successfulRecords.isEmpty()) {
                return successfulRecords.getFirst();
            }
            pauseBeforeRetry();
        } while (System.nanoTime() < deadline);

        throw new AssertionError(
            "Expected a " + status + " " + config.displayName() + " interface-file record within "
                + INGESTION_TIMEOUT.toSeconds() + " seconds; matching records: " + matchingRecords
        );
    }

    private static void pauseBeforeRetry() {
        try {
            Thread.sleep(POLL_INTERVAL.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for interface-file metadata", exception);
        }
    }

}
