package org.finos.fluxnova.controllers;

import org.finos.fluxnova.abac.services.AbacProcessDefinitionService;
import org.finos.fluxnova.abac.services.AbacTaskService;
import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.impl.identity.Authentication;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinition;
import org.finos.fluxnova.bpm.engine.task.Task;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/abac")
public class AbacTestController {

    private final AbacTaskService abacTaskService;
    private final AbacProcessDefinitionService processDefinitionService;
    private final IdentityService identityService;

    public AbacTestController(
            AbacTaskService abacTaskService,
            AbacProcessDefinitionService processDefinitionService,
            IdentityService identityService) {
        this.abacTaskService = abacTaskService;
        this.processDefinitionService = processDefinitionService;
        this.identityService = identityService;
    }

    @GetMapping("/tasks")
    public List<String> getTasks() {
        return abacTaskService.getVisibleTasks(currentUserId()).stream()
                .map(Task::getName)
                .toList();
    }

    @GetMapping("/processes")
    public List<String> getProcesses() {
        return processDefinitionService.getVisibleProcesses(currentUserId()).stream()
                .map(ProcessDefinition::getKey)
                .toList();
    }

    private String currentUserId() {
        Authentication authentication = identityService.getCurrentAuthentication();
        if (authentication == null || authentication.getUserId() == null || authentication.getUserId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return authentication.getUserId();
    }
}
