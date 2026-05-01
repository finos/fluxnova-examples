package co.summit58.feewaiver.sb.delegates;

import org.finos.fluxnova.bpm.engine.delegate.DelegateExecution;
import org.finos.fluxnova.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class FeeWaiverHistoryDelegate implements JavaDelegate {

    private final Logger LOG = Logger.getLogger(FeeWaiverHistoryDelegate.class.getName());

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String aiStepType = stringVar(execution, "aiStepType");
        String toolName = stringVar(execution, "toolName");

        if (!"TOOL_REQUEST".equals(aiStepType)) {
            execution.setVariable("toolRequestHandled", false);
            execution.setVariable("toolInteractionSummary", "No tool request was present.");
            return;
        }

        if (!"getFeeWaiverHistory".equals(toolName)) {
            throw new IllegalArgumentException("Unsupported tool requested in process: " + toolName);
        }

        execution.setVariable("toolRequestHandled", true);
        execution.setVariable(
                "toolInteractionSummary",
                "AI requested tool '" + toolName + "' and the process handled it through a conditional event subprocess."
        );
    }

    private String stringVar(DelegateExecution execution, String name) {
        Object value = execution.getVariable(name);
        return value instanceof String s ? s : null;
    }

}
