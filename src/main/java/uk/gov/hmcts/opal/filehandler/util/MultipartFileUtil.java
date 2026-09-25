package uk.gov.hmcts.opal.filehandler.util;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.common.exceptions.standard.InternalServerErrorException;
import uk.gov.hmcts.opal.filehandler.utils.StreamUtil;

public class MultipartFileUtil {


    public static String getChecksum(MultipartFile file) {
        try (InputStream stream = file.getInputStream()) {
            return StreamUtil.calculateChecksum(stream);
        } catch (IOException e) {
            throw new InternalServerErrorException("Internal Server Error",
                "Failed to read file content for checksum calculation ", e);
        }
    }
}
