package co.summit58.feewaiver.sb;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class PolicyDecisionDemo {

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
            boolean done
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

            boolean highDollar = ai.policyFlags().contains("HIGH_DOLLAR_AMOUNT");
            boolean regulatory = ai.policyFlags().contains("REGULATORY_SENSITIVITY");
            boolean vulnerable = ai.policyFlags().contains("VULNERABLE_CUSTOMER");
            boolean insufficient = ai.policyFlags().contains("INSUFFICIENT_INFORMATION");
            boolean repeatRequest = ai.policyFlags().contains("REPEAT_REQUEST");

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

    public static void main(String[] args) throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        try (InputStream input = PolicyDecisionDemo.class
                .getClassLoader()
                .getResourceAsStream("ollama-response.json")) {

            if (input == null) {
                throw new IllegalStateException("Could not find ollama-response.json in resources");
            }

            OllamaResponse ollamaResponse = mapper.readValue(input, OllamaResponse.class);

            String innerJson = ollamaResponse.message().content();
            AiAssessment aiAssessment = mapper.readValue(innerJson, AiAssessment.class);

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
        }
    }
}