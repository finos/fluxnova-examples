module.exports = {
  testEnvironment: 'jsdom',
  transform: {
    '^.+\\.(t|j)sx?$': [
      '@swc/jest',
      {
        sourceMaps: true,
        jsc: {
          parser: {
            syntax: 'typescript',
            tsx: true,
            decorators: false,
            dynamicImport: true,
          },
        },
      },
    ],
  },
  transformIgnorePatterns: [
    'node_modules/(?!(bpmn-js|diagram-js|bpmn-moddle|moddle|min-dash|min-dom)/)',
  ],
  moduleNameMapper: {
    '\\.(css|scss|sass)$': 'identity-obj-proxy',
    '\\.(svg|png|jpg|gif)$': '<rootDir>/src/__mocks__/fileMock.js',
  },
  setupFiles: [],
  globals: {},
};
