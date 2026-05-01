package co.summit58.feewaiver.sb.services;

import org.springframework.stereotype.Service;

import co.summit58.feewaiver.sb.entities.AnalysisEntity;
import co.summit58.feewaiver.sb.models.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisService {

    private final OllamaClientService ollamaClientService;
    private final PolicyDecisionService policyDecisionService;
    private final AnalysisPersistenceService analysisPersistenceService;
    private final ToolRequestService toolRequestService;
    private final MessageCorrelationService messageCorrelationService;
    private final RulesGuardrailService rulesGuardrailService;

    public AnalysisService(
            OllamaClientService ollamaClientService,
            PolicyDecisionService policyDecisionService,
            AnalysisPersistenceService analysisPersistenceService,
            ToolRequestService toolRequestService,
            MessageCorrelationService messageCorrelationService,
            RulesGuardrailService rulesGuardrailService) {
        this.ollamaClientService = ollamaClientService;
        this.policyDecisionService = policyDecisionService;
        this.analysisPersistenceService = analysisPersistenceService;
        this.toolRequestService = toolRequestService;
        this.messageCorrelationService = messageCorrelationService;
        this.rulesGuardrailService = rulesGuardrailService;
    }

    public AnalyzeResponse analyze(AnalyzeRequest request) 
            throws Exception {
        AiStepResult firstStep = ollamaClientService.analyzeFeeWaiverWithToolOption(request);

        AiAssessment aiAssessment;
        ToolRequest toolRequest = null;
        Map<String, Object> toolResult = null;
        String aiStepType = firstStep.type();

        if ("TOOL_REQUEST".equals(aiStepType)) {
            toolRequest = firstStep.toolRequest();

            if (toolRequest == null || toolRequest.toolName() == null || toolRequest.toolName().isBlank()) {
                throw new IllegalStateException("AI returned TOOL_REQUEST without a valid toolRequest payload");
            }

            toolResult = toolRequestService.execute(toolRequest);

            AiStepResult secondStep =
                    ollamaClientService.continueAfterToolResult(request, toolRequest, toolResult);

            if (!"FINAL".equals(secondStep.type()) || secondStep.aiAssessment() == null) {
                throw new IllegalStateException("AI did not return FINAL after tool execution");
            }

            aiAssessment = secondStep.aiAssessment();
        } else if ("FINAL".equals(aiStepType)) {
            if (firstStep.aiAssessment() == null) {
                throw new IllegalStateException("AI returned FINAL without aiAssessment");
            }
            aiAssessment = firstStep.aiAssessment();
        } else {
            throw new IllegalStateException("Unsupported AI step result type: " + aiStepType);
        }

        List<String> policyFlags = derivePolicyFlags(request, aiAssessment);
        boolean requiresHumanReview = requiresHumanReview(aiAssessment, policyFlags);

        FeeWaiverFacts rulesFacts = rulesGuardrailService.evaluate(request, aiAssessment);

        GovernedAssessment governedAssessment =
                new GovernedAssessment(
                        aiAssessment,
                        policyFlags,
                        requiresHumanReview,
                        aiStepType,
                        toolRequest,
                        toolResult,
                        rulesFacts.isDrlEscalate(),
                        rulesFacts.isDrlRequiresReview(),
                        rulesFacts.getDrlReason()
                );

        PolicyDecision policyDecision = policyDecisionService.evaluate(governedAssessment);

        AnalysisEntity saved = analysisPersistenceService.save(request, governedAssessment, policyDecision);

        return new AnalyzeResponse(
                saved.getId(),
                governedAssessment,
                policyDecision,
                saved.getReviewStatus(),
                saved.getResolutionStatus()
        );
    }

    private List<String> derivePolicyFlags(AnalyzeRequest input, AiAssessment ai) {
        List<String> flags = new ArrayList<>();

        if (input.priorFeeWaiverCount() != null && input.priorFeeWaiverCount() > 0) {
            flags.add("REPEAT_REQUEST");
        }

        if (input.feeAmount() != null && input.feeAmount() >= 500.0) {
            flags.add("HIGH_DOLLAR_AMOUNT");
        }

        if (Boolean.TRUE.equals(input.vulnerableCustomer())) {
            flags.add("VULNERABLE_CUSTOMER");
        }

        if (Boolean.TRUE.equals(input.regulatorySensitivity())) {
            flags.add("REGULATORY_SENSITIVITY");
        }

        if (ai.confidence() < 0.80) {
            flags.add("LOW_CONFIDENCE");
        }

        if (ai.caseSummary() == null || ai.caseSummary().isBlank()) {
            flags.add("INSUFFICIENT_INFORMATION");
        }

        return flags;
    }

    private boolean requiresHumanReview(AiAssessment ai, List<String> flags) {
        boolean mediumOrHighRisk = "MEDIUM".equals(ai.riskLevel()) || "HIGH".equals(ai.riskLevel());
        boolean lowConfidence = ai.confidence() < 0.80;
        boolean reviewFlagPresent = flags.contains("HIGH_DOLLAR_AMOUNT")
                || flags.contains("REPEAT_REQUEST")
                || flags.contains("VULNERABLE_CUSTOMER")
                || flags.contains("INSUFFICIENT_INFORMATION")
                || flags.contains("REGULATORY_SENSITIVITY");

        return mediumOrHighRisk || lowConfidence || reviewFlagPresent;
    }
}