package co.summit58.feewaiver.sb.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import co.summit58.feewaiver.sb.entities.*;
import co.summit58.feewaiver.sb.models.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisPersistenceService {

    private final AnalysisEntityRepository repository;
    private final ObjectMapper objectMapper;

    public AnalysisPersistenceService(AnalysisEntityRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public ReviewResponse review(Long id, ReviewRequest request) {
        AnalysisEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Analysis record not found: " + id));

        if (!Boolean.TRUE.equals(entity.getRequiresHumanReview())) {
            throw new IllegalStateException("Human review is not required for analysis record: " + id);
        }

        if ("COMPLETED".equals(entity.getReviewStatus())) {
            throw new IllegalStateException("Analysis record has already been reviewed: " + id);
        }

        String decision = request.decision().trim().toUpperCase();

        String resolutionStatus;
        switch (decision) {
            case "APPROVE" -> resolutionStatus = "APPROVED";
            case "REJECT" -> resolutionStatus = "REJECTED";
            case "MODIFY" -> resolutionStatus = "MODIFIED";
            default -> throw new IllegalArgumentException("Unsupported review decision: " + request.decision());
        }

        entity.setReviewStatus("COMPLETED");
        entity.setReviewDecision(decision);
        entity.setResolutionStatus(resolutionStatus);
        entity.setReviewer(request.reviewer());
        entity.setReviewNotes(request.notes());
        entity.setReviewedAt(OffsetDateTime.now());

        AnalysisEntity saved = repository.save(entity);

        return new ReviewResponse(
            saved.getId(),
            saved.getReviewStatus(),
            saved.getReviewDecision(),
            saved.getResolutionStatus(),
            saved.getReviewer(),
            saved.getReviewNotes(),
            saved.getReviewedAt() != null ? saved.getReviewedAt().toString() : null
        );
    }

    public AnalysisEntity save(
            AnalyzeRequest request,
            GovernedAssessment governedAssessment,
            PolicyDecision policyDecision
    ) throws Exception {

        AnalysisEntity entity = new AnalysisEntity();

        entity.setRequestText(request.requestText());
        entity.setPriorFeeWaiverCount(request.priorFeeWaiverCount());
        entity.setFeeAmount(BigDecimal.valueOf(request.feeAmount()));
        entity.setVulnerableCustomer(request.vulnerableCustomer());
        entity.setRegulatorySensitivity(request.regulatorySensitivity());

        AiAssessment ai = governedAssessment.aiAssessment();
        entity.setAiCaseSummary(ai.caseSummary());
        entity.setAiIntent(ai.intent());
        entity.setAiRiskLevel(ai.riskLevel());
        entity.setAiConfidence(ai.confidence());
        entity.setAiRecommendedAction(ai.recommendedAction());
        entity.setAiReasoningSummary(ai.reasoningSummary());

        entity.setPolicyFlagsJson(objectMapper.writeValueAsString(governedAssessment.policyFlags()));
        entity.setRequiresHumanReview(governedAssessment.requiresHumanReview());

        entity.setPolicyRoute(policyDecision.policyRoute());
        entity.setFinalAction(policyDecision.finalAction());
        entity.setTriggeredPoliciesJson(objectMapper.writeValueAsString(policyDecision.triggeredPolicies()));
        entity.setPolicyExplanation(policyDecision.explanation());

        entity.setCreatedAt(OffsetDateTime.now());

        entity.setReviewStatus(governedAssessment.requiresHumanReview() ? "PENDING" : "NOT_REQUIRED");

        entity.setDrlEscalate(governedAssessment.drlEscalate());
        entity.setDrlRequiresReview(governedAssessment.drlRequiresReview());
        entity.setDrlReason(governedAssessment.drlReason());

        if (!governedAssessment.requiresHumanReview()) {
            entity.setResolutionStatus("AUTO_APPROVED");
        } 
        else if ("ESCALATE".equals(policyDecision.policyRoute())) {
            entity.setResolutionStatus("ESCALATED");
        }
        else {
            entity.setResolutionStatus("PENDING");
        }

        return repository.save(entity);
    }

    public List<AnalysisRecordResponse> findAll() throws Exception {
    List<AnalysisEntity> entities = repository.findAll()
            .stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .toList();

    List<AnalysisRecordResponse> results = new ArrayList<>();
    for (AnalysisEntity entity : entities) {
        results.add(toResponse(entity));
    }
    return results;
}

    public AnalysisRecordResponse findById(Long id) throws Exception {
        AnalysisEntity entity = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Analysis record not found: " + id));

        return toResponse(entity);
    }

    public List<AnalysisRecordResponse> findPendingReview() throws Exception {
        List<AnalysisEntity> entities = repository.findByReviewStatusOrderByCreatedAtDesc("PENDING");

        List<AnalysisRecordResponse> results = new ArrayList<>();
        for (AnalysisEntity entity : entities) {
            results.add(toResponse(entity));
        }
        return results;
    }

    private AnalysisRecordResponse toResponse(AnalysisEntity entity) throws Exception {
        List<String> policyFlags = objectMapper.readValue(
                entity.getPolicyFlagsJson(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );

        List<String> triggeredPolicies = objectMapper.readValue(
                entity.getTriggeredPoliciesJson(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, String.class)
        );

        return new AnalysisRecordResponse(
            entity.getId(),
            entity.getRequestText(),
            entity.getPriorFeeWaiverCount(),
            entity.getFeeAmount().doubleValue(),
            entity.getVulnerableCustomer(),
            entity.getRegulatorySensitivity(),
            entity.getAiCaseSummary(),
            entity.getAiIntent(),
            entity.getAiRiskLevel(),
            entity.getAiConfidence(),
            entity.getAiRecommendedAction(),
            entity.getAiReasoningSummary(),
            policyFlags,
            entity.getRequiresHumanReview(),
            entity.getPolicyRoute(),
            entity.getFinalAction(),
            triggeredPolicies,
            entity.getPolicyExplanation(),
            entity.getReviewStatus(),
            entity.getReviewDecision(),
            entity.getResolutionStatus(),
            entity.getReviewer(),
            entity.getReviewNotes(),
            entity.getReviewedAt() != null ? entity.getReviewedAt().toString() : null,
            entity.getCreatedAt().toString(),
            entity.getDrlEscalate(),
            entity.getDrlRequiresReview(),
            entity.getDrlReason()
        );
    }
}