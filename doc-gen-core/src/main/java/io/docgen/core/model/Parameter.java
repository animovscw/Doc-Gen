package io.docgen.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Parameter {

    public enum In {
        path, query, header;

        @JsonValue
        public String toValue() { return name(); }
    }

    private String name;
    private In in;
    private boolean required;
    private Schema schema;
    private String description;

    public Parameter() {}


    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public In getIn() { return in; }
    public void setIn(In in) { this.in = in; }

    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }

    public Schema getSchema() { return schema; }
    public void setSchema(Schema schema) { this.schema = schema; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}

