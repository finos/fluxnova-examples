package co.summit58.feewaiver.sb.models;

import jakarta.validation.constraints.NotBlank;

public record ReviewRequest(
        @NotBlank
        String reviewer,

        @NotBlank
        String decision,

        String notes
) {}