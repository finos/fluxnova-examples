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
export interface ThresholdEntry {
  min: number;
  max?: number;
  color: string;
  overlayColor: string;
  badge?: string;
  overlay?: string;
  label: string;
  description: string;
}

export type ThresholdLevel = ThresholdEntry;

export interface BpmnHeatmapOptions {
  bpmnUrl: string;
  bpmnXml: string;
  metricField: string;
  valueField: string;
  nameField: string;
  showLabels: boolean;
  showLegend: boolean;
  showBottleneck: boolean;
  thresholds: ThresholdEntry[];
}

export const defaultThresholds: ThresholdEntry[] = [
  { min: 20, label: 'critical', color: '#ad1457', overlayColor: 'rgba(173, 20, 87, 0.30)', badge: '#ad1457', overlay: 'rgba(173, 20, 87, 0.30)', description: '20+ tokens (critical)' },
  { min: 10, label: 'high', color: '#c62828', overlayColor: 'rgba(198, 40, 40, 0.25)', badge: '#c62828', overlay: 'rgba(198, 40, 40, 0.25)', description: '10-19 tokens (bottleneck)' },
  { min: 3, label: 'medium', color: '#e65100', overlayColor: 'rgba(230, 81, 0, 0.20)', badge: '#e65100', overlay: 'rgba(230, 81, 0, 0.20)', description: '3-9 tokens (building up)' },
  { min: 1, label: 'low', color: '#2e7d32', overlayColor: 'rgba(46, 125, 50, 0.15)', badge: '#2e7d32', overlay: 'rgba(46, 125, 50, 0.15)', description: '1-2 tokens (flowing)' },
];

export interface ActivityData {
  activityId: string;
  activityName: string;
  count: number;
}

/**
 * Select the colour threshold band for a given token count.
 *
 * Thresholds are evaluated from highest `min` to lowest, so the first band
 * whose `min` the count satisfies wins. Counts below the lowest band's `min`
 * (e.g. 0) fall back to the lowest band.
 */
export function getThreshold(
  count: number,
  thresholds: ThresholdEntry[] = defaultThresholds
): ThresholdEntry {
  for (const t of thresholds) {
    if (count >= t.min) {
      return t;
    }
  }
  return thresholds[thresholds.length - 1];
}
