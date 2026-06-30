package co.summit58.feewaiver.sb.models;

public record ReviewResponse(
        Long id,
        String reviewStatus,
        String reviewDecision,
        String resolutionStatus,
        String reviewer,
        String reviewNotes,
        String reviewedAt
) {}