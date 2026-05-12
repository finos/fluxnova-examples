package co.summit58.feewaiver.sb.models;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnalyzeRequest(
            @NotBlank
            String requestText,

            @NotNull
            Integer priorFeeWaiverCount,

            @NotNull
            @DecimalMin("0.0")
            Double feeAmount,

            @NotNull
            Boolean vulnerableCustomer,

            @NotNull
            Boolean regulatorySensitivity,

            String processInstanceId
    ) {}