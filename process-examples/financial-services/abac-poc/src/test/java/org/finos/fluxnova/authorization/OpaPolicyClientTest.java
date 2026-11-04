package org.finos.fluxnova.authorization;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpaPolicyClientTest {

    @Test
    void mockAllowsOnlyEnabledCandidateGroupsForTenant() {
        OpaPolicyClient client = new OpaPolicyClient(
                true,
                "http://localhost:8181",
                "/v1/data/iac/attribute_validation/valid_candidate_group");

        assertThat(client.isValidCandidateGroup("tenant-abac-poc", "attr-4536f34c:val-10149960")).isTrue();
        assertThat(client.isValidCandidateGroup("tenant-abac-poc", "attr-4536f34c:val-20284710")).isTrue();
        assertThat(client.isValidCandidateGroup("tenant-abac-poc", "attr-4536f34c:val-99999999")).isFalse();
        assertThat(client.isValidCandidateGroup("unknown-tenant", "attr-4536f34c:val-10149960")).isFalse();
        assertThat(client.isValidCandidateGroup("tenant-abac-poc", "")).isFalse();
    }

    @Test
    void realOpaModeIsExplicitPlaceholderUntilHttpIntegrationIsImplemented() {
        OpaPolicyClient client = new OpaPolicyClient(
                false,
                "http://localhost:8181",
                "/v1/data/iac/attribute_validation/valid_candidate_group");

        assertThatThrownBy(() -> client.isValidCandidateGroup("tenant-abac-poc", "attr-4536f34c:val-10149960"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Real OPA calls are not implemented");
    }
}

