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
});
