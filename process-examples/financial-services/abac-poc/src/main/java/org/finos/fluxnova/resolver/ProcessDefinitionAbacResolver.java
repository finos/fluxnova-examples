package org.finos.fluxnova.resolver;

import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.finos.fluxnova.bpm.engine.repository.ProcessDefinition;
import org.finos.fluxnova.bpm.model.bpmn.BpmnModelInstance;
import org.finos.fluxnova.bpm.model.bpmn.instance.ExtensionElements;
import org.finos.fluxnova.bpm.model.bpmn.instance.Process;
import org.finos.fluxnova.bpm.model.bpmn.instance.fluxnova.FluxnovaProperties;
import org.finos.fluxnova.bpm.model.bpmn.instance.fluxnova.FluxnovaProperty;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Component
public class ProcessDefinitionAbacResolver implements AbacResourceResolver<ProcessDefinition> {

    private final RepositoryService repositoryService;

    public ProcessDefinitionAbacResolver(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
    }

    @Override
    public Map<String, String> getAttributes(ProcessDefinition processDefinition) {
        Map<String, String> result = new HashMap<>();

        BpmnModelInstance model = repositoryService.getBpmnModelInstance(processDefinition.getId());
        Collection<Process> processes = model.getModelElementsByType(Process.class);
        if (processes.isEmpty()) {
            return result;
        }

        ExtensionElements extensions = processes.iterator().next().getExtensionElements();
        addExtensionAttributes(extensions, result);
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

