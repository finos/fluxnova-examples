package org.finos.fluxnova.resolver;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.task.Task;
import org.finos.fluxnova.bpm.model.bpmn.BpmnModelInstance;
import org.finos.fluxnova.bpm.model.bpmn.instance.ExtensionElements;
import org.finos.fluxnova.bpm.model.bpmn.instance.FlowElement;
import org.finos.fluxnova.bpm.model.bpmn.instance.fluxnova.FluxnovaProperties;
import org.finos.fluxnova.bpm.model.bpmn.instance.fluxnova.FluxnovaProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class TaskAbacResolver implements AbacResourceResolver<Task> {

    private final RepositoryService repositoryService;

    public TaskAbacResolver(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Override
    public Map<String, String> getAttributes(Task task) {
        Map<String, String> result = new HashMap<>();

        // In a production implementation, these extension attributes could be parsed at deployment time
        // and stored in a RESOURCE_ABAC table keyed by process definition id and task definition key.
        BpmnModelInstance model = repositoryService.getBpmnModelInstance(task.getProcessDefinitionId());
        FlowElement element = model.getModelElementById(task.getTaskDefinitionKey());
        if (element == null) {
            return result;
        }

        addExtensionAttributes(element.getExtensionElements(), result);
        return result;
    }

    private void addExtensionAttributes(ExtensionElements extensions, Map<String, String> result) {
        if (extensions == null) {
            return;
        }

        Collection<FluxnovaProperties> properties = extensions.getElementsQuery()
                .filterByType(FluxnovaProperties.class)
                .list();

        for (FluxnovaProperties group : properties) {
            for (FluxnovaProperty property : group.getFluxnovaProperties()) {
                result.put(property.getFluxnovaName(), property.getFluxnovaValue());
            }
        }
    }
}

