/**
 * Grafana panel plugin webpack configuration
 * Based on @grafana/create-plugin conventions
 */
import path from 'path';
import { Configuration, DefinePlugin } from 'webpack';
import CopyWebpackPlugin from 'copy-webpack-plugin';
import ReplaceInFileWebpackPlugin from 'replace-in-file-webpack-plugin';
import ForkTsCheckerWebpackPlugin from 'fork-ts-checker-webpack-plugin';

const SOURCE_DIR = path.resolve(process.cwd(), 'src');
const DIST_DIR = path.resolve(process.cwd(), 'dist');

function getPackageJson() {
  return require(path.resolve(process.cwd(), 'package.json'));
}

function getPluginJson() {
  return require(path.resolve(SOURCE_DIR, 'plugin.json'));
}

function getEntries(): Record<string, string> {
  return { module: path.resolve(SOURCE_DIR, 'module.ts') };
}

const config = async (env: Record<string, unknown>): Promise<Configuration> => {
  const pluginJson = getPluginJson();
  const packageJson = getPackageJson();
  const production = env.production === true;

  return {
    mode: production ? 'production' : 'development',
    target: 'web',
    context: path.resolve(process.cwd()),
    devtool: production ? 'source-map' : 'eval-source-map',
    entry: getEntries(),

    output: {
      clean: true,
      filename: '[name].js',
      path: DIST_DIR,
      publicPath: '/',
      libraryTarget: 'amd',
      uniqueName: pluginJson.id,
    },

    externals: [
      'lodash',
      'jquery',
      'moment',
      'slate',
      'emotion',
      '@emotion/react',
      '@emotion/css',
      'prismjs',
      'slate-plain-serializer',
      '@grafana/slate-react',
      'react',
      'react-dom',
      'react-redux',
      'redux',
      'rxjs',
      'd3',
      'angular',
      '@grafana/data',
      '@grafana/ui',
      '@grafana/runtime',
      '@grafana/e2e-selectors',

      function (context: any, request: any, callback: any) {
        const prefix = '@grafana/';
        if (request.indexOf(prefix) === 0) {
          return callback(null, request);
        }
        callback();
      },
    ],

    resolve: {
      extensions: ['.ts', '.tsx', '.js', '.jsx', '.json'],
    },

    module: {
      rules: [
        {
          test: /\.[tj]sx?$/,
          exclude: /node_modules/,
          use: {
            loader: 'swc-loader',
            options: {
              jsc: {
                parser: {
                  syntax: 'typescript',
                  tsx: true,
                  decorators: false,
                  dynamicImport: true,
                },
                target: 'es2021',
                loose: false,
              },
            },
          },
        },
        {
          test: /\.css$/,
          use: ['style-loader', 'css-loader'],
        },
        {
          test: /\.s[ac]ss$/,
          use: ['style-loader', 'css-loader', 'sass-loader'],
        },
        {
          test: /\.(png|jpe?g|gif|svg)$/,
          type: 'asset/resource',
          generator: {
            publicPath: `public/plugins/${pluginJson.id}/`,
            outputPath: 'img/',
            filename: 'img/[hash][ext]',
          },
        },
        {
          test: /\.(woff|woff2|eot|ttf|otf)$/,
          type: 'asset/resource',
          generator: {
            publicPath: `public/plugins/${pluginJson.id}/`,
            outputPath: 'fonts/',
            filename: 'fonts/[hash][ext]',
          },
        },
      ],
    },

    plugins: [
      new CopyWebpackPlugin({
        patterns: [
          { from: 'README.md', to: '.', noErrorOnMissing: true },
          { from: 'CHANGELOG.md', to: '.', noErrorOnMissing: true },
          { from: 'LICENSE', to: '.', noErrorOnMissing: true },
          { from: path.resolve(SOURCE_DIR, 'plugin.json'), to: '.' },
          { from: path.resolve(SOURCE_DIR, 'img'), to: 'img', noErrorOnMissing: true },
        ],
      }),
      new ForkTsCheckerWebpackPlugin({
        async: Boolean(!production),
        typescript: {
          configFile: path.resolve(process.cwd(), 'tsconfig.json'),
        },
      }),
      new ReplaceInFileWebpackPlugin([
        {
          dir: DIST_DIR,
          files: ['plugin.json'],
          rules: [
            {
              search: '%VERSION%',
              replace: packageJson.version,
            },
            {
              search: '%TODAY%',
              replace: new Date().toISOString().substring(0, 10),
            },
          ],
        },
      ]),
    ],
  };
};

export default config;
