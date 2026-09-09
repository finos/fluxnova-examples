# BPMN Process Heatmap — Grafana Panel Plugin

A reusable Grafana panel plugin that renders BPMN 2.0 diagrams with real-time metric-driven heatmap overlays. Identifies process bottlenecks at a glance by colour-coding activities based on token/instance counts from Prometheus (or any Grafana datasource).

## Features

- 🗺️ **Native BPMN rendering** — Uses bpmn-js to display your process diagram directly inside Grafana
- 🎨 **Colour-coded badges** — Green (1–2), Orange (3–9), Red (10–19), Pink/pulsing (20+)
- 🏷️ **Bottleneck header** — Instantly shows which activity has the most tokens
- 📊 **Any datasource** — Works with Prometheus, InfluxDB, SQL — anything that returns activity IDs and counts
- ⚙️ **Configurable** — BPMN source, field mapping, thresholds, and display options all in the panel editor
- 🔁 **Live updates** — Refreshes with Grafana's built-in auto-refresh (no separate server needed)

## Installation

### Option 1: Local (development)

```bash
cd grafana-bpmn-heatmap-plugin
npm install --legacy-peer-deps
npm run build
```

Copy the `dist/` folder to your Grafana plugins directory:

```bash
cp -r dist/ /var/lib/grafana/plugins/fluxnova-bpmn-heatmap-panel/
```

Restart Grafana, then allow unsigned plugins in `grafana.ini`:

```ini
[plugins]
allow_loading_unsigned_plugins = fluxnova-bpmn-heatmap-panel
```

### Option 2: Podman/Docker mount

Mount the built plugin into the Grafana container:

```bash
podman run -d \
  --name grafana \
  -p 3000:3000 \
  -v ./dist:/var/lib/grafana/plugins/fluxnova-bpmn-heatmap-panel:ro \
  -e GF_PLUGINS_ALLOW_LOADING_UNSIGNED_PLUGINS=fluxnova-bpmn-heatmap-panel \
  grafana/grafana:11.1.0
```

## Configuration

### Panel Options

| Option | Default | Description |
|--------|---------|-------------|
| BPMN File URL | _(empty)_ | URL to fetch .bpmn XML file |
| BPMN XML (inline) | _(empty)_ | Paste BPMN XML directly (URL takes priority) |
| Activity ID Field | `activity_id` | Label/field name mapping to BPMN element IDs |
| Value Field | `Value` | Field containing the token count |
| Activity Name Field | `activity_name` | Field for display names (used in bottleneck header) |
| Show Count Badges | `true` | Show/hide count numbers on elements |
| Show Legend | `true` | Show/hide the threshold legend |
| Show Bottleneck Header | `true` | Show/hide the "Biggest Bottleneck" bar |

### Prometheus Query Example

```promql
fluxnova_activity_tokens{process="loan-approval-process"}
```

Use **Format: Table** or **Format: Instant** in the Prometheus datasource. The plugin reads the `activity_id` and `activity_name` labels automatically.

### Threshold Levels

| Tokens | Colour | Label |
|--------|--------|-------|
| 1–2 | 🟢 Green | Flowing |
| 3–9 | 🟠 Orange | Building up |
| 10–19 | 🔴 Red | Bottleneck |
| 20+ | 🩷 Pink (pulsing) | Critical |

## Serving the BPMN File

The plugin needs to fetch the BPMN XML. Options:

1. **Grafana's built-in file serving** — Place the `.bpmn` file in `public/` and reference as `/public/plugins/fluxnova-bpmn-heatmap-panel/processes/loan-approval.bpmn`
2. **Any web server** — Host the file anywhere accessible to the browser
3. **Inline** — Paste the XML directly in panel options (good for simple processes)

## Using with Different Projects

1. Export your BPMN from your modeller
2. Deploy the BPMN to your BPM engine
3. Ensure your exporter/metrics expose per-activity counts with an `activity_id` label matching the BPMN element IDs
4. Add this panel to any Grafana dashboard
5. Set the BPMN URL and your Prometheus query — done

## Architecture

```
Your BPM Engine → Your Exporter → Prometheus → Grafana
                                                  ↓
                                    This Plugin (renders BPMN + overlays from query data)
```

No sidecar servers, no iframes, no CORS issues.

## Development

```bash
npm install --legacy-peer-deps
npm run dev   # Watch mode
npm run build # Production build
```

> **Note:** The lock file is not committed — `dist/` is the runnable artifact
> and committing a lock file would pin registry-specific URLs. `npm install`
> generates a lock locally against your own registry. Use `--legacy-peer-deps`
> to avoid peer-dependency resolution errors with the Grafana toolkit.

## License

Apache-2.0
