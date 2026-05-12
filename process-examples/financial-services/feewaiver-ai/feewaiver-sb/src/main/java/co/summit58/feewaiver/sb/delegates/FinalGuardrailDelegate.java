package co.summit58.feewaiver.sb.delegates;

import co.summit58.feewaiver.sb.models.FinalGuardrailFacts;
import co.summit58.feewaiver.sb.services.FinalGuardrailService;

import org.finos.fluxnova.bpm.engine.delegate.DelegateExecution;
import org.finos.fluxnova.bpm.engine.delegate.JavaDelegate;

import org.springframework.stereotype.Component;

@Component
public class FinalGuardrailDelegate implements JavaDelegate {

    private final FinalGuardrailService finalGuardrailService;

    public FinalGuardrailDelegate(FinalGuardrailService finalGuardrailService) {
        this.finalGuardrailService = finalGuardrailService;
    }

    @Override
    public void execute(DelegateExecution execution) {
        String policyRoute = getRequiredString(execution, "policyRoute");
        double aiConfidence = getRequiredDouble(execution, "aiConfidence");

        FinalGuardrailFacts facts = finalGuardrailService.evaluate(policyRoute, aiConfidence);

        execution.setVariable("policyRoute", facts.getPolicyRoute());
        execution.setVariable("finalGuardrailOverride", facts.isFinalGuardrailOverride());
        execution.setVariable("finalGuardrailReason", facts.getFinalGuardrailReason());
    }

    private String getRequiredString(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (!(value instanceof String s) || s.isBlank()) {
            throw new IllegalArgumentException("Missing required String process variable: " + name);
        }
        return s;
    }

    private double getRequiredDouble(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        if (value instanceof Double d) return d;
        if (value instanceof Float f) return f.doubleValue();
        if (value instanceof Integer i) return i.doubleValue();
        if (value instanceof Long l) return l.doubleValue();
        if (value instanceof Number n) return n.doubleValue();
        if (value instanceof String s && !s.isBlank()) return Double.parseDouble(s);
        throw new IllegalArgumentException("Missing required numeric process variable: " + name);
    }
}