package co.summit58.feewaiver.sb.ui.controllers;

import org.springframework.web.bind.annotation.*;

import co.summit58.feewaiver.sb.ui.models.UiHistoryModels;
import co.summit58.feewaiver.sb.ui.services.UiHistoryService;

@RestController
@RequestMapping("/ui/history")
public class UiHistoryController {

    private final UiHistoryService uiHistoryService;

    public UiHistoryController(UiHistoryService uiHistoryService) {
        this.uiHistoryService = uiHistoryService;
    }

    @GetMapping("/instances/{processInstanceId}")
    public UiHistoryModels.InstanceHistoryResponse getInstanceHistory(
            @PathVariable("processInstanceId") String processInstanceId
    ) {
        return uiHistoryService.getInstanceHistory(processInstanceId);
    }

    @GetMapping("/process-definitions/{processDefinitionId}/bpmn")
    public UiHistoryModels.BpmnXmlResponse getBpmnXml(
            @PathVariable("processDefinitionId") String processDefinitionId
    ) throws Exception {
        return uiHistoryService.getBpmnXml(processDefinitionId);
    }
}