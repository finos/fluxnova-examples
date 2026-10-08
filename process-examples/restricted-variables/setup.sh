#!/usr/bin/env bash
# Creates the demo users, groups and authorizations for the restricted variables example,
# then deploys the process model. Run it against an engine that has authorization enabled
# (see README.md), as an administrator.
#
#   ./setup.sh
#   ENGINE_URL=http://localhost:8080/engine-rest ADMIN_USER=demo ADMIN_PASSWORD=demo ./setup.sh
#
# Safe to re-run: anything that already exists is reported as skipped, with the engine's message.
set -euo pipefail

ENGINE_URL="${ENGINE_URL:-http://localhost:8080/engine-rest}"
ADMIN_USER="${ADMIN_USER:-demo}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-demo}"
PROCESS_KEY="restricted-variables-loan-review"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Authorization resource types, see org.finos.fluxnova.bpm.engine.authorization.Resources
APPLICATION=0
FILTER=5
PROCESS_DEFINITION=6
TASK=7
PROCESS_INSTANCE=8
HISTORIC_PROCESS_INSTANCE=20
VARIABLE=22

call() {
  local description="$1" method="$2" path="$3" body="${4:-}"
  local response status
  response=$(curl -s -w '\n%{http_code}' -u "$ADMIN_USER:$ADMIN_PASSWORD" \
    -X "$method" -H 'Content-Type: application/json' ${body:+-d "$body"} "$ENGINE_URL$path")
  status="${response##*$'\n'}"
  case "$status" in
    2??) echo "  ok       $description" ;;
    *)   echo "  skipped  $description (HTTP $status: $(sed -n 's/.*"message":"\([^"]*\)".*/\1/p' <<< "${response%$'\n'*}"))" ;;
  esac
}

# True if the count endpoint at $1 returns a non-zero count.
exists() {
  ! curl -s -u "$ADMIN_USER:$ADMIN_PASSWORD" "$ENGINE_URL$1" | grep -q '"count":0'
}

group() {
  call "group $1" POST /group/create "{\"id\":\"$1\",\"name\":\"$2\",\"type\":\"WORKFLOW\"}"
}

user() {
  call "user $1" POST /user/create \
    "{\"profile\":{\"id\":\"$1\",\"firstName\":\"$2\",\"lastName\":\"$3\",\"email\":\"$1@example.com\"},\"credentials\":{\"password\":\"$1\"}}"
  if exists "/group/count?id=$4&member=$1"; then
    echo "  skipped  user $1 in group $4 (already a member)"
  else
    call "user $1 in group $4" PUT "/group/$4/members/$1"
  fi
}

grant() {
  local group_id="$1" resource_type="$2" resource_id="$3" permissions="$4"
  if exists "/authorization/count?groupIdIn=$group_id&resourceType=$resource_type&resourceId=$resource_id"; then
    echo "  skipped  grant $group_id on $resource_type/$resource_id (already exists)"
    return
  fi
  call "grant $group_id $permissions on $resource_type/$resource_id" POST /authorization/create \
    "{\"type\":1,\"groupId\":\"$group_id\",\"resourceType\":$resource_type,\"resourceId\":\"$resource_id\",\"permissions\":[$permissions]}"
}

if ! curl -sf -o /dev/null -u "$ADMIN_USER:$ADMIN_PASSWORD" "$ENGINE_URL/engine"; then
  echo "Cannot reach $ENGINE_URL as $ADMIN_USER. Is the engine running?" >&2
  exit 1
fi

echo "Groups and users"
group clerks "Clerks"
group officers "Loan Officers"
user clerk Casey Clerk clerks
user officer Olivia Officer officers

echo "Clerks: start the process and read its instances, but no VARIABLE permissions"
grant clerks $PROCESS_DEFINITION $PROCESS_KEY '"READ","CREATE_INSTANCE","READ_INSTANCE","READ_HISTORY"'
grant clerks $PROCESS_INSTANCE '*' '"CREATE","READ","UPDATE"'
grant clerks $HISTORIC_PROCESS_INSTANCE '*' '"READ"'
grant clerks $APPLICATION monitoring '"ACCESS"'

echo "Loan officers: work the review task and read restricted variables"
grant officers $PROCESS_DEFINITION $PROCESS_KEY '"READ","READ_INSTANCE","READ_HISTORY","READ_TASK","UPDATE_TASK"'
grant officers $PROCESS_INSTANCE '*' '"READ"'
grant officers $TASK '*' '"READ","UPDATE"'
grant officers $FILTER '*' '"READ"'
grant officers $VARIABLE '*' '"READ_RESTRICTED","READ_HISTORY_RESTRICTED"'
grant officers $APPLICATION tasklist '"ACCESS"'
grant officers $APPLICATION monitoring '"ACCESS"'

echo "Deployment"
status=$(curl -s -o /dev/null -w '%{http_code}' -u "$ADMIN_USER:$ADMIN_PASSWORD" \
  -F deployment-name=restricted-variables-example \
  -F enable-duplicate-filtering=true \
  -F "data=@$SCRIPT_DIR/restricted-variables-loan-review.bpmn" \
  "$ENGINE_URL/deployment/create")
echo "  HTTP $status  deploy restricted-variables-loan-review.bpmn"
[[ "$status" == 2?? ]]
