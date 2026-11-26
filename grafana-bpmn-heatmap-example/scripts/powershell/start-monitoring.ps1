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
# start-monitoring.ps1
# Starts the full monitoring stack: Exporter + Prometheus + Grafana (all in Podman containers)

Write-Host "=== Fluxnova Monitoring Stack ===" -ForegroundColor Cyan
Write-Host ""

$EXPORTER_IMAGE = "localhost/fluxnova-exporter:latest"
$PROMETHEUS_IMAGE = "docker.io/prom/prometheus:latest"
$GRAFANA_IMAGE = "docker.io/grafana/grafana:10.4.2"

$EXPORTER_CONTAINER = "fluxnova-exporter"
$PROMETHEUS_CONTAINER = "fluxnova-prometheus"
$GRAFANA_CONTAINER = "fluxnova-grafana"

# --- Clean up existing containers ---
foreach ($container in @($EXPORTER_CONTAINER, $PROMETHEUS_CONTAINER, $GRAFANA_CONTAINER)) {
    $existing = podman ps -a --filter "name=$container" --format "{{.Names}}" 2>$null
    if ($existing -eq $container) {
        Write-Host "Removing existing container: $container" -ForegroundColor Yellow
        podman rm -f $container | Out-Null
    }
}

# --- Build the exporter image ---
Write-Host "Building Fluxnova exporter image..." -ForegroundColor Green
$monitoringDir = Join-Path $PSScriptRoot "monitoring"
podman build -t fluxnova-exporter -f "$monitoringDir\Dockerfile.exporter" $monitoringDir 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to build exporter image." -ForegroundColor Red
    exit 1
}

# --- Start Exporter ---
Write-Host "Starting Fluxnova exporter..." -ForegroundColor Green
podman run -d `
    --name $EXPORTER_CONTAINER `
    -p 9091:9091 `
    -e "FLUXNOVA_URL=http://host.containers.internal:8080/engine-rest" `
    $EXPORTER_IMAGE

if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to start exporter." -ForegroundColor Red
    exit 1
}

# --- Start Prometheus ---
Write-Host "Starting Prometheus..." -ForegroundColor Green
$prometheusConfig = (Resolve-Path "$monitoringDir\prometheus.yml").Path
$alertRules = (Resolve-Path "$monitoringDir\alert-rules.yml").Path

podman run -d `
    --name $PROMETHEUS_CONTAINER `
    -p 9090:9090 `
    -v "${prometheusConfig}:/etc/prometheus/prometheus.yml:ro" `
    -v "${alertRules}:/etc/prometheus/alert-rules.yml:ro" `
    $PROMETHEUS_IMAGE

if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to start Prometheus." -ForegroundColor Red
    exit 1
}

# --- Grafana provisioning files ---
$grafanaDatasource = @"
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    access: proxy
    url: http://host.containers.internal:9090
    uid: prometheus
    isDefault: true
    editable: true
"@

$datasourcePath = Join-Path $monitoringDir "grafana-datasource.yml"
$grafanaDatasource | Set-Content -Path $datasourcePath -Encoding UTF8

$grafanaDashboardProv = @"
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
"@

$dashboardProvPath = Join-Path $monitoringDir "grafana-dashboard-provider.yml"
$grafanaDashboardProv | Set-Content -Path $dashboardProvPath -Encoding UTF8


# --- Check for BPMN heatmap plugin ---
$projectDir = (Resolve-Path (Join-Path $PSScriptRoot "../..")).Path
$pluginDir = Join-Path $projectDir "grafana-bpmn-heatmap-plugin/dist"
if (-not (Test-Path (Join-Path $pluginDir "module.js"))) {
    Write-Host "Plugin not built. Run: cd grafana-bpmn-heatmap-plugin && npm install --legacy-peer-deps && npm run build" -ForegroundColor Red
    exit 1
}
Write-Host "  BPMN Heatmap plugin detected" -ForegroundColor Green

# --- Start Grafana ---
Write-Host "Starting Grafana..." -ForegroundColor Green
$datasourceAbsPath = (Resolve-Path $datasourcePath).Path
$dashboardProvAbsPath = (Resolve-Path $dashboardProvPath).Path
$dashboardsDir = (Resolve-Path "$monitoringDir\dashboards").Path

# Check if Keycloak is running for OIDC auth
$keycloakRunning = $false
try {
    $null = Invoke-WebRequest -Uri "http://localhost:8180/realms/fluxnova" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
    $keycloakRunning = $true
    Write-Host "  Keycloak detected - enabling OIDC login for Grafana" -ForegroundColor Green
} catch {
    Write-Host "  Keycloak not running - Grafana will use local auth only" -ForegroundColor Gray
}

$grafanaArgs = @(
    "run", "-d",
    "--name", $GRAFANA_CONTAINER,
    "-p", "3000:3000",
    "-e", "GF_SECURITY_ADMIN_PASSWORD=fluxnova",
    "-e", "GF_SECURITY_ADMIN_USER=admin",
    "-e", "GF_SECURITY_DISABLE_INITIAL_ADMIN_PASSWORD_CHANGE=true",
    "-e", "GF_USERS_ALLOW_SIGN_UP=false",
    "-v", "${datasourceAbsPath}:/etc/grafana/provisioning/datasources/datasource.yml:ro",
    "-v", "${dashboardProvAbsPath}:/etc/grafana/provisioning/dashboards/dashboard.yml:ro",
    "-v", "${dashboardsDir}:/var/lib/grafana/dashboards:ro"
)

# Mount BPMN heatmap plugin
$pluginAbsPath = (Resolve-Path $pluginDir).Path
$grafanaArgs += @("-v", "${pluginAbsPath}:/var/lib/grafana/plugins/fluxnova-bpmn-heatmap-panel:ro")
$grafanaArgs += @("-e", "GF_PLUGINS_ALLOW_LOADING_UNSIGNED_PLUGINS=fluxnova-bpmn-heatmap-panel")

# Always mount grafana.ini (disables password change prompt, enables HTML panels, iframe embedding)
$grafanaIni = (Resolve-Path "$monitoringDir\grafana.ini").Path
$grafanaArgs += @("-v", "${grafanaIni}:/etc/grafana/grafana.ini:ro")

$grafanaArgs += $GRAFANA_IMAGE
& podman @grafanaArgs

if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to start Grafana." -ForegroundColor Red
    exit 1
}

# --- Wait for Grafana and disable password change prompt ---
Write-Host "Waiting for Grafana to be ready..." -ForegroundColor Gray
$grafanaReady = $false
for ($i = 0; $i -lt 20; $i++) {
    Start-Sleep -Seconds 2
    try {
        $null = Invoke-WebRequest -Uri "http://localhost:3000/api/health" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        $grafanaReady = $true
        break
    } catch {}
}

if ($grafanaReady) {
    # Confirm admin password via API to suppress the "change password" prompt
    try {
        $headers = @{
            Authorization = "Basic " + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("admin:fluxnova"))
            "Content-Type" = "application/json"
        }
        $body = '{"oldPassword":"fluxnova","newPassword":"fluxnova","confirmNew":"fluxnova"}'
        Invoke-RestMethod -Uri "http://localhost:3000/api/user/password" -Method Put -Headers $headers -Body $body -ErrorAction Stop | Out-Null
    } catch {}
}

# --- Done ---
Write-Host ""
Write-Host "======================================" -ForegroundColor Green
Write-Host "  Monitoring stack is running!" -ForegroundColor Green
Write-Host "======================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Exporter:    http://localhost:9091/metrics" -ForegroundColor White
Write-Host "  Prometheus:  http://localhost:9090" -ForegroundColor White
Write-Host "  Grafana:     http://localhost:3000  (admin / fluxnova)" -ForegroundColor White
Write-Host ""
Write-Host "  Dashboards:" -ForegroundColor Cyan
Write-Host "    Overview:  http://localhost:3000/d/fluxnova-bpm-overview" -ForegroundColor White
Write-Host "    Heatmap:   http://localhost:3000/d/fluxnova-heatmap" -ForegroundColor White
Write-Host ""
Write-Host "  Alerts:      http://localhost:9090/alerts" -ForegroundColor White
Write-Host ""
Write-Host "  To simulate bottlenecks:" -ForegroundColor Yellow
Write-Host "    .\simulate-bottleneck.ps1" -ForegroundColor White
Write-Host ""
Write-Host "  To stop:" -ForegroundColor Gray
Write-Host "    podman stop $EXPORTER_CONTAINER $PROMETHEUS_CONTAINER $GRAFANA_CONTAINER" -ForegroundColor Gray
Write-Host "    podman rm $EXPORTER_CONTAINER $PROMETHEUS_CONTAINER $GRAFANA_CONTAINER" -ForegroundColor Gray
