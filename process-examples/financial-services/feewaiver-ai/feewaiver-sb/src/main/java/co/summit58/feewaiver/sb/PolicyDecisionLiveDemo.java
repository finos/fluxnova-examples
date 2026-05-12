package co.summit58.feewaiver.sb;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

public class PolicyDecisionLiveDemo {

    private static final String OLLAMA_URL = "http://localhost:11434/api/chat";
    private static final String MODEL_NAME = "mistral-small";

    public record AiAssessment(
            @JsonProperty("case_summary")
            String caseSummary,

            String intent,

            @JsonProperty("risk_level")
            String riskLevel,

            double confidence,

            @JsonProperty("recommended_action")
            String recommendedAction,

            @JsonProperty("requires_human_review")
            boolean requiresHumanReview,

            @JsonProperty("policy_flags")
            List<String> policyFlags,

            @JsonProperty("reasoning_summary")
            String reasoningSummary
    ) {}

    public record OllamaMessage(
            String role,
            String content
    ) {}

    public record OllamaResponse(
            String model,
            @JsonProperty("created_at")
            String createdAt,
            OllamaMessage message,
            boolean done,
            String done_reason,
            Long total_duration,
            Long load_duration,
            Integer prompt_eval_count,
            Long prompt_eval_duration,
            Integer eval_count,
            Long eval_duration
    ) {}

    public record PolicyDecision(
            String policyRoute,
            String finalAction,
            List<String> triggeredPolicies,
            String explanation
    ) {}

    public static class PolicyDecisionService {

        public PolicyDecision evaluate(AiAssessment ai) {
            List<String> triggered = new ArrayList<>();

            boolean lowConfidence = ai.confidence() < 0.80;
            boolean highRisk = "HIGH".equals(ai.riskLevel());
            boolean mediumRisk = "MEDIUM".equals(ai.riskLevel());

            List<String> flags = ai.policyFlags() == null ? List.of() : ai.policyFlags();

            boolean highDollar = flags.contains("HIGH_DOLLAR_AMOUNT");
            boolean regulatory = flags.contains("REGULATORY_SENSITIVITY");
            boolean vulnerable = flags.contains("VULNERABLE_CUSTOMER");
            boolean insufficient = flags.contains("INSUFFICIENT_INFORMATION");
            boolean repeatRequest = flags.contains("REPEAT_REQUEST");

            if (lowConfidence) {
                triggered.add("CONFIDENCE_BELOW_THRESHOLD");
            }
            if (highRisk) {
                triggered.add("HIGH_RISK_REQUIRES_ESCALATION");
            }
            if (mediumRisk) {
                triggered.add("MEDIUM_RISK_REQUIRES_REVIEW");
            }
            if (highDollar) {
                triggered.add("HIGH_DOLLAR_AMOUNT_REQUIRES_REVIEW");
            }
            if (regulatory) {
                triggered.add("REGULATORY_SENSITIVITY_REQUIRES_ESCALATION");
            }
            if (vulnerable) {
                triggered.add("VULNERABLE_CUSTOMER_REQUIRES_REVIEW");
            }
            if (insufficient) {
                triggered.add("INSUFFICIENT_INFORMATION_REQUIRES_REVIEW");
            }
            if (repeatRequest) {
                triggered.add("REPEAT_REQUEST_REQUIRES_REVIEW");
            }

            if (highRisk || regulatory) {
                return new PolicyDecision(
                        "ESCALATE",
                        "SEND_TO_ESCALATION",
                        triggered,
                        "Sensitive or high-risk cases must be escalated."
                );
            }

            if (lowConfidence || mediumRisk || highDollar || vulnerable || insufficient || repeatRequest) {
                return new PolicyDecision(
                        "HUMAN_REVIEW",
                        "SEND_TO_REVIEW",
                        triggered,
                        "One or more governance rules require human review."
                );
            }

            return new PolicyDecision(
                    "AUTO_PROCESS",
                    "AUTO_APPROVE",
                    triggered,
                    "Low risk, high confidence, and no escalation flags."
            );
        }
    }

    public static class OllamaClient {
        private final HttpClient httpClient;
        private final ObjectMapper objectMapper;

        public OllamaClient() {
            this.httpClient = HttpClient.newHttpClient();
            this.objectMapper = new ObjectMapper();
        }

        public AiAssessment analyzeFeeWaiver(
                String requestText,
                int priorFeeWaiverCount,
                double feeAmount,
                boolean vulnerableCustomer,
                boolean regulatorySensitivity
        ) throws IOException, InterruptedException {

            String systemPrompt = """
                    You are an AI assistant helping classify customer fee waiver requests for a governed workflow system.

                    Your role is to:
                    1. Summarize the customer request.
                    2. Assess likely risk level.
                    3. Estimate confidence in your assessment.
                    4. Recommend an action.
                    5. Identify policy flags that may require governance review.

                    Important rules:
                    - Return only valid JSON matching the provided schema.
                    - Do not include markdown.
                    - Do not include any text outside the JSON object.
                    - Be conservative. If information is ambiguous or incomplete, lower confidence and include INSUFFICIENT_INFORMATION.
                    - Use HIGH risk when the case appears sensitive, potentially regulated, vulnerable-customer related, or could create material customer impact.
                    - Set requires_human_review to true whenever risk is MEDIUM or HIGH, confidence is below 0.80, or any policy flag suggests escalation.
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
                    requestText,
                    priorFeeWaiverCount,
                    feeAmount,
                    vulnerableCustomer,
                    regulatorySensitivity
            );

            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("model", MODEL_NAME);
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

            properties.putObject("requires_human_review").put("type", "boolean");

            ObjectNode policyFlags = properties.putObject("policy_flags");
            policyFlags.put("type", "array");
            ObjectNode policyFlagItems = policyFlags.putObject("items");
            policyFlagItems.put("type", "string");
            policyFlagItems.putArray("enum")
                    .add("LOW_CONFIDENCE")
                    .add("REPEAT_REQUEST")
                    .add("REGULATORY_SENSITIVITY")
                    .add("HIGH_DOLLAR_AMOUNT")
                    .add("VULNERABLE_CUSTOMER")
                    .add("INSUFFICIENT_INFORMATION");

            properties.putObject("reasoning_summary").put("type", "string");

            ArrayNode required = format.putArray("required");
            required.add("case_summary");
            required.add("intent");
            required.add("risk_level");
            required.add("confidence");
            required.add("recommended_action");
            required.add("requires_human_review");
            required.add("policy_flags");
            required.add("reasoning_summary");

            String requestBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(OLLAMA_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IOException("Ollama returned HTTP " + response.statusCode() + ": " + response.body());
            }

            OllamaResponse ollamaResponse = objectMapper.readValue(response.body(), OllamaResponse.class);

            if (ollamaResponse.message() == null || ollamaResponse.message().content() == null) {
                throw new IOException("Ollama response did not contain message.content");
            }

            return objectMapper.readValue(ollamaResponse.message().content(), AiAssessment.class);
        }
    }

    public static void main(String[] args) {
        try {
            //Auto-approve case
            /*String requestText = """
                    I was charged a $35 overdraft fee because my paycheck was delayed by one day.
                    I have never asked for this before. Can you please waive it?
                    """;
                    */

            //Human review case
            /*String requestText = """
                    I need all $650 in overdraft and related fees reversed.
                    This has happened multiple times because of issues with my account.
                    Please fix it today.
                    """;*/

            //Escalation case
            String requestText = """
                    I am filing a complaint because these fees were assessed after I notified the bank
                    about hardship and disputed activity on my account. Reverse every charge immediately.
                    """;

            OllamaClient ollamaClient = new OllamaClient();
            
            //Auto-approve case
            /*AiAssessment aiAssessment = ollamaClient.analyzeFeeWaiver(
                    requestText,
                    0,
                    35.00,
                    false,
                    false
            );*/

            //Human review case
            /*AiAssessment aiAssessment = ollamaClient.analyzeFeeWaiver(
                    requestText,
                    3,
                    650.00,
                    false,
                    false
            );*/

            //Escalation case
            AiAssessment aiAssessment = ollamaClient.analyzeFeeWaiver(
                    requestText,
                    1,
                    420.00,
                    true,
                    true
            );

            PolicyDecision decision = new PolicyDecisionService().evaluate(aiAssessment);

            System.out.println("=== AI Assessment ===");
            System.out.println("Summary: " + aiAssessment.caseSummary());
            System.out.println("Intent: " + aiAssessment.intent());
            System.out.println("Risk: " + aiAssessment.riskLevel());
            System.out.println("Confidence: " + aiAssessment.confidence());
            System.out.println("Recommended action: " + aiAssessment.recommendedAction());
            System.out.println("Requires human review: " + aiAssessment.requiresHumanReview());
            System.out.println("Flags: " + aiAssessment.policyFlags());
            System.out.println("Reasoning: " + aiAssessment.reasoningSummary());

            System.out.println();
            System.out.println("=== Policy Decision ===");
            System.out.println("Policy route: " + decision.policyRoute());
            System.out.println("Final action: " + decision.finalAction());
            System.out.println("Triggered policies: " + decision.triggeredPolicies());
            System.out.println("Explanation: " + decision.explanation());

        } catch (Exception e) {
            System.err.println("Error running live demo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}