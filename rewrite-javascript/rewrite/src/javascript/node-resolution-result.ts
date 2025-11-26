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
import {Marker, Markers, findMarker} from "../markers";
import {randomId, UUID} from "../uuid";
import {asRef} from "../reference";
import {RpcCodecs, RpcReceiveQueue, RpcSendQueue} from "../rpc";
import {createDraft, finishDraft} from "immer";

export const NodeResolutionResultKind = "org.openrewrite.javascript.marker.NodeResolutionResult" as const;
export const DependencyKind = "org.openrewrite.javascript.marker.NodeResolutionResult$Dependency" as const;
export const ResolvedDependencyKind = "org.openrewrite.javascript.marker.NodeResolutionResult$ResolvedDependency" as const;

/**
 * Represents the package manager used by a Node.js project.
 */
export const enum PackageManager {
    Npm = 'Npm',
    Yarn = 'Yarn',
    Pnpm = 'Ppm',
    Bun = 'Bun',
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

    // Resolved dependencies from package-lock.json - what was actually installed
    // Use getResolvedDependency() helper to look up by name
    readonly resolvedDependencies: ResolvedDependency[];

    // The package manager used by the project (npm, yarn, pnpm, etc.)
    readonly packageManager?: PackageManager;

    // Node/npm version requirements
    readonly engines?: Record<string, string>;
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
 */
export function createNodeResolutionResultMarker(
    path: string,
    packageJsonContent: any,
    packageLockContent?: any,
    workspacePackagePaths?: string[]
): NodeResolutionResult {
    // Cache for deduplicating dependencies with the same name+versionConstraint.
    const dependencyCache = new Map<string, Dependency>();
    // Cache for deduplicating resolved dependencies with the same name+version.
    const resolvedDependencyCache = new Map<string, ResolvedDependency>();

    function getOrCreateDependency(
        name: string,
        versionConstraint: string
    ): Dependency {
        const key = `${name}@${versionConstraint}`;
        let dep = dependencyCache.get(key);
        if (!dep) {
            dep = asRef({
                kind: DependencyKind,
                name,
                versionConstraint,
            });
            dependencyCache.set(key, dep);
        }
        return dep;
    }

    function parseDependencies(
        deps: Record<string, string> | undefined
    ): Dependency[] {
        if (!deps) return [];
        return Object.entries(deps).map(([name, versionConstraint]) =>
            getOrCreateDependency(name, versionConstraint)
        );
    }

    function parseBundledDependencies(
        deps: string[] | undefined
    ): Dependency[] {
        if (!deps) return [];
        // bundledDependencies is just an array of package names with no version constraint
        return deps.map(name => getOrCreateDependency(name, '*'));
    }

    /**
     * Extracts package name from a package-lock.json path.
     * e.g., "node_modules/@babel/core" -> "@babel/core"
     * e.g., "node_modules/foo/node_modules/bar" -> "bar"
     */
    function extractPackageName(pkgPath: string): string {
        // For nested packages, we want the last package name
        const nodeModulesIndex = pkgPath.lastIndexOf('node_modules/');
        if (nodeModulesIndex === -1) return pkgPath;
        return pkgPath.slice(nodeModulesIndex + 'node_modules/'.length);
    }

    /**
     * Creates or retrieves a ResolvedDependency from the cache.
     * This ensures the same resolved package is reused across the dependency tree.
     */
    function getOrCreateResolvedDependency(
        name: string,
        version: string,
        pkgEntry: any
    ): ResolvedDependency {
        const key = `${name}@${version}`;
        let resolved = resolvedDependencyCache.get(key);
        if (!resolved) {
            resolved = asRef({
                kind: ResolvedDependencyKind,
                name,
                version,
                dependencies: parseDependencies(pkgEntry.dependencies),
                devDependencies: parseDependencies(pkgEntry.devDependencies),
                peerDependencies: parseDependencies(pkgEntry.peerDependencies),
                optionalDependencies: parseDependencies(pkgEntry.optionalDependencies),
                engines: pkgEntry.engines,
                license: pkgEntry.license,
            });
            resolvedDependencyCache.set(key, resolved);
        }
        return resolved;
    }

    /**
     * Parses package-lock.json to build a list of resolved dependencies.
     */
    function parseResolutions(
        lockContent: any
    ): ResolvedDependency[] {
        if (!lockContent?.packages) return [];

        const packages = lockContent.packages as Record<string, any>;

        // Create ResolvedDependency objects for all packages
        for (const [pkgPath, pkgEntry] of Object.entries(packages)) {
            // Skip the root package (empty string key)
            if (pkgPath === '') continue;

            const name = extractPackageName(pkgPath);
            const version = pkgEntry.version;

            if (name && version) {
                getOrCreateResolvedDependency(name, version, pkgEntry);
            }
        }

        // Return all unique resolved dependencies
        return Array.from(resolvedDependencyCache.values());
    }

    const dependencies = parseDependencies(packageJsonContent.dependencies);
    const devDependencies = parseDependencies(packageJsonContent.devDependencies);
    const peerDependencies = parseDependencies(packageJsonContent.peerDependencies);
    const optionalDependencies = parseDependencies(packageJsonContent.optionalDependencies);
    const bundledDependencies = parseBundledDependencies(
        packageJsonContent.bundledDependencies || packageJsonContent.bundleDependencies
    );

    // Parse resolved dependencies from package-lock.json if provided
    const resolvedDependencies = packageLockContent ? parseResolutions(packageLockContent) : [];

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
        // packageManager is not set here - it should be determined by the caller based on lock file presence
        engines: packageJsonContent.engines,
    };
}

/**
 * Helper function to find a NodeResolutionResult marker on a compilation unit.
 */
export function findNodeResolutionResult(cu: { markers: Markers }): NodeResolutionResult | undefined {
    return findMarker<NodeResolutionResult>(cu, NodeResolutionResultKind);
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
     * Get a resolved dependency by package name.
     * Returns undefined if the package is not in resolvedDependencies.
     */
    export function getResolvedDependency(
        project: NodeResolutionResult,
        packageName: string
    ): ResolvedDependency | undefined {
        return project.resolvedDependencies.find(r => r.name === packageName);
    }

    /**
     * Find and resolve a dependency by name.
     * Returns the resolved dependency if found.
     */
    export function findResolved(
        project: NodeResolutionResult,
        packageName: string
    ): ResolvedDependency | undefined {
        return getResolvedDependency(project, packageName);
    }
}

/**
 * Register RPC codec for Dependency.
 */
RpcCodecs.registerCodec(DependencyKind, {
    async rpcReceive(before: Dependency, q: RpcReceiveQueue): Promise<Dependency> {
        const draft = createDraft(before);
        draft.kind = DependencyKind;
        draft.name = await q.receive(before.name);
        draft.versionConstraint = await q.receive(before.versionConstraint);

        return finishDraft(draft) as Dependency;
    },

    async rpcSend(after: Dependency, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.name);
        await q.getAndSend(after, a => a.versionConstraint);
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
    }
});
