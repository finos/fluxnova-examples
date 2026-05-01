package co.summit58.feewaiver.sb.models;

public class FeeWaiverFacts {

    private double feeAmount;
    private int priorFeeWaiverCount;
    private boolean regulatorySensitivity;
    private boolean vulnerableCustomer;

    private String aiRiskLevel;
    private String aiRecommendedAction;

    private boolean drlEscalate;
    private boolean drlRequiresReview;
    private String drlReason;

    public double getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(double feeAmount) {
        this.feeAmount = feeAmount;
    }

    public int getPriorFeeWaiverCount() {
        return priorFeeWaiverCount;
    }

    public void setPriorFeeWaiverCount(int priorFeeWaiverCount) {
        this.priorFeeWaiverCount = priorFeeWaiverCount;
    }

    public boolean isRegulatorySensitivity() {
        return regulatorySensitivity;
    }

    public void setRegulatorySensitivity(boolean regulatorySensitivity) {
        this.regulatorySensitivity = regulatorySensitivity;
    }

    public boolean isVulnerableCustomer() {
        return vulnerableCustomer;
    }

    public void setVulnerableCustomer(boolean vulnerableCustomer) {
        this.vulnerableCustomer = vulnerableCustomer;
    }

    public String getAiRiskLevel() {
        return aiRiskLevel;
    }

    public void setAiRiskLevel(String aiRiskLevel) {
        this.aiRiskLevel = aiRiskLevel;
    }

    public String getAiRecommendedAction() {
        return aiRecommendedAction;
    }

    public void setAiRecommendedAction(String aiRecommendedAction) {
        this.aiRecommendedAction = aiRecommendedAction;
    }

    public boolean isDrlEscalate() {
        return drlEscalate;
    }

    public void setDrlEscalate(boolean drlEscalate) {
        this.drlEscalate = drlEscalate;
    }

    public boolean isDrlRequiresReview() {
        return drlRequiresReview;
    }

    public void setDrlRequiresReview(boolean drlRequiresReview) {
        this.drlRequiresReview = drlRequiresReview;
    }

    public String getDrlReason() {
        return drlReason;
    }

    public void setDrlReason(String drlReason) {
        this.drlReason = drlReason;
    }
}