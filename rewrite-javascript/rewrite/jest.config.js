module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  testPathIgnorePatterns: ['/node_modules/', '/dist/'],
  testTimeout: 30000,
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json', 'node'],
  moduleNameMapper: {
    '^@openrewrite/rewrite$': '<rootDir>/src/index',
    '^@openrewrite/rewrite/(.*)$': '<rootDir>/src/$1'
  },
  transform: {
    '^.+\\.tsx?$': ['ts-jest', {
      tsconfig: 'tsconfig.json'
    }],
  },
  testMatch: ['**/?(*.)+(spec|test).+(ts|tsx|js)'],
  collectCoverageFrom: ['src/**/*.{ts,tsx}', '!src/**/*.d.ts'],
  // Worker configuration to prevent SIGSEGV errors
  maxWorkers: '50%', // Use only 50% of available CPU cores
  workerIdleMemoryLimit: '512MB', // Restart workers that exceed 512MB
  reporters: [
    'default', // Keeps the default console reporter
    ['jest-junit', {
      outputDirectory: './build/test-results/jest',
      outputName: 'junit.xml',
      // Optional additional configuration
      classNameTemplate: '{classname}',
      titleTemplate: '{title}',
      ancestorSeparator: ' › ', // Used to generate the classname attribute
      suiteNameTemplate: '{filename}'
    }]
  ],
};
