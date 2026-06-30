package co.summit58.feewaiver.sb.ui.services;

import co.summit58.feewaiver.sb.entities.AnalysisEntity;
import co.summit58.feewaiver.sb.entities.AnalysisEntityRepository;
import org.finos.fluxnova.bpm.engine.HistoryService;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.history.HistoricActivityInstance;
import org.finos.fluxnova.bpm.engine.history.HistoricProcessInstance;
import org.finos.fluxnova.bpm.engine.history.HistoricVariableInstance;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;

import co.summit58.feewaiver.sb.ui.models.UiHistoryModels;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UiHistoryService {

    private final HistoryService historyService;
    private final RepositoryService repositoryService;
    private final AnalysisEntityRepository analysisEntityRepository;

    public UiHistoryService(
            HistoryService historyService,
            RepositoryService repositoryService,
            AnalysisEntityRepository analysisEntityRepository
    ) {
        this.historyService = historyService;
        this.repositoryService = repositoryService;
        this.analysisEntityRepository = analysisEntityRepository;
    }

    public UiHistoryModels.InstanceHistoryResponse getInstanceHistory(String processInstanceId) {
        HistoricProcessInstance hpi = historyService
                .createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (hpi == null) {
            throw new IllegalArgumentException("No historic process instance found: " + processInstanceId);
        }

        ProcessDefinition pd = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionId(hpi.getProcessDefinitionId())
                .singleResult();

        List<HistoricVariableInstance> historicVars = historyService
                .createHistoricVariableInstanceQuery()
                .processInstanceId(processInstanceId)
                .list();

        Map<String, Object> variables = new LinkedHashMap<>();
        for (HistoricVariableInstance var : historicVars) {
                variables.put(var.getName(), var.getValue());
        }

        List<HistoricActivityInstance> haiList = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime()
                .asc()
                .list();

        List<UiHistoryModels.ActivityHistoryItem> activityHistory = new ArrayList<>();
        Set<String> completedActivityIds = new LinkedHashSet<>();
        Set<String> activeActivityIds = new LinkedHashSet<>();
        List<UiHistoryModels.DiagramOverlay> overlays = new ArrayList<>();
        Set<String> reachedActivityIds = new LinkedHashSet<>();
        Set<String> activeActivityIdsSet = new LinkedHashSet<>();

        int sequence = 1;
        for (HistoricActivityInstance hai : haiList) {
                activityHistory.add(new UiHistoryModels.ActivityHistoryItem(
                        hai.getActivityId(),
                        hai.getActivityName(),
                        hai.getActivityType(),
                        hai.getStartTime() != null ? hai.getStartTime().toInstant().toString() : null,
                        hai.getEndTime() != null ? hai.getEndTime().toInstant().toString() : null,
                        hai.getDurationInMillis(),
                        sequence++
                ));

                if (hai.getActivityId() != null && !hai.getActivityId().isBlank()) {
                        if (hai.getEndTime() != null) {
                                completedActivityIds.add(hai.getActivityId());
                        } 
                        else {
                                activeActivityIds.add(hai.getActivityId());
                        }
                }

                if (hai.getActivityId() != null && !hai.getActivityId().isBlank()) {
                        reachedActivityIds.add(hai.getActivityId());
                }

                if (hai.getActivityId() != null && !hai.getActivityId().isBlank()) {
                        reachedActivityIds.add(hai.getActivityId());

                        if (hai.getEndTime() != null) {
                                completedActivityIds.add(hai.getActivityId());
                        } else {
                                activeActivityIds.add(hai.getActivityId());
                                activeActivityIdsSet.add(hai.getActivityId());
                        }
                }
        }

        completedActivityIds.removeAll(activeActivityIds);

        /*addOverlayIfPresent(overlays, "svc-analyze-case", variables,
                "aiRiskLevel", "aiConfidence",
                (risk, confidence) -> "AI: " + risk + " / " + confidence);*/

        addOverlayIfPresent(overlays, reachedActivityIds, "svc-analyze-case", variables,
                "aiRiskLevel", "aiConfidence",
                (risk, confidence) -> {
                        String riskText = risk != null ? risk.toString() : null;
                        String confidenceText = confidence != null ? confidence.toString() : null;

                        if (riskText != null && confidenceText != null) {
                                return "AI: " + riskText + " / " + confidenceText;
                        }
                        if (riskText != null) {
                                return "AI: " + riskText;
                        }
                        if (confidenceText != null) {
                                return "AI Confidence: " + confidenceText;
                        }
                        return null;
                });

        addOverlayIfPresent(overlays, reachedActivityIds, "svc-fee-waiver-history-tool", variables,
                "toolName",
                toolName -> {
                        if (toolName == null) {
                                return null;
                        }
                        String toolText = toolName.toString().trim();
                        return toolText.isBlank() ? null : "Tool: " + toolText;
                });

        addOverlayIfPresent(overlays, reachedActivityIds, "svc-apply-final-guardrail", variables,
                "finalGuardrailOverride", "finalGuardrailReason",
                (override, reason) -> Boolean.TRUE.equals(override)
                        ? "Guardrail override"
                        : (reason != null ? "DRL checked" : null));

        /*addOverlayIfPresent(overlays, reachedActivityIds, "usr-review-case", variables,
                "reviewDecision",
                reviewDecision -> reviewDecision != null ? "Review: " + reviewDecision : null);*/

        addReviewOverlayIfPresent(overlays, reachedActivityIds, activeActivityIdsSet, "usr-review-case",
                variables);

        Long analysisId = asLong(variables.get("entityId"));
        AnalysisEntity analysis = analysisId != null
                ? analysisEntityRepository.findById(analysisId).orElse(null)
                : null;

        UiHistoryModels.AnalysisSummary analysisSummary = analysis != null
                ? new UiHistoryModels.AnalysisSummary(
                        analysis.getAiCaseSummary(),
                        analysis.getAiIntent(),
                        analysis.getAiRiskLevel(),
                        analysis.getAiConfidence(),
                        analysis.getAiRecommendedAction(),
                        analysis.getAiReasoningSummary(),
                        analysis.getDrlEscalate(),
                        analysis.getDrlRequiresReview(),
                        analysis.getDrlReason(),
                        asString(variables.get("toolInteractionSummary")),
                        asBoolean(variables.get("finalGuardrailOverride")),
                        asString(variables.get("finalGuardrailReason"))
                )
                : new UiHistoryModels.AnalysisSummary(
                        null, null, null, null, null, null,
                        null, null, null,
                        asString(variables.get("toolInteractionSummary")),
                        asBoolean(variables.get("finalGuardrailOverride")),
                        asString(variables.get("finalGuardrailReason"))
                );

        return new UiHistoryModels.InstanceHistoryResponse(
                processInstanceId,
                hpi.getProcessDefinitionId(),
                pd != null ? pd.getKey() : null,
                analysisId,
                hpi.getStartTime() != null ? hpi.getStartTime().toInstant().toString() : null,
                hpi.getEndTime() != null ? hpi.getEndTime().toInstant().toString() : null,
                asString(variables.get("policyRoute")),
                analysis != null ? analysis.getResolutionStatus() : null,
                analysis != null ? analysis.getReviewStatus() : null,
                analysis != null ? analysis.getReviewDecision() : null,
                asString(variables.get("requestText")),
                variables,
                analysisSummary,
                activityHistory,
                new UiHistoryModels.DiagramState(new ArrayList<String>(completedActivityIds), 
                        new ArrayList<String>(activeActivityIds), overlays)
        );
    }

    public UiHistoryModels.BpmnXmlResponse getBpmnXml(String processDefinitionId) throws Exception {
        ProcessDefinition pd = repositoryService
                .createProcessDefinitionQuery()
                .processDefinitionId(processDefinitionId)
                .singleResult();

        if (pd == null) {
            throw new IllegalArgumentException("No process definition found: " + processDefinitionId);
        }

        String bpmnXml = new String(
                repositoryService.getProcessModel(processDefinitionId).readAllBytes()
        );

        return new UiHistoryModels.BpmnXmlResponse(processDefinitionId, bpmnXml);
    }

    private static Long asLong(Object value) {
        if (value instanceof Long l) return l;
        if (value instanceof Integer i) return i.longValue();
        if (value instanceof String s && !s.isBlank()) return Long.valueOf(s);
        return null;
    }

    private static String asString(Object value) {
        return value != null ? value.toString() : null;
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean b) return b;
        if (value instanceof String s && !s.isBlank()) return Boolean.valueOf(s);
        return null;
    }

    @FunctionalInterface
    private interface TwoArgFormatter {
        String format(Object a, Object b);
    }

    @FunctionalInterface
    private interface OneArgFormatter {
        String format(Object a);
    }

    private static void addOverlayIfPresent(
            List<UiHistoryModels.DiagramOverlay> overlays,
            Set<String> reachedActivityIds,
            String elementId,
            Map<String, Object> vars,
            String key1,
            String key2,
            TwoArgFormatter formatter
    ) {
        if (!reachedActivityIds.contains(elementId)) {
                return;
        }

        Object a = vars.get(key1);
        Object b = vars.get(key2);
        String label = formatter.format(a, b);
        if (label != null && !label.isBlank()) {
            overlays.add(new UiHistoryModels.DiagramOverlay(elementId, label));
        }
    }

    private static void addOverlayIfPresent(
            List<UiHistoryModels.DiagramOverlay> overlays,
            Set<String> reachedActivityIds,
            String elementId,
            Map<String, Object> vars,
            String key1,
            OneArgFormatter formatter
    ) {
        if (!reachedActivityIds.contains(elementId)) {
                return;
        }

        Object a = vars.get(key1);
        String label = formatter.format(a);
        if (label != null && !label.isBlank()) {
            overlays.add(new UiHistoryModels.DiagramOverlay(elementId, label));
        }
    }

    private static void addReviewOverlayIfPresent(
        List<UiHistoryModels.DiagramOverlay> overlays,
        Set<String> reachedActivityIds,
        Set<String> activeActivityIds,
        String elementId,
        Map<String, Object> vars
    ) {
        if (!reachedActivityIds.contains(elementId)) {
                return;
        }

        Object reviewDecision = vars.get("reviewDecision");
        String reviewDecisionText = reviewDecision != null ? reviewDecision.toString().trim() : null;

        String label;
        if (reviewDecisionText != null && !reviewDecisionText.isBlank()) {
                label = "Review: " + reviewDecisionText;
        } else if (activeActivityIds.contains(elementId)) {
                label = "Review pending";
        } else {
                label = null;
        }

        if (label != null && !label.isBlank()) {
                overlays.add(new UiHistoryModels.DiagramOverlay(elementId, label));
        }
    }
}