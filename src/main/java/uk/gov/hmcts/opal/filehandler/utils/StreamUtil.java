package uk.gov.hmcts.opal.filehandler.utils;

import java.io.IOException;
import java.io.InputStream;

public class StreamUtil {

    public static byte[] readAllBytes(InputStream fileContents, String sourceDescription) {
        try {
            return fileContents.readAllBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException(sourceDescription + " could not be read", ex);
        }
    }
}