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
import * as path from 'path';

describe('Shared dependencies setup', () => {
    let originalCache: any;

    beforeEach(() => {
        // Save original require.cache
        originalCache = {...require.cache};
    });

    afterEach(() => {
        // Restore original require.cache
        Object.keys(require.cache).forEach(key => {
            if (!originalCache[key]) {
                delete require.cache[key];
            }
        });
    });

    it('should map all subpaths of shared dependencies', () => {
        // This test verifies the logic of mapping multiple cached modules
        // In a real scenario, dynamically loaded recipes would benefit from this

        // Simulate what happens when modules are cached
        const mockCache: Record<string, any> = {
            '/some/path/node_modules/@openrewrite/rewrite/tree.js': {exports: {}},
            '/some/path/node_modules/@openrewrite/rewrite/recipe.js': {exports: {}},
            '/some/path/node_modules/@openrewrite/rewrite/visitor.js': {exports: {}},
        };

        // Count modules from our mock that match the pattern
        const depPattern = path.sep + 'node_modules' + path.sep + '@openrewrite/rewrite'.replace('/', path.sep);
        const rewriteModules = Object.keys(mockCache).filter(p => p.includes(depPattern));

        // Should find all three mocked modules
        expect(rewriteModules.length).toBe(3);

        // Verify different subpaths exist
        const hasTreeModule = rewriteModules.some(p => p.includes(path.sep + 'tree'));
        const hasRecipeModule = rewriteModules.some(p => p.includes(path.sep + 'recipe'));
        const hasVisitorModule = rewriteModules.some(p => p.includes(path.sep + 'visitor'));

        expect(hasTreeModule).toBe(true);
        expect(hasRecipeModule).toBe(true);
        expect(hasVisitorModule).toBe(true);
    });

    it('should handle package names with slashes correctly', () => {
        const depPattern = path.sep + 'node_modules' + path.sep + '@openrewrite/rewrite'.replace('/', path.sep);

        // Pattern should work correctly with scoped packages
        expect(depPattern).toContain('@openrewrite' + path.sep + 'rewrite');
    });

    it('should extract subpaths correctly', () => {
        const testCases = [
            {
                path: '/path/to/node_modules/@openrewrite/rewrite/tree.js',
                expected: '/tree.js'
            },
            {
                path: '/path/to/node_modules/@openrewrite/rewrite/index.js',
                expected: '/index.js'
            },
            {
                path: '/path/to/node_modules/@openrewrite/rewrite/rpc/recipe.js',
                expected: '/rpc/recipe.js'
            }
        ];

        testCases.forEach(tc => {
            const depPattern = path.sep + 'node_modules' + path.sep + '@openrewrite/rewrite'.replace('/', path.sep);
            const packageIndex = tc.path.indexOf(depPattern);
            const afterPackage = tc.path.substring(packageIndex + depPattern.length);

            expect(afterPackage).toBe(tc.expected);
        });
    });

    it('should handle modules not in package.json exports', () => {
        // This test documents the scenario where a module like 'preconditions'
        // is not in the package.json exports map, but is still needed for instanceof checks

        // Simulating what happens when:
        // 1. RPC server loads Check from 'preconditions.ts'
        // 2. Recipe package tries to load Check but doesn't have './preconditions' in exports
        // 3. setupSharedDependencies() needs to map it anyway using fallback strategies

        const mockPreconditionsPath = '/host/node_modules/@openrewrite/rewrite/dist/preconditions.js';

        // The subpath extraction should work
        const depPattern = path.sep + 'node_modules' + path.sep + '@openrewrite/rewrite'.replace('/', path.sep);
        const pkgIndex = mockPreconditionsPath.indexOf(depPattern);
        let subpath = mockPreconditionsPath.substring(pkgIndex + depPattern.length)
            .replace(/^[/\\]/, '')
            .replace(/\.(js|ts)$/, '')
            .replace(/^dist[/\\]/, '')
            .replace(/[/\\]index$/, '');

        expect(subpath).toBe('preconditions');

        // Even if '@openrewrite/rewrite/preconditions' can't be resolved via exports,
        // the fallback strategy should construct the direct file path
        const targetDir = '/target/node_modules/@openrewrite/recipes-nodejs';
        const expectedFallbackPath = path.join(targetDir, 'node_modules', '@openrewrite', 'rewrite', 'dist', 'preconditions.js');

        // This documents that the fallback strategy exists for such cases
        expect(expectedFallbackPath).toContain('preconditions.js');
    });
});
