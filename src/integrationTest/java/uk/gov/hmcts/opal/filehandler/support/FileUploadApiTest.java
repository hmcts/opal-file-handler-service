package uk.gov.hmcts.opal.filehandler.support;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import lombok.SneakyThrows;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.ObjectMapper;

public class FileUploadApiTest extends ApiTest {

    public FileUploadApiTest(ObjectMapper objectMapper, MockMvc mockMvc, HttpMethod method, String uriTemplate) {
        super(objectMapper, mockMvc, method, uriTemplate);
    }

    private MockMultipartHttpServletRequestBuilder requestBuilder;
    private List<MockMultipartFile> includedFiles = new ArrayList<>();
    private boolean includeContentDigestHeader = false;

    public FileUploadApiTest includeMultipartBody(
        String filename, String originalFilename, String contentType, byte[] fileContents) {
        includedFiles.add(new MockMultipartFile(filename, originalFilename, contentType, fileContents));
        return this;
    }

    public FileUploadApiTest includeContentDigest() {
        includeContentDigestHeader = true;
        return this;
    }


    @Override
    @SneakyThrows
    public FileUploadApiTest build(Object... uriVariables) {
        requestBuilder = MockMvcRequestBuilders.multipart(
            method,
            uriTemplate,
            uriVariables
        ).contentType(MediaType.MULTIPART_FORM_DATA_VALUE);
        if (addAuthorisationHeader) {
            requestBuilder.with(userStateStub.getAuthenticaitonRequestPostProcessor())
                .header("Authorization", userStateStub.getBearerToken());
        }

        if (includeContentDigestHeader) {
            byte[] digest = MessageDigest.getInstance("SHA-512").digest(new byte[]{});
            requestBuilder.header("Content-Digest", "sha-512=:" + Base64.getEncoder().encodeToString(digest) + ":");
        }

        if (!includedFiles.isEmpty()) {
            includedFiles.forEach(includedFile -> requestBuilder.file(includedFile));
        }

        return this;
    }

    @SneakyThrows
    @Override
    public ApiTest.Response execute(Object... uriVariables) {
        if (requestBuilder == null) {
            build(uriVariables);
        }

        return new Response(mockMvc.perform(requestBuilder));
    }

}
