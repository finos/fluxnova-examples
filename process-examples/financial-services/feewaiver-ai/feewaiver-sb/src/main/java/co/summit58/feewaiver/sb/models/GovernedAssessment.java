package co.summit58.feewaiver.sb.models;

import java.util.List;
import java.util.Map;

public record GovernedAssessment(
        AiAssessment aiAssessment,
        List<String> policyFlags,
        boolean requiresHumanReview,
        String aiStepType,
        ToolRequest toolRequest,
        Map<String, Object> toolResult,
        boolean drlEscalate,
        boolean drlRequiresReview,
        String drlReason
    ) {}