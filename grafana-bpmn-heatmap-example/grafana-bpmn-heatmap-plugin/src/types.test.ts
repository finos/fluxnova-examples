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
import { getThreshold, defaultThresholds } from './types';

describe('getThreshold', () => {
  it('maps a zero count to the lowest band (flowing)', () => {
    expect(getThreshold(0).label).toBe('low');
  });

  it('maps 1-2 tokens to the low band', () => {
    expect(getThreshold(1).label).toBe('low');
    expect(getThreshold(2).label).toBe('low');
  });

  it('maps 3-9 tokens to the medium band', () => {
    expect(getThreshold(3).label).toBe('medium');
    expect(getThreshold(9).label).toBe('medium');
  });

  it('maps 10-19 tokens to the high band', () => {
    expect(getThreshold(10).label).toBe('high');
    expect(getThreshold(19).label).toBe('high');
  });

  it('maps 20+ tokens to the critical band', () => {
    expect(getThreshold(20).label).toBe('critical');
    expect(getThreshold(1000).label).toBe('critical');
  });

  it('returns a colour and overlay for every band', () => {
    for (const count of [0, 1, 5, 15, 50]) {
      const t = getThreshold(count);
      expect(t.color).toMatch(/^#|rgba/);
      expect(t.overlayColor).toMatch(/^#|rgba/);
    }
  });

  it('honours a custom threshold table', () => {
    const custom = [
      { min: 100, label: 'huge', color: '#000', overlayColor: 'rgba(0,0,0,0.1)', description: '100+' },
      { min: 1, label: 'tiny', color: '#fff', overlayColor: 'rgba(255,255,255,0.1)', description: '1+' },
    ];
    expect(getThreshold(150, custom).label).toBe('huge');
    expect(getThreshold(5, custom).label).toBe('tiny');
    expect(getThreshold(0, custom).label).toBe('tiny');
  });

  it('defaultThresholds is ordered from highest min to lowest', () => {
    for (let i = 1; i < defaultThresholds.length; i++) {
      expect(defaultThresholds[i - 1].min).toBeGreaterThan(defaultThresholds[i].min);
    }
  });
});
