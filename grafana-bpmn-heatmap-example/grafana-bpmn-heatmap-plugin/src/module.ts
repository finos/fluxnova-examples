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
import { PanelPlugin } from '@grafana/data';
import { BpmnHeatmapPanel } from './BpmnHeatmapPanel';
import { BpmnHeatmapOptions, defaultThresholds } from './types';

export const plugin = new PanelPlugin<BpmnHeatmapOptions>(BpmnHeatmapPanel).setPanelOptions(
  (builder) => {
    builder
      .addTextInput({
        path: 'bpmnUrl',
        name: 'BPMN File URL',
        description: 'URL to fetch the .bpmn XML file from (relative or absolute)',
        defaultValue: '',
        category: ['BPMN Source'],
      })
      .addTextInput({
        path: 'bpmnXml',
        name: 'BPMN XML (inline)',
        description: 'Paste BPMN XML directly. If both URL and inline are set, URL takes priority.',
        defaultValue: '',
        category: ['BPMN Source'],
        settings: {
          useTextarea: true,
          rows: 6,
        },
      })
      .addTextInput({
        path: 'metricField',
        name: 'Activity ID Field',
        description: 'Field name in query results that maps to BPMN element IDs',
        defaultValue: 'activity_id',
        category: ['Data Mapping'],
      })
      .addTextInput({
        path: 'valueField',
        name: 'Value Field',
        description: 'Field name for the token count value',
        defaultValue: 'Value',
        category: ['Data Mapping'],
      })
      .addTextInput({
        path: 'nameField',
        name: 'Activity Name Field',
        description: 'Field name for the activity display name (optional, used for bottleneck label)',
        defaultValue: 'activity_name',
        category: ['Data Mapping'],
      })
      .addBooleanSwitch({
        path: 'showLabels',
        name: 'Show Count Badges',
        description: 'Show token count badges on diagram elements',
        defaultValue: true,
        category: ['Display'],
      })
      .addBooleanSwitch({
        path: 'showLegend',
        name: 'Show Legend',
        description: 'Show colour threshold legend',
        defaultValue: true,
        category: ['Display'],
      })
      .addBooleanSwitch({
        path: 'showBottleneck',
        name: 'Show Bottleneck Header',
        description: 'Show the biggest bottleneck summary at the top',
        defaultValue: true,
        category: ['Display'],
      });
  }
);
