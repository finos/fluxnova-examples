package co.summit58.feewaiver.sb.models;

public record AnalyzeResponse(
        Long entityId,
        GovernedAssessment governedAssessment,
        PolicyDecision policyDecision,
        String reviewStatus,
        String resolutionStatus
    ) {}