/*
 * Copyright Fluxnova contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import React, { useEffect, useRef, useState, useCallback } from 'react';
import { PanelProps, DataFrame } from '@grafana/data';
import { useTheme2 } from '@grafana/ui';
import { BpmnHeatmapOptions, ThresholdEntry } from '../types';
import NavigatedViewer from 'bpmn-js/lib/NavigatedViewer';
import '../styles/heatmap.css';

interface ActivityData {
  activityId: string;
  value: number;
}

interface BottleneckInfo {
  activityId: string;
  value: number;
}

/**
 * Find the threshold entry for a given token count.
 * Thresholds are evaluated from highest min to lowest.
 */
function getThreshold(count: number, thresholds: ThresholdEntry[]): ThresholdEntry {
  for (const t of thresholds) {
    if (count >= t.min) {
      return t;
    }
  }
  return thresholds[thresholds.length - 1];
}

/**
 * Extract activity data from Grafana data frames.
 */
function extractActivityData(
  data: DataFrame[],
  metricField: string,
  valueField: string
): ActivityData[] {
  const activities: ActivityData[] = [];

  for (const frame of data) {
    const activityFieldObj = frame.fields.find(
      (f) => f.name === metricField || f.labels?.[metricField] !== undefined
    );
    const valueFieldObj = frame.fields.find((f) => f.name === valueField);

    if (activityFieldObj && valueFieldObj) {
      // Both fields exist as columns (table format)
      for (let i = 0; i < frame.length; i++) {
        const activityId = String(activityFieldObj.values[i] ?? activityFieldObj.values.get?.(i) ?? '');
        const rawValue = valueFieldObj.values[i] ?? valueFieldObj.values.get?.(i) ?? 0;
        const value = typeof rawValue === 'number' ? rawValue : parseFloat(String(rawValue));
        if (activityId && !isNaN(value) && value > 0) {
          activities.push({ activityId, value: Math.round(value) });
        }
      }
    } else {
      // Try label-based extraction (Prometheus instant query format)
      // Each frame may have labels containing the activity_id
      for (const field of frame.fields) {
        if (field.type === 'number' && field.labels && field.labels[metricField]) {
          const activityId = field.labels[metricField];
          const lastIdx = field.values.length ? field.values.length - 1 : 0;
          const rawValue = field.values[lastIdx] ?? field.values.get?.(lastIdx) ?? 0;
          const value = typeof rawValue === 'number' ? rawValue : parseFloat(String(rawValue));
          if (activityId && !isNaN(value) && value > 0) {
            activities.push({ activityId, value: Math.round(value) });
          }
        }
      }
    }
  }

  return activities;
}

export const BpmnHeatmapPanel: React.FC<PanelProps<BpmnHeatmapOptions>> = ({
  options,
  data,
  width,
  height,
}) => {
  const theme = useTheme2();
  const containerRef = useRef<HTMLDivElement>(null);
  const viewerRef = useRef<any>(null);
  const overlayIdsRef = useRef<string[]>([]);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [bottleneck, setBottleneck] = useState<BottleneckInfo | null>(null);

  const { bpmnUrl, bpmnXml, metricField, valueField, thresholds, showLabels } = options;

  // -------------------------------------------------------------------------
  // Load and render BPMN diagram
  // -------------------------------------------------------------------------
  const initViewer = useCallback(async () => {
    if (!containerRef.current) {
      return;
    }

    setLoading(true);
    setError(null);

    // Determine BPMN XML source
    let xml = '';
    if (bpmnUrl) {
      try {
        const response = await fetch(bpmnUrl);
        if (!response.ok) {
          throw new Error(`HTTP ${response.status} loading ${bpmnUrl}`);
        }
        xml = await response.text();
      } catch (err: any) {
        setError(`Failed to load BPMN: ${err.message}`);
        setLoading(false);
        return;
      }
    } else if (bpmnXml) {
      xml = bpmnXml;
    } else {
      setError('No BPMN source configured. Set either a BPMN URL or paste inline XML in panel options.');
      setLoading(false);
      return;
    }

    // Destroy previous viewer if any
    if (viewerRef.current) {
      viewerRef.current.destroy();
      viewerRef.current = null;
    }

    // Create new viewer
    const viewer = new NavigatedViewer({
      container: containerRef.current,
    });

    try {
      await viewer.importXML(xml);
      const canvas = viewer.get('canvas') as any;
      canvas.zoom('fit-viewport');
      viewerRef.current = viewer;
      setLoading(false);
    } catch (err: any) {
      setError(`Failed to render BPMN: ${err.message}`);
      setLoading(false);
    }
  }, [bpmnUrl, bpmnXml]);

  // Initialize viewer on mount or when BPMN source changes
  useEffect(() => {
    initViewer();
    return () => {
      if (viewerRef.current) {
        viewerRef.current.destroy();
        viewerRef.current = null;
      }
    };
  }, [initViewer]);

  // Re-fit viewport when panel resizes
  useEffect(() => {
    if (viewerRef.current) {
      try {
        const canvas = viewerRef.current.get('canvas') as any;
        canvas.zoom('fit-viewport');
      } catch {
        // viewer may not be ready
      }
    }
  }, [width, height]);

  // -------------------------------------------------------------------------
  // Apply heatmap overlays
  // -------------------------------------------------------------------------
  useEffect(() => {
    const viewer = viewerRef.current;
    if (!viewer || loading || error) {
      return;
    }

    // Clear existing overlays
    const overlaysModule = viewer.get('overlays') as any;
    for (const id of overlayIdsRef.current) {
      try {
        overlaysModule.remove(id);
      } catch {
        // already removed
      }
    }
    overlayIdsRef.current = [];

    // Extract data
    const activities = extractActivityData(data.series, metricField, valueField);

    if (activities.length === 0) {
      setBottleneck(null);
      return;
    }

    const elementRegistry = viewer.get('elementRegistry') as any;
    let maxActivity: BottleneckInfo | null = null;

    for (const activity of activities) {
      const element = elementRegistry.get(activity.activityId);
      if (!element) {
        continue;
      }

      const threshold = getThreshold(activity.value, thresholds);

      // Track bottleneck
      if (!maxActivity || activity.value > maxActivity.value) {
        maxActivity = { activityId: activity.activityId, value: activity.value };
      }

      // Background overlay
      const elWidth = element.width || 100;
      const elHeight = element.height || 80;
      const bgHtml = document.createElement('div');
      bgHtml.style.cssText = `width:${elWidth}px;height:${elHeight}px;border-radius:4px;background:${threshold.overlayColor};pointer-events:none;transition:background 0.5s ease;`;

      const bgId = overlaysModule.add(activity.activityId, 'heatmap-bg', {
        position: { top: 0, left: 0 },
        html: bgHtml,
      });
      overlayIdsRef.current.push(bgId);

      // Badge overlay
      if (showLabels) {
        const animation = threshold.label === 'critical' ? 'animation:pulse 1.5s infinite;' : '';
        const badgeHtml = document.createElement('div');
        badgeHtml.className = 'bpmn-heatmap-badge';
        badgeHtml.style.cssText = `background:${threshold.color};color:#fff;font-size:11px;font-weight:700;border-radius:50%;width:22px;height:22px;display:flex;align-items:center;justify-content:center;box-shadow:0 2px 4px rgba(0,0,0,0.4);pointer-events:none;${animation}`;
        badgeHtml.textContent = String(activity.value);

        const badgeId = overlaysModule.add(activity.activityId, 'heatmap-badge', {
          position: { top: -10, right: -10 },
          html: badgeHtml,
        });
        overlayIdsRef.current.push(badgeId);
      }
    }

    setBottleneck(maxActivity);
  }, [data, loading, error, metricField, valueField, thresholds, showLabels]);

  // -------------------------------------------------------------------------
  // Render
  // -------------------------------------------------------------------------
  const headerHeight = 36;
  const canvasHeight = height - headerHeight;

  return (
    <div className="bpmn-heatmap-panel" style={{ width, height, overflow: 'hidden' }}>
      {/* Summary header */}
      <div
        className="bpmn-heatmap-header"
        style={{
          height: headerHeight,
          background: theme.isDark ? theme.colors.background.secondary : '#1e293b',
          color: theme.isDark ? theme.colors.text.primary : '#e2e8f0',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          padding: '0 12px',
          fontSize: '12px',
          borderBottom: `1px solid ${theme.colors.border.weak}`,
        }}
      >
        <span style={{ fontWeight: 600 }}>BPMN Process Heatmap</span>
        {bottleneck && (
          <span>
            <span style={{ opacity: 0.7 }}>Biggest Bottleneck:</span>{' '}
            <strong>{bottleneck.activityId}</strong> ({bottleneck.value} tokens)
          </span>
        )}
        {!bottleneck && !loading && !error && (
          <span style={{ opacity: 0.7 }}>No active tokens</span>
        )}
      </div>

      {/* BPMN Canvas */}
      <div style={{ position: 'relative', width, height: canvasHeight }}>
        {loading && (
          <div className="bpmn-heatmap-status">Loading BPMN diagram...</div>
        )}
        {error && (
          <div className="bpmn-heatmap-error">{error}</div>
        )}
        <div
          ref={containerRef}
          style={{ width: '100%', height: '100%' }}
        />

        {/* Legend */}
        {!loading && !error && (
          <div className="bpmn-heatmap-legend">
            <div style={{ fontWeight: 600, marginBottom: 4, fontSize: '11px' }}>Token Legend</div>
            {[...thresholds].reverse().map((t) => (
              <div key={t.label} className="bpmn-heatmap-legend-item">
                <div
                  className="bpmn-heatmap-legend-color"
                  style={{ background: t.color }}
                />
                <span>{t.description}</span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
