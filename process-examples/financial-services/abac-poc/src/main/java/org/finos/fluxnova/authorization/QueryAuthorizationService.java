package org.finos.fluxnova.authorization;

import org.finos.fluxnova.iac.MockIacClient;
import org.finos.fluxnova.resolver.AbacResourceResolver;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class QueryAuthorizationService {

    private final MockIacClient iacClient;

    public QueryAuthorizationService(MockIacClient iacClient) {
        this.iacClient = iacClient;
    }

    public <T> List<T> filter(List<T> resources, String userId, AbacResourceResolver<T> resolver) {
        return resources.stream()
                .filter(resource -> isAuthorized(resource, userId, resolver))
                .toList();
    }

    public <T> boolean isAuthorized(T resource, String userId, AbacResourceResolver<T> resolver) {
        Map<String, String> userAttributes = iacClient.getAttributes(userId);
        return matches(resolver.getAttributes(resource), userAttributes);
    }

    public <T> boolean isAuthorizedForWrite(T resource, String userId, AbacResourceResolver<T> resolver) {
        Map<String, String> resourceAttributes = resolver.getAttributes(resource);
        return !resourceAttributes.isEmpty()
                && matches(resourceAttributes, iacClient.getAttributes(userId));
    }

    boolean matches(Map<String, String> resourceAttributes, Map<String, String> userAttributes) {
        if (resourceAttributes == null || resourceAttributes.isEmpty()) {
            return true;
        }

        for (Map.Entry<String, String> entry : resourceAttributes.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String userValue = userAttributes.get(entry.getKey());
            if (!entry.getValue().equals(userValue)) {
                return false;
            }
        }

        return true;
    }
}
