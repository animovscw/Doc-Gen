package io.docgen.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestBody {

    private boolean required;
    private Map<String, MediaType> content;

    public RequestBody() {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MediaType {
        private Schema schema;

        public MediaType() {}
        public MediaType(Schema schema) { this.schema = schema; }

        public Schema getSchema() { return schema; }
        public void setSchema(Schema schema) { this.schema = schema; }
    }


    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public Map<String, MediaType> getContent() { return content; }
    public void setContent(Map<String, MediaType> content) { this.content = content; }
}

