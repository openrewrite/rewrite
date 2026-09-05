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
import fs from "node:fs";
import * as path from "path";

/**
 * What a project gets to decide: where a module specifier resolves to, and which ambient
 * declarations are in scope. The shape of the types themselves stays with the parser — `target`
 * and `lib` bound the global scope, `strict` adds a `null` to every nullable type — which pins
 * the widest view it can rather than the one a project compiles against.
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
    readonly options: CompilerOptions;
}

/** Compiler options under the names a tsconfig states them with. */
export type CompilerOptions = Record<string, unknown>;

/** Reads the options a config file states, resolving its `extends` chain. */
export type ConfigParser = (configFilePath: string) => CompilerOptions;

/**
 * Finds the config file governing each file and merges its resolution options over a set of
 * parser defaults, so that type-aware recipes see the same modules `tsc` would.
 */
export class TsConfigResolver {
    private readonly configByDir = new Map<string, string | undefined>();
    private readonly optionsByConfig = new Map<string, CompilerOptions | undefined>();
    private readonly root?: string;

    /**
     * @param defaults Options to fall back on, and to merge a project config over.
     * @param parse Reads a config file. The compiler owns config parsing, so this is supplied
     *   rather than done here.
     * @param relativeTo Root of the project being parsed, bounding the search. Without one,
     *   walking up from the process working directory picks up whatever config sits above it.
     */
    constructor(private readonly defaults: CompilerOptions, private readonly parse: ConfigParser,
                relativeTo?: string) {
        this.root = relativeTo ? path.resolve(relativeTo) : undefined;
    }

    forFile(filePath: string): ResolvedTsConfig {
        const configFilePath = this.findConfig(path.dirname(path.resolve(filePath)));
        const options = configFilePath === undefined ? undefined : this.optionsFor(configFilePath);
        return options === undefined ? {options: this.defaults} : {configFilePath, options};
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
                if (fs.existsSync(candidate)) {
                    return memoize(candidate);
                }
            }
            if (dir === this.root || path.dirname(dir) === dir) {
                return memoize(undefined);
            }
        }
    }

    private optionsFor(configFilePath: string): CompilerOptions | undefined {
        if (!this.optionsByConfig.has(configFilePath)) {
            this.optionsByConfig.set(configFilePath, this.merge(configFilePath));
        }
        return this.optionsByConfig.get(configFilePath);
    }

    /** Undefined where the config cannot be read, leaving the caller on its defaults. */
    private merge(configFilePath: string): CompilerOptions | undefined {
        let projectOptions: CompilerOptions;
        try {
            projectOptions = this.parse(configFilePath);
        } catch {
            return undefined;
        }

        // A project's own `paths` stand alone, including where it states none, so that a nested
        // project resolves as its config says rather than against the repository root.
        const {paths: _, ...defaults} = this.defaults;

        const resolution: CompilerOptions = {};
        for (const option of RESOLUTION_OPTIONS) {
            if (projectOptions[option] !== undefined) {
                resolution[option] = projectOptions[option];
            }
        }
        rootPaths(resolution, path.dirname(configFilePath));

        // Where the project states an option of its own that its resolution mode also implies,
        // the project's is the more direct statement of the two.
        const stated = resolution.moduleResolution as number | undefined;
        delete resolution.moduleResolution;
        const mode = stated === undefined ? pairedModuleResolution(projectOptions) : MODULE_RESOLUTION[stated];
        return {...defaults, ...mode, ...resolution};
    }
}

/**
 * Rewrites `baseUrl` and `paths` into the absolute `paths` that stand for them. `baseUrl` names
 * a directory every bare specifier is tried under, which is a `*` pattern rooted there; a relative
 * `paths` entry resolves against the config stating it, which is not the config the parser writes.
 */
function rootPaths(options: CompilerOptions, configDirectory: string): void {
    const basePath = (options.pathsBasePath as string | undefined) ?? configDirectory;
    const baseUrl = options.baseUrl as string | undefined;
    const stated = options.paths as Record<string, string[]> | undefined;
    delete options.pathsBasePath;
    delete options.baseUrl;
    if (!stated && !baseUrl) {
        return;
    }
    const paths: Record<string, string[]> = {};
    for (const [pattern, targets] of Object.entries(stated ?? {})) {
        paths[pattern] = targets.map(target => path.resolve(basePath, target));
    }
    // A pattern the project states for `*` is the more specific statement of the two.
    if (baseUrl && !paths["*"]) {
        paths["*"] = [path.join(baseUrl, "*")];
    }
    options.paths = paths;
}

/**
 * What a `ModuleResolutionKind` states in a config, by value, the enum itself not being on the
 * compiler's public surface. TypeScript 7 has dropped the two modes predating `exports`:
 * `node10` reads neither an `exports` nor an `imports` map, which `bundler` states directly,
 * while `classic` searches no `node_modules` at all and so has nothing to stand in for it.
 */
const MODULE_RESOLUTION: Record<number, CompilerOptions | undefined> = {
    1: undefined,
    2: {moduleResolution: "bundler", resolvePackageJsonExports: false, resolvePackageJsonImports: false},
    3: {moduleResolution: "node16"},
    99: {moduleResolution: "nodenext"},
    100: {moduleResolution: "bundler"},
};

/** `ModuleKind` by value, for the module kinds that imply a resolution. */
const MODULE_KIND = {CommonJS: 1, ESNext: 99, Node16: 100, NodeNext: 199, Preserve: 200};

/**
 * The resolution TypeScript pairs with a project that states only its `module`. Only the pairing
 * is adopted, since a module kind such as `node16` classifies files as CJS or ESM, and an ESM-only
 * import from a CJS file is then the error TS1479, costing the file its LST.
 */
function pairedModuleResolution(projectOptions: CompilerOptions): CompilerOptions | undefined {
    switch (projectOptions.module) {
        case MODULE_KIND.CommonJS:
            return MODULE_RESOLUTION[2];
        case MODULE_KIND.Node16:
            return MODULE_RESOLUTION[3];
        case MODULE_KIND.NodeNext:
            return MODULE_RESOLUTION[99];
        case MODULE_KIND.Preserve:
            return MODULE_RESOLUTION[100];
        default:
            return undefined;
    }
}

function isAtOrUnder(dir: string, root: string): boolean {
    const relative = path.relative(root, dir);
    return relative === "" ||
        (relative !== ".." && !relative.startsWith(".." + path.sep) && !path.isAbsolute(relative));
}
