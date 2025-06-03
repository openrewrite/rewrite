module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  testPathIgnorePatterns: ['/node_modules/', '/dist/'],
  testTimeout: 30000,
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json', 'node'],
  moduleNameMapper: {
    '^@openrewrite/rewrite/(.*)$': '<rootDir>/dist/src/$1'
  },
  transform: {
    '^.+\\.tsx?$': ['ts-jest', {
      tsconfig: 'tsconfig.test.json', // Adjust if your tsconfig file is named or located differently
    }],
  },
  testMatch: ['**/?(*.)+(spec|test).+(ts|tsx|js)'],
  collectCoverageFrom: ['src/**/*.{ts,tsx}', '!src/**/*.d.ts'],
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
