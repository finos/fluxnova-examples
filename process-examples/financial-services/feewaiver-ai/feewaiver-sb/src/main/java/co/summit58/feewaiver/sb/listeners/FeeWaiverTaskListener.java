package co.summit58.feewaiver.sb.listeners;

import org.finos.fluxnova.bpm.engine.delegate.DelegateTask;
import org.finos.fluxnova.bpm.engine.delegate.TaskListener;
import org.finos.fluxnova.bpm.engine.task.IdentityLink;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.logging.Logger;

@Component
public class FeeWaiverTaskListener implements TaskListener {

    private final Logger LOG = Logger.getLogger(FeeWaiverTaskListener.class.getName());

    @Override
    public void notify(DelegateTask task) {

        if(task.getAssignee() != null) {
            String assignee = task.getAssignee();
            LOG.info("The assignee for task " + task.getId() + " is: " + assignee);
        }
        else {
            LOG.info("Task " + task.getId() + " is not yet assigned. Displaying all candidate users and groups...");

            Set<IdentityLink> identityLinks = task.getCandidates();
            if(identityLinks.isEmpty())
                LOG.info("No candidate users or groups exist for this User Task. Id: " + task.getId());
            else {
                for (IdentityLink identityLink : identityLinks) {
                    if(identityLink.getGroupId() != null)
                        LOG.info("Task " + task.getId() + " has an identity link for a group with id: "
                                + identityLink.getGroupId());
                    else if(identityLink.getUserId() != null)
                        LOG.info("Task " + task.getId() + " has an identity link for a user with id: "
                                + identityLink.getUserId());
                }
            }
        }
    }

}
