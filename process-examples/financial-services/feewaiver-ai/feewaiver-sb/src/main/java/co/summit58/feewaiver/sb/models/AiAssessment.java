package co.summit58.feewaiver.sb.models;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AiAssessment(
            @JsonProperty("case_summary")
            String caseSummary,

            String intent,

            @JsonProperty("risk_level")
            String riskLevel,

            double confidence,

            @JsonProperty("recommended_action")
            String recommendedAction,

            @JsonProperty("reasoning_summary")
            String reasoningSummary
    ) {}