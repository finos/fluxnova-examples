package org.finos.fluxnova.abac.services;

import org.finos.fluxnova.authorization.QueryAuthorizationService;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinition;
import org.finos.fluxnova.resolver.ProcessDefinitionAbacResolver;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AbacProcessDefinitionService {

    private final RepositoryService repositoryService;
    private final QueryAuthorizationService queryAuthorizationService;
    private final ProcessDefinitionAbacResolver resolver;

    public AbacProcessDefinitionService(
            RepositoryService repositoryService,
            QueryAuthorizationService queryAuthorizationService,
            ProcessDefinitionAbacResolver resolver) {
        this.repositoryService = repositoryService;
        this.queryAuthorizationService = queryAuthorizationService;
        this.resolver = resolver;
    }

    public List<ProcessDefinition> getVisibleProcesses(String userId) {
        List<ProcessDefinition> processDefinitions = repositoryService
                .createProcessDefinitionQuery()
                .active()
                .list();

        return queryAuthorizationService.filter(processDefinitions, userId, resolver);
    }
}

