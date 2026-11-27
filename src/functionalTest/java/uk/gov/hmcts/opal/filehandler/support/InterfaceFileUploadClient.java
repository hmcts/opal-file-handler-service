package uk.gov.hmcts.opal.filehandler.support;

import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.nio.charset.StandardCharsets;

/** Multipart contract for Add Interface Files, using the caller's authenticated request. */
public final class InterfaceFileUploadClient {
    private InterfaceFileUploadClient() {
    }

    public static Response upload(RequestSpecification request, String baseUrl, String fileName,
                                  byte[] content, String metadata) {
        return request.contentType("multipart/form-data")
            .multiPart("file", fileName, content, "application/octet-stream")
            .multiPart("metadata", "metadata.json", metadata.getBytes(StandardCharsets.UTF_8), "application/json")
            .when().post(baseUrl + "/interface-files");
    }
}
