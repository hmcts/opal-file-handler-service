package uk.gov.hmcts.opal.filehandler.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.InputStream;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.opal.filehandler.testutil.StreamTestUtil;

class StreamUtilTest {

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
