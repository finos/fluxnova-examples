package co.summit58.feewaiver.sb.models;

import java.util.List;

public record AnalysisRecordResponse(
        Long id,
        String requestText,
        Integer priorFeeWaiverCount,
        Double feeAmount,
        Boolean vulnerableCustomer,
        Boolean regulatorySensitivity,
        String aiCaseSummary,
        String aiIntent,
        String aiRiskLevel,
        Double aiConfidence,
        String aiRecommendedAction,
        String aiReasoningSummary,
        List<String> policyFlags,
        Boolean requiresHumanReview,
        String policyRoute,
        String finalAction,
        List<String> triggeredPolicies,
        String policyExplanation,
        String reviewStatus,
        String reviewDecision,
        String resolutionStatus,
        String reviewer,
        String reviewNotes,
        String reviewedAt,
        String createdAt,
        Boolean drlEscalate,
        Boolean drlRequiresReview,
        String drlReason
) {}