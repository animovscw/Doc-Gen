package io.docgen.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Operation {

    private String operationId;
    private String summary;
    private String description;
    private List<String> tags;
    private List<Parameter> parameters;
    private RequestBody requestBody;
    private Map<String, Response> responses;

    public Operation() {}


    public String getOperationId() { return operationId; }
    public void setOperationId(String operationId) { this.operationId = operationId; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<Parameter> getParameters() { return parameters; }
    public void setParameters(List<Parameter> parameters) { this.parameters = parameters; }

    public RequestBody getRequestBody() { return requestBody; }
    public void setRequestBody(RequestBody requestBody) { this.requestBody = requestBody; }

    public Map<String, Response> getResponses() { return responses; }
    public void setResponses(Map<String, Response> responses) { this.responses = responses; }
}

