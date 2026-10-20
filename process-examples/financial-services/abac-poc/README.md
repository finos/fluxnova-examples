# FluxNova ABAC proof of concept

This standalone Spring Boot example demonstrates one small attribute-based access control (ABAC)
read path and one write path using a BPMN approval process. It is intended to show the integration
points and the allow/deny behavior, not to provide production security or a complete OPA integration.

## Scope

| Operation | Demonstration |
|---|---|
| Read process definitions | `GET /abac/processes` filters process definitions using attributes on the BPMN process. |
| Read user tasks | `GET /abac/tasks` filters tasks using attributes on each BPMN user task. |
| Write task state | Completing a task checks the current user, the task's BPMN attributes, its candidate group, and the mock policy's enabled groups. Only `complete` is intercepted. |

Read filtering applies only to the two `/abac` endpoints. It does not replace authorization on
all FluxNova/Camunda query APIs. The write interceptor covers task completion only; it does not
cover claim, assign, delegate, delete, variable updates, process start, or other commands.

## How the example works

### Read

1. The `/abac/*` demo authentication filter accepts one of the demo HTTP Basic credentials below
   and sets the current FluxNova identity for the request.
2. The controller obtains the authenticated user ID. The caller cannot choose a different ID in
   the URL or request.
3. The service loads process definitions or tasks and their ABAC attributes from BPMN extension
   properties.
4. `QueryAuthorizationService` compares every resource attribute with the same key in the user's
   attributes from `MockIacClient`. Matching resources are returned; non-matching resources are
   omitted. Resources without ABAC attributes are treated as public by this read filter.

### Write

1. The engine REST authentication filter authenticates the demo user and sets the current identity.
2. Completing a task invokes `OpaCompleteTaskAuthorizationInterceptor` in the engine command
   chain.
3. The interceptor resolves the target task and its candidate groups.
4. It denies anonymous requests, tasks with no candidate group, tasks with candidate groups not
   enabled for the configured tenant, tasks without ABAC attributes, and tasks whose BPMN attributes
   do not match the authenticated user's attributes.
5. If all checks pass, the completion command proceeds. Otherwise, it throws an
   `AuthorizationException` before task state is changed.

The write decision reuses the same user-to-resource attribute comparison as the read decision.
`OpaPolicyClient` is an **in-memory mock** that checks whether the task's candidate group is enabled
for the tenant. Despite its name, it makes no network request to OPA. `opa-policy.txt` is an example
of a possible future policy, not an active policy in this POC.

## Demo identities and policy data

All demo passwords are `demo`.

| User | Mock attributes | Read access | Task completion |
|---|---|---|---|
| `wealth-us-user` | US / WEALTH / TRADING | The process definition and tasks with matching attributes | Wealth Business Review |
| `operations-eu-user` | EU / OPERATIONS / SETTLEMENT | Tasks with matching attributes; not the US/WEALTH process definition | Operations Review |
| `apac-wealth-user` | APAC / WEALTH / OPERATIONS | Tasks with matching attributes; not the US/WEALTH process definition | Denied: its candidate group is disabled in the mock policy |
| `demo` | Same as `wealth-us-user` | Same as `wealth-us-user` | Wealth Business Review |

The mock attribute values are in `MockIacClient`. The sample workflow, resource attributes, and
candidate groups are in `src/main/resources/process-abac-poc.bpmn`. The tenant and mock-policy
configuration are in `src/main/resources/application.yaml` and `OpaPolicyClient`.

## Run locally

Requirements: Java and Maven compatible with the versions configured by the project.

From this directory:

```sh
mvn clean spring-boot:run
```

The app listens on port `8080`, uses an in-memory H2 database, and deploys the BPMN process at
startup. No external OPA server is required. The mock is enabled by default; setting
`opa.mock-enabled=false` does not connect to OPA and will throw an explicit
`UnsupportedOperationException` until the client is implemented.

## Try the read endpoints

Each endpoint returns a JSON array of visible process keys or task names:

```sh
curl -u wealth-us-user:demo http://localhost:8080/abac/processes
curl -u wealth-us-user:demo http://localhost:8080/abac/tasks
curl -u operations-eu-user:demo http://localhost:8080/abac/tasks
curl -u apac-wealth-user:demo http://localhost:8080/abac/tasks
```

Requests without valid demo credentials receive HTTP `401`. The ABAC endpoints derive the user
from the authenticated request; they do not accept a `userId` parameter.

## Try task completion

Start the process and inspect the active task:

```sh
curl -u demo:demo -X POST -H "Content-Type: application/json" -d '{}' \
  http://localhost:8080/engine-rest/process-definition/key/process-abac-poc/start
curl -u demo:demo http://localhost:8080/engine-rest/task
```

Use the task ID returned by the task query to complete the active task. The workflow proceeds
through Wealth Business Review, Operations Review, and APAC Approval, one at a time:

```sh
curl -u wealth-us-user:demo -X POST -H "Content-Type: application/json" -d '{}' \
  http://localhost:8080/engine-rest/task/<wealthTaskId>/complete
curl -u operations-eu-user:demo -X POST -H "Content-Type: application/json" -d '{}' \
  http://localhost:8080/engine-rest/task/<operationsTaskId>/complete
curl -u apac-wealth-user:demo -X POST -H "Content-Type: application/json" -d '{}' \
  http://localhost:8080/engine-rest/task/<apacTaskId>/complete
```

The first two requests should complete successfully (HTTP `204`). The APAC request should be
denied (HTTP `403`) because the mock write policy has not enabled that candidate group. To try a
user/resource attribute mismatch, attempt the Wealth task as `operations-eu-user`; it should also
be denied.

## Tests

Run the module tests from this directory:

```sh
mvn test
```

The tests cover the mocked candidate-group policy, read/write attribute matching, demo
authentication, and the authenticated identity behavior of the ABAC controller.

## Deliberate POC limitations and next steps

- Demo credentials are hard-coded and shared, and the attribute source is in-memory. Do not use
  this authentication or identity setup outside a local demonstration.
- The sample policy is a Java mock; there is no OPA process, HTTP client, or live policy evaluation.
- Candidate groups are checked as enabled/disabled for the configured tenant. Real deployments
  need policy inputs that identify the user and evaluate their entitlements.
- Only the listed `/abac` routes apply read filtering. Other engine REST query routes are not
  ABAC-filtered by this example.
- Only task completion is guarded for writes. Add equivalent checks for other state-changing
  commands before using them in a broader scenario.
- Resource attributes are resolved from BPMN on each request/command for simplicity. A production
  design should consider indexing or caching them and define behavior for missing/invalid metadata.
- The diagrams in `docs/images` illustrate the broader target architecture; this code implements
  only the small flow described above.
