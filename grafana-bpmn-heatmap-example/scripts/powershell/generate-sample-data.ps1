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
# generate-sample-data.ps1
# Deploys the process, starts multiple instances, and completes some tasks
# to populate the Grafana dashboard with real metrics

param(
    [string]$EngineUrl = "http://localhost:8080/engine-rest",
    [int]$InstanceCount = 5,
    [int]$CompleteCount = 3
)

Write-Host "=== Generate Sample Data for Grafana ===" -ForegroundColor Cyan
Write-Host ""

# --- Check engine is running ---
Write-Host "Checking Fluxnova engine..." -ForegroundColor Green
try {
    $null = Invoke-RestMethod -Uri "$EngineUrl/engine" -Method Get -TimeoutSec 5
} catch {
    Write-Host "Error: Fluxnova is not running. Start it first: .\start-fluxnova.ps1" -ForegroundColor Red
    exit 1
}

# --- Deploy the process if not already deployed ---
Write-Host "Checking for deployed process..." -ForegroundColor Green
$definitions = Invoke-RestMethod -Uri "$EngineUrl/process-definition?key=sample-approval-process" -Method Get -TimeoutSec 5
if ($definitions.Count -eq 0) {
    Write-Host "Deploying sample-approval process..." -ForegroundColor Yellow
    & "$PSScriptRoot\deploy-process.ps1"
    Start-Sleep -Seconds 2
}

# --- Start multiple process instances ---
Write-Host ""
Write-Host "Starting $InstanceCount process instances..." -ForegroundColor Green

$items = @("New laptop", "Standing desk", "Software license", "Conference ticket", "Training course")
$requesters = @("Alice", "Bob", "Charlie", "Diana", "Eve")

for ($i = 0; $i -lt $InstanceCount; $i++) {
    $body = @{
        variables = @{
            requester = @{ value = $requesters[$i % $requesters.Count]; type = "String" }
            item = @{ value = $items[$i % $items.Count]; type = "String" }
            amount = @{ value = (Get-Random -Minimum 100 -Maximum 5000); type = "Integer" }
        }
    } | ConvertTo-Json -Depth 4

    try {
        $result = Invoke-RestMethod `
            -Uri "$EngineUrl/process-definition/key/sample-approval-process/start" `
            -Method Post `
            -ContentType "application/json" `
            -Body $body

        Write-Host "  Started instance $($i+1)/$InstanceCount (ID: $($result.id.Substring(0,8))...)" -ForegroundColor White
    } catch {
        Write-Host "  Failed to start instance $($i+1): $_" -ForegroundColor Red
    }

    Start-Sleep -Milliseconds 500
}

# --- Complete some tasks ---
Write-Host ""
Write-Host "Completing $CompleteCount tasks..." -ForegroundColor Green

Start-Sleep -Seconds 1

$tasks = Invoke-RestMethod -Uri "$EngineUrl/task?assignee=demo&maxResults=$CompleteCount" -Method Get -TimeoutSec 5

$completed = 0
foreach ($task in $tasks) {
    if ($completed -ge $CompleteCount) { break }

    $approved = ($completed % 2 -eq 0)  # alternate approve/reject
    $body = @{
        variables = @{
            approved = @{ value = $approved; type = "Boolean" }
        }
    } | ConvertTo-Json -Depth 4

    try {
        Invoke-RestMethod `
            -Uri "$EngineUrl/task/$($task.id)/complete" `
            -Method Post `
            -ContentType "application/json" `
            -Body $body

        $decision = if ($approved) { "approved" } else { "rejected" }
        Write-Host "  Completed task $($completed+1): $($task.name) -> $decision" -ForegroundColor White
        $completed++
    } catch {
        Write-Host "  Failed to complete task: $_" -ForegroundColor Red
    }

    Start-Sleep -Milliseconds 500
}

# --- Summary ---
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Data generated!" -ForegroundColor Green
Write-Host "  Instances started:  $InstanceCount" -ForegroundColor White
Write-Host "  Tasks completed:    $completed" -ForegroundColor White
Write-Host "  Tasks still open:   $($InstanceCount - $completed)" -ForegroundColor White
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Give Prometheus ~10-15 seconds to scrape, then check Grafana:" -ForegroundColor Yellow
Write-Host "  http://localhost:3000/d/fluxnova-bpm-overview" -ForegroundColor White
Write-Host ""
Write-Host "Run this script again to add more data over time." -ForegroundColor Gray
