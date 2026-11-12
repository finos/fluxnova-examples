package org.finos.fluxnova.authorization;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class OpaPolicyClient {

    private static final Map<String, Set<String>> MOCK_ENABLED_CANDIDATE_GROUPS = Map.of(
            "tenant-abac-poc", Set.of(
                    "attr-4536f34c:val-10149960",
                    "attr-4536f34c:val-20284710"));

    private final boolean mockEnabled;
    private final String opaBaseUrl;
    private final String validCandidateGroupPath;

    public OpaPolicyClient(
            @Value("${opa.mock-enabled:true}") boolean mockEnabled,
            @Value("${opa.base-url:http://localhost:8181}") String opaBaseUrl,
            @Value("${opa.valid-candidate-group-path:/v1/data/iac/attribute_validation/valid_candidate_group}") String validCandidateGroupPath) {
        this.mockEnabled = mockEnabled;
        this.opaBaseUrl = opaBaseUrl;
        this.validCandidateGroupPath = validCandidateGroupPath;
    }

    public boolean isValidCandidateGroup(String tenantId, String candidateGroup) {
        if (tenantId == null || tenantId.isBlank() || candidateGroup == null || candidateGroup.isBlank()) {
            return false;
        }

        if (mockEnabled) {
            return isValidCandidateGroupFromMock(tenantId, candidateGroup);
        }

        return isValidCandidateGroupFromOpaPlaceholder(tenantId, candidateGroup);
    }

    private boolean isValidCandidateGroupFromMock(String tenantId, String candidateGroup) {
        return MOCK_ENABLED_CANDIDATE_GROUPS.getOrDefault(tenantId, Set.of()).contains(candidateGroup);
    }

    private boolean isValidCandidateGroupFromOpaPlaceholder(String tenantId, String candidateGroup) {
        // Real OPA integration placeholder:
        // 1. Inject RestClient.Builder or WebClient.Builder into this service.
        // 2. POST to opaBaseUrl + validCandidateGroupPath.
        // 3. Use this JSON body shape:
        //    { "input": { "tenant_id": tenantId, "candidate_group": candidateGroup } }
        // 4. Deserialize OPA's response shape:
        //    { "result": true }
        // 5. Return response.result; throw or deny on transport/deserialization failures.
        //
        // The fields are referenced here so configuration remains visible and validated by tests/tools.
        throw new UnsupportedOperationException(
                "Real OPA calls are not implemented in this POC. Configure opa.mock-enabled=true or implement POST "
                        + opaBaseUrl + validCandidateGroupPath);
    }
}

