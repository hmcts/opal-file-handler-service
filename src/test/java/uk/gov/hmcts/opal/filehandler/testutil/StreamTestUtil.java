package uk.gov.hmcts.opal.filehandler.testutil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class StreamTestUtil {

    private StreamTestUtil() {
    }

    public static InputStream unreadableStream() {
        return unreadableStream("Failed to read stream");
    }

    public static InputStream unreadableStream(String failureMessage) {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException(failureMessage);
            }
        };
    }

    public static ByteArrayInputStream stream() {
        return new ByteArrayInputStream("contents".getBytes(StandardCharsets.UTF_8));
    }

    public static InputStream stream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    public static InputStream resourceStream(String path) {
        InputStream stream = StreamTestUtil.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalArgumentException("Could not load test fixture: " + path);
        }
        return stream;
    }
}