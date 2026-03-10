import {defineConfig} from 'vitest/config';
import path from 'path';
import {fileURLToPath} from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
    test: {
        globals: true,
        testTimeout: 60_000,
        include: ['**/?(*.)+(spec|test).+(ts|tsx|js)'],
        exclude: ['**/node_modules/**', '**/dist/**'],
        reporters: [
            'default',
            ['junit', {
                outputFile: './build/test-results/vitest/junit.xml',
                classname: '{classname}',
                suiteName: '{filename}',
            }],
        ],
        pool: 'forks',
        maxWorkers: '50%',
    },
    resolve: {
        alias: [
            // Subpath imports must come before the root alias
            {find: /^@openrewrite\/rewrite\/(.+)$/, replacement: path.resolve(__dirname, 'src/$1/index')},
            {find: '@openrewrite/rewrite', replacement: path.resolve(__dirname, 'src/index')},
        ],
    },
});
