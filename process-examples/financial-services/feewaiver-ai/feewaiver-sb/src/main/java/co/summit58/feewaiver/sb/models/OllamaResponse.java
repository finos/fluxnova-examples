package co.summit58.feewaiver.sb.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OllamaResponse(
            String model,
            @JsonProperty("created_at")
            String createdAt,
            OllamaMessage message,
            boolean done,
            String done_reason,
            Long total_duration,
            Long load_duration,
            Integer prompt_eval_count,
            Long prompt_eval_duration,
            Integer eval_count,
            Long eval_duration
    ) {}