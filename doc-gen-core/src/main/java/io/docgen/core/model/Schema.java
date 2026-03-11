package io.docgen.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Schema {

    private String type;
    private String format;

    @JsonProperty("$ref")
    private String ref;

    @JsonProperty("enum")
    private List<String> enumValues;

    private Map<String, Schema> properties;
    private List<String> required;

    private Schema items;

    private Schema additionalProperties;

    public Schema() {}

    public static Schema ofType(String type) {
        Schema s = new Schema();
        s.type = type;
        return s;
    }

    public static Schema ofType(String type, String format) {
        Schema s = new Schema();
        s.type = type;
        s.format = format;
        return s;
    }

    public static Schema ofRef(String refPath) {
        Schema s = new Schema();
        s.ref = refPath;
        return s;
    }

    public static Schema ofArray(Schema items) {
        Schema s = new Schema();
        s.type = "array";
        s.items = items;
        return s;
    }

    public static Schema ofMap(Schema valueSchema) {
        Schema s = new Schema();
        s.type = "object";
        s.additionalProperties = valueSchema;
        return s;
    }

    public static Schema ofEnum(List<String> values) {
        Schema s = new Schema();
        s.type = "string";
        s.enumValues = values;
        return s;
    }


    public void addProperty(String name, Schema propSchema) {
        if (properties == null) properties = new LinkedHashMap<>();
        properties.put(name, propSchema);
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }

    public List<String> getEnumValues() { return enumValues; }
    public void setEnumValues(List<String> enumValues) { this.enumValues = enumValues; }

    public Map<String, Schema> getProperties() { return properties; }
    public void setProperties(Map<String, Schema> properties) { this.properties = properties; }

    public List<String> getRequired() { return required; }
    public void setRequired(List<String> required) { this.required = required; }

    public Schema getItems() { return items; }
    public void setItems(Schema items) { this.items = items; }

    public Schema getAdditionalProperties() { return additionalProperties; }
    public void setAdditionalProperties(Schema additionalProperties) { this.additionalProperties = additionalProperties; }
}

