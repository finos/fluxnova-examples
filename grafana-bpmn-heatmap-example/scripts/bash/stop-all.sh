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
# stop-all.sh — Stop and remove all Fluxnova demo containers and processes.
set -euo pipefail

echo "Stopping Fluxnova demo stack..."

# Stop containers
for container in fluxnova-bpm fluxnova-exporter fluxnova-prometheus fluxnova-grafana; do
    if podman ps -a --format '{{.Names}}' | grep -q "^${container}$"; then
        podman rm -f "$container" >/dev/null 2>&1
        echo "  Removed: $container"
    fi
done

echo ""
echo "✅ All stopped."
