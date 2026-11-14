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
# verify-monitoring.ps1
# End-to-end verification that Fluxnova -> Exporter -> Prometheus -> Grafana pipeline works

param(
    [string]$FluxnovaUrl = "http://localhost:8080/engine-rest",
    [string]$ExporterUrl = "http://localhost:9091",
    [string]$PrometheusUrl = "http://localhost:9090",
    [string]$GrafanaUrl = "http://localhost:3000"
)

$passed = 0
$failed = 0

function Test-Step {
    param([string]$Name, [scriptblock]$Test)

    Write-Host "  [$Name] " -NoNewline -ForegroundColor White
    try {
        $result = & $Test
        if ($result) {
            Write-Host "PASS" -ForegroundColor Green
            $script:passed++
        } else {
            Write-Host "FAIL" -ForegroundColor Red
            $script:failed++
        }
    } catch {
        Write-Host "FAIL ($_)" -ForegroundColor Red
        $script:failed++
    }
}

Write-Host "=== Fluxnova Monitoring Verification ===" -ForegroundColor Cyan
Write-Host ""

# --- Step 1: Fluxnova Engine ---
Write-Host "1. Fluxnova Engine" -ForegroundColor Yellow
Test-Step "Engine reachable" {
    $r = Invoke-RestMethod -Uri "$FluxnovaUrl/engine" -TimeoutSec 5
    $r.Count -gt 0
}

# --- Step 2: Metrics Exporter ---
Write-Host "2. Metrics Exporter" -ForegroundColor Yellow
Test-Step "Exporter /metrics endpoint" {
    $r = Invoke-WebRequest -Uri "$ExporterUrl/metrics" -TimeoutSec 5
    $r.StatusCode -eq 200
}
Test-Step "Exporter returns fluxnova_up" {
    $r = (Invoke-WebRequest -Uri "$ExporterUrl/metrics" -TimeoutSec 5).Content
    $r -match "fluxnova_up 1"
}
Test-Step "Exporter returns process metrics" {
    $r = (Invoke-WebRequest -Uri "$ExporterUrl/metrics" -TimeoutSec 5).Content
    $r -match "fluxnova_process_definitions_total"
}

# --- Step 3: Prometheus ---
Write-Host "3. Prometheus" -ForegroundColor Yellow
Test-Step "Prometheus reachable" {
    $r = Invoke-WebRequest -Uri "$PrometheusUrl/-/healthy" -TimeoutSec 5
    $r.StatusCode -eq 200
}
Test-Step "Prometheus scraping target" {
    $r = Invoke-RestMethod -Uri "$PrometheusUrl/api/v1/targets" -TimeoutSec 5
    $active = $r.data.activeTargets | Where-Object { $_.labels.job -eq "fluxnova-exporter" }
    $active.Count -gt 0
}
Test-Step "Prometheus has fluxnova_up metric" {
    # Give Prometheus a moment to scrape if just started
    Start-Sleep -Seconds 2
    $r = Invoke-RestMethod -Uri "$PrometheusUrl/api/v1/query?query=fluxnova_up" -TimeoutSec 5
    $r.data.result.Count -gt 0
}

# --- Step 4: Grafana ---
Write-Host "4. Grafana" -ForegroundColor Yellow
Test-Step "Grafana reachable" {
    $r = Invoke-WebRequest -Uri "$GrafanaUrl/api/health" -TimeoutSec 5
    $r.StatusCode -eq 200
}
Test-Step "Grafana datasource configured" {
    $cred = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:fluxnova"))
    $headers = @{ Authorization = "Basic $cred" }
    $r = Invoke-RestMethod -Uri "$GrafanaUrl/api/datasources" -Headers $headers -TimeoutSec 5
    ($r | Where-Object { $_.type -eq "prometheus" }).Count -gt 0
}
Test-Step "Grafana dashboard loaded" {
    $cred = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:fluxnova"))
    $headers = @{ Authorization = "Basic $cred" }
    $r = Invoke-RestMethod -Uri "$GrafanaUrl/api/dashboards/uid/fluxnova-bpm-overview" -Headers $headers -TimeoutSec 5
    $r.dashboard.title -eq "Fluxnova BPM Overview"
}

# --- Step 5: End-to-end data flow ---
Write-Host "5. End-to-End Data Flow" -ForegroundColor Yellow
Test-Step "Prometheus query returns process data" {
    $r = Invoke-RestMethod -Uri "$PrometheusUrl/api/v1/query?query=fluxnova_process_definitions_total" -TimeoutSec 5
    $r.data.result.Count -gt 0 -and [int]$r.data.result[0].value[1] -ge 0
}

# --- Summary ---
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Results: $passed passed, $failed failed" -ForegroundColor $(if ($failed -eq 0) { "Green" } else { "Red" })
Write-Host "========================================" -ForegroundColor Cyan

if ($failed -eq 0) {
    Write-Host ""
    Write-Host "All checks passed! Your monitoring stack is working." -ForegroundColor Green
    Write-Host "  Open Grafana: http://localhost:3000 (admin/admin)" -ForegroundColor White
    Write-Host "  Dashboard: http://localhost:3000/d/fluxnova-bpm-overview" -ForegroundColor White
} else {
    Write-Host ""
    Write-Host "Some checks failed. Make sure all components are running:" -ForegroundColor Yellow
    Write-Host "  1. Fluxnova:  .\start-fluxnova.ps1" -ForegroundColor White
    Write-Host "  2. Monitoring: .\start-monitoring.ps1" -ForegroundColor White
    exit 1
}
