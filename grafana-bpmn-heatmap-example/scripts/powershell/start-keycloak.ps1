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
# start-keycloak.ps1
# Starts Keycloak as a local OIDC identity provider via Podman
# Pre-configured with the Fluxnova realm, users, groups, and clients

Write-Host "=== Keycloak Identity Provider ===" -ForegroundColor Cyan
Write-Host ""

$IMAGE = "quay.io/keycloak/keycloak:latest"
$CONTAINER_NAME = "fluxnova-keycloak"
$PORT = 8180

# --- Clean up existing container ---
$existing = podman ps -a --filter "name=$CONTAINER_NAME" --format "{{.Names}}" 2>$null
if ($existing -eq $CONTAINER_NAME) {
    Write-Host "Removing existing container: $CONTAINER_NAME" -ForegroundColor Yellow
    podman rm -f $CONTAINER_NAME | Out-Null
}

# --- Get realm config path ---
$realmConfig = (Resolve-Path "$PSScriptRoot\keycloak\fluxnova-realm.json").Path

# --- Start Keycloak ---
Write-Host "Starting Keycloak (this may take 30-60 seconds to initialize)..." -ForegroundColor Green
podman run -d `
    --name $CONTAINER_NAME `
    -p "${PORT}:8080" `
    -e "KEYCLOAK_ADMIN=admin" `
    -e "KEYCLOAK_ADMIN_PASSWORD=admin" `
    -v "${realmConfig}:/opt/keycloak/data/import/fluxnova-realm.json:ro" `
    $IMAGE `
    start-dev --import-realm

if ($LASTEXITCODE -ne 0) {
    Write-Host "Failed to start Keycloak." -ForegroundColor Red
    exit 1
}

# --- Wait for Keycloak to be ready ---
Write-Host "Waiting for Keycloak to start..." -ForegroundColor Gray
$maxWait = 90
$elapsed = 0
while ($elapsed -lt $maxWait) {
    Start-Sleep -Seconds 3
    $elapsed += 3
    try {
        $response = Invoke-WebRequest -Uri "http://localhost:$PORT/realms/master" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        if ($response.StatusCode -eq 200) {
            break
        }
    } catch {
        Write-Host "  Still starting... ($elapsed s)" -ForegroundColor Gray
    }
}

# --- Check if fluxnova realm was imported ---
try {
    $realm = Invoke-RestMethod -Uri "http://localhost:$PORT/realms/fluxnova" -TimeoutSec 5
    Write-Host ""
    Write-Host "Keycloak is ready!" -ForegroundColor Green
    Write-Host ""
} catch {
    Write-Host ""
    Write-Host "Keycloak is running but realm may not have imported." -ForegroundColor Yellow
    Write-Host "Check logs: podman logs $CONTAINER_NAME" -ForegroundColor Gray
    Write-Host ""
}

Write-Host "======================================" -ForegroundColor Cyan
Write-Host "  Keycloak Admin Console:" -ForegroundColor White
Write-Host "    http://localhost:$PORT/admin" -ForegroundColor White
Write-Host "    Login: admin / admin" -ForegroundColor White
Write-Host ""
Write-Host "  Fluxnova Realm:" -ForegroundColor White
Write-Host "    http://localhost:$PORT/realms/fluxnova" -ForegroundColor White
Write-Host ""
Write-Host "  OIDC Endpoints:" -ForegroundColor White
Write-Host "    Discovery: http://localhost:$PORT/realms/fluxnova/.well-known/openid-configuration" -ForegroundColor White
Write-Host "    Token:     http://localhost:$PORT/realms/fluxnova/protocol/openid-connect/token" -ForegroundColor White
Write-Host "    Auth:      http://localhost:$PORT/realms/fluxnova/protocol/openid-connect/auth" -ForegroundColor White
Write-Host ""
Write-Host "  Users (password = username or username123):" -ForegroundColor Yellow
Write-Host "    demo     - intake, reviewers" -ForegroundColor White
Write-Host "    alice    - intake, reviewers" -ForegroundColor White
Write-Host "    bob      - reviewers, risk-team" -ForegroundColor White
Write-Host "    charlie  - risk-team, senior-managers" -ForegroundColor White
Write-Host "    diana    - customer-service" -ForegroundColor White
Write-Host "    admin    - all groups" -ForegroundColor White
Write-Host ""
Write-Host "  OIDC Clients:" -ForegroundColor Yellow
Write-Host "    fluxnova-app  (secret: fluxnova-local-secret)" -ForegroundColor White
Write-Host "    grafana       (secret: grafana-local-secret)" -ForegroundColor White
Write-Host "======================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  To stop: podman stop $CONTAINER_NAME" -ForegroundColor Gray
Write-Host "  To remove: podman rm $CONTAINER_NAME" -ForegroundColor Gray
