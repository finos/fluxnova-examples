package co.summit58.feewaiver.sb.models;

public record AiStepResult(
        String type, //"FINAL" or "TOOL_REQUEST"
        AiAssessment aiAssessment,
        ToolRequest toolRequest
) {}