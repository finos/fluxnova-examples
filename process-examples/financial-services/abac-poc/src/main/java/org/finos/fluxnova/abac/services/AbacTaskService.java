package org.finos.fluxnova.abac.services;

import org.finos.fluxnova.authorization.QueryAuthorizationService;
import org.finos.fluxnova.bpm.engine.TaskService;
import org.finos.fluxnova.bpm.engine.task.Task;
import org.finos.fluxnova.resolver.TaskAbacResolver;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AbacTaskService {

    private final TaskService taskService;
    private final QueryAuthorizationService queryAuthorizationService;
    private final TaskAbacResolver taskResolver;

    public AbacTaskService(
            TaskService taskService,
            QueryAuthorizationService queryAuthorizationService,
            TaskAbacResolver taskResolver) {
        this.taskService = taskService;
        this.queryAuthorizationService = queryAuthorizationService;
        this.taskResolver = taskResolver;
    }

    public List<Task> getVisibleTasks(String userId) {
        List<Task> tasks = taskService.createTaskQuery().list();
        return queryAuthorizationService.filter(tasks, userId, taskResolver);
    }
}

