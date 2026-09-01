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
import ts from "typescript";
import * as path from "path";

/**
 * The options that decide what a module specifier resolves to. Confined to those, because the
 * rest of a project's tsconfig only narrows attribution — `target` and `lib` shrink the global
 * scope, `strict` reshapes types — where the parser wants the widest view it can get.
 */
const RESOLUTION_OPTIONS: string[] = [
    "baseUrl",
    "paths",
    // Internal companion to `paths`: the directory its entries resolve against.
    "pathsBasePath",
    "rootDirs",
    "types",
    "typeRoots",
    "moduleResolution",
    "moduleSuffixes",
    "customConditions",
    "resolvePackageJsonExports",
    "resolvePackageJsonImports",
    "resolveJsonModule",
];

/**
 * Config file names in precedence order. TypeScript reads jsconfig.json for JavaScript
 * projects, which is where a JS-only codebase states its `paths`.
 */
const CONFIG_FILE_NAMES = ["tsconfig.json", "jsconfig.json"];

export interface ResolvedTsConfig {
    /**
     * Absolute path of the config file the options came from. Undefined when no project
     * config applied, in which case {@link options} are the parser's defaults.
     */
    readonly configFilePath?: string;
    readonly options: ts.CompilerOptions;
}

/**
 * Finds the config file governing each file and merges its resolution options over a set of
 * parser defaults, so that type-aware recipes see the same modules `tsc` would.
 */
export class TsConfigResolver {
    private readonly configByDir = new Map<string, string | undefined>();
    private readonly optionsByConfig = new Map<string, ts.CompilerOptions>();
    private readonly root?: string;

    /**
     * @param defaults Options to fall back on, and to merge a project config over.
     * @param relativeTo Root of the project being parsed, bounding the search. Without one,
     *   walking up from the process working directory picks up whatever config sits above it.
     */
    constructor(private readonly defaults: ts.CompilerOptions, relativeTo?: string) {
        this.root = relativeTo ? path.resolve(relativeTo) : undefined;
    }

    forFile(filePath: string): ResolvedTsConfig {
        const configFilePath = this.findConfig(path.dirname(path.resolve(filePath)));
        return configFilePath ?
            {configFilePath, options: this.optionsFor(configFilePath)} :
            {options: this.defaults};
    }

    /** The nearest config file at or below the project root, searching upwards. */
    private findConfig(from: string): string | undefined {
        if (!this.root || !isAtOrUnder(from, this.root)) {
            return undefined;
        }

        const searched: string[] = [];
        const memoize = (found: string | undefined) => {
            for (const dir of searched) {
                this.configByDir.set(dir, found);
            }
            return found;
        };

        for (let dir = from; ; dir = path.dirname(dir)) {
            if (this.configByDir.has(dir)) {
                return memoize(this.configByDir.get(dir));
            }
            searched.push(dir);

            for (const configFileName of CONFIG_FILE_NAMES) {
                const candidate = path.join(dir, configFileName);
                if (ts.sys.fileExists(candidate)) {
                    return memoize(candidate);
                }
            }
            if (dir === this.root || path.dirname(dir) === dir) {
                return memoize(undefined);
            }
        }
    }

    private optionsFor(configFilePath: string): ts.CompilerOptions {
        let options = this.optionsByConfig.get(configFilePath);
        if (!options) {
            this.optionsByConfig.set(configFilePath, options = this.merge(configFilePath));
        }
        return options;
    }

    private merge(configFilePath: string): ts.CompilerOptions {
        const {config, error} = ts.readConfigFile(configFilePath, ts.sys.readFile);
        if (error || !config) {
            return this.defaults;
        }

        const {options: projectOptions} = ts.parseJsonConfigFileContent(
            config, ts.sys, path.dirname(configFilePath), undefined, configFilePath);

        // `baseUrl` takes precedence over `pathsBasePath`, so a default rooted at the repository
        // would resolve a nested project's `paths` against the wrong directory.
        const {baseUrl: _, ...defaults} = this.defaults;

        const resolution: ts.CompilerOptions = {};
        for (const option of RESOLUTION_OPTIONS) {
            if (projectOptions[option] !== undefined) {
                resolution[option] = projectOptions[option];
            }
        }
        if (resolution.moduleResolution === undefined) {
            const paired = pairedModuleResolution(projectOptions);
            if (paired !== undefined) {
                resolution.moduleResolution = paired;
            }
        }
        return {...defaults, ...resolution};
    }
}

/**
 * The resolution TypeScript infers for a project that states only its `module`. Only the
 * inference is adopted, since a module kind such as `node16` classifies files as CJS or ESM,
 * and an ESM-only import from a CJS file is then the error TS1479, costing the file its LST.
 * `Classic` is declined as well, searching no `node_modules`, so the default stands in.
 */
function pairedModuleResolution(projectOptions: ts.CompilerOptions): ts.ModuleResolutionKind | undefined {
    const infer = (ts as Partial<{ getEmitModuleResolutionKind(options: ts.CompilerOptions): ts.ModuleResolutionKind }>)
        .getEmitModuleResolutionKind;
    if (projectOptions.module === undefined || !infer) {
        return undefined;
    }
    const inferred = infer(projectOptions);
    return inferred === ts.ModuleResolutionKind.Classic ? undefined : inferred;
}

function isAtOrUnder(dir: string, root: string): boolean {
    const relative = path.relative(root, dir);
    return relative === "" || (!relative.startsWith("..") && !path.isAbsolute(relative));
}
