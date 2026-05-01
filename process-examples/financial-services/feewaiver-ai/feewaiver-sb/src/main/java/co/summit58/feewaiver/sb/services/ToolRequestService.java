package co.summit58.feewaiver.sb.services;

import org.springframework.stereotype.Service;

import co.summit58.feewaiver.sb.models.*;

import java.util.HashMap;
import java.util.Map;

@Service
public class ToolRequestService {

    public Map<String, Object> execute(ToolRequest toolRequest) {
        if (toolRequest == null || toolRequest.toolName() == null) {
            throw new IllegalArgumentException("Tool request is missing toolName");
        }

        return switch (toolRequest.toolName()) {
            case "getFeeWaiverHistory" -> stubFeeWaiverHistory(toolRequest.arguments());
            default -> throw new IllegalArgumentException(
                    "Unsupported tool request: " + toolRequest.toolName()
            );
        };
    }

    private Map<String, Object> stubFeeWaiverHistory(Map<String, Object> arguments) {
        String customerId = arguments != null && arguments.get("customerId") != null
                ? arguments.get("customerId").toString()
                : "UNKNOWN";

        Map<String, Object> result = new HashMap<>();
        result.put("customerId", customerId);
        result.put("priorFeeWaiverCount", 3);
        result.put("lastFeeWaiverDate", "2026-02-10");
        result.put("historySummary", "Customer has received 3 prior fee waivers in the last 12 months.");
        return result;
    }
}