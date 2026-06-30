package co.summit58.feewaiver.sb.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.summit58.feewaiver.sb.models.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import java.util.Map;

@Service
public class OllamaClientService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String ollamaUrl;
    private final String modelName;

    public OllamaClientService(
            ObjectMapper objectMapper,
            @Value("${ollama.base-url}") String baseUrl,
            @Value("${ollama.chat-path}") String chatPath,
            @Value("${ollama.model}") String modelName
    ) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = objectMapper;
        this.ollamaUrl = baseUrl + chatPath;
        this.modelName = modelName;
    }

    public AiAssessment analyzeFeeWaiver(AnalyzeRequest input)
            throws IOException, InterruptedException {

        String systemPrompt = """
                You are an AI assistant helping classify customer fee waiver requests for a governed workflow system.

                Your role is to:
                1. Summarize the customer request.
                2. Classify the intent.
                3. Assess likely risk level.
                4. Estimate confidence in your assessment.
                5. Recommend an action.
                6. Briefly explain your reasoning.

                Important rules:
                - Return only valid JSON matching the provided schema.
                - Do not include markdown.
                - Do not include any text outside the JSON object.
                - Be conservative. If information is ambiguous or incomplete, lower confidence.
                - Use HIGH risk only when the case appears sensitive, potentially regulated, vulnerable-customer related, involves legal/compliance exposure, or clearly requires specialist escalation.
                - High dollar amount alone does not make a case HIGH risk.
                - Repeat requests alone do not make a case HIGH risk.
                - Larger dollar amounts and repeat requests should usually be MEDIUM risk unless regulatory or vulnerable-customer factors are present.
                - recommended_action is only a recommendation. Final approval will be determined by policy outside the model.
                """;

        String userPrompt = """
                Analyze the following customer fee waiver request.

                Customer request:
                %s

                Additional context:
                - Prior fee waiver count in last 12 months: %d
                - Fee amount requested: %.2f
                - Customer marked as vulnerable: %s
                - Sensitive/regulatory context present: %s

                Return only JSON.
                """.formatted(
                input.requestText(),
                input.priorFeeWaiverCount(),
                input.feeAmount(),
                input.vulnerableCustomer(),
                input.regulatorySensitivity()
        );

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", modelName);
        payload.put("stream", false);

        ArrayNode messages = payload.putArray("messages");

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        ObjectNode format = payload.putObject("format");
        format.put("type", "object");

        ObjectNode properties = format.putObject("properties");
        properties.putObject("case_summary").put("type", "string");

        ObjectNode intent = properties.putObject("intent");
        intent.put("type", "string");
        intent.putArray("enum").add("FEE_WAIVER_REQUEST");

        ObjectNode riskLevel = properties.putObject("risk_level");
        riskLevel.put("type", "string");
        riskLevel.putArray("enum").add("LOW").add("MEDIUM").add("HIGH");

        ObjectNode confidence = properties.putObject("confidence");
        confidence.put("type", "number");
        confidence.put("minimum", 0);
        confidence.put("maximum", 1);

        ObjectNode recommendedAction = properties.putObject("recommended_action");
        recommendedAction.put("type", "string");
        recommendedAction.putArray("enum")
                .add("APPROVE")
                .add("APPROVE_PARTIAL")
                .add("DENY")
                .add("ESCALATE");

        properties.putObject("reasoning_summary").put("type", "string");

        ArrayNode required = format.putArray("required");
        required.add("case_summary");
        required.add("intent");
        required.add("risk_level");
        required.add("confidence");
        required.add("recommended_action");
        required.add("reasoning_summary");

        String requestBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ollamaUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
        }

        OllamaResponse ollamaResponse =
                objectMapper.readValue(response.body(), OllamaResponse.class);

        if (ollamaResponse.message() == null || ollamaResponse.message().content() == null) {
            throw new IOException("Ollama response did not contain message.content");
        }

        return objectMapper.readValue(ollamaResponse.message().content(), AiAssessment.class);
    }

    public AiStepResult analyzeFeeWaiverWithToolOption(AnalyzeRequest input)
                throws IOException, InterruptedException {

        String systemPrompt = """
                You are an AI assistant helping classify customer fee waiver requests for a governed workflow system.

                You may do one of two things:

                1. Return a FINAL assessment if you have enough information.
                2. Return a TOOL_REQUEST if you need additional context before making a final recommendation.

                Important rules:
                - Return only valid JSON matching the provided schema.
                - Do not include markdown.
                - Do not include any text outside the JSON object.
                - If you need more information about prior waiver history, you may request the tool named getFeeWaiverHistory.
                - Only request one tool.
                - If you request a tool, do not return a final assessment yet.
                - If you return a final assessment, set type to FINAL and populate aiAssessment.
                - If you request a tool, set type to TOOL_REQUEST and populate toolRequest.
                - High dollar amount alone does not make a case HIGH risk.
                - Repeat requests alone do not make a case HIGH risk.
                - Larger dollar amounts and repeat requests should usually be MEDIUM risk unless regulatory or vulnerable-customer factors are present.
                """;

        String userPrompt = """
                Analyze the following customer fee waiver request.

                Customer request:
                %s

                Additional context:
                - Prior fee waiver count in last 12 months: %d
                - Fee amount requested: %.2f
                - Customer marked as vulnerable: %s
                - Sensitive/regulatory context present: %s

                If you need additional history, request the getFeeWaiverHistory tool using customerId C12345.

                Return only JSON.
                """.formatted(
                input.requestText(),
                input.priorFeeWaiverCount(),
                input.feeAmount(),
                input.vulnerableCustomer(),
                input.regulatorySensitivity()
        );

        return callAiStep(systemPrompt, userPrompt);
    }
    public AiStepResult continueAfterToolResult(
                AnalyzeRequest input,
                ToolRequest toolRequest,
                Map<String, Object> toolResult) throws IOException, InterruptedException {

        String systemPrompt = """
                You are an AI assistant helping classify customer fee waiver requests for a governed workflow system.

                You have already requested a tool and received the tool result.
                You must now return a FINAL assessment.

                Important rules:
                - Return only valid JSON matching the provided schema.
                - Do not include markdown.
                - Do not include any text outside the JSON object.
                - Set type to FINAL.
                - Populate aiAssessment.
                - Do not request another tool.
                - Use the provided tool result as additional context.
                - High dollar amount alone does not make a case HIGH risk.
                - Repeat requests alone do not make a case HIGH risk.
                - Larger dollar amounts and repeat requests should usually be MEDIUM risk unless regulatory or vulnerable-customer factors are present.
                """;

        String userPrompt = """
                Continue the analysis of the following customer fee waiver request.

                Customer request:
                %s

                Original context:
                - Prior fee waiver count in last 12 months: %d
                - Fee amount requested: %.2f
                - Customer marked as vulnerable: %s
                - Sensitive/regulatory context present: %s

                Tool requested:
                - toolName: %s
                - arguments: %s

                Tool result:
                %s

                Return only JSON.
                """.formatted(
                input.requestText(),
                input.priorFeeWaiverCount(),
                input.feeAmount(),
                input.vulnerableCustomer(),
                input.regulatorySensitivity(),
                toolRequest.toolName(),
                objectMapper.writeValueAsString(toolRequest.arguments()),
                objectMapper.writeValueAsString(toolResult)
                );

        return callAiStep(systemPrompt, userPrompt);
    }

    private AiStepResult callAiStep(String systemPrompt, String userPrompt)
                throws IOException, InterruptedException {

        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("model", modelName);
        payload.put("stream", false);

        ArrayNode messages = payload.putArray("messages");

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", userPrompt);
        messages.add(userMessage);

        ObjectNode format = payload.putObject("format");
        format.put("type", "object");

        ObjectNode properties = format.putObject("properties");

        ObjectNode typeNode = properties.putObject("type");
        typeNode.put("type", "string");
        typeNode.putArray("enum").add("FINAL").add("TOOL_REQUEST");

        ObjectNode aiAssessment = properties.putObject("aiAssessment");
        aiAssessment.put("type", "object");
        ObjectNode aiProps = aiAssessment.putObject("properties");

        aiProps.putObject("case_summary").put("type", "string");

        ObjectNode intent = aiProps.putObject("intent");
        intent.put("type", "string");
        intent.putArray("enum").add("FEE_WAIVER_REQUEST");

        ObjectNode riskLevel = aiProps.putObject("risk_level");
        riskLevel.put("type", "string");
        riskLevel.putArray("enum").add("LOW").add("MEDIUM").add("HIGH");

        ObjectNode confidence = aiProps.putObject("confidence");
        confidence.put("type", "number");
        confidence.put("minimum", 0);
        confidence.put("maximum", 1);

        ObjectNode recommendedAction =
                aiProps.putObject("recommended_action");
        recommendedAction.put("type", "string");
        recommendedAction.putArray("enum")
                .add("APPROVE")
                .add("APPROVE_PARTIAL")
                .add("DENY")
                .add("ESCALATE");

        aiProps.putObject("reasoning_summary").put("type", "string");

        ArrayNode aiRequired = aiAssessment.putArray("required");
        aiRequired.add("case_summary");
        aiRequired.add("intent");
        aiRequired.add("risk_level");
        aiRequired.add("confidence");
        aiRequired.add("recommended_action");
        aiRequired.add("reasoning_summary");

        ObjectNode toolRequest = properties.putObject("toolRequest");
        toolRequest.put("type", "object");
        ObjectNode toolProps = toolRequest.putObject("properties");
        toolProps.putObject("toolName").put("type", "string");
        toolProps.putObject("arguments").put("type", "object");

        String requestBody = objectMapper.writeValueAsString(payload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(ollamaUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
                throw new IOException("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
        }

        OllamaResponse ollamaResponse =
                objectMapper.readValue(response.body(), OllamaResponse.class);

        if (ollamaResponse.message() == null || ollamaResponse.message().content() == null) {
                throw new IOException("Ollama response did not contain message.content");
        }

        return objectMapper.readValue(ollamaResponse.message().content(), AiStepResult.class);
    }
}