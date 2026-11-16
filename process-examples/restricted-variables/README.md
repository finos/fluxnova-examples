# Restricted Variables Example

A **restricted** process variable is only visible to users with the `READ_RESTRICTED` permission.
Other users can still work on the process instance, but the restricted values are hidden from
them, and they cannot change or delete them.

In this loan review process, the credit check writes a restricted `creditScore`. The review task
gets a restricted, task-local `riskBand`. A clerk starts the loan but cannot see either value. A
loan officer can.

Requires Fluxnova 3.0.0 or later and Docker.

## Start

```sh
docker compose up -d
```

This starts the engine on http://localhost:8080 with authorization enabled. A one-shot `setup`
container creates the users below and deploys the process (`docker compose logs setup`).

| User      | Password  | Can see restricted variables |
|-----------|-----------|------------------------------|
| `clerk`   | `clerk`   | No                           |
| `officer` | `officer` | Yes                          |
| `demo`    | `demo`    | Yes (admin)                  |

## Try it

1. **Start a loan application as the clerk:**

   ```sh
   curl -u clerk:clerk -H 'Content-Type: application/json' \
     -d '{"businessKey":"LOAN-1001","variables":{
           "applicantName":{"value":"Jane Doe","type":"String"},
           "loanAmount":{"value":25000,"type":"Long"}}}' \
     http://localhost:8080/engine-rest/process-definition/key/restricted-variables-loan-review/start
   ```

2. **Monitoring as the clerk.** Open http://localhost:8080/fluxnova/app/monitoring/ and log in as
   `clerk`. Open **Loan Review (Restricted Variables)**, then the `LOAN-1001` instance, then the
   **Variables** tab. You see `applicantName`, `loanAmount` and `creditCheckStatus`. There is no
   `creditScore` and no `riskBand`.

3. **Monitoring as the officer.** Log out, then log in as `officer` and open the same instance.
   `creditScore` (780) and `riskBand` (`LOW`) are now listed as well.

4. **The clerk cannot change a restricted variable:**

   ```sh
   PI=$(curl -s -u clerk:clerk "http://localhost:8080/engine-rest/process-instance?businessKey=LOAN-1001" \
     | sed 's/.*"id":"\([^"]*\)".*/\1/')
   curl -u clerk:clerk -X PUT -H 'Content-Type: application/json' -d '{"value":850,"type":"Long"}' \
     "http://localhost:8080/engine-rest/process-instance/$PI/variables/creditScore"
   ```

   The response is HTTP 403:
   `The user with id 'clerk' does not have 'UPDATE_RESTRICTED' permission on resource '*' of type 'Variable'.`

5. **Tasklist as the officer.** Open http://localhost:8080/fluxnova/app/tasklist/, log in as
   `officer`, and open **Review loan application**. Its variables include `creditScore` and
   `riskBand`. Claim the task and complete it.

6. **History.** Monitoring only shows running instances, so check history through the API:

   ```sh
   curl -s -u clerk:clerk   "http://localhost:8080/engine-rest/history/variable-instance?processInstanceId=$PI"
   curl -s -u officer:officer "http://localhost:8080/engine-rest/history/variable-instance?processInstanceId=$PI"
   ```

   Only the officer's result contains `creditScore` and `riskBand`.

Always log out before switching users. The web apps check which apps a user may open at login
time, so a session from before `setup` ran can get a 403 page.

Stop with `docker compose down`. This deletes all data.

## How it works

**BPMN.** Add `restricted="true"` to an input or output parameter (or to a call
activity's `in` / `out` mapping):

```xml
<camunda:outputParameter name="creditScore" restricted="true">${...}</camunda:outputParameter>
```

The `fluxnova` prefix must be bound to `http://fluxnova.finos.org/schema/1.0/bpmn`. Under
`http://fluxnova.org/schema/1.0/bpmn` the attribute is silently ignored.

Through the REST API, send `"valueInfo": {"restricted": true}`. In Java, use
`Variables.stringValue("...", VariableOptions.options(false, true))`.

**Engine.** Restrictions are enforced only when authorization is enabled **and** the caller is
authenticated. Unauthenticated REST calls and the job executor see everything.

- Spring Boot (`fluxnova-springboot-example`, which already has REST basic auth): set
  `fluxnova.bpm.authorization.enabled: true` in `application.yaml`.
- Fluxnova Run / Docker image: `FLUXNOVA_BPM_AUTHORIZATION_ENABLED=true` and
  `FLUXNOVA_BPM_RUN_AUTH_ENABLED=true`, as in `docker-compose.yml`.

**Permissions** are on the `VARIABLE` resource (type `22`), with resource ID `*`:
`READ_RESTRICTED`, `READ_HISTORY_RESTRICTED`, `CREATE_RESTRICTED`, `UPDATE_RESTRICTED`,
`DELETE_RESTRICTED`, `DELETE_HISTORY_RESTRICTED`. See `setup.sh` for the full set of grants.

## Gotchas

- A restricted mapping that runs synchronously while a process starts needs `CREATE_RESTRICTED`
  for the user who started it. That is why **Run credit check** is `asyncBefore`. Mappings that run
  after a task is completed, or in the job executor, are not checked.
- Group and user IDs may only contain letters and digits (`[a-zA-Z0-9]+`).
- In 3.0.0, the Docker image's bundled invoice example crashes the engine when authorization is
  enabled, so `docker-compose.yml` turns it off (`FLUXNOVA_BPM_RUN_EXAMPLE_ENABLED=false`).
