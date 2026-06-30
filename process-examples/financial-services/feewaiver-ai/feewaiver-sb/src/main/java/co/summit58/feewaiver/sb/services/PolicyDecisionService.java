package co.summit58.feewaiver.sb.services;

import org.springframework.stereotype.Service;

import co.summit58.feewaiver.sb.models.*;

import java.util.ArrayList;
import java.util.List;

@Service
public class PolicyDecisionService {

    public PolicyDecision evaluate(GovernedAssessment governed) {
        AiAssessment ai = governed.aiAssessment();
        List<String> triggered = new ArrayList<>();

        boolean lowConfidence = ai.confidence() < 0.80;
        boolean highRisk = "HIGH".equals(ai.riskLevel());
        boolean mediumRisk = "MEDIUM".equals(ai.riskLevel());

        List<String> flags = governed.policyFlags() == null ? List.of() : governed.policyFlags();

        boolean highDollar = flags.contains("HIGH_DOLLAR_AMOUNT");
        boolean regulatory = flags.contains("REGULATORY_SENSITIVITY");
        boolean vulnerable = flags.contains("VULNERABLE_CUSTOMER");
        boolean insufficient = flags.contains("INSUFFICIENT_INFORMATION");
        boolean repeatRequest = flags.contains("REPEAT_REQUEST");

        boolean drlEscalate = governed.drlEscalate();
        boolean drlRequiresReview = governed.drlRequiresReview();
        String drlReason = governed.drlReason();

        if (lowConfidence) {
            triggered.add("CONFIDENCE_BELOW_THRESHOLD");
        }
        if (highRisk) {
            triggered.add("HIGH_RISK_IDENTIFIED");
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
        if (drlEscalate) {
            triggered.add("DRL_ESCALATE");
        }
        if (drlRequiresReview) {
            triggered.add("DRL_REQUIRES_REVIEW");
        }

        if (drlEscalate) {
            return new PolicyDecision(
                    "ESCALATE",
                    "SEND_TO_ESCALATION",
                    triggered,
                    drlReason != null ? drlReason : "DRL guardrail requires escalation."
            );
        }

        if (regulatory) {
            return new PolicyDecision(
                    "ESCALATE",
                    "SEND_TO_ESCALATION",
                    triggered,
                    "Regulatory-sensitive cases must be escalated."
            );
        }

        if (highRisk && vulnerable) {
            return new PolicyDecision(
                    "ESCALATE",
                    "SEND_TO_ESCALATION",
                    triggered,
                    "High-risk cases involving vulnerable customers must be escalated."
            );
        }

        if (drlRequiresReview) {
            return new PolicyDecision(
                    "HUMAN_REVIEW",
                    "SEND_TO_REVIEW",
                    triggered,
                    drlReason != null ? drlReason : "DRL guardrail requires human review."
            );
        }

        if (lowConfidence || mediumRisk || highRisk || highDollar || vulnerable || insufficient || repeatRequest) {
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