package uk.gov.hmcts.opal.filehandler.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StreamUtil {

    public static List<String> readLines(InputStream fileContents) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fileContents, StandardCharsets.UTF_8))) {
            return reader.lines().toList();
        } catch (Exception ex) {
            throw new IllegalArgumentException("BACS Standard 18 file could not be read", ex);
        }
    }
}
