package org.finos.fluxnova.authorization;

import org.finos.fluxnova.iac.MockIacClient;
import org.finos.fluxnova.resolver.AbacResourceResolver;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QueryAuthorizationServiceTest {

    private final QueryAuthorizationService service = new QueryAuthorizationService(new MockIacClient());

    @Test
    void filtersResourcesByMockedIacAttributes() {
        List<String> resources = List.of("wealthTask", "operationsTask", "publicTask");
        AbacResourceResolver<String> resolver = resource -> switch (resource) {
            case "wealthTask" -> Map.of(
                    "attr-region", "US",
                    "attr-business", "WEALTH",
                    "attr-department", "TRADING");
            case "operationsTask" -> Map.of(
                    "attr-region", "EU",
                    "attr-business", "OPERATIONS",
                    "attr-department", "SETTLEMENT");
            default -> Map.of();
        };

        List<String> visibleResources = service.filter(resources, "wealth-us-user", resolver);

        assertThat(visibleResources).containsExactly("wealthTask", "publicTask");
    }

    @Test
    void authorizesTaskWritesUsingTheSameMockedAttributesAsReads() {
        AbacResourceResolver<String> resolver = resource -> switch (resource) {
            case "wealthTask" -> Map.of(
                    "attr-region", "US",
                    "attr-business", "WEALTH",
                    "attr-department", "TRADING");
            case "operationsTask" -> Map.of(
                    "attr-region", "EU",
                    "attr-business", "OPERATIONS",
                    "attr-department", "SETTLEMENT");
            default -> Map.of("attr-region", "APAC");
        };

        assertThat(service.isAuthorized("wealthTask", "wealth-us-user", resolver)).isTrue();
        assertThat(service.isAuthorized("operationsTask", "wealth-us-user", resolver)).isFalse();
        assertThat(service.isAuthorized("operationsTask", "operations-eu-user", resolver)).isTrue();
        assertThat(service.isAuthorized("apacTask", "apac-wealth-user", resolver)).isTrue();
        assertThat(service.isAuthorizedForWrite("wealthTask", "unknown-user", resolver)).isFalse();
        assertThat(service.isAuthorizedForWrite("publicTask", "wealth-us-user", resolver)).isFalse();
    }

    @Test
    void resourcesWithoutAbacAttributesAreVisibleToUnknownUsers() {
        List<String> resources = List.of("publicTask", "restrictedTask");
        AbacResourceResolver<String> resolver = resource -> "publicTask".equals(resource)
                ? Map.of()
                : Map.of("attr-region", "US");

        List<String> visibleResources = service.filter(resources, "unknown-user", resolver);

        assertThat(visibleResources).containsExactly("publicTask");
    }
}
