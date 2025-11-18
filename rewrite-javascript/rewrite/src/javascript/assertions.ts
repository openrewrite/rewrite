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
import {AfterRecipeText, dedentAfter, SourceSpec} from "../test";
import {JavaScriptParser} from "./parser";
import {JS} from "./tree";
import ts from 'typescript';
import {json, Json} from "../json";
import {DependencyWorkspace} from "./dependency-workspace";
import {setTemplateSourceFileCache} from "./templating/engine";

/**
 * Shared TypeScript source file cache for test parsers.
 * Automatically configured for template parsing in tests.
 */
export const sourceFileCache: Map<string, ts.SourceFile> = new Map();

// Automatically enable sourceFileCache for template parsing in tests
setTemplateSourceFileCache(sourceFileCache);

export async function* npm(relativeTo: string, ...sourceSpecs: SourceSpec<any>[]): AsyncGenerator<SourceSpec<any>, void, unknown> {
    const fs = require('fs');
    const path = require('path');

    // Ensure the target directory exists
    if (!fs.existsSync(relativeTo)) {
        fs.mkdirSync(relativeTo, { recursive: true });
    }

    for (const spec of sourceSpecs) {
        if (spec.path === 'package.json') {
            // Parse package.json to extract dependencies
            const packageJsonContent = JSON.parse(spec.before!);
            const dependencies = {
                ...packageJsonContent.dependencies,
                ...packageJsonContent.devDependencies
            };

            // Get or create cached workspace with node_modules (don't pass targetDir to use caching)
            if (Object.keys(dependencies).length > 0) {
                const cachedWorkspace = await DependencyWorkspace.getOrCreateWorkspace(dependencies);

                // Symlink node_modules from cached workspace to test directory
                const cachedNodeModules = path.join(cachedWorkspace, 'node_modules');
                const testNodeModules = path.join(relativeTo, 'node_modules');

                // Remove existing node_modules if present
                if (fs.existsSync(testNodeModules)) {
                    fs.rmSync(testNodeModules, { recursive: true, force: true });
                }

                // Create symlink
                fs.symlinkSync(cachedNodeModules, testNodeModules, 'junction');
            }

            // Write the actual package.json from the test spec
            fs.writeFileSync(
                path.join(relativeTo, 'package.json'),
                spec.before
            );

            yield spec;
        }
    }
    for (const spec of sourceSpecs) {
        if (spec.path !== 'package.json') {
            if (spec.kind === JS.Kind.CompilationUnit) {
                yield {
                    ...spec,
                    parser: () => new JavaScriptParser({sourceFileCache, relativeTo})
                }
            } else {
                yield spec;
            }
        }
    }
}

export function packageJson(before: string, after?: AfterRecipeText): SourceSpec<Json.Document> {
    return {
        ...json(before, after),
        path: 'package.json'
    };
}

export function javascript(before: string | null, after?: AfterRecipeText): SourceSpec<JS.CompilationUnit> {
    return {
        kind: JS.Kind.CompilationUnit,
        before: before,
        after: dedentAfter(after),
        ext: 'js',
        parser: ctx => new JavaScriptParser({sourceFileCache})
    };
}

export function typescript(before: string | null, after?: AfterRecipeText): SourceSpec<JS.CompilationUnit> {
    return {
        kind: JS.Kind.CompilationUnit,
        before: before,
        after: dedentAfter(after),
        ext: 'ts',
        parser: () => new JavaScriptParser({sourceFileCache})
    };
}

export function tsx(before: string | null, after?: AfterRecipeText): SourceSpec<JS.CompilationUnit> {
    return {
        kind: JS.Kind.CompilationUnit,
        before: before,
        after: dedentAfter(after),
        ext: 'tsx',
        parser: () => new JavaScriptParser({sourceFileCache})
    };
}

export function jsx(before: string | null, after?: AfterRecipeText): SourceSpec<JS.CompilationUnit> {
    return {
        kind: JS.Kind.CompilationUnit,
        before: before,
        after: dedentAfter(after),
        ext: 'jsx',
        parser: () => new JavaScriptParser({sourceFileCache})
    };
}
