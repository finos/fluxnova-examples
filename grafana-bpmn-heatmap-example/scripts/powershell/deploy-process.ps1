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
# deploy-process.ps1
# Deploys the sample BPMN process to the running Fluxnova engine via REST API

param(
    [string]$EngineUrl = "http://localhost:8080/engine-rest",
    [string]$BpmnFile = "processes/sample-approval.bpmn",
    [string]$DeploymentName = "sample-approval-deployment"
)

Write-Host "=== Deploy BPMN Process to Fluxnova ===" -ForegroundColor Cyan

# Check if the BPMN file exists
if (-not (Test-Path $BpmnFile)) {
    Write-Host "Error: BPMN file not found: $BpmnFile" -ForegroundColor Red
    exit 1
}

# Check if engine is reachable
Write-Host "Checking engine at $EngineUrl..." -ForegroundColor Green
try {
    $response = Invoke-RestMethod -Uri "$EngineUrl/engine" -Method Get -TimeoutSec 5
    Write-Host "Engine is running." -ForegroundColor Green
} catch {
    Write-Host "Error: Cannot reach Fluxnova engine at $EngineUrl" -ForegroundColor Red
    Write-Host "Make sure the container is running (.\start-fluxnova.ps1)" -ForegroundColor Yellow
    exit 1
}

# Deploy the BPMN file
Write-Host "Deploying: $BpmnFile" -ForegroundColor Green

$fileName = Split-Path $BpmnFile -Leaf
$fileBytes = [System.IO.File]::ReadAllBytes((Resolve-Path $BpmnFile))

$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"

$bodyLines = @(
    "--$boundary",
    "Content-Disposition: form-data; name=`"deployment-name`"$LF",
    $DeploymentName,
    "--$boundary",
    "Content-Disposition: form-data; name=`"enable-duplicate-filtering`"$LF",
    "true",
    "--$boundary",
    "Content-Disposition: form-data; name=`"deploy-changed-only`"$LF",
    "true",
    "--$boundary",
    "Content-Disposition: form-data; name=`"$fileName`"; filename=`"$fileName`"",
    "Content-Type: application/octet-stream$LF",
    [System.Text.Encoding]::UTF8.GetString($fileBytes),
    "--$boundary--$LF"
) -join $LF

try {
    $result = Invoke-RestMethod `
        -Uri "$EngineUrl/deployment/create" `
        -Method Post `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -Body $bodyLines

    Write-Host ""
    Write-Host "Deployment successful!" -ForegroundColor Green
    Write-Host "  Deployment ID:   $($result.id)" -ForegroundColor White
    Write-Host "  Deployment Name: $($result.name)" -ForegroundColor White
    Write-Host "  Deployed At:     $($result.deploymentTime)" -ForegroundColor White
    Write-Host ""
    Write-Host "Next steps:" -ForegroundColor Cyan
    Write-Host "  1. Open http://localhost:8080/fluxnova/app/tasklist/ (login: demo/demo)" -ForegroundColor White
    Write-Host "  2. Or start a process instance via REST:" -ForegroundColor White
    Write-Host "     .\start-process.ps1" -ForegroundColor White
} catch {
    Write-Host "Deployment failed: $_" -ForegroundColor Red
    exit 1
}
