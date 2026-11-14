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
# start-all.sh — One command to start the entire Fluxnova demo stack.
#
# Usage:
#   ./start-all.sh                          # Uses default image registry
#   REGISTRY=artifactory.corp/virtual ./start-all.sh  # Corporate registry
#
# What it does:
#   1. Starts Fluxnova BPM engine
#   2. Waits for it to be ready
#   3. Starts monitoring stack (Exporter + Prometheus + Grafana)
#   4. Deploys the loan-approval BPMN process
#   5. Generates sample data (25 instances, partial completion)
#   6. Starts the heatmap server
#   7. Prints dashboard URL
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
MONITORING_DIR="$PROJECT_DIR/monitoring"
HEATMAP_DIR="$PROJECT_DIR/heatmap"

# ---------------------------------------------------------------------------
# Image Registry — override with REGISTRY env var for corporate environments
# ---------------------------------------------------------------------------
REGISTRY="${REGISTRY:-docker.io}"
FLUXNOVA_IMAGE="${FLUXNOVA_IMAGE:-$REGISTRY/finos/fluxnova-bpm-platform:latest}"
PROMETHEUS_IMAGE="${PROMETHEUS_IMAGE:-$REGISTRY/prom/prometheus:latest}"
GRAFANA_IMAGE="${GRAFANA_IMAGE:-$REGISTRY/grafana/grafana:10.4.2}"

# ---------------------------------------------------------------------------
# Networking — Linux needs --network=host; macOS/Windows use port mapping
# ---------------------------------------------------------------------------
if [[ "$(uname -s)" == "Linux" ]]; then
    NETWORK_MODE="--network=host"
    PROM_URL="http://localhost:9090"
    ENGINE_URL="http://localhost:8080/engine-rest"
    EXPORTER_URL="http://localhost:9091"
else
    NETWORK_MODE=""
    PROM_URL="http://host.containers.internal:9090"
    ENGINE_URL="http://host.containers.internal:8080/engine-rest"
    EXPORTER_URL="http://host.containers.internal:9091"
fi

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------
cleanup_container() {
    if podman ps -a --format '{{.Names}}' | grep -q "^${1}$"; then
        podman rm -f "$1" >/dev/null 2>&1
    fi
}

wait_for() {
    local url="$1" name="$2" max="${3:-30}"
    echo -n "  Waiting for $name..."
    for i in $(seq 1 "$max"); do
        if curl -sf --max-time 2 "$url" >/dev/null 2>&1; then
            echo " ready ✅"
            return 0
        fi
        sleep 2
    done
    echo " TIMEOUT ❌"
    echo "  ERROR: $name did not start within ${max}s. Check: podman logs $name"
    exit 1
}

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║          Fluxnova BPM — Process Heatmap Demo               ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""
echo "Registry: $REGISTRY"
echo "Network:  ${NETWORK_MODE:-port-mapping}"
echo ""

# ---------------------------------------------------------------------------
# 1. Fluxnova BPM Engine
# ---------------------------------------------------------------------------
echo "▶ [1/5] Starting Fluxnova BPM Engine..."
cleanup_container fluxnova-bpm
if [[ -n "$NETWORK_MODE" ]]; then
    podman run -d --name fluxnova-bpm $NETWORK_MODE "$FLUXNOVA_IMAGE" >/dev/null
else
    podman run -d --name fluxnova-bpm -p 8080:8080 "$FLUXNOVA_IMAGE" >/dev/null
fi
wait_for "http://localhost:8080/engine-rest/engine" "Fluxnova" 40

# ---------------------------------------------------------------------------
# 2. Exporter
# ---------------------------------------------------------------------------
echo "▶ [2/5] Starting Fluxnova Exporter..."
cleanup_container fluxnova-exporter
podman build -t fluxnova-exporter -f "$MONITORING_DIR/Dockerfile.exporter" "$MONITORING_DIR" >/dev/null 2>&1
if [[ -n "$NETWORK_MODE" ]]; then
    podman run -d --name fluxnova-exporter $NETWORK_MODE \
        -e "FLUXNOVA_URL=http://localhost:8080/engine-rest" \
        -e "EXPORTER_PORT=9091" \
        localhost/fluxnova-exporter:latest >/dev/null
else
    podman run -d --name fluxnova-exporter -p 9091:9091 \
        -e "FLUXNOVA_URL=$ENGINE_URL" \
        localhost/fluxnova-exporter:latest >/dev/null
fi
wait_for "http://localhost:9091/metrics" "Exporter" 15

# ---------------------------------------------------------------------------
# 3. Prometheus
# ---------------------------------------------------------------------------
echo "▶ [3/5] Starting Prometheus..."
cleanup_container fluxnova-prometheus
if [[ -n "$NETWORK_MODE" ]]; then
    podman run -d --name fluxnova-prometheus $NETWORK_MODE \
        -v "$MONITORING_DIR/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
        -v "$MONITORING_DIR/alert-rules.yml:/etc/prometheus/alert-rules.yml:ro" \
        "$PROMETHEUS_IMAGE" >/dev/null
else
    podman run -d --name fluxnova-prometheus -p 9090:9090 \
        -v "$MONITORING_DIR/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
        -v "$MONITORING_DIR/alert-rules.yml:/etc/prometheus/alert-rules.yml:ro" \
        "$PROMETHEUS_IMAGE" >/dev/null
fi
wait_for "http://localhost:9090/-/ready" "Prometheus" 15

# ---------------------------------------------------------------------------
# 4. Grafana
# ---------------------------------------------------------------------------
echo "▶ [4/5] Starting Grafana..."
cleanup_container fluxnova-grafana

# Generate provisioning files
cat > "$MONITORING_DIR/grafana-datasource.yml" <<EOF
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: ${PROM_URL}
    uid: prometheus
    isDefault: true
    editable: true
EOF

cat > "$MONITORING_DIR/grafana-dashboard-provider.yml" <<EOF
apiVersion: 1
providers:
  - name: 'Fluxnova'
    orgId: 1
    folder: ''
    type: file
    disableDeletion: false
    editable: true
    options:
      path: /var/lib/grafana/dashboards
      foldersFromFilesStructure: false
EOF

# Check for pre-built heatmap plugin
PLUGIN_DIR="$PROJECT_DIR/grafana-bpmn-heatmap-plugin/dist"
if [ ! -d "$PLUGIN_DIR" ] || [ ! -f "$PLUGIN_DIR/module.js" ]; then
    echo "  ❌ Plugin not built. Run: cd grafana-bpmn-heatmap-plugin && npm install --legacy-peer-deps && npm run build"
    exit 1
fi
echo "  → BPMN Heatmap plugin detected"
PLUGIN_ARGS="-v $PLUGIN_DIR:/var/lib/grafana/plugins/fluxnova-bpmn-heatmap-panel:ro -e GF_PLUGINS_ALLOW_LOADING_UNSIGNED_PLUGINS=fluxnova-bpmn-heatmap-panel"

if [[ -n "$NETWORK_MODE" ]]; then
    podman run -d --name fluxnova-grafana $NETWORK_MODE \
        -e "GF_SECURITY_ADMIN_PASSWORD=fluxnova" \
        -e "GF_SECURITY_ADMIN_USER=admin" \
        -e "GF_SECURITY_DISABLE_INITIAL_ADMIN_PASSWORD_CHANGE=true" \
        -e "GF_USERS_ALLOW_SIGN_UP=false" \
        -v "$MONITORING_DIR/grafana-datasource.yml:/etc/grafana/provisioning/datasources/datasource.yml:ro" \
        -v "$MONITORING_DIR/grafana-dashboard-provider.yml:/etc/grafana/provisioning/dashboards/dashboard.yml:ro" \
        -v "$MONITORING_DIR/dashboards:/var/lib/grafana/dashboards:ro" \
        -v "$MONITORING_DIR/grafana.ini:/etc/grafana/grafana.ini:ro" \
        $PLUGIN_ARGS \
        "$GRAFANA_IMAGE" >/dev/null
else
    podman run -d --name fluxnova-grafana -p 3000:3000 \
        -e "GF_SECURITY_ADMIN_PASSWORD=fluxnova" \
        -e "GF_SECURITY_ADMIN_USER=admin" \
        -e "GF_SECURITY_DISABLE_INITIAL_ADMIN_PASSWORD_CHANGE=true" \
        -e "GF_USERS_ALLOW_SIGN_UP=false" \
        -v "$MONITORING_DIR/grafana-datasource.yml:/etc/grafana/provisioning/datasources/datasource.yml:ro" \
        -v "$MONITORING_DIR/grafana-dashboard-provider.yml:/etc/grafana/provisioning/dashboards/dashboard.yml:ro" \
        -v "$MONITORING_DIR/dashboards:/var/lib/grafana/dashboards:ro" \
        -v "$MONITORING_DIR/grafana.ini:/etc/grafana/grafana.ini:ro" \
        $PLUGIN_ARGS \
        "$GRAFANA_IMAGE" >/dev/null
fi
wait_for "http://localhost:3000/api/health" "Grafana" 20

# ---------------------------------------------------------------------------
# 5. Deploy process + generate sample data
# ---------------------------------------------------------------------------
echo "▶ [5/5] Deploying process & generating sample data..."
curl -sf -X POST http://localhost:8080/engine-rest/deployment/create \
    -F "deployment-name=loan-approval" \
    -F "enable-duplicate-filtering=false" \
    -F "data=@$PROJECT_DIR/processes/loan-approval.bpmn" >/dev/null
echo "  Process deployed"

# Start 25 instances
for i in $(seq 1 25); do
    AMOUNT=$((RANDOM % 50000 + 5000))
    curl -sf -X POST "http://localhost:8080/engine-rest/process-definition/key/loan-approval-process/start" \
        -H "Content-Type: application/json" \
        -d "{\"variables\":{\"applicantName\":{\"value\":\"Applicant $i\",\"type\":\"String\"},\"loanAmount\":{\"value\":$AMOUNT,\"type\":\"Integer\"}}}" >/dev/null
done
echo "  25 process instances started"

# Complete tasks to create distribution
sleep 2
TASKS=$(curl -sf "http://localhost:8080/engine-rest/task?name=Validate+Application&maxResults=100")
echo "$TASKS" | python3 -c "import sys,json; [print(t['id']) for t in json.load(sys.stdin)[:18]]" 2>/dev/null | while read tid; do
    curl -sf -X POST "http://localhost:8080/engine-rest/task/$tid/complete" \
        -H "Content-Type: application/json" -d '{"variables":{"valid":{"value":true,"type":"Boolean"}}}' >/dev/null
done

sleep 1
TASKS=$(curl -sf "http://localhost:8080/engine-rest/task?name=Review+Documents&maxResults=100")
echo "$TASKS" | python3 -c "import sys,json; [print(t['id']) for t in json.load(sys.stdin)[:10]]" 2>/dev/null | while read tid; do
    curl -sf -X POST "http://localhost:8080/engine-rest/task/$tid/complete" \
        -H "Content-Type: application/json" -d '{"variables":{}}' >/dev/null
done

sleep 1
TASKS=$(curl -sf "http://localhost:8080/engine-rest/task?name=Assess+Risk+Level&maxResults=100")
echo "$TASKS" | python3 -c "import sys,json; [print(t['id']) for t in json.load(sys.stdin)[:5]]" 2>/dev/null | while read tid; do
    curl -sf -X POST "http://localhost:8080/engine-rest/task/$tid/complete" \
        -H "Content-Type: application/json" -d "{\"variables\":{\"riskLevel\":{\"value\":\"low\",\"type\":\"String\"}}}" >/dev/null
done
echo "  Sample data generated (tasks at various stages)"

# ---------------------------------------------------------------------------
# Done
# ---------------------------------------------------------------------------
echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║  ✅ All services running!                                   ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║                                                            ║"
echo "║  🌐 Grafana Dashboard:                                     ║"
echo "║     http://localhost:3000/d/fluxnova-heatmap               ║"
echo "║     Login: admin / fluxnova                                ║"
echo "║                                                            ║"
echo "║  📊 Other UIs:                                             ║"
echo "║     Prometheus:  http://localhost:9090                     ║"
echo "║     Fluxnova:    http://localhost:8080/fluxnova/app/       ║"
echo "║                                                            ║"
echo "╠══════════════════════════════════════════════════════════════╣"
echo "║  To stop: ./stop-all.sh                                    ║"
echo "╚══════════════════════════════════════════════════════════════╝"
