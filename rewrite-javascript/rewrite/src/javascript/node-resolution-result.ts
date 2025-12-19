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
import {findMarker, Marker, Markers} from "../markers";
import {randomId, UUID} from "../uuid";
import {asRef} from "../reference";
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc/queue";
import {castDraft, createDraft, finishDraft} from "immer";
import * as semver from "semver";
import * as fsp from "fs/promises";
import * as path from "path";
import {homedir} from "os";

export const NodeResolutionResultKind = "org.openrewrite.javascript.marker.NodeResolutionResult" as const;
export const DependencyKind = "org.openrewrite.javascript.marker.NodeResolutionResult$Dependency" as const;
export const ResolvedDependencyKind = "org.openrewrite.javascript.marker.NodeResolutionResult$ResolvedDependency" as const;

/**
 * Parsed package.json content structure.
 */
export interface PackageJsonContent {
    readonly name?: string;
    readonly version?: string;
    readonly description?: string;
    readonly dependencies?: Record<string, string>;
    readonly devDependencies?: Record<string, string>;
    readonly peerDependencies?: Record<string, string>;
    readonly optionalDependencies?: Record<string, string>;
    readonly bundledDependencies?: string[];
    readonly bundleDependencies?: string[];  // Legacy alias
    readonly engines?: Record<string, string>;
}

/**
 * Package entry in a package-lock.json packages map.
 */
export interface PackageLockEntry {
    readonly version?: string;
    readonly resolved?: string;
    readonly integrity?: string;
    readonly license?: string;
    readonly dependencies?: Record<string, string>;
    readonly devDependencies?: Record<string, string>;
    readonly peerDependencies?: Record<string, string>;
    readonly optionalDependencies?: Record<string, string>;
    readonly engines?: Record<string, string> | string[];  // Can be legacy array format
}

/**
 * Parsed package-lock.json content structure (npm lockfile v3 format).
 */
export interface PackageLockContent {
    readonly name?: string;
    readonly version?: string;
    readonly lockfileVersion?: number;
    readonly packages?: Record<string, PackageLockEntry>;
}

/**
 * Represents the package manager used by a Node.js project.
 */
export const enum PackageManager {
    Npm = 'Npm',
    YarnClassic = 'YarnClassic',
    YarnBerry = 'YarnBerry',
    Pnpm = 'Pnpm',
    Bun = 'Bun',
}

/**
 * Represents the scope/source of an npmrc configuration.
 * Listed from lowest to highest priority.
 */
export const enum NpmrcScope {
    Global = 'Global',       // $PREFIX/etc/npmrc
    User = 'User',           // $HOME/.npmrc
    Project = 'Project',     // .npmrc in project root
}

/**
 * Represents a dependency scope in package.json that uses object structure {name: version}.
 * Note: `bundledDependencies` is excluded because it's a string[] of package names, not a version map.
 */
export type DependencyScope = 'dependencies' | 'devDependencies' | 'peerDependencies' | 'optionalDependencies';

/**
 * All dependency scopes in package.json that use object structure {name: version}.
 */
export const allDependencyScopes: readonly DependencyScope[] = [
    'dependencies',
    'devDependencies',
    'peerDependencies',
    'optionalDependencies'
] as const;

export const NpmrcKind = "org.openrewrite.javascript.marker.NodeResolutionResult$Npmrc" as const;

/**
 * Represents npm configuration from a specific scope.
 * Multiple Npmrc objects can be collected (one per scope) to allow
 * recipes to merge configurations or modify specific scopes.
 */
export interface Npmrc {
    readonly kind: typeof NpmrcKind;
    readonly scope: NpmrcScope;
    readonly properties: Record<string, string>;
}

/**
 * Represents a dependency request as declared in package.json.
 * This is what a package asks for (name + version constraint).
 *
 * When the same name+versionConstraint appears multiple times, the same
 * Dependency instance is reused. This enables reference deduplication
 * during RPC serialization via asRef().
 */
export interface Dependency {
    readonly kind: typeof DependencyKind;
    readonly name: string;              // Package name (e.g., "react")
    readonly versionConstraint: string; // Version constraint (e.g., "^18.2.0")
    readonly resolved?: ResolvedDependency; // The resolved version of this dependency
}

/**
 * Represents a resolved dependency from package-lock.json.
 * This is what was actually installed (name + resolved version + its own dependencies).
 *
 * Each ResolvedDependency's dependency arrays contain Dependency objects (requests),
 * which can be looked up in NodeResolutionResult.resolvedDependencies to find their resolved versions.
 */
export interface ResolvedDependency {
    readonly kind: typeof ResolvedDependencyKind;
    readonly name: string;    // Package name (e.g., "react")
    readonly version: string; // Actual resolved version (e.g., "18.3.1")

    // This package's own dependency requests
    readonly dependencies?: Dependency[];
    readonly devDependencies?: Dependency[];
    readonly peerDependencies?: Dependency[];
    readonly optionalDependencies?: Dependency[];

    // Node/npm version requirements for this package
    readonly engines?: Record<string, string>;

    // SPDX license identifier (e.g., "MIT", "Apache-2.0")
    readonly license?: string;
}

/**
 * Contains metadata about a Node.js project, parsed from package.json and package-lock.json.
 * Attached as a marker to JS.CompilationUnit to provide dependency context for recipes.
 *
 * Similar to MavenResolutionResult marker, this allows recipes to:
 * - Query project dependencies
 * - Check if specific packages are in use
 * - Modify dependencies programmatically
 * - Understand the project structure
 *
 * The model separates requests (Dependency) from resolutions (ResolvedDependency):
 * - The dependency arrays contain Dependency objects (what was requested)
 * - The resolvedDependencies list contains what was actually installed
 */
export interface NodeResolutionResult extends Marker {
    readonly kind: typeof NodeResolutionResultKind;
    readonly id: UUID;

    // Project metadata from package.json
    readonly name?: string;           // Project name
    readonly version?: string;        // Project version
    readonly description?: string;    // Project description
    readonly path: string;            // Path to the package.json file

    // Paths to workspace package.json files (only populated on workspace root)
    readonly workspacePackagePaths?: string[];

    // Dependency requests organized by scope (from package.json)
    readonly dependencies: Dependency[];          // Regular dependencies
    readonly devDependencies: Dependency[];       // Development dependencies
    readonly peerDependencies: Dependency[];      // Peer dependencies
    readonly optionalDependencies: Dependency[];  // Optional dependencies
    readonly bundledDependencies: Dependency[];   // Bundled dependencies

    // Resolved dependencies from lock file - what was actually installed
    // Use getAllResolvedVersions() to look up by name (handles multiple versions)
    readonly resolvedDependencies: ResolvedDependency[];

    // The package manager used by the project (npm, yarn, pnpm, etc.)
    readonly packageManager?: PackageManager;

    // Node/npm version requirements
    readonly engines?: Record<string, string>;

    // npm configuration from various scopes (global, user, project, env)
    readonly npmrcConfigs?: Npmrc[];
}

/**
 * Creates a NodeResolutionResult marker from a package.json file.
 * Should be called during parsing to attach to JS.CompilationUnit.
 *
 * All Dependency instances are wrapped with asRef() to enable
 * reference deduplication during RPC serialization.
 *
 * @param path Path to the package.json file
 * @param packageJsonContent Parsed package.json content
 * @param packageLockContent Optional parsed package-lock.json content for resolution info
 * @param workspacePackagePaths Optional resolved paths to workspace package.json files (only for workspace root)
 * @param packageManager Optional package manager that was detected from lock file
 * @param npmrcConfigs Optional npm configuration from various scopes
 */
export function createNodeResolutionResultMarker(
    path: string,
    packageJsonContent: PackageJsonContent,
    packageLockContent?: PackageLockContent,
    workspacePackagePaths?: string[],
    packageManager?: PackageManager,
    npmrcConfigs?: Npmrc[]
): NodeResolutionResult {
    // Cache for deduplicating resolved dependencies with the same name+version.
    const resolvedDependencyCache = new Map<string, ResolvedDependency>();

    // Index from package name to all resolved versions (for O(1) semver fallback lookup)
    const nameToResolved = new Map<string, ResolvedDependency[]>();

    // Map from lock file path to ResolvedDependency for path-based lookups.
    // e.g., "node_modules/is-odd" -> ResolvedDependency for is-odd@3.0.1
    const pathToResolved = new Map<string, ResolvedDependency>();

    // Cache for deduplicating dependencies with the same name+versionConstraint+contextPath.
    const dependencyCache = new Map<string, Dependency>();

    /**
     * Normalizes the engines field from package-lock.json.
     * Some older packages have engines as an array like ["node >=0.6.0"] instead of
     * the standard object format {"node": ">=0.6.0"}.
     */
    function normalizeEngines(engines?: Record<string, string> | string[]): Record<string, string> | undefined {
        if (!engines) return undefined;
        if (Array.isArray(engines)) {
            // Convert array format to object format
            // e.g., ["node >=0.6.0"] -> {"node": ">=0.6.0"}
            const result: Record<string, string> = {};
            for (const entry of engines) {
                const spaceIdx = entry.indexOf(' ');
                if (spaceIdx > 0) {
                    const key = entry.substring(0, spaceIdx);
                    result[key] = entry.substring(spaceIdx + 1);
                }
            }
            return Object.keys(result).length > 0 ? result : undefined;
        }
        return engines;
    }

    /**
     * Extracts package name and optionally version from a package-lock.json path.
     * e.g., "node_modules/@babel/core" -> { name: "@babel/core" }
     * e.g., "node_modules/foo/node_modules/bar" -> { name: "bar" }
     * e.g., "node_modules/is-odd@3.0.1" -> { name: "is-odd", version: "3.0.1" }
     */
    function extractPackageInfo(pkgPath: string): { name: string; version?: string } {
        // For nested packages, we want the last package name
        const nodeModulesIndex = pkgPath.lastIndexOf('node_modules/');
        if (nodeModulesIndex === -1) return { name: pkgPath };

        let nameWithVersion = pkgPath.slice(nodeModulesIndex + 'node_modules/'.length);

        // Check if the path has a version suffix (e.g., "is-odd@3.0.1")
        // Handle scoped packages (@scope/name@version) correctly
        const atIndex = nameWithVersion.lastIndexOf('@');
        if (atIndex > 0 && !nameWithVersion.substring(0, atIndex).includes('/')) {
            // Not a scoped package, the @ is a version separator
            return {
                name: nameWithVersion.substring(0, atIndex),
                version: nameWithVersion.substring(atIndex + 1)
            };
        } else if (atIndex > 0) {
            // Could be scoped package with version: @scope/name@version
            const parts = nameWithVersion.split('@');
            if (parts.length === 3 && parts[0] === '') {
                // @scope/name@version -> ["", "scope/name", "version"]
                return {
                    name: `@${parts[1]}`,
                    version: parts[2]
                };
            }
        }

        return { name: nameWithVersion };
    }

    /**
     * Creates or retrieves a ResolvedDependency from the cache.
     * This ensures the same resolved package is reused across the dependency tree.
     * Also maintains the nameToResolved index for O(1) name lookups.
     */
    function getOrCreateResolvedDependency(
        name: string,
        version: string,
        pkgEntry?: PackageLockEntry
    ): ResolvedDependency {
        const key = `${name}@${version}`;
        let resolved = resolvedDependencyCache.get(key);
        if (!resolved) {
            // Create a placeholder first - dependencies will be populated later
            resolved = asRef({
                kind: ResolvedDependencyKind,
                name,
                version,
                dependencies: undefined,
                devDependencies: undefined,
                peerDependencies: undefined,
                optionalDependencies: undefined,
                engines: normalizeEngines(pkgEntry?.engines),
                license: pkgEntry?.license,
            });
            resolvedDependencyCache.set(key, resolved);

            // Maintain name index for O(1) lookup during semver fallback
            const existing = nameToResolved.get(name);
            if (existing) {
                existing.push(resolved);
            } else {
                nameToResolved.set(name, [resolved]);
            }
        }
        return resolved;
    }

    /**
     * Resolves a dependency name from a given context path using Node.js-style resolution.
     * Looks for the package in nested node_modules first, then walks up to parent directories.
     * Falls back to semver matching when path-based resolution fails (e.g., for yarn/pnpm).
     *
     * @param name Package name to resolve
     * @param versionConstraint Version constraint (e.g., "^3.0.1") for semver fallback
     * @param contextPath The path of the parent package (e.g., "node_modules/is-even")
     *                    Use "" for root-level dependencies
     */
    function resolveFromContext(
        name: string,
        versionConstraint: string,
        contextPath: string
    ): ResolvedDependency | undefined {
        // Start from the context path and walk up looking for the package
        let currentPath = contextPath;

        while (true) {
            // Try to find the package in node_modules at this level
            const candidatePath = currentPath
                ? `${currentPath}/node_modules/${name}`
                : `node_modules/${name}`;

            const resolved = pathToResolved.get(candidatePath);
            if (resolved) {
                return resolved;
            }

            // Walk up to parent directory
            if (!currentPath) {
                break; // Already at root
            }

            // Remove the last /node_modules/pkg segment to go up one level
            const lastNodeModules = currentPath.lastIndexOf('/node_modules/');
            if (lastNodeModules === -1) {
                currentPath = ''; // Try root level next
            } else {
                currentPath = currentPath.substring(0, lastNodeModules);
            }
        }

        // Fallback: use semver matching to find a version that satisfies the constraint
        // This is needed for yarn/pnpm which don't encode nesting in their lock files
        const candidates = nameToResolved.get(name);

        if (!candidates || candidates.length === 0) {
            return undefined;
        }

        if (candidates.length === 1) {
            return candidates[0];
        }

        // Multiple versions - use semver to find the best match
        // First try to find one that satisfies the constraint
        for (const candidate of candidates) {
            try {
                if (semver.satisfies(candidate.version, versionConstraint)) {
                    return candidate;
                }
            } catch {
                // Invalid semver, skip this candidate for matching
            }
        }

        // If no exact match, return the highest version as fallback (O(n) linear scan)
        let maxCandidate = candidates[0];
        for (let i = 1; i < candidates.length; i++) {
            try {
                if (semver.compare(candidates[i].version, maxCandidate.version) > 0) {
                    maxCandidate = candidates[i];
                }
            } catch {
                // Invalid semver, skip comparison
            }
        }
        return maxCandidate;
    }

    /**
     * Creates or retrieves a Dependency from the cache.
     * Links to resolved dependency using path-based Node.js-style resolution.
     *
     * @param name Package name
     * @param versionConstraint Version constraint from package.json
     * @param contextPath The path context for resolution (parent package path)
     */
    function getOrCreateDependency(
        name: string,
        versionConstraint: string,
        contextPath: string
    ): Dependency {
        // Resolve first to determine the actual resolved version
        const resolved = resolveFromContext(name, versionConstraint, contextPath);

        // Key by name, constraint, and resolved version (not context path).
        // This allows sharing Dependency objects when different contexts resolve to the same version.
        const resolvedKey = resolved ? `${resolved.name}@${resolved.version}` : 'unresolved';
        const key = `${name}@${versionConstraint}@${resolvedKey}`;

        let dep = dependencyCache.get(key);
        if (!dep) {
            dep = asRef({
                kind: DependencyKind,
                name,
                versionConstraint,
                resolved,
            });
            dependencyCache.set(key, dep);
        }
        return dep;
    }

    /**
     * Parses dependencies from a Record, using the given context path for resolution.
     */
    function parseDependencies(
        deps: Record<string, string> | undefined,
        contextPath: string
    ): Dependency[] {
        if (!deps) return [];
        return Object.entries(deps).map(([name, versionConstraint]) =>
            getOrCreateDependency(name, versionConstraint, contextPath)
        );
    }

    /**
     * Parses bundled dependencies (array of names with no version constraint).
     */
    function parseBundledDependencies(
        deps: string[] | undefined,
        contextPath: string
    ): Dependency[] {
        if (!deps) return [];
        return deps.map(name => getOrCreateDependency(name, '*', contextPath));
    }

    /**
     * Parses package-lock.json to build a list of resolved dependencies.
     * Two-pass approach:
     * 1. First pass: Create all ResolvedDependency objects and build path->resolved map
     * 2. Second pass: Populate dependencies using path-based resolution
     */
    function parseResolutions(
        lockContent: PackageLockContent
    ): ResolvedDependency[] {
        if (!lockContent.packages) return [];

        const packages = lockContent.packages;

        // First pass: Create all ResolvedDependency placeholders and build path map
        const packageInfos: Array<{path: string; name: string; version: string; entry: PackageLockEntry}> = [];
        for (const [pkgPath, pkgEntry] of Object.entries(packages)) {
            // Skip the root package (empty string key)
            if (pkgPath === '') continue;

            const pkgInfo = extractPackageInfo(pkgPath);
            const name = pkgInfo.name;
            // Use version from path if available, otherwise from entry
            const version = pkgInfo.version || pkgEntry.version;

            if (name && version) {
                const resolved = getOrCreateResolvedDependency(name, version, pkgEntry);
                pathToResolved.set(pkgPath, resolved);
                packageInfos.push({path: pkgPath, name, version, entry: pkgEntry});
            }
        }

        // Second pass: Populate dependencies using path-based resolution.
        // Note: Using castDraft here is safe because all objects are created within this
        // parsing context and haven't been returned to callers yet. The objects in
        // resolvedDependencyCache are plain JS objects marked with asRef() for RPC
        // reference deduplication, not frozen Immer drafts.
        for (const {path: pkgPath, name, version, entry} of packageInfos) {
            const key = `${name}@${version}`;
            const resolved = resolvedDependencyCache.get(key);
            if (resolved) {
                const mutableResolved = castDraft(resolved);
                if (entry.dependencies) {
                    mutableResolved.dependencies = parseDependencies(entry.dependencies, pkgPath);
                }
                if (entry.devDependencies) {
                    mutableResolved.devDependencies = parseDependencies(entry.devDependencies, pkgPath);
                }
                if (entry.peerDependencies) {
                    mutableResolved.peerDependencies = parseDependencies(entry.peerDependencies, pkgPath);
                }
                if (entry.optionalDependencies) {
                    mutableResolved.optionalDependencies = parseDependencies(entry.optionalDependencies, pkgPath);
                }
            }
        }

        // Return all unique resolved dependencies
        return Array.from(resolvedDependencyCache.values());
    }

    // Parse resolved dependencies first (before dependencies) so we can link them
    const resolvedDependencies = packageLockContent ? parseResolutions(packageLockContent) : [];

    // Now parse dependencies from package.json - they resolve from root context ("")
    const dependencies = parseDependencies(packageJsonContent.dependencies, '');
    const devDependencies = parseDependencies(packageJsonContent.devDependencies, '');
    const peerDependencies = parseDependencies(packageJsonContent.peerDependencies, '');
    const optionalDependencies = parseDependencies(packageJsonContent.optionalDependencies, '');
    const bundledDependencies = parseBundledDependencies(
        packageJsonContent.bundledDependencies || packageJsonContent.bundleDependencies,
        ''
    );

    return {
        kind: NodeResolutionResultKind,
        id: randomId(),
        name: packageJsonContent.name,
        version: packageJsonContent.version,
        description: packageJsonContent.description,
        path,
        workspacePackagePaths,
        dependencies,
        devDependencies,
        peerDependencies,
        optionalDependencies,
        bundledDependencies,
        resolvedDependencies,
        packageManager,
        engines: packageJsonContent.engines,
        npmrcConfigs,
    };
}

/**
 * Helper function to find a NodeResolutionResult marker on a compilation unit.
 */
export function findNodeResolutionResult(cu: { markers: Markers }): NodeResolutionResult | undefined {
    return findMarker<NodeResolutionResult>(cu, NodeResolutionResultKind);
}

/**
 * Parses an .npmrc file content into a key-value map.
 *
 * .npmrc format:
 * - Lines are key=value pairs
 * - Lines starting with # or ; are comments
 * - Empty lines are ignored
 * - Values can contain ${VAR} or ${VAR:-default} for env variable substitution
 */
function parseNpmrc(content: string): Record<string, string> {
    const properties: Record<string, string> = {};

    for (const line of content.split('\n')) {
        const trimmed = line.trim();

        // Skip comments and empty lines
        if (!trimmed || trimmed.startsWith('#') || trimmed.startsWith(';')) {
            continue;
        }

        // Parse key=value
        const eqIndex = trimmed.indexOf('=');
        if (eqIndex === -1) continue;

        const key = trimmed.substring(0, eqIndex).trim();
        const value = trimmed.substring(eqIndex + 1).trim();

        if (key) {
            properties[key] = value;
        }
    }

    return properties;
}

/**
 * Helper to check if a file exists asynchronously.
 */
async function fileExists(filePath: string): Promise<boolean> {
    try {
        await fsp.access(filePath);
        return true;
    } catch {
        return false;
    }
}

/**
 * Reads .npmrc configurations from all scope levels.
 * Returns an array of Npmrc objects, one for each scope that has configuration.
 *
 * Scopes (from lowest to highest priority):
 * - Global: $PREFIX/etc/npmrc (npm's installation directory)
 * - User: $HOME/.npmrc (user's home directory)
 * - Project: .npmrc in project root (sibling of package.json)
 * - Env: npm_config_* environment variables
 *
 * @param projectDir The project directory containing package.json
 * @returns Promise resolving to array of Npmrc objects for each scope with configuration
 */
export async function readNpmrcConfigs(projectDir: string): Promise<Npmrc[]> {
    const configs: Npmrc[] = [];

    // 1. Global config: $PREFIX/etc/npmrc
    // Try to get npm prefix from npm itself, fall back to common locations
    const globalNpmrcPaths = [
        // Try NPM_CONFIG_GLOBALCONFIG env var first
        process.env.NPM_CONFIG_GLOBALCONFIG,
        // Common global locations
        '/usr/local/etc/npmrc',
        '/etc/npmrc',
    ].filter(Boolean) as string[];

    // Also try to detect from npm prefix if node is installed
    const nodeDir = process.execPath ? path.dirname(path.dirname(process.execPath)) : undefined;
    if (nodeDir) {
        globalNpmrcPaths.unshift(path.join(nodeDir, 'etc', 'npmrc'));
    }

    for (const globalPath of globalNpmrcPaths) {
        if (await fileExists(globalPath)) {
            try {
                const content = await fsp.readFile(globalPath, 'utf-8');
                const properties = parseNpmrc(content);
                if (Object.keys(properties).length > 0) {
                    configs.push({
                        kind: NpmrcKind,
                        scope: NpmrcScope.Global,
                        properties
                    });
                    break; // Only use the first found global config
                }
            } catch {
                // Ignore read errors
            }
        }
    }

    // 2. User config: $HOME/.npmrc
    const userNpmrcPath = process.env.NPM_CONFIG_USERCONFIG || path.join(homedir(), '.npmrc');
    if (await fileExists(userNpmrcPath)) {
        try {
            const content = await fsp.readFile(userNpmrcPath, 'utf-8');
            const properties = parseNpmrc(content);
            if (Object.keys(properties).length > 0) {
                configs.push({
                    kind: NpmrcKind,
                    scope: NpmrcScope.User,
                    properties
                });
            }
        } catch {
            // Ignore read errors
        }
    }

    // 3. Project config: .npmrc in project root
    const projectNpmrcPath = path.join(projectDir, '.npmrc');
    if (await fileExists(projectNpmrcPath)) {
        try {
            const content = await fsp.readFile(projectNpmrcPath, 'utf-8');
            const properties = parseNpmrc(content);
            if (Object.keys(properties).length > 0) {
                configs.push({
                    kind: NpmrcKind,
                    scope: NpmrcScope.Project,
                    properties
                });
            }
        } catch {
            // Ignore read errors
        }
    }

    // Note: We intentionally don't capture npm_config_* environment variables.
    // While users can set config via env vars, npm also automatically injects
    // many npm_config_* vars when running child processes, and there's no way
    // to distinguish user-set vars from npm-injected ones at runtime.

    return configs;
}

/**
 * Helper functions for querying dependencies
 */
export namespace NodeResolutionResultQueries {

    /**
     * Get all dependency requests from all scopes.
     */
    export function getAllDependencies(project: NodeResolutionResult): Dependency[] {
        return [
            ...project.dependencies,
            ...project.devDependencies,
            ...project.peerDependencies,
            ...project.optionalDependencies,
            ...project.bundledDependencies
        ];
    }

    /**
     * Check if project has a specific dependency request, optionally filtered by scope.
     */
    export function hasDependency(
        project: NodeResolutionResult,
        packageName: string,
        scope?: 'dependencies' | 'devDependencies' | 'peerDependencies' | 'optionalDependencies' | 'bundledDependencies'
    ): boolean {
        const deps = scope
            ? project[scope]
            : getAllDependencies(project);
        return deps.some(dep => dep.name === packageName);
    }

    /**
     * Find a specific dependency request by name across all scopes.
     */
    export function findDependency(
        project: NodeResolutionResult,
        packageName: string
    ): Dependency | undefined {
        return getAllDependencies(project).find(dep => dep.name === packageName);
    }

    /**
     * Get all dependency requests matching a predicate.
     */
    export function findDependencies(
        project: NodeResolutionResult,
        predicate: (dep: Dependency) => boolean
    ): Dependency[] {
        return getAllDependencies(project).filter(predicate);
    }

    /**
     * Get all resolved dependencies with a specific name (handles multiple versions).
     * Returns an empty array if no versions are found.
     *
     * For navigation, prefer using the Dependency.resolved property:
     * @example
     * const express = project.dependencies.find(d => d.name === 'express')?.resolved;
     * const accepts = express?.dependencies?.find(d => d.name === 'accepts')?.resolved;
     */
    export function getAllResolvedVersions(
        project: NodeResolutionResult,
        packageName: string
    ): ResolvedDependency[] {
        return project.resolvedDependencies.filter(r => r.name === packageName);
    }
}

/**
 * Register RPC codec for Npmrc.
 */
RpcCodecs.registerCodec(NpmrcKind, {
    async rpcReceive(before: Npmrc, q: RpcReceiveQueue): Promise<Npmrc> {
        const draft = createDraft(before);
        draft.kind = NpmrcKind;
        draft.scope = await q.receive(before.scope);
        draft.properties = await q.receive(before.properties);

        return finishDraft(draft) as Npmrc;
    },

    async rpcSend(after: Npmrc, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.scope);
        await q.getAndSend(after, a => a.properties);
    }
});

/**
 * Register RPC codec for Dependency.
 */
RpcCodecs.registerCodec(DependencyKind, {
    async rpcReceive(before: Dependency, q: RpcReceiveQueue): Promise<Dependency> {
        const draft = createDraft(before);
        draft.kind = DependencyKind;
        draft.name = await q.receive(before.name);
        draft.versionConstraint = await q.receive(before.versionConstraint);
        draft.resolved = await q.receive(before.resolved);

        return finishDraft(draft) as Dependency;
    },

    async rpcSend(after: Dependency, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.name);
        await q.getAndSend(after, a => a.versionConstraint);
        await q.getAndSend(after, a => a.resolved);
    }
});

/**
 * Register RPC codec for ResolvedDependency.
 */
RpcCodecs.registerCodec(ResolvedDependencyKind, {
    async rpcReceive(before: ResolvedDependency, q: RpcReceiveQueue): Promise<ResolvedDependency> {
        const draft = createDraft(before);
        draft.kind = ResolvedDependencyKind;
        draft.name = await q.receive(before.name);
        draft.version = await q.receive(before.version);
        draft.dependencies = (await q.receiveList(before.dependencies)) || undefined;
        draft.devDependencies = (await q.receiveList(before.devDependencies)) || undefined;
        draft.peerDependencies = (await q.receiveList(before.peerDependencies)) || undefined;
        draft.optionalDependencies = (await q.receiveList(before.optionalDependencies)) || undefined;
        draft.engines = await q.receive(before.engines);
        draft.license = await q.receive(before.license);

        return finishDraft(draft) as ResolvedDependency;
    },

    async rpcSend(after: ResolvedDependency, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.name);
        await q.getAndSend(after, a => a.version);
        await q.getAndSendList(after, a => (a.dependencies || []).map(d => asRef(d)),
            dep => `${dep.name}@${dep.versionConstraint}`);
        await q.getAndSendList(after, a => (a.devDependencies || []).map(d => asRef(d)),
            dep => `${dep.name}@${dep.versionConstraint}`);
        await q.getAndSendList(after, a => (a.peerDependencies || []).map(d => asRef(d)),
            dep => `${dep.name}@${dep.versionConstraint}`);
        await q.getAndSendList(after, a => (a.optionalDependencies || []).map(d => asRef(d)),
            dep => `${dep.name}@${dep.versionConstraint}`);
        await q.getAndSend(after, a => a.engines);
        await q.getAndSend(after, a => a.license);
    }
});

/**
 * Register RPC codec for NodeResolutionResult marker.
 * This handles serialization/deserialization for communication between JS and Java.
 */
RpcCodecs.registerCodec(NodeResolutionResultKind, {
    async rpcReceive(before: NodeResolutionResult, q: RpcReceiveQueue): Promise<NodeResolutionResult> {
        const draft = createDraft(before);
        draft.id = await q.receive(before.id);
        draft.name = await q.receive(before.name);
        draft.version = await q.receive(before.version);
        draft.description = await q.receive(before.description);
        draft.path = await q.receive(before.path);
        draft.workspacePackagePaths = await q.receive(before.workspacePackagePaths);

        draft.dependencies = (await q.receiveList(before.dependencies)) || [];
        draft.devDependencies = (await q.receiveList(before.devDependencies)) || [];
        draft.peerDependencies = (await q.receiveList(before.peerDependencies)) || [];
        draft.optionalDependencies = (await q.receiveList(before.optionalDependencies)) || [];
        draft.bundledDependencies = (await q.receiveList(before.bundledDependencies)) || [];
        draft.resolvedDependencies = (await q.receiveList(before.resolvedDependencies)) || [];

        draft.packageManager = await q.receive(before.packageManager);
        draft.engines = await q.receive(before.engines);
        draft.npmrcConfigs = (await q.receiveList(before.npmrcConfigs)) || undefined;

        return finishDraft(draft) as NodeResolutionResult;
    },

    async rpcSend(after: NodeResolutionResult, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.name);
        await q.getAndSend(after, a => a.version);
        await q.getAndSend(after, a => a.description);
        await q.getAndSend(after, a => a.path);
        await q.getAndSend(after, a => a.workspacePackagePaths);

        await q.getAndSendList(after, a => a.dependencies.map(d => asRef(d)),
            dep => `${dep.name}@${dep.versionConstraint}`);
        await q.getAndSendList(after, a => a.devDependencies.map(d => asRef(d)),
            dep => `${dep.name}@${dep.versionConstraint}`);
        await q.getAndSendList(after, a => a.peerDependencies.map(d => asRef(d)),
            dep => `${dep.name}@${dep.versionConstraint}`);
        await q.getAndSendList(after, a => a.optionalDependencies.map(d => asRef(d)),
            dep => `${dep.name}@${dep.versionConstraint}`);
        await q.getAndSendList(after, a => a.bundledDependencies.map(d => asRef(d)),
            dep => `${dep.name}@${dep.versionConstraint}`);
        await q.getAndSendList(after, a => a.resolvedDependencies.map(r => asRef(r)),
            resolved => `${resolved.name}@${resolved.version}`);

        await q.getAndSend(after, a => a.packageManager);
        await q.getAndSend(after, a => a.engines);
        await q.getAndSendList(after, a => a.npmrcConfigs || [],
            npmrc => npmrc.scope);
    }
});
