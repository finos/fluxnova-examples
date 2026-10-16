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
# build-and-commit.sh
# Run this ONCE from a machine with working npm (e.g. your home Windows/WSL machine).
# It installs deps, builds the plugin, and commits the dist/ folder so that
# other developers can clone and run without needing npm at all.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Building BPMN Heatmap Grafana Plugin ==="
echo ""

# Install dependencies
echo "Installing dependencies..."
npm install --legacy-peer-deps

# Build
echo "Building plugin..."
npm run build

# Verify dist
if [ ! -f dist/module.js ]; then
    echo "ERROR: Build failed — dist/module.js not found"
    exit 1
fi

echo ""
echo "✅ Build complete. Contents of dist/:"
ls -la dist/
echo ""

# Stage the dist folder for commit
git add -f dist/
echo "dist/ staged for commit."
echo ""
echo "Now run:"
echo "  git commit -m 'chore: commit pre-built plugin dist for portable deployment'"
echo "  git push"
echo ""
echo "After this, anyone cloning the repo can use the plugin without npm."
