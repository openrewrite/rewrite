/*
 * Copyright 2025 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import {
    TsConfig,
    TsConfigKind,
    createTsConfig,
    createModernEsmConfig,
    createNodeCommonJsConfig,
    createReactConfig,
    getTargetVersion,
    supportsVersion,
    isEsModule,
    hasJsx,
    usesNewJsxTransform,
    readTsConfigFile,
    detectTsConfigPath,
} from '../../src/javascript/tsconfig';
import {findMarker, replaceMarkerByKind, markers} from '../../src/markers';
import {randomId} from '../../src/uuid';
import {JavaScriptParser} from '../../src/javascript/parser';
import * as path from 'path';

describe('TsConfig marker', () => {
    describe('createTsConfig', () => {
        test('creates marker with default values', () => {
            const tsConfig = createTsConfig();

            expect(tsConfig.kind).toBe(TsConfigKind);
            expect(tsConfig.id).toBeDefined();
            expect(tsConfig.target).toBe('ES2020');
            expect(tsConfig.module).toBe('CommonJS');
            expect(tsConfig.moduleResolution).toBe('Node');
            expect(tsConfig.jsx).toBe('None');
            expect(tsConfig.strict).toBe(false);
            expect(tsConfig.esModuleInterop).toBe(true);
        });

        test('creates marker with custom values', () => {
            const tsConfig = createTsConfig({
                configPath: 'tsconfig.json',
                target: 'ES2022',
                module: 'ESNext',
                moduleResolution: 'Bundler',
                jsx: 'ReactJSX',
                strict: true,
                esModuleInterop: false,
                baseUrl: './src',
                paths: {'@/*': ['src/*']},
                lib: ['DOM', 'ES2022']
            });

            expect(tsConfig.configPath).toBe('tsconfig.json');
            expect(tsConfig.target).toBe('ES2022');
            expect(tsConfig.module).toBe('ESNext');
            expect(tsConfig.moduleResolution).toBe('Bundler');
            expect(tsConfig.jsx).toBe('ReactJSX');
            expect(tsConfig.strict).toBe(true);
            expect(tsConfig.esModuleInterop).toBe(false);
            expect(tsConfig.baseUrl).toBe('./src');
            expect(tsConfig.paths).toEqual({'@/*': ['src/*']});
            expect(tsConfig.lib).toEqual(['DOM', 'ES2022']);
        });

        test('allows custom id', () => {
            const customId = randomId();
            const tsConfig = createTsConfig({id: customId});

            expect(tsConfig.id).toBe(customId);
        });
    });

    describe('factory functions', () => {
        test('createModernEsmConfig creates ESM-focused config', () => {
            const tsConfig = createModernEsmConfig();

            expect(tsConfig.target).toBe('ES2022');
            expect(tsConfig.module).toBe('ESNext');
            expect(tsConfig.moduleResolution).toBe('Bundler');
            expect(tsConfig.strict).toBe(true);
            expect(tsConfig.esModuleInterop).toBe(true);
        });

        test('createModernEsmConfig allows overrides', () => {
            const tsConfig = createModernEsmConfig({target: 'ES2024'});

            expect(tsConfig.target).toBe('ES2024');
            expect(tsConfig.module).toBe('ESNext');
        });

        test('createNodeCommonJsConfig creates Node.js-focused config', () => {
            const tsConfig = createNodeCommonJsConfig();

            expect(tsConfig.target).toBe('ES2020');
            expect(tsConfig.module).toBe('CommonJS');
            expect(tsConfig.moduleResolution).toBe('Node');
            expect(tsConfig.strict).toBe(true);
        });

        test('createReactConfig creates React-focused config', () => {
            const tsConfig = createReactConfig();

            expect(tsConfig.target).toBe('ES2020');
            expect(tsConfig.module).toBe('ESNext');
            expect(tsConfig.moduleResolution).toBe('Bundler');
            expect(tsConfig.jsx).toBe('ReactJSX');
            expect(tsConfig.lib).toEqual(['DOM', 'DOM.Iterable', 'ES2020']);
        });
    });

    describe('getTargetVersion', () => {
        test('returns correct version for ES targets', () => {
            expect(getTargetVersion(createTsConfig({target: 'ES3'}))).toBe(1999);
            expect(getTargetVersion(createTsConfig({target: 'ES5'}))).toBe(2009);
            expect(getTargetVersion(createTsConfig({target: 'ES2015'}))).toBe(2015);
            expect(getTargetVersion(createTsConfig({target: 'ES2020'}))).toBe(2020);
            expect(getTargetVersion(createTsConfig({target: 'ES2022'}))).toBe(2022);
            expect(getTargetVersion(createTsConfig({target: 'ES2024'}))).toBe(2024);
        });

        test('returns 9999 for ESNext/Latest', () => {
            expect(getTargetVersion(createTsConfig({target: 'ESNext'}))).toBe(9999);
            expect(getTargetVersion(createTsConfig({target: 'Latest'}))).toBe(9999);
        });
    });

    describe('supportsVersion', () => {
        test('returns true when target meets or exceeds version', () => {
            const es2020Config = createTsConfig({target: 'ES2020'});

            expect(supportsVersion(es2020Config, 2015)).toBe(true);
            expect(supportsVersion(es2020Config, 2020)).toBe(true);
            expect(supportsVersion(es2020Config, 2022)).toBe(false);
        });

        test('ESNext supports all versions', () => {
            const esNextConfig = createTsConfig({target: 'ESNext'});

            expect(supportsVersion(esNextConfig, 2020)).toBe(true);
            expect(supportsVersion(esNextConfig, 2030)).toBe(true);
        });
    });

    describe('isEsModule', () => {
        test('returns true for ESM modules', () => {
            expect(isEsModule(createTsConfig({module: 'ES2015'}))).toBe(true);
            expect(isEsModule(createTsConfig({module: 'ES2020'}))).toBe(true);
            expect(isEsModule(createTsConfig({module: 'ES2022'}))).toBe(true);
            expect(isEsModule(createTsConfig({module: 'ESNext'}))).toBe(true);
            expect(isEsModule(createTsConfig({module: 'Node16'}))).toBe(true);
            expect(isEsModule(createTsConfig({module: 'Node18'}))).toBe(true);
            expect(isEsModule(createTsConfig({module: 'Node20'}))).toBe(true);
            expect(isEsModule(createTsConfig({module: 'NodeNext'}))).toBe(true);
            expect(isEsModule(createTsConfig({module: 'Preserve'}))).toBe(true);
        });

        test('returns false for non-ESM modules', () => {
            expect(isEsModule(createTsConfig({module: 'CommonJS'}))).toBe(false);
            expect(isEsModule(createTsConfig({module: 'AMD'}))).toBe(false);
            expect(isEsModule(createTsConfig({module: 'UMD'}))).toBe(false);
            expect(isEsModule(createTsConfig({module: 'None'}))).toBe(false);
        });
    });

    describe('hasJsx', () => {
        test('returns true when JSX is enabled', () => {
            expect(hasJsx(createTsConfig({jsx: 'React'}))).toBe(true);
            expect(hasJsx(createTsConfig({jsx: 'ReactJSX'}))).toBe(true);
            expect(hasJsx(createTsConfig({jsx: 'ReactJSXDev'}))).toBe(true);
            expect(hasJsx(createTsConfig({jsx: 'ReactNative'}))).toBe(true);
            expect(hasJsx(createTsConfig({jsx: 'Preserve'}))).toBe(true);
        });

        test('returns false when JSX is disabled', () => {
            expect(hasJsx(createTsConfig({jsx: 'None'}))).toBe(false);
            expect(hasJsx(createTsConfig())).toBe(false); // Default is 'None'
        });
    });

    describe('usesNewJsxTransform', () => {
        test('returns true for new JSX transform modes', () => {
            expect(usesNewJsxTransform(createTsConfig({jsx: 'ReactJSX'}))).toBe(true);
            expect(usesNewJsxTransform(createTsConfig({jsx: 'ReactJSXDev'}))).toBe(true);
        });

        test('returns false for classic JSX transform modes', () => {
            expect(usesNewJsxTransform(createTsConfig({jsx: 'React'}))).toBe(false);
            expect(usesNewJsxTransform(createTsConfig({jsx: 'ReactNative'}))).toBe(false);
            expect(usesNewJsxTransform(createTsConfig({jsx: 'Preserve'}))).toBe(false);
            expect(usesNewJsxTransform(createTsConfig({jsx: 'None'}))).toBe(false);
        });
    });

    describe('marker integration', () => {
        test('can be found in markers collection', () => {
            const tsConfig = createTsConfig({target: 'ES2022'});
            const markerCollection = markers(tsConfig);

            const found = findMarker<TsConfig>(
                {markers: markerCollection},
                TsConfigKind
            );

            expect(found).toBeDefined();
            expect(found?.target).toBe('ES2022');
        });

        test('can be replaced in markers collection', () => {
            const oldConfig = createTsConfig({target: 'ES2020'});
            const newConfig = createTsConfig({target: 'ES2022'});
            const markerCollection = markers(oldConfig);

            const updated = replaceMarkerByKind(markerCollection, newConfig);
            const found = findMarker<TsConfig>(
                {markers: updated},
                TsConfigKind
            );

            expect(found?.target).toBe('ES2022');
        });
    });

    describe('readTsConfigFile', () => {
        test('reads tsconfig.json from current project', () => {
            // This test uses the actual tsconfig.json in rewrite-javascript/rewrite
            const projectRoot = path.resolve(__dirname, '../..');
            const tsConfig = readTsConfigFile({
                searchPath: projectRoot,
                relativeTo: projectRoot
            });

            expect(tsConfig).toBeDefined();
            expect(tsConfig?.configPath).toBe('tsconfig.json');
            // The project uses ES2016 target (from tsconfig.json)
            expect(tsConfig?.target).toBe('ES2016');
            expect(tsConfig?.module).toBe('Node16');
            expect(tsConfig?.strict).toBe(true);
            expect(tsConfig?.esModuleInterop).toBe(true);
        });

        test('returns undefined when no tsconfig.json found', () => {
            // Use a path that definitely has no tsconfig.json
            const tsConfig = readTsConfigFile({
                searchPath: '/tmp'
            });

            expect(tsConfig).toBeUndefined();
        });
    });

    describe('parser integration', () => {
        test('parser attaches TsConfig marker when provided', async () => {
            // Create a TsConfig marker to pass to the parser
            const config = createTsConfig({
                target: 'ES2022',
                module: 'ESNext',
                strict: true
            });

            const parser = new JavaScriptParser({tsConfig: config});

            // Parse a simple TypeScript file
            const sourceFiles: any[] = [];
            for await (const sf of parser.parse({
                text: 'const x: number = 42;',
                sourcePath: 'test.ts'
            })) {
                sourceFiles.push(sf);
            }

            expect(sourceFiles.length).toBe(1);
            const sourceFile = sourceFiles[0];

            // Find the TsConfig marker
            const tsConfig = findMarker<TsConfig>(sourceFile, TsConfigKind);

            expect(tsConfig).toBeDefined();
            expect(tsConfig?.target).toBe('ES2022');
            expect(tsConfig?.module).toBe('ESNext');
            expect(tsConfig?.strict).toBe(true);
        });

        test('parser does not attach TsConfig marker when not provided', async () => {
            const parser = new JavaScriptParser();

            const sourceFiles: any[] = [];
            for await (const sf of parser.parse({
                text: 'const x: number = 42;',
                sourcePath: 'test.ts'
            })) {
                sourceFiles.push(sf);
            }

            expect(sourceFiles.length).toBe(1);
            const sourceFile = sourceFiles[0];

            // No TsConfig marker should be present
            const tsConfig = findMarker<TsConfig>(sourceFile, TsConfigKind);
            expect(tsConfig).toBeUndefined();
        });

        test('same TsConfig instance is attached to all source files', async () => {
            const config = createTsConfig({target: 'ES2020'});
            const parser = new JavaScriptParser({tsConfig: config});

            const sourceFiles: any[] = [];
            for await (const sf of parser.parse(
                {text: 'const a = 1;', sourcePath: 'a.ts'},
                {text: 'const b = 2;', sourcePath: 'b.ts'}
            )) {
                sourceFiles.push(sf);
            }

            expect(sourceFiles.length).toBe(2);

            const config1 = findMarker<TsConfig>(sourceFiles[0], TsConfigKind);
            const config2 = findMarker<TsConfig>(sourceFiles[1], TsConfigKind);

            // Same instance should be reused
            expect(config1).toBe(config2);
            expect(config1?.id).toBe(config.id);
        });
    });

    describe('detectTsConfigPath', () => {
        test('detects tsc with no -p flag (defaults to tsconfig.json)', () => {
            const pkg = {scripts: {build: 'tsc'}};
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.json');
        });

        test('detects tsc with -p flag', () => {
            const pkg = {scripts: {build: 'tsc -p tsconfig.build.json'}};
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.build.json');
        });

        test('detects tsc with -p= flag', () => {
            const pkg = {scripts: {build: 'tsc -p=tsconfig.prod.json'}};
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.prod.json');
        });

        test('detects tsc with --project flag', () => {
            const pkg = {scripts: {build: 'tsc --project tsconfig.lib.json'}};
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.lib.json');
        });

        test('detects tsc with --project= flag', () => {
            const pkg = {scripts: {build: 'tsc --project=./config/tsconfig.json'}};
            expect(detectTsConfigPath(pkg)).toBe('./config/tsconfig.json');
        });

        test('detects vue-tsc', () => {
            const pkg = {scripts: {typecheck: 'vue-tsc -p tsconfig.app.json'}};
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.app.json');
        });

        test('prioritizes build script', () => {
            const pkg = {
                scripts: {
                    test: 'tsc -p tsconfig.test.json',
                    build: 'tsc -p tsconfig.build.json'
                }
            };
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.build.json');
        });

        test('handles complex scripts with && chains', () => {
            const pkg = {scripts: {build: 'rimraf dist && tsc -p tsconfig.build.json'}};
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.build.json');
        });

        test('returns undefined for non-typescript projects', () => {
            const pkg = {scripts: {build: 'babel src -d dist'}};
            expect(detectTsConfigPath(pkg)).toBeUndefined();
        });

        test('returns undefined when no scripts', () => {
            const pkg = {};
            expect(detectTsConfigPath(pkg)).toBeUndefined();
        });

        test('does not match tsc as part of another word', () => {
            const pkg = {scripts: {build: 'typescript-plugin-transform'}};
            expect(detectTsConfigPath(pkg)).toBeUndefined();
        });

        test('detects tsc --build flag', () => {
            const pkg = {scripts: {build: 'tsc --build tsconfig.build.json'}};
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.build.json');
        });

        test('detects tsc -b flag', () => {
            const pkg = {scripts: {build: 'tsc -b tsconfig.build.json'}};
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.build.json');
        });

        test('detects --build with path prefix', () => {
            const pkg = {scripts: {build: 'tsc --build ./config/tsconfig.json'}};
            expect(detectTsConfigPath(pkg)).toBe('./config/tsconfig.json');
        });

        test('detects --build in complex script', () => {
            const pkg = {scripts: {build: 'rm -rf ./dist tsconfig.build.tsbuildinfo && tsc --build tsconfig.build.json'}};
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.build.json');
        });

        test('handles quoted paths with spaces', () => {
            const pkg = {scripts: {build: 'tsc -p "path with spaces/tsconfig.json"'}};
            expect(detectTsConfigPath(pkg)).toBe('path with spaces/tsconfig.json');
        });

        test('handles single-quoted paths', () => {
            const pkg = {scripts: {build: "tsc -p 'config/tsconfig.json'"}};
            expect(detectTsConfigPath(pkg)).toBe('config/tsconfig.json');
        });

        test('prefers explicit config from second tsc command', () => {
            // First tsc has no explicit config, second tsc has explicit config
            const pkg = {scripts: {build: 'tsc && tsc -p tsconfig.build.json'}};
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.build.json');
        });

        test('handles multiple tsc commands with different configs', () => {
            // Should return the first explicit config found
            const pkg = {scripts: {build: 'tsc -p tsconfig.types.json && tsc -p tsconfig.build.json'}};
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.types.json');
        });

        test('handles tsc after pipe', () => {
            const pkg = {scripts: {build: 'echo "Building..." | tsc -p tsconfig.build.json'}};
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.build.json');
        });

        test('handles tsc after semicolon', () => {
            const pkg = {scripts: {build: 'echo start; tsc -p tsconfig.build.json'}};
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.build.json');
        });

        test('handles tsc with OR operator', () => {
            const pkg = {scripts: {build: 'tsc -p tsconfig.main.json || tsc -p tsconfig.fallback.json'}};
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.main.json');
        });

        test('handles npx tsc', () => {
            const pkg = {scripts: {build: 'npx tsc -p tsconfig.build.json'}};
            // npx runs tsc, so tsc should be detected after npx
            expect(detectTsConfigPath(pkg)).toBe('tsconfig.build.json');
        });
    });
});
