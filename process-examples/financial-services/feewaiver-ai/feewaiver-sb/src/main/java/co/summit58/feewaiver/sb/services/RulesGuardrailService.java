package co.summit58.feewaiver.sb.services;

import co.summit58.feewaiver.sb.models.*;
import org.kie.api.KieServices;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.utils.KieHelper;
import org.springframework.stereotype.Service;

@Service
public class RulesGuardrailService {

    private final StatelessKieSession statelessKieSession;

    public RulesGuardrailService() {
        KieHelper kieHelper = new KieHelper();
        kieHelper.addResource(
                KieServices.get().getResources()
                        .newClassPathResource("rules/fee-waiver-guardrails.drl"),
                ResourceType.DRL
        );

        this.statelessKieSession = kieHelper.build().newStatelessKieSession();
    }

    public FeeWaiverFacts evaluate(AnalyzeRequest request, AiAssessment aiAssessment) {
        FeeWaiverFacts facts = new FeeWaiverFacts();
        facts.setFeeAmount(request.feeAmount() != null ? request.feeAmount() : 0.0);
        facts.setPriorFeeWaiverCount(request.priorFeeWaiverCount() != null ? request.priorFeeWaiverCount() : 0);
        facts.setRegulatorySensitivity(Boolean.TRUE.equals(request.regulatorySensitivity()));
        facts.setVulnerableCustomer(Boolean.TRUE.equals(request.vulnerableCustomer()));
        facts.setAiRiskLevel(aiAssessment.riskLevel());
        facts.setAiRecommendedAction(aiAssessment.recommendedAction());

        statelessKieSession.execute(facts);
        return facts;
    }
}