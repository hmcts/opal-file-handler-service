package uk.gov.hmcts.opal.filehandler.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class StreamUtil {

    public static List<String> readLines(InputStream fileContents) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileContents, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (IOException ex) {
            throw new IllegalArgumentException("BACS Standard 18 file could not be read", ex);
        } catch (UncheckedIOException ex) {
            throw new IllegalArgumentException("BACS Standard 18 file could not be read", ex);
        }
    }
}
