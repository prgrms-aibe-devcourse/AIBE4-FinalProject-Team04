package kr.java.patchnotedemo.util;

import java.nio.charset.StandardCharsets;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

public class PromptUtils {
    public static String loadTemplate(Resource resource) {
        try {
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load prompt template", e);
        }
    }
}
