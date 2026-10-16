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
# complete-task.ps1
# Completes the "Review Request" user task with an approval decision

param(
    [string]$EngineUrl = "http://localhost:8080/engine-rest",
    [bool]$Approved = $true
)

Write-Host "=== Complete User Task ===" -ForegroundColor Cyan

# Find tasks assigned to 'demo'
try {
    $tasks = Invoke-RestMethod `
        -Uri "$EngineUrl/task?assignee=demo" `
        -Method Get
} catch {
    Write-Host "Failed to fetch tasks: $_" -ForegroundColor Red
    exit 1
}

if ($tasks.Count -eq 0) {
    Write-Host "No tasks found for user 'demo'." -ForegroundColor Yellow
    Write-Host "Start a process first: .\start-process.ps1" -ForegroundColor Gray
    exit 0
}

$task = $tasks[0]
Write-Host "Found task: $($task.name) (ID: $($task.id))" -ForegroundColor White

$decision = if ($Approved) { "approved" } else { "rejected" }
Write-Host "Completing with decision: $decision" -ForegroundColor Green

$body = @{
    variables = @{
        approved = @{ value = $Approved; type = "Boolean" }
    }
} | ConvertTo-Json -Depth 4

try {
    Invoke-RestMethod `
        -Uri "$EngineUrl/task/$($task.id)/complete" `
        -Method Post `
        -ContentType "application/json" `
        -Body $body

    Write-Host ""
    Write-Host "Task completed! The process will now follow the '$decision' path." -ForegroundColor Green
    Write-Host ""
    if ($Approved) {
        Write-Host "The 'Send Approval Notification' service task will execute." -ForegroundColor White
    } else {
        Write-Host "The 'Send Rejection Notification' service task will execute." -ForegroundColor White
    }
} catch {
    Write-Host "Failed to complete task: $_" -ForegroundColor Red
    exit 1
}
