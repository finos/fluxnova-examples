# MCP workflow query example

A Fluxnova Spring Boot application that exposes the process engine's query API as
[MCP](https://modelcontextprotocol.io/) tools using the
[`mcp-query-workflow`](https://github.com/finos/fluxnova-plugins/tree/main/agentic/mcp-query-workflow) plugin. An LLM
connected to the MCP server can answer questions such as *"Which orders have open incidents?"* or *"What is Alice
working on?"* without using Cockpit.

On start-up the application deploys an order fulfilment process and a discount decision table, then seeds three
orders:

| Order | Reviewer task | Credit check (external task) | Notes |
|---|---|---|---|
| `ORD-1001` | alice | waiting on topic `credit-check` | tier Gold |
| `ORD-1002` | bob | waiting on topic `credit-check` | tier Silver |
| `ORD-1003` | alice | waiting on topic `credit-check` | tier Bronze, **open incident** `paymentProviderUnavailable` |

## Requirements

- Java 21
- Maven 3.9+
- The `fluxnova-plugins` agentic modules in your local Maven repository. Until `mcp-query-workflow` is released, build
  them from [finos/fluxnova-plugins](https://github.com/finos/fluxnova-plugins):

  ```bash
  cd fluxnova-plugins/agentic
  mvn install -DskipTests -pl mcp-server-plugin,mcp-query-workflow -am
  ```

  If you build a different version, pass `-Dfluxnova-ai-plugins.version=<version>` to the commands below.

## Run

```bash
mvn spring-boot:run
```

- MCP endpoint (streamable HTTP): `http://localhost:8080/mcp`
- Fluxnova webapps: `http://localhost:8080` (user `demo` / `demo`)

Set `example.seed-data=false` to start without the demo data.

### Connect an MCP client

With [MCP Inspector](https://github.com/modelcontextprotocol/inspector):

```bash
npx @modelcontextprotocol/inspector
```

Choose transport **Streamable HTTP**, URL `http://localhost:8080/mcp`, then **List Tools**. Try `queryIncidents` with
no arguments, or `queryTasks` with:

```json
{ "queryDto": { "assignee": "alice" } }
```

Any MCP-capable client (Claude Desktop, Claude Code, a Spring AI `ChatClient`, ...) can be pointed at the same URL.

## Configuration

See [`application.yaml`](src/main/resources/application.yaml). The query plugin is configured under
`fluxnova.mcp.query`:

```yaml
fluxnova:
  mcp:
    query:
      max-results: 50          # ceiling for any single tool call
      tools:
        exclude: querySchemaLog  # hide individual tools
        # history:
        #   enabled: false       # or switch off a whole engine service
```

The full list of tools and options is in the plugin's README.

## Tests

`McpWorkflowQueryE2ETest` starts the application on a random port and drives the tools through a real MCP client
(MCP Java SDK, streamable HTTP): tool discovery and exclusions, process/decision definitions, BPMN/DMN XML retrieval,
process instances, incidents, variables, user and external tasks, history, users (no passwords), `maxResults`
handling, and that the plugin does not override the host application's configuration. No LLM is involved.

```bash
mvn verify
```
