package org.finos.fluxnova.controllers;

import org.finos.fluxnova.abac.services.AbacProcessDefinitionService;
import org.finos.fluxnova.abac.services.AbacTaskService;
import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.impl.identity.Authentication;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinition;
import org.finos.fluxnova.bpm.engine.task.Task;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AbacTestControllerTest {

    private final AbacTaskService taskService = mock(AbacTaskService.class);
    private final AbacProcessDefinitionService processDefinitionService = mock(AbacProcessDefinitionService.class);
    private final IdentityService identityService = mock(IdentityService.class);
    private final AbacTestController controller = new AbacTestController(
            taskService,
            processDefinitionService,
            identityService);

    @Test
    void getsVisibleTasksForAuthenticatedUser() {
        Authentication authentication = new Authentication("wealth-us-user", List.of());
        Task task = mock(Task.class);

        when(identityService.getCurrentAuthentication()).thenReturn(authentication);
        when(taskService.getVisibleTasks("wealth-us-user")).thenReturn(List.of(task));
        when(task.getName()).thenReturn("Wealth Business Review");

        assertThat(controller.getTasks()).containsExactly("Wealth Business Review");
    }

    @Test
    void getsVisibleProcessesForAuthenticatedUser() {
        Authentication authentication = new Authentication("wealth-us-user", List.of());
        ProcessDefinition processDefinition = mock(ProcessDefinition.class);

        when(identityService.getCurrentAuthentication()).thenReturn(authentication);
        when(processDefinitionService.getVisibleProcesses("wealth-us-user")).thenReturn(List.of(processDefinition));
        when(processDefinition.getKey()).thenReturn("process-abac-poc");

        assertThat(controller.getProcesses()).containsExactly("process-abac-poc");
    }

    @Test
    void rejectsAnonymousRequests() {
        when(identityService.getCurrentAuthentication()).thenReturn(null);

        assertThatThrownBy(() -> controller.getTasks())
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Authentication required");
    }
}
