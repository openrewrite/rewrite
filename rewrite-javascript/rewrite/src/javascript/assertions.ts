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
import * as fs from "fs";
import * as path from "path";
import {DependencyWorkspace} from "./dependency-workspace";

const sourceFileCache: Map<string, ts.SourceFile> = new Map();

export async function* npm(relativeTo: string, ...sourceSpecs: SourceSpec<any>[]): AsyncGenerator<SourceSpec<any>, void, unknown> {
    for (const spec of sourceSpecs) {
        if (spec.path === 'package.json') {
            // Parse package.json to extract dependencies
            const packageJsonContent = JSON.parse(spec.before!);
            const dependencies = {
                ...packageJsonContent.dependencies,
                ...packageJsonContent.devDependencies
            };

            // Use DependencyWorkspace to create workspace in relativeTo directory
            // This will check if it's already valid and skip npm install if so
            if (Object.keys(dependencies).length > 0) {
                await DependencyWorkspace.getOrCreateWorkspace(dependencies, relativeTo);
            }

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
