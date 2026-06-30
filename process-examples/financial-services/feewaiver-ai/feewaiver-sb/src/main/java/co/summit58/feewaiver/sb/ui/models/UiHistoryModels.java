package co.summit58.feewaiver.sb.ui.models;

import java.util.List;
import java.util.Map;

public class UiHistoryModels {

    public record InstanceHistoryResponse(
            String processInstanceId,
            String processDefinitionId,
            String processDefinitionKey,
            Long analysisId,
            String startTime,
            String endTime,
            String policyRoute,
            String resolutionStatus,
            String reviewStatus,
            String reviewDecision,
            String requestText,
            Map<String, Object> variables,
            AnalysisSummary analysis,
            List<ActivityHistoryItem> activityHistory,
            DiagramState diagramState
    ) {}

    public record AnalysisSummary(
            String aiCaseSummary,
            String aiIntent,
            String aiRiskLevel,
            Double aiConfidence,
            String aiRecommendedAction,
            String aiReasoningSummary,
            Boolean drlEscalate,
            Boolean drlRequiresReview,
            String drlReason,
            String toolInteractionSummary,
            Boolean finalGuardrailOverride,
            String finalGuardrailReason
    ) {}

    public record ActivityHistoryItem(
            String activityId,
            String activityName,
            String activityType,
            String startTime,
            String endTime,
            Long durationInMillis,
            Integer sequence
    ) {}

    public record DiagramState(
        List<String> completedActivityIds,
        List<String> activeActivityIds,
        List<DiagramOverlay> overlays
    ) {}

    public record DiagramOverlay(
            String elementId,
            String label
    ) {}

    public record BpmnXmlResponse(
            String processDefinitionId,
            String bpmnXml
    ) {}
}