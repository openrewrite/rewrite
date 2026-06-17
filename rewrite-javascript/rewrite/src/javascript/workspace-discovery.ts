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
import * as path from "path";
import * as fs from "fs";
import * as ts from "typescript";
import * as YAML from "yaml";
import picomatch from "picomatch";
import {PackageManager} from "./node-resolution-result";
import {detectPackageManager, getAllLockFileNames} from "./package-manager";
import {ProjectParser} from "./project-parser";

/**
 * Prettier config file names checked per project (presence-only; not parsed). The
 * package.json "prettier" key is covered by package.json itself already being watched.
 */
const PRETTIER_CONFIG_FILES = [
    ".prettierrc", ".prettierrc.json", ".prettierrc.json5", ".prettierrc.yaml", ".prettierrc.yml",
    ".prettierrc.toml", ".prettierrc.js", ".prettierrc.cjs", ".prettierrc.mjs", ".prettierrc.ts",
    "prettier.config.js", "prettier.config.cjs", "prettier.config.mjs", "prettier.config.ts"
];

/** Jest config file names checked per project (presence-only; not parsed). */
const JEST_CONFIG_FILES = [
    "jest.config.js", "jest.config.cjs", "jest.config.mjs", "jest.config.ts", "jest.config.json"
];

/** Vitest (and the Vite config it reads) file names checked per project (presence-only; not parsed). */
const VITEST_CONFIG_FILES = [
    "vitest.config.ts", "vitest.config.js", "vitest.config.mts", "vitest.config.mjs", "vitest.config.cts", "vitest.config.cjs",
    "vite.config.ts", "vite.config.js", "vite.config.mts", "vite.config.mjs", "vite.config.cts", "vite.config.cjs"
];

/**
 * Globs (relative to a project path) identifying test sources by convention. These
 * are emitted verbatim as the test set's {@code includes} and the main set's
 * {@code excludes}, and are also used to decide whether a project has any test files.
 */
const TEST_GLOBS = ["**/*.test.*", "**/*.spec.*", "**/__tests__/**", "test/**"];

/** Matcher for {@link TEST_GLOBS}, compiled once and reused across all projects. */
const isTestFile = picomatch(TEST_GLOBS);

/**
 * Resolution-relevant compiler configuration the parser needs. v1 carries only the
 * path to the nearest tsconfig; the object stays open so resolved fields (paths,
 * baseUrl, jsx, moduleResolution, ...) can be added additively without a reshape.
 */
export interface ParserSettings {
    /** Relative, "/"-normalized path to the nearest tsconfig.json, if any. */
    tsconfigPath?: string;
}

/**
 * One source set ("main"/"test") within a project. Modeled as a list element (not a
 * boolean) so divergent prod/test parser settings are additive later (Decision #6).
 */
export interface SourceSetDescriptor {
    name: "main" | "test";
    /** Globs (relative to the project path) selecting this set's files. */
    includes?: string[];
    excludes?: string[];
    parserSettings: ParserSettings;
}

/**
 * An independent JavaScript/TypeScript project (a package.json root, workspace-aware).
 */
export interface ProjectDescriptor {
    /** package.json directory, relative to {@link WorkspaceDiscoveryOptions.relativeTo}, "/"-normalized. */
    path: string;
    packageManager: PackageManager;
    /** Watch-set: files whose change forces a full re-parse. Relative, "/"-normalized, deduped. */
    configInputs: string[];
    sourceSets: SourceSetDescriptor[];
    /** Populated only when install ownership moves into the server (Decision #5/#8); always null in v1. */
    resolution: null;
}

export interface WorkspaceDiscoveryOptions {
    /** Glob patterns to exclude from discovery. */
    exclusions?: string[];
    /** Base directory for the returned relative paths (default: repositoryRoot). */
    relativeTo?: string;
}

/** Normalizes an absolute path to one relative to {@code base}, using "/" separators. */
function relativeNormalized(base: string, target: string): string {
    return path.relative(base, target).split(path.sep).join("/");
}

/**
 * Reads the workspace member globs declared at the repository root, if any:
 * npm/yarn/bun via root package.json {@code workspaces} (array or {@code {packages}}),
 * pnpm via {@code pnpm-workspace.yaml}'s {@code packages}. Returns an empty array when
 * the repo is not a workspace.
 */
function readWorkspaceGlobs(root: string): string[] {
    const globs: string[] = [];

    const rootPackageJson = path.join(root, "package.json");
    if (fs.existsSync(rootPackageJson)) {
        try {
            const pkg = JSON.parse(fs.readFileSync(rootPackageJson, "utf-8"));
            const ws = pkg.workspaces;
            const list = Array.isArray(ws) ? ws : Array.isArray(ws?.packages) ? ws.packages : [];
            for (const g of list) {
                if (typeof g === "string") {
                    globs.push(g);
                }
            }
        } catch {
            // Malformed root package.json — treat as non-workspace.
        }
    }

    const pnpmWorkspace = path.join(root, "pnpm-workspace.yaml");
    if (fs.existsSync(pnpmWorkspace)) {
        try {
            const doc = YAML.parse(fs.readFileSync(pnpmWorkspace, "utf-8"));
            for (const g of (Array.isArray(doc?.packages) ? doc.packages : [])) {
                if (typeof g === "string") {
                    globs.push(g);
                }
            }
        } catch {
            // Malformed pnpm-workspace.yaml — ignore.
        }
    }

    return globs;
}

/**
 * Determines the project roots: when the repo is a workspace, the member directories
 * matching its globs (the root manager is excluded unless it is itself a member);
 * otherwise the shallowest package.json per branch (a nested package.json under another
 * project's directory is pruned). Returns absolute directories.
 */
function determineProjectDirs(root: string, packageJsonFiles: string[]): string[] {
    const allDirs = packageJsonFiles.map(f => path.dirname(f));
    const globs = readWorkspaceGlobs(root);

    if (globs.length > 0) {
        // Honor npm/yarn/pnpm semantics: "!"-prefixed entries subtract members, and a
        // trailing slash on a directory glob (e.g. "packages/*/") still matches the dir.
        const normalize = (g: string) => g.replace(/\/$/, "");
        const includes = globs.filter(g => !g.startsWith("!")).map(normalize);
        const excludes = globs.filter(g => g.startsWith("!")).map(g => normalize(g.slice(1)));
        const isIncluded = includes.length > 0 ? picomatch(includes) : () => false;
        const isExcluded = excludes.length > 0 ? picomatch(excludes) : () => false;
        return allDirs.filter(dir => {
            if (dir === root) {
                return false; // root manager is not itself a member
            }
            const rel = relativeNormalized(root, dir);
            return isIncluded(rel) && !isExcluded(rel);
        });
    }

    // Non-workspace: keep only dirs with no ancestor package.json dir in the set.
    const dirSet = new Set(allDirs);
    return allDirs.filter(dir => {
        let parent = path.dirname(dir);
        while (parent !== dir) {
            if (dirSet.has(parent)) {
                return false;
            }
            const next = path.dirname(parent);
            if (next === parent) {
                break;
            }
            dir = parent;
            parent = next;
        }
        return true;
    });
}

/**
 * Resolves a project's package manager: from the nearest lock file if present, else from
 * the corepack {@code packageManager} field in the nearest package.json (e.g.
 * {@code "pnpm@8.15.0"}), else Npm. {@code lockFile} is the precomputed nearest lock file.
 */
function resolvePackageManager(projectDir: string, root: string, lockFile: string | undefined): PackageManager {
    if (lockFile) {
        const fromLock = detectPackageManager(path.dirname(lockFile));
        if (fromLock) {
            return fromLock;
        }
    }
    return readCorepackPackageManager(projectDir, root) ?? PackageManager.Npm;
}

/**
 * Reads the corepack {@code packageManager} field from the nearest package.json at or
 * above {@code projectDir} (bounded by {@code root}) and maps it to a {@link PackageManager}.
 */
function readCorepackPackageManager(projectDir: string, root: string): PackageManager | undefined {
    let dir = projectDir;
    while (true) {
        const packageJson = path.join(dir, "package.json");
        if (fs.existsSync(packageJson)) {
            try {
                const spec = JSON.parse(fs.readFileSync(packageJson, "utf-8")).packageManager;
                if (typeof spec === "string") {
                    const pm = parsePackageManagerSpec(spec);
                    if (pm) {
                        return pm;
                    }
                }
            } catch {
                // Malformed package.json — keep walking up.
            }
        }
        if (dir === root) {
            return undefined;
        }
        const parent = path.dirname(dir);
        if (parent === dir) {
            return undefined;
        }
        dir = parent;
    }
}

/**
 * Parses a corepack {@code packageManager} spec ({@code "<name>@<version>"}) into a
 * {@link PackageManager}. Yarn Classic vs Berry is determined by the major version.
 */
function parsePackageManagerSpec(spec: string): PackageManager | undefined {
    const at = spec.indexOf("@");
    const name = (at >= 0 ? spec.slice(0, at) : spec).trim().toLowerCase();
    switch (name) {
        case "npm":
            return PackageManager.Npm;
        case "pnpm":
            return PackageManager.Pnpm;
        case "bun":
            return PackageManager.Bun;
        case "yarn":
            return parseInt(spec.slice(at + 1), 10) >= 2 ? PackageManager.YarnBerry : PackageManager.YarnClassic;
        default:
            return undefined;
    }
}

/**
 * Assigns each file to the project whose directory is its longest-matching ancestor.
 * Files outside every project directory are dropped.
 */
function assignFilesToProjects(projectDirs: string[], files: string[]): Map<string, string[]> {
    // Deepest project dir first so the most specific (nested) project wins.
    const byDepth = [...projectDirs].sort((a, b) => b.length - a.length);
    const result = new Map<string, string[]>(projectDirs.map(d => [d, []]));
    for (const file of files) {
        for (const dir of byDepth) {
            if (file === dir || file.startsWith(dir + path.sep)) {
                result.get(dir)!.push(file);
                break;
            }
        }
    }
    return result;
}

/**
 * Finds the nearest tsconfig.json at or above {@code projectDir} (bounded by {@code root}),
 * returned relative to {@code relativeTo} and "/"-normalized, or undefined if none.
 */
function findNearestTsconfig(projectDir: string, root: string, relativeTo: string): string | undefined {
    let dir = projectDir;
    while (true) {
        const candidate = path.join(dir, "tsconfig.json");
        if (fs.existsSync(candidate)) {
            return relativeNormalized(relativeTo, candidate);
        }
        if (dir === root) {
            return undefined;
        }
        const parent = path.dirname(dir);
        if (parent === dir) {
            return undefined;
        }
        dir = parent;
    }
}

/**
 * Collects the tsconfig.json at {@code tsconfigAbs} plus every local file in its
 * {@code extends} chain and {@code references}, into {@code collected}. Only relative
 * (local) extends/references are followed; npm-package extends (e.g. "@tsconfig/node20")
 * are skipped — they're pinned by the already-watched lock file (Decision #4). Bounded
 * to files within {@code root}; cycle-safe via {@code collected}.
 */
function collectTsconfigChain(tsconfigAbs: string, root: string, collected: Set<string>): void {
    if (collected.has(tsconfigAbs) || !isWithin(root, tsconfigAbs) || !fs.existsSync(tsconfigAbs)) {
        return;
    }
    collected.add(tsconfigAbs);

    const {config} = ts.readConfigFile(tsconfigAbs, p => ts.sys.readFile(p));
    if (!config) {
        return;
    }
    const dir = path.dirname(tsconfigAbs);

    const extendsField: unknown = config.extends;
    const extendsList = Array.isArray(extendsField) ? extendsField : extendsField != null ? [extendsField] : [];
    for (const ext of extendsList) {
        if (typeof ext === "string" && (ext.startsWith("./") || ext.startsWith("../"))) {
            let target = path.resolve(dir, ext);
            if (!target.endsWith(".json")) {
                target += ".json";
            }
            collectTsconfigChain(target, root, collected);
        }
    }

    for (const ref of (Array.isArray(config.references) ? config.references : [])) {
        const refPath: unknown = ref?.path;
        if (typeof refPath === "string") {
            // TypeScript's rule: a reference path ending in ".json" is the config file;
            // otherwise it names a directory whose tsconfig.json is the target.
            let target = path.resolve(dir, refPath);
            if (!target.endsWith(".json")) {
                target = path.join(target, "tsconfig.json");
            }
            collectTsconfigChain(target, root, collected);
        }
    }
}

/** True when {@code target} is {@code root} or nested below it. */
function isWithin(root: string, target: string): boolean {
    return target === root || target.startsWith(root + path.sep);
}

/**
 * Resolves the config-input watch-set for a project: its package.json, the (precomputed)
 * nearest lock file, the local tsconfig extends chain + references, and any
 * prettier/jest/vitest config present. Files that don't exist are omitted. Returned
 * relative to {@code relativeTo}, "/"-normalized and deduped.
 */
function resolveConfigInputs(projectDir: string, root: string, relativeTo: string, lockFile: string | undefined): string[] {
    const abs = new Set<string>();

    const packageJson = path.join(projectDir, "package.json");
    if (fs.existsSync(packageJson)) {
        abs.add(packageJson);
    }

    if (lockFile) {
        abs.add(lockFile);
    }

    collectTsconfigChain(path.join(projectDir, "tsconfig.json"), root, abs);

    for (const name of [...PRETTIER_CONFIG_FILES, ...JEST_CONFIG_FILES, ...VITEST_CONFIG_FILES]) {
        const candidate = path.join(projectDir, name);
        if (fs.existsSync(candidate)) {
            abs.add(candidate);
        }
    }

    return [...abs].map(p => relativeNormalized(relativeTo, p));
}

/** Finds the nearest lock file at or above {@code projectDir} (bounded by {@code root}). */
function findNearestLockFile(projectDir: string, root: string): string | undefined {
    const lockFileNames = getAllLockFileNames();
    let dir = projectDir;
    while (true) {
        for (const name of lockFileNames) {
            const candidate = path.join(dir, name);
            if (fs.existsSync(candidate)) {
                return candidate;
            }
        }
        if (dir === root) {
            return undefined;
        }
        const parent = path.dirname(dir);
        if (parent === dir) {
            return undefined;
        }
        dir = parent;
    }
}

/**
 * Partitions a project's source files into a "main" set and, when test files are
 * present by convention, a "test" set. The two sets share v1 {@code parserSettings};
 * the list shape lets prod/test settings diverge additively later (Decision #6).
 */
function partitionSourceSets(
    projectDir: string,
    projectFiles: string[],
    parserSettings: ParserSettings
): SourceSetDescriptor[] {
    const hasTests = projectFiles.some(file => isTestFile(relativeNormalized(projectDir, file)));

    if (!hasTests) {
        return [{name: "main", parserSettings}];
    }
    return [
        {name: "main", excludes: [...TEST_GLOBS], parserSettings},
        {name: "test", includes: [...TEST_GLOBS], parserSettings}
    ];
}

/**
 * Discovers the independent projects in a repository (or partition) for the Prebuild
 * RPC: workspace-aware project roots, their main/test source sets, the config-input
 * watch-set, and the parser settings each set needs.
 *
 * v1 is read-only — no dependency install, no NodeResolutionResult attachment.
 */
export async function discoverProjects(
    repositoryRoot: string,
    options: WorkspaceDiscoveryOptions = {}
): Promise<ProjectDescriptor[]> {
    const root = path.resolve(repositoryRoot);
    const relativeTo = options.relativeTo ? path.resolve(options.relativeTo) : root;

    const parser = new ProjectParser(root, {exclusions: options.exclusions});
    const discovered = await parser.discoverFiles();

    const projectDirs = determineProjectDirs(root, discovered.packageJsonFiles);
    const filesByProject = assignFilesToProjects(projectDirs, discovered.jsFiles);

    const projects: ProjectDescriptor[] = [];
    for (const projectDir of projectDirs) {
        const lockFile = findNearestLockFile(projectDir, root);
        const tsconfigPath = findNearestTsconfig(projectDir, root, relativeTo);
        const parserSettings: ParserSettings = tsconfigPath ? {tsconfigPath} : {};
        projects.push({
            path: relativeNormalized(relativeTo, projectDir),
            packageManager: resolvePackageManager(projectDir, root, lockFile),
            configInputs: resolveConfigInputs(projectDir, root, relativeTo, lockFile),
            sourceSets: partitionSourceSets(projectDir, filesByProject.get(projectDir) ?? [], parserSettings),
            resolution: null
        });
    }
    return projects;
}
