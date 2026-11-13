package org.finos.fluxnova.example.ai.mcp.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import org.finos.fluxnova.bpm.engine.RepositoryService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * End-to-end test: boots the example application (Fluxnova engine + MCP server plugin + MCP workflow query
 * plugin) on a random port and drives the query tools through a real MCP client over streamable HTTP.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class McpWorkflowQueryE2ETest {

  private static final TypeReference<List<Map<String, Object>>> LIST_OF_MAPS = new TypeReference<>() {
  };

  private final ObjectMapper json = JsonMapper.builder().build();

  @LocalServerPort
  int port;

  @Autowired
  Environment environment;

  @Autowired
  RepositoryService repositoryService;

  McpSyncClient client;

  @BeforeAll
  void connect() {
    var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port).build();
    client = McpClient.sync(transport).requestTimeout(Duration.ofSeconds(30)).build();
    client.initialize();
  }

  @AfterAll
  void disconnect() {
    if (client != null) {
      client.closeGracefully();
    }
  }

  // ---- discovery ----

  @Test
  @SuppressWarnings("unchecked")
  void listsQueryToolsWithQueryDtoSchemaAndHonoursExclusions() {
    Map<String, Tool> tools = client.listTools().tools().stream()
        .collect(Collectors.toMap(Tool::name, t -> t));

    assertThat(tools.keySet()).contains(
        "queryProcessDefinitions", "queryProcessInstances", "queryTasks", "queryExternalTasks",
        "queryIncidents", "queryVariableInstances", "queryHistoricProcessInstances", "queryUsers",
        "queryDecisionDefinitions", "getProcessModelXml", "getDecisionModelXml");
    // excluded via fluxnova.mcp.query.tools.exclude in application.yaml
    assertThat(tools).doesNotContainKey("querySchemaLog");

    Tool queryTasks = tools.get("queryTasks");
    assertThat(queryTasks.description()).isNotBlank();
    assertThat((Map<Object, Object>) queryTasks.inputSchema().get("properties")).containsKeys("queryDto", "maxResults");

    Tool xml = tools.get("getProcessModelXml");
    assertThat((List<Object>) xml.inputSchema().get("required")).containsExactly("processDefinitionId");
  }

  // ---- repository ----

  @Test
  void findsDeployedProcessAndDecisionDefinitions() {
    var processes = callForList("queryProcessDefinitions", Map.of("key", OrderDataSeeder.PROCESS_KEY));
    assertThat(processes).singleElement().satisfies(pd -> {
      assertThat(pd).containsEntry("key", OrderDataSeeder.PROCESS_KEY);
      assertThat(pd).containsEntry("name", "Order Fulfilment");
    });

    var decisions = callForList("queryDecisionDefinitions", Map.of("key", "orderDiscount"));
    assertThat(decisions).singleElement().satisfies(dd -> assertThat(dd).containsEntry("key", "orderDiscount"));
  }

  @Test
  void returnsBpmnAndDmnXml() {
    String processDefinitionId = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey(OrderDataSeeder.PROCESS_KEY).singleResult().getId();
    String decisionDefinitionId = repositoryService.createDecisionDefinitionQuery()
        .decisionDefinitionKey("orderDiscount").singleResult().getId();

    String bpmn = text(callOk("getProcessModelXml", Map.of("processDefinitionId", processDefinitionId)));
    assertThat(bpmn).contains("<bpmn:process id=\"orderFulfilment\"").contains("fluxnova:topic=\"credit-check\"");

    String dmn = text(callOk("getDecisionModelXml", Map.of("decisionDefinitionId", decisionDefinitionId)));
    assertThat(dmn).contains("<decision id=\"orderDiscount\"");
  }

  @Test
  void unknownDefinitionIdIsReportedAsToolError() {
    CallToolResult result = call("getProcessModelXml", Map.of("processDefinitionId", "does-not-exist"));
    assertThat(result.isError()).isTrue();
  }

  // ---- runtime ----

  @Test
  void findsProcessInstanceByBusinessKey() {
    var instances = callForList("queryProcessInstances", Map.of("businessKey", "ORD-1002"));
    assertThat(instances).singleElement().satisfies(pi -> assertThat(pi).containsEntry("businessKey", "ORD-1002"));
  }

  @Test
  void findsOpenIncidentAndTheInstancesThatHaveOne() {
    var incidents = callForList("queryIncidents", Map.of("incidentType", "paymentProviderUnavailable"));
    assertThat(incidents).singleElement().satisfies(i ->
        assertThat(i).containsEntry("incidentMessage", "Payment provider timed out while reserving funds"));

    var withIncident = callForList("queryProcessInstances", Map.of("withIncident", true));
    assertThat(withIncident).extracting(pi -> pi.get("businessKey")).containsExactly(OrderDataSeeder.INCIDENT_ORDER);
  }

  @Test
  void readsProcessVariables() {
    var variables = callForList("queryVariableInstances", Map.of("variableName", "customerTier"));
    assertThat(variables).hasSize(3)
        .extracting(v -> v.get("value"))
        .containsExactlyInAnyOrder("Gold", "Silver", "Bronze");
  }

  // ---- tasks ----

  @Test
  void findsUserTasksByAssignee() {
    var aliceTasks = callForList("queryTasks", Map.of("assignee", "alice"));
    assertThat(aliceTasks).hasSize(2)
        .allSatisfy(t -> assertThat(t).containsEntry("taskDefinitionKey", "ReviewOrder"));
  }

  @Test
  void findsExternalTasksByTopic() {
    var externalTasks = callForList("queryExternalTasks", Map.of("topicName", "credit-check"));
    assertThat(externalTasks).hasSize(3)
        .allSatisfy(t -> assertThat(t).containsEntry("activityId", "CreditCheck"));
  }

  // ---- history ----

  @Test
  void findsHistoricProcessInstances() {
    var history = callForList("queryHistoricProcessInstances",
        Map.of("processDefinitionKey", OrderDataSeeder.PROCESS_KEY));
    assertThat(history).hasSize(3);
  }

  // ---- identity ----

  @Test
  void returnsUsersWithoutPasswords() {
    CallToolResult result = callOk("queryUsers", Map.of("queryDto", Map.of("userId", "alice")));
    String body = text(result);
    assertThat(body).contains("alice@example.org").doesNotContainIgnoringCase("password")
        .doesNotContain("alice-secret");
  }

  // ---- limits and inputs ----

  @Test
  void maxResultsLimitsResults() {
    var limited = callForList("queryExternalTasks", Map.of("topicName", "credit-check"), 1);
    assertThat(limited).hasSize(1);
  }

  @Test
  void nonPositiveMaxResultsFallsBackToTheConfiguredLimit() {
    assertThat(callForList("queryExternalTasks", Map.of("topicName", "credit-check"), 0)).hasSize(3);
    assertThat(callForList("queryExternalTasks", Map.of("topicName", "credit-check"), -1)).hasSize(3);
  }

  @Test
  void callingWithoutAnyArgumentsReturnsEverything() {
    CallToolResult result = client.callTool(new CallToolRequest("queryProcessInstances", Map.of()));
    assertThat(result.isError()).isFalse();
    assertThat(readList(result)).hasSize(3);
  }

  // ---- library must not leak configuration into the host application ----

  @Test
  void hostApplicationNameIsNotOverriddenByThePlugin() {
    assertThat(environment.getProperty("spring.application.name")).isEqualTo("mcp-workflow-query-example");
  }

  // ---- helpers ----

  private List<Map<String, Object>> callForList(String tool, Map<String, Object> queryDto) {
    return readList(callOk(tool, Map.of("queryDto", queryDto)));
  }

  private List<Map<String, Object>> callForList(String tool, Map<String, Object> queryDto, int maxResults) {
    return readList(callOk(tool, Map.of("queryDto", queryDto, "maxResults", maxResults)));
  }

  private CallToolResult callOk(String tool, Map<String, Object> args) {
    CallToolResult result = call(tool, args);
    assertThat(result.isError()).as("%s returned an error: %s", tool, result.content()).isFalse();
    return result;
  }

  private CallToolResult call(String tool, Map<String, Object> args) {
    return client.callTool(new CallToolRequest(tool, args));
  }

  private List<Map<String, Object>> readList(CallToolResult result) {
    return json.readValue(text(result), LIST_OF_MAPS);
  }

  private static String text(CallToolResult result) {
    return result.content().stream()
        .filter(TextContent.class::isInstance)
        .map(c -> ((TextContent) c).text())
        .collect(Collectors.joining());
  }
}
