package io.docgen.core.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PathItem {

    private final Map<String, Operation> operations = new LinkedHashMap<>();

    public void setOperation(String httpMethod, Operation operation) {
        operations.put(httpMethod.toLowerCase(), operation);
    }

    public Operation getOperation(String httpMethod) {
        return operations.get(httpMethod.toLowerCase());
    }

    @JsonAnyGetter
    public Map<String, Operation> getOperations() {
        return operations;
    }
}

