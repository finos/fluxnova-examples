package co.summit58.feewaiver.sb.models;

import java.util.Map;

public record ToolRequest(
        String toolName,
        java.util.Map<String, Object> arguments
) {}