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
# start-process.sh - Starts a new instance of the loan approval process
set -euo pipefail

ENGINE_URL="${1:-http://localhost:8080/engine-rest}"
PROCESS_KEY="${2:-loan-approval-process}"

echo "=== Start Process Instance ==="

RESULT=$(curl -sf -X POST "$ENGINE_URL/process-definition/key/$PROCESS_KEY/start" \
    -H "Content-Type: application/json" \
    -d '{
        "variables": {
            "applicantName": {"value": "John Doe", "type": "String"},
            "loanAmount": {"value": 25000, "type": "Integer"}
        }
    }')

if [[ $? -eq 0 ]]; then
    ID=$(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null || echo "?")
    echo ""
    echo "Process instance started!"
    echo "  Instance ID: $ID"
    echo "  Process Key: $PROCESS_KEY"
    echo ""
    echo "Open tasklist: http://localhost:8080/fluxnova/app/tasklist/"
else
    echo "Failed to start process. Is it deployed?"
    echo "Run: ./deploy-process.sh"
    exit 1
fi
