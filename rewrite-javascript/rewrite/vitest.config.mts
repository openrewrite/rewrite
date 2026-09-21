import {defineConfig} from 'vitest/config';
import path from 'path';
import {fileURLToPath} from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
    test: {
        globals: true,
        testTimeout: 60_000,
        include: ['**/?(*.)+(spec|test).+(ts|tsx|js)'],
        setupFiles: ['./test/setup.ts'],
        exclude: ['**/node_modules/**', '**/dist/**'],
        // VERBOSE_TESTS (Gradle: -PverboseTests) restores the full per-test reporter
        reporters: [
            process.env.VERBOSE_TESTS ? 'default' : 'dot',
            ['junit', {
                outputFile: './build/test-results/vitest/junit.xml',
                classname: '{classname}',
                suiteName: '{filename}',
            }],
        ],
        maxWorkers: '50%',
        // A worker keeps its module registry across the files it runs, so a parsed lib file, a
        // compiled template or a TypeScript program one file built is still there for the next.
        isolate: false,
        // With that sharing, a spy one file leaves behind reaches every later file in its worker.
        restoreMocks: true,
    },
    resolve: {
        alias: [
            // Subpath imports must come before the root alias
            {find: /^@openrewrite\/rewrite\/(.+)$/, replacement: path.resolve(__dirname, 'src/$1/index')},
            {find: '@openrewrite/rewrite', replacement: path.resolve(__dirname, 'src/index')},
        ],
    },
});
