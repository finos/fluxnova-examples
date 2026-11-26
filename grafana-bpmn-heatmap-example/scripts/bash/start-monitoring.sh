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
# start-monitoring.sh - Starts the full monitoring stack: Exporter + Prometheus + Grafana
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
MONITORING_DIR="$SCRIPT_DIR/monitoring"

EXPORTER_IMAGE="localhost/fluxnova-exporter:latest"
PROMETHEUS_IMAGE="${PROMETHEUS_IMAGE:-${REGISTRY:-docker.io}/prom/prometheus:latest}"
GRAFANA_IMAGE="${GRAFANA_IMAGE:-${REGISTRY:-docker.io}/grafana/grafana:10.4.2}"

EXPORTER_CONTAINER="fluxnova-exporter"
PROMETHEUS_CONTAINER="fluxnova-prometheus"
GRAFANA_CONTAINER="fluxnova-grafana"

echo "=== Fluxnova Monitoring Stack ==="
echo ""

# --- Clean up existing containers ---
for container in $EXPORTER_CONTAINER $PROMETHEUS_CONTAINER $GRAFANA_CONTAINER; do
    if podman ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
        echo "Removing existing container: $container"
        podman rm -f "$container" >/dev/null
    fi
done

# --- Build exporter ---
echo "Building Fluxnova exporter image..."
podman build -t fluxnova-exporter -f "$MONITORING_DIR/Dockerfile.exporter" "$MONITORING_DIR" >/dev/null 2>&1

# --- Start Exporter ---
echo "Starting Fluxnova exporter..."
podman run -d \
    --name "$EXPORTER_CONTAINER" \
    -p 9091:9091 \
    -e "FLUXNOVA_URL=http://host.containers.internal:8080/engine-rest" \
    "$EXPORTER_IMAGE"

# --- Start Prometheus ---
echo "Starting Prometheus..."
podman run -d \
    --name "$PROMETHEUS_CONTAINER" \
    -p 9090:9090 \
    -v "$MONITORING_DIR/prometheus.yml:/etc/prometheus/prometheus.yml:ro" \
    -v "$MONITORING_DIR/alert-rules.yml:/etc/prometheus/alert-rules.yml:ro" \
    "$PROMETHEUS_IMAGE"

# --- Grafana provisioning ---
cat > "$MONITORING_DIR/grafana-datasource.yml" <<EOF
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://host.containers.internal:9090
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

# --- Start Grafana ---
echo "Starting Grafana..."

# Mount the BPMN heatmap plugin if the dist/ folder exists (pre-built)
PLUGIN_DIR="$SCRIPT_DIR/grafana-bpmn-heatmap-plugin/dist"
PLUGIN_MOUNT=""
PLUGIN_ENV=""
if [ -d "$PLUGIN_DIR" ] && [ -f "$PLUGIN_DIR/module.js" ]; then
    echo "  → BPMN Heatmap plugin detected — mounting into Grafana"
    PLUGIN_MOUNT="-v $PLUGIN_DIR:/var/lib/grafana/plugins/fluxnova-bpmn-heatmap-panel:ro"
    PLUGIN_ENV="-e GF_PLUGINS_ALLOW_LOADING_UNSIGNED_PLUGINS=fluxnova-bpmn-heatmap-panel"
fi

podman run -d \
    --name "$GRAFANA_CONTAINER" \
    -p 3000:3000 \
    -e "GF_SECURITY_ADMIN_PASSWORD=fluxnova" \
    -e "GF_SECURITY_ADMIN_USER=admin" \
    -e "GF_SECURITY_DISABLE_INITIAL_ADMIN_PASSWORD_CHANGE=true" \
    -e "GF_USERS_ALLOW_SIGN_UP=false" \
    $PLUGIN_ENV \
    -v "$MONITORING_DIR/grafana-datasource.yml:/etc/grafana/provisioning/datasources/datasource.yml:ro" \
    -v "$MONITORING_DIR/grafana-dashboard-provider.yml:/etc/grafana/provisioning/dashboards/dashboard.yml:ro" \
    -v "$MONITORING_DIR/dashboards:/var/lib/grafana/dashboards:ro" \
    -v "$MONITORING_DIR/grafana.ini:/etc/grafana/grafana.ini:ro" \
    $PLUGIN_MOUNT \
    "$GRAFANA_IMAGE"

# --- Wait for Grafana ---
echo "Waiting for Grafana..."
for i in $(seq 1 20); do
    if curl -sf --max-time 3 http://localhost:3000/api/health >/dev/null 2>&1; then
        break
    fi
    sleep 2
done

# Confirm password via API to suppress change prompt
curl -sf -X PUT http://localhost:3000/api/user/password \
    -H "Authorization: Basic $(echo -n 'admin:fluxnova' | base64)" \
    -H "Content-Type: application/json" \
    -d '{"oldPassword":"fluxnova","newPassword":"fluxnova","confirmNew":"fluxnova"}' >/dev/null 2>&1 || true

echo ""
echo "======================================"
echo "  Monitoring stack is running!"
echo "======================================"
echo ""
echo "  Exporter:    http://localhost:9091/metrics"
echo "  Prometheus:  http://localhost:9090"
echo "  Grafana:     http://localhost:3000  (admin / fluxnova)"
echo ""
echo "  Dashboards:"
echo "    Overview:  http://localhost:3000/d/fluxnova-bpm-overview"
echo "    Heatmap:   http://localhost:3000/d/fluxnova-heatmap"
echo ""
echo "  To stop:"
echo "    podman stop $EXPORTER_CONTAINER $PROMETHEUS_CONTAINER $GRAFANA_CONTAINER"
echo "    podman rm $EXPORTER_CONTAINER $PROMETHEUS_CONTAINER $GRAFANA_CONTAINER"
