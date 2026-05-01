package co.summit58.feewaiver.sb.models;

public class FinalGuardrailFacts {

    private String policyRoute;
    private double aiConfidence;

    private boolean finalGuardrailOverride;
    private String finalGuardrailReason;

    public String getPolicyRoute() {
        return policyRoute;
    }

    public void setPolicyRoute(String policyRoute) {
        this.policyRoute = policyRoute;
    }

    public double getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(double aiConfidence) {
        this.aiConfidence = aiConfidence;
    }

    public boolean isFinalGuardrailOverride() {
        return finalGuardrailOverride;
    }

    public void setFinalGuardrailOverride(boolean finalGuardrailOverride) {
        this.finalGuardrailOverride = finalGuardrailOverride;
    }

    public String getFinalGuardrailReason() {
        return finalGuardrailReason;
    }

    public void setFinalGuardrailReason(String finalGuardrailReason) {
        this.finalGuardrailReason = finalGuardrailReason;
    }
}