package co.summit58.feewaiver.sb.services;

import co.summit58.feewaiver.sb.models.FinalGuardrailFacts;
import org.kie.api.KieServices;
import org.kie.api.io.ResourceType;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.internal.utils.KieHelper;
import org.springframework.stereotype.Service;

@Service
public class FinalGuardrailService {

    private final StatelessKieSession statelessKieSession;

    public FinalGuardrailService() {
        KieHelper kieHelper = new KieHelper();
        kieHelper.addResource(
                KieServices.get().getResources()
                        .newClassPathResource("rules/final-guardrail.drl"),
                ResourceType.DRL
        );

        this.statelessKieSession = kieHelper.build().newStatelessKieSession();
    }

    public FinalGuardrailFacts evaluate(String policyRoute, double aiConfidence) {
        FinalGuardrailFacts facts = new FinalGuardrailFacts();
        facts.setPolicyRoute(policyRoute);
        facts.setAiConfidence(aiConfidence);

        statelessKieSession.execute(facts);
        return facts;
    }
}