package org.finos.fluxnova.example.ai.mcp.query;

import java.util.Map;

import org.finos.fluxnova.bpm.engine.IdentityService;
import org.finos.fluxnova.bpm.engine.RuntimeService;
import org.finos.fluxnova.bpm.engine.identity.User;
import org.finos.fluxnova.bpm.engine.runtime.ProcessInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Seeds the engine with a few order process instances so the MCP query tools have something to find:
 * user tasks assigned to different reviewers, external tasks waiting on a worker, process variables,
 * and one instance with an open incident.
 */
@Component
@ConditionalOnProperty(name = "example.seed-data", havingValue = "true", matchIfMissing = true)
public class OrderDataSeeder implements ApplicationRunner {

  public static final String PROCESS_KEY = "orderFulfilment";
  public static final String INCIDENT_ORDER = "ORD-1003";

  private static final Logger LOG = LoggerFactory.getLogger(OrderDataSeeder.class);

  private final RuntimeService runtimeService;
  private final IdentityService identityService;

  public OrderDataSeeder(RuntimeService runtimeService, IdentityService identityService) {
    this.runtimeService = runtimeService;
    this.identityService = identityService;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (runtimeService.createProcessInstanceQuery().processDefinitionKey(PROCESS_KEY).count() > 0) {
      return;
    }

    createUser("alice", "Alice", "Reviewer");
    createUser("bob", "Bob", "Reviewer");

    startOrder("ORD-1001", "alice", 120.50, "Gold");
    startOrder("ORD-1002", "bob", 2400.00, "Silver");
    ProcessInstance problem = startOrder(INCIDENT_ORDER, "alice", 99.99, "Bronze");

    String creditCheckExecution = runtimeService.createExecutionQuery()
        .processInstanceId(problem.getId()).activityId("CreditCheck").singleResult().getId();
    runtimeService.createIncident("paymentProviderUnavailable", creditCheckExecution, "payment-gateway",
        "Payment provider timed out while reserving funds");

    LOG.info("Seeded {} order process instances", 3);
  }

  private void createUser(String id, String firstName, String lastName) {
    if (identityService.createUserQuery().userId(id).count() > 0) {
      return;
    }
    User user = identityService.newUser(id);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setEmail(id + "@example.org");
    user.setPassword(id + "-secret");
    identityService.saveUser(user);
  }

  private ProcessInstance startOrder(String orderId, String reviewer, double amount, String customerTier) {
    return runtimeService.startProcessInstanceByKey(PROCESS_KEY, orderId, Map.of(
        "orderId", orderId,
        "reviewer", reviewer,
        "amount", amount,
        "customerTier", customerTier));
  }
}
