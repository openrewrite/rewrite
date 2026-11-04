import {defineConfig} from 'vitest/config';
import path from 'path';
import {fileURLToPath} from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

const exclude = ['**/node_modules/**', '**/dist/**'];
// The tests that start the Java RPC server or read this module's Java sources. Gradle runs them as npmTestJava, which
// a change to the Java side reruns too, and the rest as npmTestTypeScript.
const javaTests = ['test/rpc/java-*.test.ts', 'test/javascript/completeness.test.ts'];

export default defineConfig({
    test: {
        globals: true,
        testTimeout: 60_000,
        setupFiles: ['./test/setup.ts'],
        exclude,
        // QUIET_TESTS (Gradle's test tasks) prints only failing tests, with their console output, and the summary.
        // VERBOSE_TESTS (Gradle: -PverboseTests) restores the full per-test reporter.
        reporters: [
            process.env.VERBOSE_TESTS ? 'default' : process.env.QUIET_TESTS ? 'minimal' : 'dot',
            ['junit', {
                classname: '{classname}',
                suiteName: '{filename}',
            }],
        ],
        // Gradle's test tasks each pass their own --outputFile.junit
        outputFile: {junit: './build/test-results/vitest/junit.xml'},
        maxWorkers: '50%',
        // A worker keeps its module registry across the files it runs, so a parsed lib file, a
        // compiled template or a TypeScript program one file built is still there for the next.
        isolate: false,
        // With that sharing, a spy one file leaves behind reaches every later file in its worker.
        restoreMocks: true,
        // `vitest run` runs both, and `--project` one of them
        projects: [
            {extends: true, test: {name: 'java', include: javaTests}},
            {
                extends: true,
                test: {name: 'typescript', include: ['**/?(*.)+(spec|test).+(ts|tsx|js)'], exclude: [...exclude, ...javaTests]},
            },
        ],
    },
    resolve: {
        alias: [
            // Subpath imports must come before the root alias
            {find: /^@openrewrite\/rewrite\/(.+)$/, replacement: path.resolve(__dirname, 'src/$1/index')},
            {find: '@openrewrite/rewrite', replacement: path.resolve(__dirname, 'src/index')},
        ],
    },
});
