package uk.gov.hmcts.opal.filehandler.util;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;

public class StringUtil {

    public static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static String toMessageJson(ObjectMapper objectMapper, String... message) {
        ArrayNode arrayNode = objectMapper.createObjectNode()
            .putArray("messages");
        for (String msg : message) {
            arrayNode.add(msg);
        }
        return arrayNode.toString();
    }
}
