package org.finos.fluxnova.authorization;

import org.finos.fluxnova.bpm.engine.AuthorizationException;
import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.TaskService;
import org.finos.fluxnova.bpm.engine.impl.interceptor.Command;
import org.finos.fluxnova.bpm.engine.impl.interceptor.CommandInterceptor;
import org.finos.fluxnova.bpm.engine.task.IdentityLink;
import org.finos.fluxnova.bpm.engine.task.Task;
import org.finos.fluxnova.resolver.TaskAbacResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.List;

@Component
public class OpaCompleteTaskAuthorizationInterceptor extends CommandInterceptor {

    private static final String COMPLETE_TASK_CMD_CLASS = "CompleteTaskCmd";
    private static final String CANDIDATE_LINK_TYPE = "candidate";

    private final TaskService taskService;
    private final IdentityService identityService;
    private final OpaPolicyClient policyClient;
    private final QueryAuthorizationService queryAuthorizationService;
    private final TaskAbacResolver taskResolver;
    private final String configuredTenantId;

    public OpaCompleteTaskAuthorizationInterceptor(
            @Lazy TaskService taskService,
            @Lazy IdentityService identityService,
            OpaPolicyClient policyClient,
            QueryAuthorizationService queryAuthorizationService,
            @Lazy TaskAbacResolver taskResolver,
            @Value("${opa.tenant-id:}") String configuredTenantId) {
        this.taskService = taskService;
        this.identityService = identityService;
        this.policyClient = policyClient;
        this.queryAuthorizationService = queryAuthorizationService;
        this.taskResolver = taskResolver;
        this.configuredTenantId = configuredTenantId;
    }

    @Override
    public <T> T execute(Command<T> command) {
        if (isCompleteTaskCmd(command)) {
            authorizeTaskComplete(command);
        }
        return next.execute(command);
    }

    private boolean isCompleteTaskCmd(Command<?> command) {
        return command.getClass().getSimpleName().equals(COMPLETE_TASK_CMD_CLASS);
    }

    private void authorizeTaskComplete(Command<?> command) {
        String taskId = extractTaskId(command);
        if (taskId == null || taskId.isBlank()) {
            throw new AuthorizationException("Access denied: unable to resolve task id for completeTask command");
        }

        var authentication = identityService.getCurrentAuthentication();
        String userId = authentication == null ? null : authentication.getUserId();
        if (userId == null || userId.isBlank()) {
            throw new AuthorizationException("Access denied: authentication is required to complete task '" + taskId + "'");
        }

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new AuthorizationException("Access denied: task not found for id " + taskId);
        }

        List<String> candidateGroups = taskService.getIdentityLinksForTask(taskId).stream()
                .filter(link -> CANDIDATE_LINK_TYPE.equals(link.getType()))
                .map(IdentityLink::getGroupId)
                .filter(groupId -> groupId != null && !groupId.isBlank())
                .distinct()
                .toList();

        if (candidateGroups.isEmpty()) {
            throw new AuthorizationException(
                    "Access denied for user '" + userId + "' to complete task '" + taskId
                            + "': task has no candidate group");
        }

        String tenantId = resolveTenantId(task);
        List<String> invalidCandidateGroups = candidateGroups.stream()
                .filter(candidateGroup -> !policyClient.isValidCandidateGroup(tenantId, candidateGroup))
                .toList();

        if (!invalidCandidateGroups.isEmpty()) {
            throw new AuthorizationException(
                    "Access denied for user '" + userId + "' to complete task '" + taskId
                            + "': invalid candidate groups " + invalidCandidateGroups
                            + " for tenant '" + tenantId + "'");
        }

        if (!queryAuthorizationService.isAuthorizedForWrite(task, userId, taskResolver)) {
            throw new AuthorizationException(
                    "Access denied for user '" + userId + "' to complete task '" + taskId
                            + "': task attributes do not match user attributes");
        }
    }

    private String resolveTenantId(Task task) {
        String taskTenantId = task.getTenantId();
        if (taskTenantId != null && !taskTenantId.isBlank()) {
            return taskTenantId;
        }
        if (configuredTenantId != null && !configuredTenantId.isBlank()) {
            return configuredTenantId;
        }
        throw new AuthorizationException("Access denied: tenant id is required to validate candidate groups via OPA");
    }

    private String extractTaskId(Command<?> command) {
        Class<?> type = command.getClass();
        while (type != null) {
            try {
                Field taskIdField = type.getDeclaredField("taskId");
                taskIdField.setAccessible(true);
                Object value = taskIdField.get(command);
                return value instanceof String taskId ? taskId : null;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }
}
