package io.docgen.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {

    private String description;
    private Map<String, RequestBody.MediaType> content;

    public Response() {}

    public Response(String description) {
        this.description = description;
    }


    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, RequestBody.MediaType> getContent() { return content; }
    public void setContent(Map<String, RequestBody.MediaType> content) { this.content = content; }
}

