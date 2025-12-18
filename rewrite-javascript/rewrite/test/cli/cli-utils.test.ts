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

import {beforeEach, describe, expect, test} from '@jest/globals';
import * as fs from 'fs';
import * as path from 'path';
import * as os from 'os';
import {discoverFiles, findRecipe, isAcceptedFile, parseRecipeOptions, parseRecipeSpec} from '../../src/cli/cli-utils';
import {Recipe, RecipeRegistry} from '../../src';

// Test recipe classes for findRecipe tests
class TestRecipe extends Recipe {
    readonly name = 'org.openrewrite.test.my-recipe';
    readonly displayName = 'Test Recipe';
    readonly description = 'A test recipe';

    constructor(public readonly options?: { text?: string }) {
        super(options);
    }
}

class AnotherTestRecipe extends Recipe {
    readonly name = 'org.openrewrite.test.another-recipe';
    readonly displayName = 'Another Test Recipe';
    readonly description = 'Another test recipe';
}

class SimilarRecipe extends Recipe {
    readonly name = 'org.openrewrite.example.my-recipe';
    readonly displayName = 'Similar Recipe';
    readonly description = 'A similarly named recipe';
}

describe('parseRecipeSpec', () => {
    test('parses simple package:recipe format', () => {
        const result = parseRecipeSpec('some-package:my-recipe');
        expect(result).toEqual({
            packageName: 'some-package',
            recipeName: 'my-recipe',
            isLocalPath: false
        });
    });

    test('parses scoped package:recipe format', () => {
        const result = parseRecipeSpec('@openrewrite/recipes-nodejs:replace-deprecated-slice');
        expect(result).toEqual({
            packageName: '@openrewrite/recipes-nodejs',
            recipeName: 'replace-deprecated-slice',
            isLocalPath: false
        });
    });

    test('parses package with FQN recipe name', () => {
        const result = parseRecipeSpec('@scope/package:org.openrewrite.recipe.name');
        expect(result).toEqual({
            packageName: '@scope/package',
            recipeName: 'org.openrewrite.recipe.name',
            isLocalPath: false
        });
    });

    test('returns null for missing colon', () => {
        const result = parseRecipeSpec('package-without-recipe');
        expect(result).toBeNull();
    });

    test('returns null for empty package name', () => {
        const result = parseRecipeSpec(':recipe-name');
        expect(result).toBeNull();
    });

    test('returns null for empty recipe name', () => {
        const result = parseRecipeSpec('package-name:');
        expect(result).toBeNull();
    });

    test('handles multiple colons - uses last one', () => {
        const result = parseRecipeSpec('pkg:recipe:with:colons');
        expect(result).toEqual({
            packageName: 'pkg:recipe:with',
            recipeName: 'colons',
            isLocalPath: false
        });
    });

    test('detects absolute Unix path as local', () => {
        const result = parseRecipeSpec('/Users/dev/my-recipes:my-recipe');
        expect(result).toEqual({
            packageName: '/Users/dev/my-recipes',
            recipeName: 'my-recipe',
            isLocalPath: true
        });
    });

    test('detects relative path with ./ as local', () => {
        const result = parseRecipeSpec('./local-recipes:my-recipe');
        expect(result).toEqual({
            packageName: './local-recipes',
            recipeName: 'my-recipe',
            isLocalPath: true
        });
    });

    test('detects relative path with ../ as local', () => {
        const result = parseRecipeSpec('../other-recipes:my-recipe');
        expect(result).toEqual({
            packageName: '../other-recipes',
            recipeName: 'my-recipe',
            isLocalPath: true
        });
    });

    test('detects Windows absolute path as local', () => {
        const result = parseRecipeSpec('C:\\Users\\dev\\recipes:my-recipe');
        expect(result).toEqual({
            packageName: 'C:\\Users\\dev\\recipes',
            recipeName: 'my-recipe',
            isLocalPath: true
        });
    });
});

describe('parseRecipeOptions', () => {
    test('parses key=value pairs', () => {
        const result = parseRecipeOptions(['text=hello', 'count=42']);
        expect(result).toEqual({
            text: 'hello',
            count: 42
        });
    });

    test('parses boolean flags', () => {
        const result = parseRecipeOptions(['verbose', 'debug']);
        expect(result).toEqual({
            verbose: true,
            debug: true
        });
    });

    test('parses JSON array values', () => {
        const result = parseRecipeOptions(['items=[1,2,3]']);
        expect(result).toEqual({
            items: [1, 2, 3]
        });
    });

    test('parses JSON object values', () => {
        const result = parseRecipeOptions(['config={"key":"value"}']);
        expect(result).toEqual({
            config: {key: 'value'}
        });
    });

    test('keeps strings as strings when not valid JSON', () => {
        const result = parseRecipeOptions(['name=John Doe']);
        expect(result).toEqual({
            name: 'John Doe'
        });
    });

    test('handles mixed options', () => {
        const result = parseRecipeOptions([
            'text=hello',
            'verbose',
            'count=5',
            'items=["a","b"]'
        ]);
        expect(result).toEqual({
            text: 'hello',
            verbose: true,
            count: 5,
            items: ['a', 'b']
        });
    });

    test('returns empty object for empty input', () => {
        const result = parseRecipeOptions([]);
        expect(result).toEqual({});
    });
});

describe('isAcceptedFile', () => {
    test('accepts JavaScript files', () => {
        expect(isAcceptedFile('/path/to/file.js')).toBe(true);
        expect(isAcceptedFile('/path/to/file.jsx')).toBe(true);
        expect(isAcceptedFile('/path/to/file.mjs')).toBe(true);
        expect(isAcceptedFile('/path/to/file.cjs')).toBe(true);
    });

    test('accepts TypeScript files', () => {
        expect(isAcceptedFile('/path/to/file.ts')).toBe(true);
        expect(isAcceptedFile('/path/to/file.tsx')).toBe(true);
        expect(isAcceptedFile('/path/to/file.mts')).toBe(true);
        expect(isAcceptedFile('/path/to/file.cts')).toBe(true);
    });

    test('accepts JSON files', () => {
        expect(isAcceptedFile('/path/to/file.json')).toBe(true);
        expect(isAcceptedFile('/path/to/package.json')).toBe(true);
    });

    test('rejects other file types', () => {
        expect(isAcceptedFile('/path/to/file.txt')).toBe(false);
        expect(isAcceptedFile('/path/to/file.md')).toBe(false);
        expect(isAcceptedFile('/path/to/file.css')).toBe(false);
        expect(isAcceptedFile('/path/to/file.html')).toBe(false);
    });

    test('is case insensitive for extensions', () => {
        expect(isAcceptedFile('/path/to/file.JS')).toBe(true);
        expect(isAcceptedFile('/path/to/file.TS')).toBe(true);
        expect(isAcceptedFile('/path/to/file.JSON')).toBe(true);
    });
});

describe('findRecipe', () => {
    let registry: RecipeRegistry;
    let consoleSpy: jest.SpiedFunction<typeof console.error>;

    beforeEach(() => {
        registry = new RecipeRegistry();
        registry.register(TestRecipe);
        registry.register(AnotherTestRecipe);
        consoleSpy = jest.spyOn(console, 'error').mockImplementation(() => {});
    });

    afterEach(() => {
        consoleSpy.mockRestore();
    });

    test('finds recipe by exact name', () => {
        const recipe = findRecipe(registry, 'org.openrewrite.test.my-recipe', {});
        expect(recipe).not.toBeNull();
        expect(recipe!.name).toBe('org.openrewrite.test.my-recipe');
    });

    test('finds recipe by suffix with dot separator', () => {
        const recipe = findRecipe(registry, 'my-recipe', {});
        expect(recipe).not.toBeNull();
        expect(recipe!.name).toBe('org.openrewrite.test.my-recipe');
    });

    test('finds recipe by suffix with dash separator', () => {
        const recipe = findRecipe(registry, 'recipe', {});
        // This should match by partial (includes) since -recipe matches
        expect(recipe).toBeNull(); // Multiple matches
    });

    test('passes options to recipe constructor', () => {
        const recipe = findRecipe(registry, 'org.openrewrite.test.my-recipe', {text: 'hello'}) as TestRecipe;
        expect(recipe).not.toBeNull();
        expect(recipe.options?.text).toBe('hello');
    });

    test('returns null for non-existent recipe', () => {
        const recipe = findRecipe(registry, 'non-existent-recipe', {});
        expect(recipe).toBeNull();
        expect(consoleSpy).toHaveBeenCalled();
    });

    test('returns null and prints error for ambiguous name', () => {
        // Add a recipe with similar suffix
        registry.register(SimilarRecipe);

        const recipe = findRecipe(registry, 'my-recipe', {});
        expect(recipe).toBeNull();
        expect(consoleSpy).toHaveBeenCalledWith(expect.stringContaining('Ambiguous'));
    });

    test('finds recipe by partial match when no suffix match', () => {
        const recipe = findRecipe(registry, 'another', {});
        expect(recipe).not.toBeNull();
        expect(recipe!.name).toBe('org.openrewrite.test.another-recipe');
    });
});

describe('discoverFiles', () => {
    let tmpDir: string;

    beforeEach(() => {
        // Create a unique temp directory for each test
        tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'rewrite-cli-test-'));
    });

    afterEach(() => {
        // Clean up temp directory
        fs.rmSync(tmpDir, {recursive: true, force: true});
    });

    test('discovers files in non-git directory', async () => {
        // Create test files
        fs.writeFileSync(path.join(tmpDir, 'index.js'), 'console.log("hello")');
        fs.writeFileSync(path.join(tmpDir, 'package.json'), '{"name": "test"}');
        fs.writeFileSync(path.join(tmpDir, 'README.md'), '# Test'); // Should be excluded

        const files = await discoverFiles(tmpDir);

        expect(files).toHaveLength(2);
        expect(files.some(f => f.endsWith('index.js'))).toBe(true);
        expect(files.some(f => f.endsWith('package.json'))).toBe(true);
        expect(files.some(f => f.endsWith('README.md'))).toBe(false);
    });

    test('discovers files in subdirectories of non-git directory', async () => {
        // Create nested structure
        fs.mkdirSync(path.join(tmpDir, 'src'));
        fs.writeFileSync(path.join(tmpDir, 'src', 'app.ts'), 'export const x = 1');
        fs.writeFileSync(path.join(tmpDir, 'package.json'), '{}');

        const files = await discoverFiles(tmpDir);

        expect(files).toHaveLength(2);
        expect(files.some(f => f.includes('src') && f.endsWith('app.ts'))).toBe(true);
    });

    test('excludes node_modules in non-git directory', async () => {
        // Create node_modules directory with files
        fs.mkdirSync(path.join(tmpDir, 'node_modules'));
        fs.writeFileSync(path.join(tmpDir, 'node_modules', 'dep.js'), 'module.exports = {}');
        fs.writeFileSync(path.join(tmpDir, 'index.js'), 'require("dep")');

        const files = await discoverFiles(tmpDir);

        expect(files).toHaveLength(1);
        expect(files.some(f => f.endsWith('index.js'))).toBe(true);
        expect(files.some(f => f.includes('node_modules'))).toBe(false);
    });
});
