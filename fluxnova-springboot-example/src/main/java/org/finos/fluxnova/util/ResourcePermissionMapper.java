package org.finos.fluxnova.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum ResourcePermissionMapper {
    APPLICATION(0, Arrays.asList("ALL")),
    USER(1, Arrays.asList("READ", "UPDATE" , "CREATE")),
    GROUP(2, Arrays.asList("READ", "UPDATE" , "CREATE")),
    GROUP_MEMBERSHIP(3, Arrays.asList("CREATE")),
    AUTHORIZATION(4, Arrays.asList("READ", "UPDATE" , "CREATE")),
    FILTER(5, Arrays.asList("READ", "UPDATE" , "CREATE")),
    PROCESS_DEFINITION(6, Arrays.asList("READ", "UPDATE")),
    TASK(7, Arrays.asList("READ", "UPDATE" , "CREATE")),
    PROCESS_INSTANCE(8, Arrays.asList("READ", "UPDATE" , "CREATE")),
    DEPLOYMENT(9, Arrays.asList("READ", "CREATE")),
    DECISION_DEFINITION( 10, Arrays.asList("READ", "UPDATE")),
    TENANT(11, Arrays.asList("READ", "UPDATE" , "CREATE")),
    TENANT_MEMBERSHIP(12, Arrays.asList("CREATE")),
    BATCH(13, Arrays.asList("READ", "UPDATE" , "CREATE")),
    DECISION_REQUIREMENTS_DEFINITION( 14, Arrays.asList("READ")),
    REPORT(15, Arrays.asList("READ", "UPDATE" , "CREATE")),
    DASHBOARD( 16, Arrays.asList("READ", "UPDATE", "CREATE" )),
    OPERATION_LOG_CATEGORY( 17, Arrays.asList("READ", "UPDATE")),
    HISTORIC_TASK(19, Arrays.asList("READ")),
    HISTORIC_PROCESS_INSTANCE(20, Arrays.asList("READ")),
    SYSTEM(21, Arrays.asList("READ"));

    private final int resourceType;
    private final List<String> permissions;

    ResourcePermissionMapper(int resourceType, List<String> permissions) {
        this.resourceType = resourceType;
        this.permissions = permissions;
    }

    public int getResourceType() {
        return resourceType;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public static List<String> getPermissionsByResourceType(int resourceType) {
        for (ResourcePermissionMapper mapper : values()) {
            if (mapper.resourceType == resourceType) {
                return mapper.permissions;
            }
        }
        return Collections.emptyList();
    }
}
