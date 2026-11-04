package org.finos.fluxnova.iac;

import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MockIacClient {

    public Map<String, String> getAttributes(String userId) {
        return switch (userId) {
            case "wealth-us-user", "demo" -> Map.of(
                    "attr-region", "US",
                    "attr-business", "WEALTH",
                    "attr-department", "TRADING");
            case "operations-eu-user" -> Map.of(
                    "attr-region", "EU",
                    "attr-business", "OPERATIONS",
                    "attr-department", "SETTLEMENT");
            case "apac-wealth-user" -> Map.of(
                    "attr-region", "APAC",
                    "attr-business", "WEALTH",
                    "attr-department", "OPERATIONS");
            default -> Map.of();
        };
    }
}

