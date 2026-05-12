package co.summit58.feewaiver.sb.delegates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.bpm.engine.delegate.BpmnError;
import org.finos.fluxnova.bpm.engine.delegate.DelegateExecution;
import org.finos.fluxnova.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component("submitReviewDelegate")
public class SubmitReviewDelegate implements JavaDelegate {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl;

    public SubmitReviewDelegate(
            ObjectMapper objectMapper,
            @Value("${app.analysis.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        Long entityId = getRequiredLong(execution, "entityId");
        String reviewer = getRequiredString(execution, "reviewer");
        String reviewDecision = getRequiredString(execution, "reviewDecision");
        String reviewNotes = getOptionalString(execution, "reviewComments");

        String url = baseUrl + "/analyze/" + entityId + "/review";

        JsonNode requestBody = objectMapper.createObjectNode()
                .put("reviewer", reviewer)
                .put("decision", reviewDecision)
                .put("notes", reviewNotes == null ? "" : reviewNotes);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BpmnError(
                    "SUBMIT_REVIEW_FAILED",
                    "Submit review endpoint returned HTTP " + response.statusCode() + ": " + response.body()
            );
        }

        JsonNode root = objectMapper.readTree(response.body());

        execution.setVariable("reviewStatus", textOrNull(root.get("reviewStatus")));
        execution.setVariable("reviewDecision", textOrNull(root.get("reviewDecision")));
        execution.setVariable("resolutionStatus", textOrNull(root.get("resolutionStatus")));
        execution.setVariable("reviewedAt", textOrNull(root.get("reviewedAt")));
        execution.setVariable("reviewComments", textOrNull(root.get("reviewNotes")));
    }

    private Long getRequiredLong(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof String s && !s.isBlank()) return Long.valueOf(s);
        throw new IllegalArgumentException("Missing required Long process variable: " + name);
    }

    private String getRequiredString(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        
        if (!(value instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("Missing required String process variable: " + name);
        }
        return s;
    }

    private String getOptionalString(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        return value instanceof String s ? s : null;
    }

    private String textOrNull(JsonNode node) {
        return (node == null || node.isNull() || node.isMissingNode()) ? null : node.asText();
    }
}