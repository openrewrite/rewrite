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
import {PackageJsonParser} from "./package-json-parser";
import {JS} from "./tree";
import ts from 'typescript';
import {json, Json} from "../json";
import {DependencyWorkspace} from "./dependency-workspace";
import {setTemplateSourceFileCache} from "./templating/engine";
import {PrettierConfigLoader} from "./format/prettier-config-loader";
import {PrettierStyle} from "./style";
import {create as produce} from "mutative";
import * as fs from "fs";
import * as path from "path";
import {Autodetect} from "./autodetect";
import {replaceMarkerByKind} from "../markers";
import {Parser, ParserInput} from "../parser";
import {SourceFile} from "../tree";

/**
 * Shared TypeScript source file cache for test parsers.
 * Automatically configured for template parsing in tests.
 */
export const sourceFileCache: Map<string, ts.SourceFile> = new Map();

// Pooled so a spec reuses the program the previous spec left behind; see `JavaScriptParser.parse`.
const parserPool: Map<string, Parser> = new Map();

function pooledParser(relativeTo?: string, detectStyle = false, prettierLoader?: PrettierConfigLoader): Parser {
    const key = relativeTo ?? "";
    let parser = parserPool.get(key);
    if (!parser || (parser instanceof StyleDetectingParser) !== detectStyle) {
        // A removed directory, such as a finished test's temporary one, is never parsed again.
        for (const dir of parserPool.keys()) {
            if (dir && !fs.existsSync(dir)) {
                parserPool.delete(dir);
            }
        }
        const javaScriptParser = new JavaScriptParser({sourceFileCache, relativeTo});
        parser = detectStyle ? new StyleDetectingParser(javaScriptParser) : javaScriptParser;
        parserPool.set(key, parser);
    }
    if (parser instanceof StyleDetectingParser) {
        parser.prettierLoader = prettierLoader;
    }
    return parser;
}

/**
 * A parser that gives the files it parses together the style they are written in.
 */
class StyleDetectingParser extends Parser {
    // Wraps `JavaScriptParser` rather than extending it. Loading the package from some of its internal
    // modules runs this module before `parser.ts` has finished.
    constructor(private readonly parser: JavaScriptParser, public prettierLoader?: PrettierConfigLoader) {
        super();
    }

    async* parse(...inputs: ParserInput[]): AsyncGenerator<SourceFile> {
        const parsed: {sourceFile: SourceFile, prettierMarker?: PrettierStyle}[] = [];
        const detector = Autodetect.detector();
        for await (const sourceFile of this.parser.parse(...inputs)) {
            const prettierMarker = await this.prettierLoader?.getConfigMarker(sourceFile.sourcePath);
            parsed.push({sourceFile, prettierMarker});
            if (!prettierMarker) {
                await detector.sample(sourceFile);
            }
        }
        const style = detector.build();
        for (const {sourceFile, prettierMarker} of parsed) {
            yield sourceFile.kind === JS.Kind.CompilationUnit ? produce(sourceFile, draft => {
                draft.markers = replaceMarkerByKind(draft.markers, prettierMarker ?? style);
            }) : sourceFile;
        }
    }
}

// Automatically enable sourceFileCache for template parsing in tests
setTemplateSourceFileCache(sourceFileCache);

export async function* npm(relativeTo: string, ...sourceSpecs: SourceSpec<any>[]): AsyncGenerator<SourceSpec<any>, void, unknown> {
    // Ensure the target directory exists
    if (!fs.existsSync(relativeTo)) {
        fs.mkdirSync(relativeTo, { recursive: true });
    }

    // Find package.json and package-lock.json specs
    const packageJsonSpec = sourceSpecs.find(spec => spec.path === 'package.json');
    const packageLockSpec = sourceSpecs.find(spec => spec.path === 'package-lock.json');

    // Write package-lock.json first (if provided) so PackageJsonParser can read it
    if (packageLockSpec && packageLockSpec.before) {
        fs.writeFileSync(
            path.join(relativeTo, 'package-lock.json'),
            packageLockSpec.before
        );
    }

    // Write non-JS/TS/JSON files to disk FIRST so Prettier config detection can find them
    // Exclude all package.json files (root and workspace members) - they're handled separately
    for (const spec of sourceSpecs) {
        const isPackageJson = spec.path?.endsWith('package.json');
        const isPackageLock = spec.path === 'package-lock.json';
        const isJsTs = spec.kind === JS.Kind.CompilationUnit;

        if (!isPackageJson && !isPackageLock && !isJsTs) {
            if (spec.before && spec.path) {
                const filePath = path.join(relativeTo, spec.path);
                const dir = path.dirname(filePath);
                if (!fs.existsSync(dir)) {
                    fs.mkdirSync(dir, { recursive: true });
                }
                fs.writeFileSync(filePath, spec.before);
            }
        }
    }

    // Collect all package.json specs (root and workspace members)
    const allPackageJsonSpecs = sourceSpecs.filter(spec => spec.path?.endsWith('package.json'));

    // Yield package.json FIRST so its PackageJsonParser is used for all JSON specs
    // (The test framework uses the first spec's parser for all specs of the same kind)
    if (packageJsonSpec) {
        // Parse package.json to check if there are dependencies or workspaces
        const packageJsonContent = JSON.parse(packageJsonSpec.before!);
        const hasDependencies = Object.keys({
            ...packageJsonContent.dependencies,
            ...packageJsonContent.devDependencies
        }).length > 0;
        const hasWorkspaces = Array.isArray(packageJsonContent.workspaces) && packageJsonContent.workspaces.length > 0;

        // Get or create cached workspace with node_modules
        // If packageLockSpec is provided, use it for deterministic installs with npm ci
        // For workspaces, include all workspace member package.json files
        if (hasDependencies || hasWorkspaces) {
            // Build workspace packages map for DependencyWorkspace
            const workspacePackages: Record<string, string> | undefined = hasWorkspaces
                ? Object.fromEntries(
                    allPackageJsonSpecs
                        .filter(spec => spec.path !== 'package.json' && spec.before)
                        .map(spec => [spec.path!, spec.before!])
                )
                : undefined;

            const cachedWorkspace = await DependencyWorkspace.getOrCreateWorkspace({
                packageJsonContent: packageJsonSpec.before!,
                packageLockContent: packageLockSpec?.before ?? undefined,
                workspacePackages
            });

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
            packageJsonSpec.before!
        );

        // Use PackageJsonParser to parse and attach NodeResolutionResult marker
        yield {
            ...packageJsonSpec,
            parser: () => new PackageJsonParser({relativeTo})
        };
    }

    // Write and yield workspace member package.json files with PackageJsonParser
    for (const spec of allPackageJsonSpecs) {
        if (spec.path !== 'package.json') {
            // Write workspace member package.json to disk
            if (spec.before && spec.path) {
                const filePath = path.join(relativeTo, spec.path);
                const dir = path.dirname(filePath);
                if (!fs.existsSync(dir)) {
                    fs.mkdirSync(dir, { recursive: true });
                }
                fs.writeFileSync(filePath, spec.before);
            }

            yield {
                ...spec,
                parser: () => new PackageJsonParser({relativeTo})
            };
        }
    }

    // Yield package-lock.json after package.json (so PackageJsonParser is used as the group parser)
    if (packageLockSpec) {
        yield packageLockSpec;
    }

    // Detect Prettier configuration for the project
    const prettierLoader = new PrettierConfigLoader(relativeTo);
    await prettierLoader.detectPrettier();

    for (const spec of sourceSpecs) {
        const isPackageJson = spec.path?.endsWith('package.json');
        const isPackageLock = spec.path === 'package-lock.json';
        if (!isPackageJson && !isPackageLock) {
            if (spec.kind === JS.Kind.CompilationUnit) {
                yield {
                    ...spec,
                    parser: () => pooledParser(relativeTo, true, prettierLoader)
                }
            } else {
                // Non-JS/TS files were already written to disk above
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

export function packageLockJson(before: string, after?: AfterRecipeText): SourceSpec<Json.Document> {
    return {
        ...json(before, after),
        path: 'package-lock.json'
    };
}

/**
 * Gives a source file the style its own text is written in. A spec that formats its output is
 * measured against the built-in defaults unless it opts in with
 * `{...typescript(src), beforeRecipe: withDetectedStyle}`; a file parsed through {@link npm} is
 * sampled alongside its siblings and needs no opt-in.
 */
export async function withDetectedStyle(sourceFile: JS.CompilationUnit): Promise<JS.CompilationUnit> {
    const detector = Autodetect.detector();
    await detector.sample(sourceFile);
    const detected = detector.build();
    return produce(sourceFile, draft => {
        draft.markers = replaceMarkerByKind(draft.markers, detected);
    });
}

export function javascript(before: string | null, after?: AfterRecipeText): SourceSpec<JS.CompilationUnit> {
    return {
        kind: JS.Kind.CompilationUnit,
        before: before,
        after: dedentAfter(after),
        ext: 'js',
        parser: ctx => pooledParser()
    };
}

export function typescript(before: string | null, after?: AfterRecipeText): SourceSpec<JS.CompilationUnit> {
    return {
        kind: JS.Kind.CompilationUnit,
        before: before,
        after: dedentAfter(after),
        ext: 'ts',
        parser: () => pooledParser()
    };
}

export function tsx(before: string | null, after?: AfterRecipeText): SourceSpec<JS.CompilationUnit> {
    return {
        kind: JS.Kind.CompilationUnit,
        before: before,
        after: dedentAfter(after),
        ext: 'tsx',
        parser: () => pooledParser()
    };
}

export function jsx(before: string | null, after?: AfterRecipeText): SourceSpec<JS.CompilationUnit> {
    return {
        kind: JS.Kind.CompilationUnit,
        before: before,
        after: dedentAfter(after),
        ext: 'jsx',
        parser: () => pooledParser()
    };
}
