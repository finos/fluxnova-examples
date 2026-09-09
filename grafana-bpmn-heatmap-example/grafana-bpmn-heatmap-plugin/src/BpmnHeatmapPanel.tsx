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
import React, { useEffect, useRef, useState, useMemo } from 'react';
import { PanelProps, FieldType } from '@grafana/data';
import { BpmnHeatmapOptions, ActivityData, defaultThresholds, getThreshold } from './types';
import NavigatedViewer from 'bpmn-js/lib/NavigatedViewer';
import './styles.css';

interface Props extends PanelProps<BpmnHeatmapOptions> {}

export const BpmnHeatmapPanel: React.FC<Props> = ({ options, data, width, height }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const viewerRef = useRef<any>(null);
  const [bpmnXml, setBpmnXml] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // Extract activity data from Grafana data frames
  const activities: ActivityData[] = useMemo(() => {
    const result: ActivityData[] = [];
    if (!data.series || data.series.length === 0) {
      return result;
    }

    for (const frame of data.series) {
      const idField = frame.fields.find(
        (f) => f.name === options.metricField || f.labels?.activity_id !== undefined
      );
      const valueField = frame.fields.find(
        (f) => f.name === options.valueField || f.type === FieldType.number
      );
      const nameField = frame.fields.find((f) => f.name === options.nameField);

      if (idField && valueField) {
        for (let i = 0; i < frame.length; i++) {
          const activityId = idField.labels?.activity_id ?? String(idField.values[i] ?? '');
          const activityName = nameField
            ? String(nameField.values[i] ?? '')
            : idField.labels?.activity_name ?? activityId;
          const count = Number(valueField.values[i] ?? 0);
          if (activityId && count > 0) {
            result.push({ activityId, activityName, count });
          }
        }
      } else if (frame.fields.length > 0) {
        // Handle Prometheus instant query format: labels on each series
        const numField = frame.fields.find((f) => f.type === FieldType.number);
        if (numField && frame.fields[0].labels) {
          const labels = frame.fields[0].labels;
          const activityId = labels[options.metricField] || labels['activity_id'] || '';
          const activityName = labels[options.nameField] || labels['activity_name'] || activityId;
          const count = Number(numField.values[numField.values.length - 1] ?? 0);
          if (activityId && count > 0) {
            result.push({ activityId, activityName, count });
          }
        }
      }
    }

    return result;
  }, [data.series, options.metricField, options.valueField, options.nameField]);

  // Load BPMN XML
  useEffect(() => {
    let cancelled = false;

    async function loadBpmn() {
      setLoading(true);
      setError(null);

      if (options.bpmnUrl) {
        try {
          const response = await fetch(options.bpmnUrl);
          if (!response.ok) {
            throw new Error(`HTTP ${response.status} loading ${options.bpmnUrl}`);
          }
          const xml = await response.text();
          if (!cancelled) {
            setBpmnXml(xml);
          }
        } catch (err: any) {
          if (!cancelled) {
            setError(`Failed to load BPMN: ${err.message}`);
          }
        }
      } else if (options.bpmnXml) {
        setBpmnXml(options.bpmnXml);
      } else {
        setError('No BPMN source configured. Set a BPMN URL or paste XML in panel options.');
      }

      if (!cancelled) {
        setLoading(false);
      }
    }

    loadBpmn();
    return () => { cancelled = true; };
  }, [options.bpmnUrl, options.bpmnXml]);

  // Render BPMN diagram
  useEffect(() => {
    if (!bpmnXml || !containerRef.current) {
      return;
    }

    // Destroy previous viewer
    if (viewerRef.current) {
      viewerRef.current.destroy();
      viewerRef.current = null;
    }

    const viewer = new NavigatedViewer({
      container: containerRef.current,
    });

    viewerRef.current = viewer;

    viewer.importXML(bpmnXml).then(() => {
      const canvas = viewer.get('canvas') as any;
      canvas.zoom('fit-viewport');
    }).catch((err: any) => {
      setError(`Failed to render BPMN: ${err.message}`);
    });

    return () => {
      viewer.destroy();
      viewerRef.current = null;
    };
  }, [bpmnXml]);

  // Apply heatmap overlays when data changes
  useEffect(() => {
    const viewer = viewerRef.current;
    if (!viewer || !bpmnXml || activities.length === 0) {
      return;
    }

    // Small delay to ensure diagram is rendered
    const timer = setTimeout(() => {
      try {
        const overlays = viewer.get('overlays') as any;
        const elementRegistry = viewer.get('elementRegistry') as any;

        // Clear existing overlays
        overlays.remove({ type: 'heatmap-bg' });
        overlays.remove({ type: 'heatmap-badge' });

        activities.forEach(({ activityId, count }) => {
          const element = elementRegistry.get(activityId);
          if (!element) {
            return;
          }

          const threshold = getThreshold(count);
          const elWidth = element.width || 100;
          const elHeight = element.height || 80;

          // Background overlay
          const bgHtml = document.createElement('div');
          bgHtml.style.cssText = `width:${elWidth}px;height:${elHeight}px;border-radius:4px;background:${threshold.overlay};pointer-events:none;transition:background 0.5s ease;`;
          overlays.add(activityId, 'heatmap-bg', {
            position: { top: 0, left: 0 },
            html: bgHtml,
          });

          // Badge overlay
          if (options.showLabels) {
            const animation = threshold.label === 'critical' ? 'animation:pulse 1.5s infinite;' : '';
            const badge = document.createElement('div');
            badge.style.cssText = `background:${threshold.badge};color:#fff;font-size:11px;font-weight:700;border-radius:50%;width:22px;height:22px;display:flex;align-items:center;justify-content:center;box-shadow:0 2px 4px rgba(0,0,0,0.4);pointer-events:none;${animation}`;
            badge.textContent = String(count);
            overlays.add(activityId, 'heatmap-badge', {
              position: { top: -10, right: -10 },
              html: badge,
            });
          }
        });
      } catch (err) {
        console.warn('[bpmn-heatmap] Failed to apply overlays:', err);
      }
    }, 200);

    return () => clearTimeout(timer);
  }, [activities, bpmnXml, options.showLabels]);

  // Find biggest bottleneck
  const bottleneck = useMemo(() => {
    if (activities.length === 0) {
      return null;
    }
    return activities.reduce((max, a) => (a.count > max.count ? a : max), activities[0]);
  }, [activities]);

  // Render
  if (error) {
    return (
      <div className="bpmn-heatmap-panel bpmn-heatmap-error" style={{ width, height }}>
        <div className="bpmn-heatmap-error-msg">⚠️ {error}</div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="bpmn-heatmap-panel bpmn-heatmap-loading" style={{ width, height }}>
        <div>Loading BPMN diagram...</div>
      </div>
    );
  }

  return (
    <div className="bpmn-heatmap-panel" style={{ width, height }}>
      {options.showBottleneck && bottleneck && (
        <div className="bpmn-heatmap-header">
          <span className="bpmn-heatmap-header-label">Biggest Bottleneck:</span>
          <span className="bpmn-heatmap-header-value">
            {bottleneck.activityName} ({bottleneck.count} tokens)
          </span>
        </div>
      )}
      <div
        className="bpmn-heatmap-canvas"
        ref={containerRef}
        style={{ height: options.showBottleneck ? height - 36 : height }}
      />
      {options.showLegend && (
        <div className="bpmn-heatmap-legend">
          <div className="bpmn-heatmap-legend-title">Token Density</div>
          {[...defaultThresholds].reverse().map((t) => (
            <div key={t.label} className="bpmn-heatmap-legend-item">
              <span className="bpmn-heatmap-legend-color" style={{ background: t.badge }} />
              <span>{t.description}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
