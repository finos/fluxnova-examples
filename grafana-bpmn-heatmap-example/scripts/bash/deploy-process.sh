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
# deploy-process.sh - Deploys a BPMN process to the running Fluxnova engine
set -euo pipefail

ENGINE_URL="${1:-http://localhost:8080/engine-rest}"
BPMN_FILE="${2:-processes/loan-approval.bpmn}"
DEPLOYMENT_NAME="${3:-loan-approval-deployment}"
SCRIPT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

# Resolve BPMN file path
if [[ ! "$BPMN_FILE" = /* ]]; then
    BPMN_FILE="$SCRIPT_DIR/$BPMN_FILE"
fi

echo "=== Deploy BPMN Process to Fluxnova ==="

if [[ ! -f "$BPMN_FILE" ]]; then
    echo "Error: BPMN file not found: $BPMN_FILE"
    exit 1
fi

# Check engine is reachable
echo "Checking engine at $ENGINE_URL..."
if ! curl -sf --max-time 5 "$ENGINE_URL/engine" >/dev/null; then
    echo "Error: Cannot reach Fluxnova engine at $ENGINE_URL"
    echo "Make sure the container is running (./start-fluxnova.sh)"
    exit 1
fi
echo "Engine is running."

# Deploy
FILENAME=$(basename "$BPMN_FILE")
echo "Deploying: $BPMN_FILE"

RESULT=$(curl -sf -X POST "$ENGINE_URL/deployment/create" \
    -F "deployment-name=$DEPLOYMENT_NAME" \
    -F "enable-duplicate-filtering=true" \
    -F "deploy-changed-only=true" \
    -F "$FILENAME=@$BPMN_FILE")

if [[ $? -eq 0 ]]; then
    echo ""
    echo "Deployment successful!"
    echo "  ID:   $(echo "$RESULT" | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])" 2>/dev/null || echo "?")"
    echo "  Name: $DEPLOYMENT_NAME"
    echo ""
    echo "Next: start a process instance with ./start-process.sh"
else
    echo "Deployment failed."
    exit 1
fi
