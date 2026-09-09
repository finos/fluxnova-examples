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
"""
Fluxnova Prometheus Exporter

Bridges the Fluxnova BPM REST API to Prometheus metrics format.
Exposes both engine-level summary metrics and per-activity token tracking
used by the BPMN heatmap dashboard.

Metrics exported:
  - fluxnova_up                              Engine reachability (0 or 1)
  - fluxnova_process_definitions_total       Deployed process definitions
  - fluxnova_process_instances_active        Running process instances
  - fluxnova_tasks_open                      Open user tasks
  - fluxnova_process_instances_completed     Finished process instances
  - fluxnova_incidents_open                  Open incidents
  - fluxnova_deployments_total               Total deployments
  - fluxnova_activity_tokens                 Active tokens per activity (labels: activity_id, activity_name, activity_type, process)
  - fluxnova_activity_oldest_token_age_seconds   Age of oldest token per activity
  - fluxnova_activity_avg_duration_seconds   Average token duration per activity
  - fluxnova_activity_token_ratio            Ratio of total tokens at each activity (0-1)

Environment variables:
  FLUXNOVA_URL    - Fluxnova REST API base URL (default: http://host.containers.internal:8080/engine-rest)
  EXPORTER_PORT   - Port to serve metrics on (default: 9091)

Usage:
  python exporter.py
  curl http://localhost:9091/metrics
"""

import http.server
import urllib.request
import json
import os
import time
from datetime import datetime


# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

FLUXNOVA_URL = os.environ.get("FLUXNOVA_URL", "http://host.containers.internal:8080/engine-rest")
PORT = int(os.environ.get("EXPORTER_PORT", "9091"))


# ---------------------------------------------------------------------------
# HTTP Client
# ---------------------------------------------------------------------------

def fetch_json(path):
    """
    Fetch a JSON response from the Fluxnova REST API.

    Args:
        path: API path (e.g., "/engine" or "/task/count")

    Returns:
        Parsed JSON as dict/list, or None if the request fails.
    """
    try:
        req = urllib.request.Request(f"{FLUXNOVA_URL}{path}")
        with urllib.request.urlopen(req, timeout=10) as resp:
            return json.loads(resp.read().decode())
    except Exception:
        return None


# ---------------------------------------------------------------------------
# Timestamp Parsing
# ---------------------------------------------------------------------------

def parse_timestamp(ts_str):
    """
    Parse a Fluxnova ISO-8601 timestamp to Unix epoch seconds.

    Handles formats like: 2026-08-12T18:38:38.621+00:00

    Args:
        ts_str: ISO-8601 timestamp string from the Fluxnova API.

    Returns:
        Float epoch seconds, or None if parsing fails.
    """
    if not ts_str:
        return None
    try:
        dt = datetime.fromisoformat(ts_str)
        return dt.timestamp()
    except Exception:
        try:
            # Fallback: strip milliseconds
            clean = ts_str.split(".")[0]
            if "+" in ts_str:
                tz_part = ts_str.rsplit("+", 1)[-1]
                clean += "+" + tz_part
            elif ts_str.endswith("Z"):
                clean += "+00:00"
            dt = datetime.fromisoformat(clean)
            return dt.timestamp()
        except Exception:
            return None


# ---------------------------------------------------------------------------
# Activity Metrics (Per-Activity Token Tracking)
# ---------------------------------------------------------------------------

def get_activity_metrics():
    """
    Query Fluxnova's history API for unfinished activity instances and
    compute per-activity metrics: token counts, ages, and ratios.

    Returns:
        List of Prometheus-formatted metric lines.
    """
    lines = []
    now = time.time()

    # Fetch all currently active (unfinished) activity instances
    active_activities = fetch_json("/history/activity-instance?unfinished=true&maxResults=1000")
    if not active_activities:
        return lines

    # Aggregate by activity ID
    activity_data = {}
    for act in active_activities:
        act_id = act.get("activityId", "unknown")
        act_name = (act.get("activityName") or act_id).replace("\n", " ").replace("\r", "").replace('"', "'").replace("\\", "/").strip()
        act_type = act.get("activityType", "unknown")
        proc_key = act.get("processDefinitionKey", "unknown")
        start_epoch = parse_timestamp(act.get("startTime"))

        if act_id not in activity_data:
            activity_data[act_id] = {
                "name": act_name,
                "type": act_type,
                "process": proc_key,
                "count": 0,
                "oldest_start": None,
                "total_duration": 0.0,
            }

        entry = activity_data[act_id]
        entry["count"] += 1

        if start_epoch:
            duration = now - start_epoch
            entry["total_duration"] += duration
            if entry["oldest_start"] is None or start_epoch < entry["oldest_start"]:
                entry["oldest_start"] = start_epoch

    # --- Emit metrics ---

    def labels_for(act_id, data):
        return f'activity_id="{act_id}",activity_name="{data["name"]}",activity_type="{data["type"]}",process="{data["process"]}"'

    # Token counts
    lines.append("# HELP fluxnova_activity_tokens Number of active tokens at each activity")
    lines.append("# TYPE fluxnova_activity_tokens gauge")
    for act_id, data in activity_data.items():
        lines.append(f"fluxnova_activity_tokens{{{labels_for(act_id, data)}}} {data['count']}")

    # Oldest token age
    lines.append("# HELP fluxnova_activity_oldest_token_age_seconds Age of the oldest token at each activity")
    lines.append("# TYPE fluxnova_activity_oldest_token_age_seconds gauge")
    for act_id, data in activity_data.items():
        if data["oldest_start"]:
            age = now - data["oldest_start"]
            lines.append(f"fluxnova_activity_oldest_token_age_seconds{{{labels_for(act_id, data)}}} {age:.0f}")

    # Average duration
    lines.append("# HELP fluxnova_activity_avg_duration_seconds Average time tokens have spent at each activity")
    lines.append("# TYPE fluxnova_activity_avg_duration_seconds gauge")
    for act_id, data in activity_data.items():
        if data["count"] > 0:
            avg = data["total_duration"] / data["count"]
            lines.append(f"fluxnova_activity_avg_duration_seconds{{{labels_for(act_id, data)}}} {avg:.0f}")

    # Token ratio (percentage distribution)
    total_tokens = sum(d["count"] for d in activity_data.values())
    if total_tokens > 0:
        lines.append("# HELP fluxnova_activity_token_ratio Ratio of total tokens at each activity (0-1)")
        lines.append("# TYPE fluxnova_activity_token_ratio gauge")
        for act_id, data in activity_data.items():
            ratio = data["count"] / total_tokens
            lines.append(f"fluxnova_activity_token_ratio{{{labels_for(act_id, data)}}} {ratio:.4f}")

    return lines


# ---------------------------------------------------------------------------
# Engine-Level Metrics
# ---------------------------------------------------------------------------

def get_basic_metrics():
    """
    Query Fluxnova for top-level engine metrics: instance counts, task counts, etc.

    Returns:
        List of Prometheus-formatted metric lines.
    """
    lines = []

    # Engine health check
    lines.append("# HELP fluxnova_up Whether Fluxnova engine is reachable (1=up, 0=down)")
    lines.append("# TYPE fluxnova_up gauge")

    engine = fetch_json("/engine")
    if engine is None:
        lines.append("fluxnova_up 0")
        return lines
    lines.append("fluxnova_up 1")

    # Process definitions
    data = fetch_json("/process-definition/count")
    if data:
        lines.append("# HELP fluxnova_process_definitions_total Deployed process definitions")
        lines.append("# TYPE fluxnova_process_definitions_total gauge")
        lines.append(f"fluxnova_process_definitions_total {data.get('count', 0)}")

    # Active instances
    data = fetch_json("/process-instance/count")
    if data:
        lines.append("# HELP fluxnova_process_instances_active Active process instances")
        lines.append("# TYPE fluxnova_process_instances_active gauge")
        lines.append(f"fluxnova_process_instances_active {data.get('count', 0)}")

    # Open tasks
    data = fetch_json("/task/count")
    if data:
        lines.append("# HELP fluxnova_tasks_open Open user tasks")
        lines.append("# TYPE fluxnova_tasks_open gauge")
        lines.append(f"fluxnova_tasks_open {data.get('count', 0)}")

    # Completed instances
    data = fetch_json("/history/process-instance/count?finished=true")
    if data:
        lines.append("# HELP fluxnova_process_instances_completed Completed process instances")
        lines.append("# TYPE fluxnova_process_instances_completed gauge")
        lines.append(f"fluxnova_process_instances_completed {data.get('count', 0)}")

    # Open incidents
    data = fetch_json("/incident/count")
    if data:
        lines.append("# HELP fluxnova_incidents_open Open incidents")
        lines.append("# TYPE fluxnova_incidents_open gauge")
        lines.append(f"fluxnova_incidents_open {data.get('count', 0)}")

    # Deployments
    data = fetch_json("/deployment/count")
    if data:
        lines.append("# HELP fluxnova_deployments_total Total deployments")
        lines.append("# TYPE fluxnova_deployments_total gauge")
        lines.append(f"fluxnova_deployments_total {data.get('count', 0)}")

    return lines


# ---------------------------------------------------------------------------
# Metrics Endpoint
# ---------------------------------------------------------------------------

def get_metrics():
    """Combine all metrics into a single Prometheus-compatible text response."""
    lines = get_basic_metrics()
    lines.append("")
    try:
        lines.extend(get_activity_metrics())
    except Exception as e:
        lines.append(f"# ERROR in activity metrics: {e}")
    return "\n".join(lines) + "\n"


# ---------------------------------------------------------------------------
# HTTP Server
# ---------------------------------------------------------------------------

class MetricsHandler(http.server.BaseHTTPRequestHandler):
    """Simple HTTP handler exposing /metrics and /health endpoints."""

    def do_GET(self):
        if self.path == "/metrics":
            body = get_metrics().encode("utf-8")
            self.send_response(200)
            self.send_header("Content-Type", "text/plain; charset=utf-8")
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
        elif self.path == "/health":
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"OK")
        else:
            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"Fluxnova Exporter - GET /metrics for Prometheus scraping")

    def log_message(self, format, *args):
        pass  # Suppress per-request logging


# ---------------------------------------------------------------------------
# Entry Point
# ---------------------------------------------------------------------------

if __name__ == "__main__":
    print(f"Fluxnova Exporter starting on port {PORT}")
    print(f"  Fluxnova API: {FLUXNOVA_URL}")
    print(f"  Metrics:      http://0.0.0.0:{PORT}/metrics")
    print(f"  Health:       http://0.0.0.0:{PORT}/health")

    server = http.server.HTTPServer(("0.0.0.0", PORT), MetricsHandler)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down.")
        server.shutdown()
