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
# simulate-bottleneck.ps1
# Creates a realistic bottleneck scenario for the loan approval process.
# Starts many instances, moves most through early stages, but leaves them
# stuck at "Review Documents" and "Senior Manager Review" to demonstrate
# the heatmap and alerting.

param(
    [string]$EngineUrl = "http://localhost:8080/engine-rest",
    [int]$TotalInstances = 20
)

Write-Host "=== Bottleneck Simulation ===" -ForegroundColor Cyan
Write-Host "Process: Loan Approval" -ForegroundColor White
Write-Host "Strategy: Start $TotalInstances instances, move most past Validate," -ForegroundColor White
Write-Host "          but leave them stuck at Doc Review and Senior Review." -ForegroundColor White
Write-Host ""

# --- Check engine ---
try {
    $null = Invoke-RestMethod -Uri "$EngineUrl/engine" -Method Get -TimeoutSec 5
} catch {
    Write-Host "Error: Fluxnova not running. Start it first: .\start-fluxnova.ps1" -ForegroundColor Red
    exit 1
}

# --- Deploy loan-approval process ---
Write-Host "[1/4] Deploying loan-approval process..." -ForegroundColor Yellow

$bpmnFile = "processes/loan-approval.bpmn"
if (-not (Test-Path $bpmnFile)) {
    Write-Host "Error: $bpmnFile not found" -ForegroundColor Red
    exit 1
}

$fileName = Split-Path $bpmnFile -Leaf
$fileBytes = [System.IO.File]::ReadAllBytes((Resolve-Path $bpmnFile))
$boundary = [System.Guid]::NewGuid().ToString()
$LF = "`r`n"
$bodyLines = @(
    "--$boundary",
    "Content-Disposition: form-data; name=`"deployment-name`"$LF",
    "loan-approval-deployment",
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
    $null = Invoke-RestMethod `
        -Uri "$EngineUrl/deployment/create" `
        -Method Post `
        -ContentType "multipart/form-data; boundary=$boundary" `
        -Body $bodyLines
    Write-Host "  Deployed successfully." -ForegroundColor Green
} catch {
    Write-Host "  Deployment failed (may already exist): $_" -ForegroundColor Yellow
}

Start-Sleep -Seconds 2

# --- Start instances ---
Write-Host "[2/4] Starting $TotalInstances loan applications..." -ForegroundColor Yellow

$applicants = @("Alice Johnson", "Bob Smith", "Charlie Brown", "Diana Prince", "Eve Wilson",
                "Frank Miller", "Grace Lee", "Henry Davis", "Iris Chen", "Jack Thompson",
                "Karen White", "Liam Murphy", "Mia Garcia", "Noah Taylor", "Olivia Martinez",
                "Peter Anderson", "Quinn Harris", "Rachel Clark", "Sam Lewis", "Tara King")

for ($i = 0; $i -lt $TotalInstances; $i++) {
    $body = @{
        variables = @{
            applicant = @{ value = $applicants[$i % $applicants.Count]; type = "String" }
            loanAmount = @{ value = (Get-Random -Minimum 10000 -Maximum 500000); type = "Integer" }
            loanType = @{ value = @("Mortgage", "Personal", "Business", "Auto")[(Get-Random -Maximum 4)]; type = "String" }
            valid = @{ value = $true; type = "Boolean" }
        }
    } | ConvertTo-Json -Depth 4

    try {
        $null = Invoke-RestMethod `
            -Uri "$EngineUrl/process-definition/key/loan-approval-process/start" `
            -Method Post -ContentType "application/json" -Body $body
        Write-Host "  Started instance $($i+1)/$TotalInstances" -ForegroundColor Gray
    } catch {
        Write-Host "  Failed instance $($i+1): $_" -ForegroundColor Red
    }
    Start-Sleep -Milliseconds 200
}

# --- Complete "Validate Application" tasks (move all forward quickly) ---
Write-Host "[3/4] Completing 'Validate Application' tasks (fast stage)..." -ForegroundColor Yellow
Start-Sleep -Seconds 2

$validateTasks = Invoke-RestMethod -Uri "$EngineUrl/task?name=Validate+Application&maxResults=100" -Method Get -TimeoutSec 10
$validated = 0
foreach ($task in $validateTasks) {
    $body = @{
        variables = @{
            valid = @{ value = $true; type = "Boolean" }
        }
    } | ConvertTo-Json -Depth 4

    try {
        Invoke-RestMethod -Uri "$EngineUrl/task/$($task.id)/complete" -Method Post -ContentType "application/json" -Body $body
        $validated++
    } catch {
        # Some may fail if gateway needs different vars
    }
    Start-Sleep -Milliseconds 100
}
Write-Host "  Validated $validated applications (moved to parallel review stage)." -ForegroundColor Green

# --- Complete only a FEW "Review Documents" tasks (create bottleneck) ---
Write-Host "[4/4] Creating bottleneck at 'Review Documents'..." -ForegroundColor Yellow
Write-Host "  (Only completing 3 out of ~$validated document reviews)" -ForegroundColor Gray
Start-Sleep -Seconds 2

$docTasks = Invoke-RestMethod -Uri "$EngineUrl/task?name=Review+Documents&maxResults=100" -Method Get -TimeoutSec 10
$docsCompleted = 0
foreach ($task in $docTasks) {
    if ($docsCompleted -ge 3) { break }  # Only complete 3 — leave the rest stuck!

    $body = @{ variables = @{} } | ConvertTo-Json -Depth 4
    try {
        Invoke-RestMethod -Uri "$EngineUrl/task/$($task.id)/complete" -Method Post -ContentType "application/json" -Body $body
        $docsCompleted++
    } catch {}
    Start-Sleep -Milliseconds 100
}
Write-Host "  Completed $docsCompleted document reviews." -ForegroundColor Green
Write-Host "  Left $($docTasks.Count - $docsCompleted) stuck at 'Review Documents' ← BOTTLENECK" -ForegroundColor Red

# --- Complete risk assessment for the few that made it through ---
Start-Sleep -Seconds 2
$riskTasks = Invoke-RestMethod -Uri "$EngineUrl/task?name=Assess+Risk+Level&maxResults=100" -Method Get -TimeoutSec 10
$riskDone = 0
foreach ($task in $riskTasks) {
    # Mark most as high risk to create another bottleneck at Senior Review
    $body = @{
        variables = @{
            riskLevel = @{ value = "high"; type = "String" }
        }
    } | ConvertTo-Json -Depth 4
    try {
        Invoke-RestMethod -Uri "$EngineUrl/task/$($task.id)/complete" -Method Post -ContentType "application/json" -Body $body
        $riskDone++
    } catch {}
    Start-Sleep -Milliseconds 100
}
if ($riskDone -gt 0) {
    Write-Host "  Completed $riskDone risk assessments (all marked HIGH risk)." -ForegroundColor Green
    Write-Host "  These are now stuck at 'Senior Manager Review' ← SECOND BOTTLENECK" -ForegroundColor Red
}

# --- Summary ---
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Bottleneck Simulation Complete!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Instances started:     $TotalInstances" -ForegroundColor White
Write-Host "  Passed validation:     $validated" -ForegroundColor White
Write-Host "  Stuck at Doc Review:   ~$($docTasks.Count - $docsCompleted)" -ForegroundColor Red
Write-Host "  Stuck at Senior Review: ~$riskDone" -ForegroundColor Red
Write-Host ""
Write-Host "  The heatmap dashboard should now show:" -ForegroundColor Yellow
Write-Host "    - 'Review Documents' bar glowing RED (most tokens)" -ForegroundColor White
Write-Host "    - 'Senior Manager Review' bar in ORANGE" -ForegroundColor White
Write-Host "    - Token ratio gauge near 50%+ for Doc Review" -ForegroundColor White
Write-Host ""
Write-Host "  View: http://localhost:3000/d/fluxnova-heatmap" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Note: For the 24h alert to fire, tokens need to be stuck" -ForegroundColor Gray
Write-Host "  for 24 hours. The 4h warning alert will fire first." -ForegroundColor Gray
