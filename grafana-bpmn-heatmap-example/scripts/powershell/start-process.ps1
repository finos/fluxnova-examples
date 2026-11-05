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
# start-process.ps1
# Starts a new instance of the sample approval process

param(
    [string]$EngineUrl = "http://localhost:8080/engine-rest",
    [string]$ProcessKey = "sample-approval-process"
)

Write-Host "=== Start Process Instance ===" -ForegroundColor Cyan

$body = @{
    variables = @{
        requester = @{ value = "John Doe"; type = "String" }
        item = @{ value = "New laptop"; type = "String" }
        amount = @{ value = 1500; type = "Integer" }
    }
} | ConvertTo-Json -Depth 4

try {
    $result = Invoke-RestMethod `
        -Uri "$EngineUrl/process-definition/key/$ProcessKey/start" `
        -Method Post `
        -ContentType "application/json" `
        -Body $body

    Write-Host ""
    Write-Host "Process instance started!" -ForegroundColor Green
    Write-Host "  Instance ID:  $($result.id)" -ForegroundColor White
    Write-Host "  Process Key:  $ProcessKey" -ForegroundColor White
    Write-Host ""
    Write-Host "A task is now waiting for user 'demo' in the Tasklist." -ForegroundColor Yellow
    Write-Host "Open: http://localhost:8080/fluxnova/app/tasklist/" -ForegroundColor White
    Write-Host ""
    Write-Host "To complete the task via REST, use:" -ForegroundColor Cyan
    Write-Host "  .\complete-task.ps1" -ForegroundColor White
} catch {
    Write-Host "Failed to start process: $_" -ForegroundColor Red
    Write-Host "Make sure the process is deployed (.\deploy-process.ps1)" -ForegroundColor Yellow
    exit 1
}
