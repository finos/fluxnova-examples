#!/usr/bin/env bash
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
# start-fluxnova.sh - Starts the Fluxnova BPM Platform using Podman
set -euo pipefail

CONTAINER_NAME="fluxnova-bpm"
IMAGE="${FLUXNOVA_IMAGE:-${REGISTRY:-docker.io}/finos/fluxnova-bpm-platform:latest}"
SCRIPT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

echo "=== Fluxnova BPM Platform (Podman) ==="

# Remove existing container
if podman ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    echo "Removing existing container: $CONTAINER_NAME"
    podman rm -f "$CONTAINER_NAME" >/dev/null
fi

# Auto-detect Keycloak
USE_KEYCLOAK=false
if [[ "${1:-}" == "--with-keycloak" ]]; then
    USE_KEYCLOAK=true
elif [[ "${1:-}" != "--without-keycloak" ]]; then
    if curl -sf --max-time 3 http://localhost:8180/realms/fluxnova >/dev/null 2>&1; then
        USE_KEYCLOAK=true
    fi
fi

if [ "$USE_KEYCLOAK" = true ]; then
    echo "Mode: Keycloak Identity Provider"
    # Build custom image if needed
    if ! podman images -q fluxnova-keycloak 2>/dev/null | grep -q .; then
        echo "Building custom Fluxnova image with Keycloak plugin..."
        podman build -t fluxnova-keycloak -f "$SCRIPT_DIR/docker/Dockerfile" "$SCRIPT_DIR/docker" >/dev/null 2>&1
    fi
    IMAGE="localhost/fluxnova-keycloak:latest"
else
    echo "Mode: Built-in authentication (demo/demo)"
    echo "Pulling image: $IMAGE"
    podman pull "$IMAGE"
fi

# Run the container
echo "Starting Fluxnova BPM Platform..."
podman run -d \
    --name "$CONTAINER_NAME" \
    -p 8080:8080 \
    "$IMAGE"

echo ""
echo "Fluxnova is starting up! Give it 20-30 seconds to initialize."
echo ""
echo "Access points:"
echo "  Web Apps: http://localhost:8080/fluxnova/app/"
echo "  REST API: http://localhost:8080/engine-rest/"
echo ""
echo "Credentials: demo / demo"
echo ""
echo "To view logs: podman logs -f $CONTAINER_NAME"
echo "To stop:      podman stop $CONTAINER_NAME && podman rm $CONTAINER_NAME"
