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
# complete-risk-tasks.ps1 - helper to push tokens through to Senior Review
$EngineUrl = "http://localhost:8080/engine-rest"

$riskTasks = Invoke-RestMethod -Uri "$EngineUrl/task?name=Assess+Risk+Level&maxResults=20" -Method Get -TimeoutSec 10
$done = 0
foreach ($task in $riskTasks) {
    $body = @{ variables = @{ riskLevel = @{ value = "high"; type = "String" } } } | ConvertTo-Json -Depth 4
    try {
        Invoke-RestMethod -Uri "$EngineUrl/task/$($task.id)/complete" -Method Post -ContentType "application/json" -Body $body
        $done++
    } catch {}
    Start-Sleep -Milliseconds 100
}
Write-Host "Completed $done risk assessments (all HIGH risk -> Senior Review)"

$instances = Invoke-RestMethod -Uri "$EngineUrl/process-instance/count" -TimeoutSec 5
$tasks = Invoke-RestMethod -Uri "$EngineUrl/task/count" -TimeoutSec 5
$completed = Invoke-RestMethod -Uri "$EngineUrl/history/process-instance/count?finished=true" -TimeoutSec 5
Write-Host ""
Write-Host "Final state:" -ForegroundColor Cyan
Write-Host "  Active instances: $($instances.count)" -ForegroundColor White
Write-Host "  Open tasks:       $($tasks.count)" -ForegroundColor White
Write-Host "  Completed:        $($completed.count)" -ForegroundColor White
