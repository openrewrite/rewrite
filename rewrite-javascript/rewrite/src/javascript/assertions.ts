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
import {create as produce} from "mutative";
import * as fs from "fs";
import * as path from "path";
import {Autodetect} from "./autodetect";
import {Marker, replaceMarkerByKind} from "../markers";

/**
 * Shared TypeScript source file cache for test parsers.
 * Automatically configured for template parsing in tests.
 */
export const sourceFileCache: Map<string, ts.SourceFile> = new Map();

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

            const cachedWorkspace = DependencyWorkspace.getOrCreateWorkspace({
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
    const detection = await prettierLoader.detectPrettier();

    // Collect JS/TS specs for potential auto-detection
    const jsSpecs = sourceSpecs.filter(
        spec => spec.path !== 'package.json' && spec.path !== 'package-lock.json' && spec.kind === JS.Kind.CompilationUnit
    );

    // Build style marker: either PrettierStyle per-file or Autodetect for all
    let autodetectMarker: Marker | null = null;
    if (!detection.available && jsSpecs.length > 0) {
        // Prettier is NOT available: auto-detect styles from the source files
        // Pre-parse all JS specs to sample them
        const detector = Autodetect.detector();
        const tempParser = new JavaScriptParser({sourceFileCache, relativeTo});

        for (const spec of jsSpecs) {
            if (spec.before) {
                const fileName = spec.path ?? `file.${spec.ext}`;
                const filePath = path.join(relativeTo, fileName);
                for await (const sf of tempParser.parse({text: spec.before, sourcePath: filePath})) {
                    if (sf.kind === JS.Kind.CompilationUnit) {
                        detector.sample(sf as JS.CompilationUnit);
                    }
                }
            }
        }
        autodetectMarker = detector.build();
    }

    for (const spec of sourceSpecs) {
        const isPackageJson = spec.path?.endsWith('package.json');
        const isPackageLock = spec.path === 'package-lock.json';
        if (!isPackageJson && !isPackageLock) {
            if (spec.kind === JS.Kind.CompilationUnit) {
                // For JS/TS files, use a parser that adds style marker if available
                // spec.path may be undefined, so generate a reasonable path from the extension
                const fileName = spec.path ?? `file.${spec.ext}`;
                const filePath = path.join(relativeTo, fileName);

                // Get the appropriate style marker
                let styleMarker: Marker | undefined;
                if (detection.available) {
                    // Use per-file PrettierStyle marker
                    styleMarker = await prettierLoader.getConfigMarker(filePath);
                } else {
                    // Use shared Autodetect marker
                    styleMarker = autodetectMarker ?? undefined;
                }

                yield {
                    ...spec,
                    parser: () => new JavaScriptParser({sourceFileCache, relativeTo}),
                    // Add style marker before recipe runs if available
                    // Compose with existing beforeRecipe if present
                    beforeRecipe: styleMarker ? (sf: JS.CompilationUnit) => {
                        const withMarker = produce(sf, draft => {
                            draft.markers = replaceMarkerByKind(draft.markers, styleMarker);
                        });
                        return spec.beforeRecipe ? spec.beforeRecipe(withMarker) : withMarker;
                    } : spec.beforeRecipe
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
