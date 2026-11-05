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
# complete-task.sh - Completes the first open user task
set -euo pipefail

ENGINE_URL="${1:-http://localhost:8080/engine-rest}"
APPROVED="${2:-true}"

echo "=== Complete User Task ==="

TASKS=$(curl -sf "$ENGINE_URL/task?assignee=demo&maxResults=1")
TASK_ID=$(echo "$TASKS" | python3 -c "import sys,json; t=json.load(sys.stdin); print(t[0]['id'] if t else '')" 2>/dev/null)

if [[ -z "$TASK_ID" ]]; then
    echo "No tasks found for user 'demo'."
    echo "Start a process first: ./start-process.sh"
    exit 0
fi

TASK_NAME=$(echo "$TASKS" | python3 -c "import sys,json; print(json.load(sys.stdin)[0].get('name','?'))" 2>/dev/null)
echo "Found task: $TASK_NAME (ID: $TASK_ID)"
echo "Completing with approved=$APPROVED"

curl -sf -X POST "$ENGINE_URL/task/$TASK_ID/complete" \
    -H "Content-Type: application/json" \
    -d "{\"variables\":{\"approved\":{\"value\":$APPROVED,\"type\":\"Boolean\"}}}"

echo ""
echo "Task completed!"
