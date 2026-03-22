package io.docgen.core.generator;

import io.docgen.core.model.ApiSpec;
import io.docgen.core.serializer.JsonSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class HtmlGenerator {

    private static final Logger logger = LoggerFactory.getLogger(HtmlGenerator.class);

    private final JsonSerializer jsonSerializer = new JsonSerializer();

    public String generate(ApiSpec spec) {
        String specJson = jsonSerializer.serialize(spec);
        String css = loadResource("/ui/doc-gen-ui.css");
        String js = loadResource("/ui/doc-gen-ui.js");
        String title = spec.getInfo() != null ? spec.getInfo().getTitle() : "API Documentation";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        sb.append("  <meta charset=\"UTF-8\"/>\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\"/>\n");
        sb.append("  <title>").append(escapeHtml(title)).append("</title>\n");
        sb.append("  <style>\n").append(css).append("\n  </style>\n");
        sb.append("</head>\n<body>\n");
        sb.append("  <script>\n    window.__API_SPEC__ = ");
        sb.append(specJson).append(";\n  </script>\n");
        sb.append("  <div id=\"app\"></div>\n");
        sb.append("  <script>\n").append(js).append("\n  </script>\n");
        sb.append("</body>\n</html>\n");

        return sb.toString();
    }

    private String loadResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                logger.warn("Resource not found: {}", path);
                return "/* resource " + path + " not found */";
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read resource: {}", path, e);
            throw new RuntimeException("Failed to read resource: " + path, e);
        }
    }

    private String escapeHtml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

