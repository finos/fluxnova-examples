package co.summit58.feewaiver.sb.services;

import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MessageCorrelationService {

    private final RuntimeService runtimeService;

    public MessageCorrelationService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
    }

    public void correlateToolRequestedMessage(String processInstanceId, Map<String, Object> variables) {
        runtimeService.createMessageCorrelation("AI_TOOL_REQUESTED")
                .processInstanceId(processInstanceId)
                .setVariables(variables)
                .correlate();
    }
}