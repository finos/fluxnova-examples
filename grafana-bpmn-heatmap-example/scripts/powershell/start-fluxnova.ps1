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
# start-fluxnova.ps1
# Starts the Fluxnova BPM Platform using Podman
# Automatically detects if Keycloak is running and uses the Keycloak-integrated image

param(
    [switch]$WithKeycloak,
    [switch]$WithoutKeycloak
)

$CONTAINER_NAME = "fluxnova-bpm"

Write-Host "=== Fluxnova BPM Platform (Podman) ===" -ForegroundColor Cyan

# Check if container is already running
$existing = podman ps -a --filter "name=$CONTAINER_NAME" --format "{{.Names}}" 2>$null
if ($existing -eq $CONTAINER_NAME) {
    Write-Host "Container '$CONTAINER_NAME' already exists. Removing it..." -ForegroundColor Yellow
    podman rm -f $CONTAINER_NAME | Out-Null
}

# Determine which mode to use
$useKeycloak = $false
if ($WithKeycloak) {
    $useKeycloak = $true
} elseif (-not $WithoutKeycloak) {
    # Auto-detect: check if Keycloak is running
    try {
        $null = Invoke-WebRequest -Uri "http://localhost:8180/realms/fluxnova" -UseBasicParsing -TimeoutSec 3 -ErrorAction Stop
        $useKeycloak = $true
    } catch {
        $useKeycloak = $false
    }
}

if ($useKeycloak) {
    Write-Host "Mode: Keycloak Identity Provider" -ForegroundColor Green
    Write-Host "  Users and groups are managed in Keycloak" -ForegroundColor Gray

    # Build the custom image if needed
    $imageExists = podman images -q fluxnova-keycloak 2>$null
    if (-not $imageExists) {
        Write-Host "Building custom Fluxnova image with Keycloak plugin..." -ForegroundColor Yellow
        $dockerDir = Join-Path $PSScriptRoot "docker"
        podman build -t fluxnova-keycloak -f "$dockerDir\Dockerfile" $dockerDir 2>&1 | Out-Null
    }

    $IMAGE = "localhost/fluxnova-keycloak:latest"
} else {
    Write-Host "Mode: Built-in authentication (demo/demo)" -ForegroundColor Yellow
    Write-Host "  Start Keycloak first for OIDC auth: .\start-keycloak.ps1" -ForegroundColor Gray
    $IMAGE = "finos/fluxnova-bpm-platform:latest"
}

# Pull the image (only for remote images)
if ($IMAGE -notlike "localhost/*") {
    Write-Host "Pulling image: $IMAGE" -ForegroundColor Green
    podman pull $IMAGE
}

# Run the container
Write-Host "Starting Fluxnova BPM Platform..." -ForegroundColor Green
podman run -d `
    --name $CONTAINER_NAME `
    -p 8080:8080 `
    $IMAGE

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "Fluxnova is starting up! Give it 20-30 seconds to initialize." -ForegroundColor Green
    Write-Host ""
    Write-Host "Access points:" -ForegroundColor Cyan
    Write-Host "  Web Apps (Tasklist/Admin/Monitoring): http://localhost:8080/fluxnova/app/" -ForegroundColor White
    Write-Host "  REST API:                            http://localhost:8080/engine-rest/" -ForegroundColor White
    Write-Host ""
    if ($useKeycloak) {
        Write-Host "Login credentials (from Keycloak):" -ForegroundColor Yellow
        Write-Host "  admin / admin      (full admin - camunda-admin group)" -ForegroundColor White
        Write-Host "  demo / demo        (user - intake, reviewers)" -ForegroundColor White
        Write-Host "  charlie / charlie123 (manager - senior-managers)" -ForegroundColor White
        Write-Host ""
        Write-Host "Manage users: http://localhost:8180/admin (admin/admin)" -ForegroundColor Gray
    } else {
        Write-Host "Default credentials:  demo / demo" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "To view logs:   podman logs -f $CONTAINER_NAME" -ForegroundColor Gray
    Write-Host "To stop:        podman stop $CONTAINER_NAME" -ForegroundColor Gray
    Write-Host "To remove:      podman rm $CONTAINER_NAME" -ForegroundColor Gray
} else {
    Write-Host "Failed to start container. Check podman is running." -ForegroundColor Red
    exit 1
}
