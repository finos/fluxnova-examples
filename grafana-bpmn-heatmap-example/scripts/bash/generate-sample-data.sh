#!/usr/bin/env bash
# Copyright Fluxnova contributors
# SPDX-License-Identifier: Apache-2.0
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# generate-sample-data.sh - Deploys process, starts instances, completes tasks for demo data
set -euo pipefail

ENGINE_URL="${1:-http://localhost:8080/engine-rest}"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
INSTANCE_COUNT="${2:-25}"

echo "=== Generate Sample Data ==="
echo ""

# Check engine
if ! curl -sf --max-time 5 "$ENGINE_URL/engine" >/dev/null; then
    echo "Error: Fluxnova not running. Start it first."
    exit 1
fi

# Deploy
echo "Deploying loan-approval process..."
"$SCRIPT_DIR/deploy-process.sh" "$ENGINE_URL" "processes/loan-approval.bpmn" "loan-approval" >/dev/null 2>&1 || true

# Start instances
echo "Starting $INSTANCE_COUNT process instances..."
for i in $(seq 1 "$INSTANCE_COUNT"); do
    AMOUNT=$((RANDOM % 50000 + 5000))
    curl -sf -X POST "$ENGINE_URL/process-definition/key/loan-approval-process/start" \
        -H "Content-Type: application/json" \
        -d "{\"variables\":{\"applicantName\":{\"value\":\"Applicant $i\",\"type\":\"String\"},\"loanAmount\":{\"value\":$AMOUNT,\"type\":\"Integer\"}}}" >/dev/null
done
echo "  Started $INSTANCE_COUNT instances"

# Complete validations (leave some stuck)
echo "Completing validation tasks..."
sleep 1
TASKS=$(curl -sf "$ENGINE_URL/task?name=Validate+Application&maxResults=100")
TASK_IDS=$(echo "$TASKS" | python3 -c "import sys,json; [print(t['id']) for t in json.load(sys.stdin)[:18]]" 2>/dev/null)
DONE=0
while IFS= read -r tid; do
    [[ -z "$tid" ]] && continue
    curl -sf -X POST "$ENGINE_URL/task/$tid/complete" \
        -H "Content-Type: application/json" \
        -d '{"variables":{"valid":{"value":true,"type":"Boolean"}}}' >/dev/null && ((DONE++)) || true
done <<< "$TASK_IDS"
echo "  Completed $DONE validations"

# Complete some doc reviews
echo "Completing document reviews..."
sleep 1
TASKS=$(curl -sf "$ENGINE_URL/task?name=Review+Documents&maxResults=100")
TASK_IDS=$(echo "$TASKS" | python3 -c "import sys,json; [print(t['id']) for t in json.load(sys.stdin)[:10]]" 2>/dev/null)
DONE=0
while IFS= read -r tid; do
    [[ -z "$tid" ]] && continue
    curl -sf -X POST "$ENGINE_URL/task/$tid/complete" \
        -H "Content-Type: application/json" \
        -d '{"variables":{}}' >/dev/null && ((DONE++)) || true
done <<< "$TASK_IDS"
echo "  Completed $DONE doc reviews"

# Complete some risk assessments
echo "Completing risk assessments..."
sleep 1
TASKS=$(curl -sf "$ENGINE_URL/task?name=Assess+Risk+Level&maxResults=100")
TASK_IDS=$(echo "$TASKS" | python3 -c "import sys,json; tasks=json.load(sys.stdin)[:5]; [print(t['id']) for t in tasks]" 2>/dev/null)
COUNT=0
while IFS= read -r tid; do
    [[ -z "$tid" ]] && continue
    if [[ $COUNT -lt 3 ]]; then
        RISK="low"
    else
        RISK="high"
    fi
    curl -sf -X POST "$ENGINE_URL/task/$tid/complete" \
        -H "Content-Type: application/json" \
        -d "{\"variables\":{\"riskLevel\":{\"value\":\"$RISK\",\"type\":\"String\"}}}" >/dev/null && ((COUNT++)) || true
done <<< "$TASK_IDS"
echo "  Completed $COUNT risk assessments"

# Summary
echo ""
echo "=== Done ==="
OPEN=$(curl -sf "$ENGINE_URL/task/count" | python3 -c "import sys,json; print(json.load(sys.stdin)['count'])" 2>/dev/null)
echo "  Open tasks: $OPEN"
echo "  View heatmap: http://localhost:3000/d/fluxnova-heatmap"
