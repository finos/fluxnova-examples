/**
 * Grafana panel plugin webpack configuration.
 * Extends the base configuration with project-specific overrides.
 */
import type { Configuration } from 'webpack';
import baseConfig from '../.grafana/webpack.config';

const config = async (env: Record<string, unknown>): Promise<Configuration> => {
  const base = await baseConfig(env);
  return base;
};

export default config;
