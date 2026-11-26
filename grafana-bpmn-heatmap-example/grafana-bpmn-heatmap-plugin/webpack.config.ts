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
import path from 'path';
import CopyWebpackPlugin from 'copy-webpack-plugin';
import type { Configuration } from 'webpack';

const config = (env: any): Configuration => ({
  mode: env.production ? 'production' : 'development',
  entry: './src/module.ts',
  devtool: env.production ? 'source-map' : 'eval-source-map',
  externals: [
    'react',
    'react-dom',
    '@grafana/data',
    '@grafana/ui',
    '@grafana/runtime',
  ],
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: 'module.js',
    library: {
      type: 'amd',
    },
  },
  resolve: {
    extensions: ['.tsx', '.ts', '.js'],
  },
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: 'ts-loader',
        exclude: /node_modules/,
      },
      {
        test: /\.css$/,
        use: ['style-loader', 'css-loader'],
      },
    ],
  },
  plugins: [
    new CopyWebpackPlugin({
      patterns: [
        { from: 'src/plugin.json', to: '.' },
        { from: 'src/img', to: 'img', noErrorOnMissing: true },
        { from: '../processes/*.bpmn', to: 'bpmn/[name][ext]' },
      ],
    }),
  ],
});

export default config;
