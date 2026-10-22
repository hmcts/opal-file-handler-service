package uk.gov.hmcts.opal.filehandler.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.opal.filehandler.testutil.StreamTestUtil;

class StreamUtilTest {

    @Test
    void shouldReadInputStreamBytes() {
        InputStream stream = StreamTestUtil.stream("contents");

        assertThat(StreamUtil.readAllBytes(stream, "Test file"))
            .containsExactly("contents".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void shouldWrapInputStreamByteReadFailure() throws IOException {
        try (InputStream unreadableStream = StreamTestUtil.unreadableStream()) {
            assertThatThrownBy(() -> StreamUtil.readAllBytes(unreadableStream, "Test file"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Test file could not be read");
        }
    }

    @Test
    void shouldReadInputStreamLines() {
        InputStream stream = StreamTestUtil.stream("line one\nline two\n");

        assertThat(StreamUtil.readLines(stream)).containsExactly("line one", "line two");
    }

    @Test
    void shouldWrapInputStreamReadFailure() {
        assertThatThrownBy(() -> StreamUtil.readLines(StreamTestUtil.unreadableStream()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("BACS Standard 18 file could not be read")
            .hasCauseInstanceOf(UncheckedIOException.class);
    }
}
