package io.docgen.core.analyzer;

import io.docgen.core.model.ApiSpec;
import io.docgen.core.model.PathItem;
import io.docgen.core.model.Schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApiSpecBuilder {

    private String title = "API Documentation";
    private String version = "1.0.0";
    private String description;
    private String serverUrl;
    private JavadocResolver javadocResolver;

    public ApiSpecBuilder title(String title) { this.title = title; return this; }
    public ApiSpecBuilder version(String version) { this.version = version; return this; }
    public ApiSpecBuilder description(String description) { this.description = description; return this; }
    public ApiSpecBuilder serverUrl(String serverUrl) { this.serverUrl = serverUrl; return this; }
    public ApiSpecBuilder javadocResolver(JavadocResolver javadocResolver) { this.javadocResolver = javadocResolver; return this; }

    public ApiSpec build(List<Class<?>> classes) {
        Map<String, Schema> schemaRegistry = new LinkedHashMap<>();
        ControllerScanner scanner = new ControllerScanner(schemaRegistry, javadocResolver);
        Map<String, PathItem> paths = scanner.scan(classes);

        ApiSpec spec = new ApiSpec();

        ApiSpec.Info info = new ApiSpec.Info(title, version);
        if (description != null) info.setDescription(description);
        spec.setInfo(info);

        if (serverUrl != null && !serverUrl.isBlank()) {
            spec.setServers(List.of(new ApiSpec.Server(serverUrl)));
        }

        spec.setPaths(paths);

        if (!schemaRegistry.isEmpty()) {
            spec.setComponents(new ApiSpec.Components(schemaRegistry));
        }

        return spec;
    }
}

