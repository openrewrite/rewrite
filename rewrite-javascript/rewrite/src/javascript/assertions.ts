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
import {execSync} from "child_process";

const sourceFileCache: Map<string, ts.SourceFile> = new Map();

export function* npm(relativeTo: string, ...sourceSpecs: SourceSpec<any>[]): Generator<SourceSpec<any>, void, unknown> {
    for (const spec of sourceSpecs) {
        if (spec.path === 'package.json') {
            // Write package.json to disk so npm install can be run
            fs.mkdirSync(relativeTo, {recursive: true});
            fs.writeFileSync(path.join(relativeTo, 'package.json'), spec.before!);
            execSync('npm install', {
                cwd: relativeTo,
                stdio: 'inherit' // Show npm output for debugging
            });
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
