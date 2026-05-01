package co.summit58.feewaiver.sb.delegates;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.fluxnova.bpm.engine.delegate.BpmnError;
import org.finos.fluxnova.bpm.engine.delegate.DelegateExecution;
import org.finos.fluxnova.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Objects;

import java.util.logging.Logger;

@Component("llmCaseAnalysisDelegate")
public class LlmCaseAnalysisDelegate implements JavaDelegate {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String analyzeUrl;

    private final Logger LOG = Logger.getLogger(LlmCaseAnalysisDelegate.class.getName());

    public LlmCaseAnalysisDelegate(
            ObjectMapper objectMapper,
            @Value("${app.analysis.base-url:http://localhost:8080}") String baseUrl
    ) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.analyzeUrl = baseUrl + "/analyze";
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String requestText = getRequiredString(execution, "requestText");
        Integer priorFeeWaiverCount = getRequiredInteger(execution, "priorFeeWaiverCount");
        BigDecimal feeAmount = getRequiredBigDecimal(execution, "feeAmount");
        Boolean vulnerableCustomer = getRequiredBoolean(execution, "vulnerableCustomer");
        Boolean regulatorySensitivity = getRequiredBoolean(execution, "regulatorySensitivity");

        JsonNode requestBody = objectMapper.createObjectNode()
                .put("requestText", requestText)
                .put("priorFeeWaiverCount", priorFeeWaiverCount)
                .put("feeAmount", feeAmount)
                .put("vulnerableCustomer", vulnerableCustomer)
                .put("regulatorySensitivity", regulatorySensitivity)
                .put("processInstanceId", execution.getProcessInstanceId());

        String requestBodyAsString = objectMapper.writeValueAsString(requestBody);
        LOG.info("Request: " + requestBodyAsString);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(analyzeUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        LOG.info("Response: " + response);

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new BpmnError(
                    "ANALYZE_CASE_FAILED",
                    "Analyze endpoint returned HTTP " + response.statusCode() + ": " + response.body()
            );
        }

        JsonNode root = objectMapper.readTree(response.body());

        Long entityId = requiredLong(root, "entityId");
        String policyRoute = requiredText(root, "policyDecision", "policyRoute");
        String finalAction = requiredText(root, "policyDecision", "finalAction");
        Boolean requiresHumanReview = requiredBoolean(root, "governedAssessment", "requiresHumanReview");

        execution.setVariable("entityId", entityId);
        execution.setVariable("policyRoute", policyRoute);
        execution.setVariable("finalAction", finalAction);
        execution.setVariable("requiresHumanReview", requiresHumanReview);

        JsonNode governedAssessment = root.path("governedAssessment");
        JsonNode aiAssessment = governedAssessment.path("aiAssessment");
        JsonNode policyDecision = root.path("policyDecision");

        setIfPresent(execution, "aiStepType", governedAssessment.get("aiStepType"));

        JsonNode toolRequest = governedAssessment.path("toolRequest");
        if(!toolRequest.isMissingNode() && !toolRequest.isNull()) {
            setIfPresent(execution, "toolName", toolRequest.get("toolName"));
            execution.setVariable("toolArgumentsJson", objectMapper.writeValueAsString(toolRequest.path("arguments")));
            execution.setVariable("toolConversationOccurred", true);
        }
        else {
            execution.setVariable("toolConversationOccurred", false);
        }

        JsonNode toolResult = governedAssessment.path("toolResult");
        if(!toolResult.isMissingNode() && !toolResult.isNull()) {
            execution.setVariable("toolResultJson", objectMapper.writeValueAsString(toolResult));
        }

        if (!aiAssessment.isMissingNode()) {
            setIfPresent(execution, "aiRiskLevel", aiAssessment.get("riskLevel"));
            setIfPresent(execution, "aiConfidence", aiAssessment.get("confidence"));
            setIfPresent(execution, "aiRecommendedAction", aiAssessment.get("recommendedAction"));
            setIfPresent(execution, "aiCaseSummary", aiAssessment.get("caseSummary"));
        }

        if (!policyDecision.isMissingNode()) {
            setIfPresent(execution, "policyExplanation", policyDecision.get("explanation"));
        }
    }

    private String getRequiredString(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (!(value instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("Missing required String process variable: " + name);
        }
        return s;
    }

    private Integer getRequiredInteger(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value instanceof Integer i) return i;
        if (value instanceof Long l) return Math.toIntExact(l);
        if (value instanceof String s && !s.isBlank()) return Integer.valueOf(s);
        throw new IllegalArgumentException("Missing required Integer process variable: " + name);
    }

    private BigDecimal getRequiredBigDecimal(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Double d) return BigDecimal.valueOf(d);
        if (value instanceof Float f) return BigDecimal.valueOf(f.doubleValue());
        if (value instanceof Integer i) return BigDecimal.valueOf(i);
        if (value instanceof Long l) return BigDecimal.valueOf(l);
        if (value instanceof String s && !s.isBlank()) return new BigDecimal(s);
        throw new IllegalArgumentException("Missing required numeric process variable: " + name);
    }

    private Boolean getRequiredBoolean(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value instanceof Boolean b) return b;
        if (value instanceof String s && !s.isBlank()) return Boolean.valueOf(s);
        throw new IllegalArgumentException("Missing required Boolean process variable: " + name);
    }

    private Long requiredLong(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            throw new IllegalArgumentException("Missing required response field: " + field);
        }
        return node.asLong();
    }

    private String requiredText(JsonNode root, String parent, String field) {
        JsonNode node = root.path(parent).path(field);
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            throw new IllegalArgumentException("Missing required response field: " + parent + "." + field);
        }
        return node.asText();
    }

    private Boolean requiredBoolean(JsonNode root, String parent, String field) {
        JsonNode node = root.path(parent).path(field);
        if (node.isMissingNode() || node.isNull()) {
            throw new IllegalArgumentException("Missing required response field: " + parent + "." + field);
        }
        return node.asBoolean();
    }

    private void setIfPresent(DelegateExecution execution, String variableName, JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return;
        }
        if (node.isTextual()) {
            execution.setVariable(variableName, node.asText());
        } else if (node.isBoolean()) {
            execution.setVariable(variableName, node.asBoolean());
        } else if (node.isNumber()) {
            execution.setVariable(variableName, node.numberValue());
        } else {
            execution.setVariable(variableName, Objects.toString(node));
        }
    }
}