package co.summit58.feewaiver.sb.models;

import java.util.List;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PolicyDecision(
            String policyRoute,
            String finalAction,
            List<String> triggeredPolicies,
            String explanation
    ) {}