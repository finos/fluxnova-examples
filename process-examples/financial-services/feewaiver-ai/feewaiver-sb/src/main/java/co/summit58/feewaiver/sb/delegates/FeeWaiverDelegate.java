package co.summit58.feewaiver.sb.delegates;

import org.finos.fluxnova.bpm.engine.delegate.DelegateExecution;
import org.finos.fluxnova.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class FeeWaiverDelegate implements JavaDelegate {

    private final Logger LOG = Logger.getLogger(FeeWaiverDelegate.class.getName());

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LOG.info("Placeholder service for fee waiving logging...");
    }

}
