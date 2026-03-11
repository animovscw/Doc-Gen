package io.docgen.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"openapi", "info", "servers", "paths", "components"})
public class ApiSpec {

    private String openapi = "3.0.3";
    private Info info;
    private List<Server> servers;
    private Map<String, PathItem> paths;
    private Components components;


    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Info {
        private String title;
        private String version;
        private String description;

        public Info() {}
        public Info(String title, String version) {
            this.title = title;
            this.version = version;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Server {
        private String url;

        public Server() {}
        public Server(String url) { this.url = url; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Components {
        private Map<String, Schema> schemas;

        public Components() {}
        public Components(Map<String, Schema> schemas) { this.schemas = schemas; }

        public Map<String, Schema> getSchemas() { return schemas; }
        public void setSchemas(Map<String, Schema> schemas) { this.schemas = schemas; }
    }


    public String getOpenapi() { return openapi; }
    public void setOpenapi(String openapi) { this.openapi = openapi; }

    public Info getInfo() { return info; }
    public void setInfo(Info info) { this.info = info; }

    public List<Server> getServers() { return servers; }
    public void setServers(List<Server> servers) { this.servers = servers; }

    public Map<String, PathItem> getPaths() { return paths; }
    public void setPaths(Map<String, PathItem> paths) { this.paths = paths; }

    public Components getComponents() { return components; }
    public void setComponents(Components components) { this.components = components; }
}

