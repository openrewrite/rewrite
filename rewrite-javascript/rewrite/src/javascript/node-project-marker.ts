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

export const NodeProjectMarkerKind = "org.openrewrite.javascript.marker.NodeProject" as const;
export const DependencyKind = "org.openrewrite.javascript.marker.NodeProject$Dependency" as const;
export const ResolvedDependencyKind = "org.openrewrite.javascript.marker.NodeProject$ResolvedDependency" as const;

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
 * which can be looked up in NodeProject.resolutions to find their resolved versions.
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
 * Similar to GradleProject marker, this allows recipes to:
 * - Query project dependencies
 * - Check if specific packages are in use
 * - Modify dependencies programmatically
 * - Understand the project structure
 *
 * The model separates requests (Dependency) from resolutions (ResolvedDependency):
 * - The dependency arrays contain Dependency objects (what was requested)
 * - The resolutions map links each Dependency to its ResolvedDependency
 */
export interface NodeProject extends Marker {
    readonly kind: typeof NodeProjectMarkerKind;
    readonly id: UUID;

    // Project metadata from package.json
    readonly name?: string;           // Project name
    readonly version?: string;        // Project version
    readonly description?: string;    // Project description
    readonly packageJsonPath: string; // Path to the package.json file

    // Dependency requests organized by scope (from package.json)
    readonly dependencies: Dependency[];          // Regular dependencies
    readonly devDependencies: Dependency[];       // Development dependencies
    readonly peerDependencies: Dependency[];      // Peer dependencies
    readonly optionalDependencies: Dependency[];  // Optional dependencies
    readonly bundledDependencies: Dependency[];   // Bundled dependencies

    // Resolution map (from package-lock.json) - maps requests to resolved versions
    // Key is Dependency (by value equality on name+versionConstraint)
    readonly resolutions?: Map<Dependency, ResolvedDependency>;

    // Node/npm version requirements
    readonly engines?: Record<string, string>;
}

/**
 * Creates a NodeProject marker from a package.json file.
 * Should be called during parsing to attach to JS.CompilationUnit.
 *
 * All Dependency instances are wrapped with asRef() to enable
 * reference deduplication during RPC serialization.
 *
 * @param packageJsonPath Path to the package.json file
 * @param packageJsonContent Parsed package.json content
 * @param packageLockContent Optional parsed package-lock.json content for resolution info
 */
export function createNodeProjectMarker(
    packageJsonPath: string,
    packageJsonContent: any,
    packageLockContent?: any
): NodeProject {
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
     * Parses package-lock.json to build the resolutions map.
     * The map links Dependency requests to their ResolvedDependency.
     */
    function parseResolutions(
        lockContent: any
    ): Map<Dependency, ResolvedDependency> | undefined {
        if (!lockContent?.packages) return undefined;

        const resolutions = new Map<Dependency, ResolvedDependency>();
        const packages = lockContent.packages as Record<string, any>;

        // First pass: create all ResolvedDependency objects
        for (const [pkgPath, pkgEntry] of Object.entries(packages)) {
            // Skip the root package (empty string key)
            if (pkgPath === '') continue;

            const name = extractPackageName(pkgPath);
            const version = pkgEntry.version;

            if (name && version) {
                getOrCreateResolvedDependency(name, version, pkgEntry);
            }
        }

        // Second pass: link dependencies to their resolutions
        // We need to map each Dependency (name + versionConstraint) to its ResolvedDependency
        for (const [pkgPath, pkgEntry] of Object.entries(packages)) {
            const allDeps: Record<string, string> = {
                ...(pkgEntry.dependencies || {}),
                ...(pkgEntry.devDependencies || {}),
                ...(pkgEntry.peerDependencies || {}),
                ...(pkgEntry.optionalDependencies || {}),
            };

            for (const [depName, versionConstraint] of Object.entries(allDeps)) {
                const dep = getOrCreateDependency(depName, versionConstraint);

                // If we haven't resolved this dependency yet, find its resolution
                if (!resolutions.has(dep)) {
                    // Look for the resolved package in the packages map
                    // First try direct path, then nested paths
                    const directPath = `node_modules/${depName}`;

                    // For nested dependencies, we need to find the correct resolution
                    // by walking up from the current package's node_modules
                    let resolvedEntry: any = null;
                    let searchPath = pkgPath === '' ? '' : pkgPath;

                    // Walk up the directory tree to find the resolved package
                    while (true) {
                        const candidatePath = searchPath
                            ? `${searchPath}/node_modules/${depName}`
                            : `node_modules/${depName}`;

                        if (packages[candidatePath]) {
                            resolvedEntry = packages[candidatePath];
                            break;
                        }

                        // Move up one level
                        const lastNodeModules = searchPath.lastIndexOf('/node_modules');
                        if (lastNodeModules === -1) {
                            // We're at the root, try the direct path one more time
                            if (packages[directPath]) {
                                resolvedEntry = packages[directPath];
                            }
                            break;
                        }
                        searchPath = searchPath.slice(0, lastNodeModules);
                    }

                    if (resolvedEntry && resolvedEntry.version) {
                        const resolved = getOrCreateResolvedDependency(
                            depName,
                            resolvedEntry.version,
                            resolvedEntry
                        );
                        resolutions.set(dep, resolved);
                    }
                }
            }
        }

        return resolutions.size > 0 ? resolutions : undefined;
    }

    const dependencies = parseDependencies(packageJsonContent.dependencies);
    const devDependencies = parseDependencies(packageJsonContent.devDependencies);
    const peerDependencies = parseDependencies(packageJsonContent.peerDependencies);
    const optionalDependencies = parseDependencies(packageJsonContent.optionalDependencies);
    const bundledDependencies = parseBundledDependencies(
        packageJsonContent.bundledDependencies || packageJsonContent.bundleDependencies
    );

    // Parse resolutions from package-lock.json if provided
    const resolutions = packageLockContent ? parseResolutions(packageLockContent) : undefined;

    return {
        kind: NodeProjectMarkerKind,
        id: randomId(),
        name: packageJsonContent.name,
        version: packageJsonContent.version,
        description: packageJsonContent.description,
        packageJsonPath,
        dependencies,
        devDependencies,
        peerDependencies,
        optionalDependencies,
        bundledDependencies,
        resolutions,
        engines: packageJsonContent.engines,
    };
}

/**
 * Helper function to find a NodeProject marker on a compilation unit.
 */
export function findNodeProject(cu: { markers: Markers }): NodeProject | undefined {
    return findMarker<NodeProject>(cu, NodeProjectMarkerKind);
}

/**
 * Helper functions for querying dependencies
 */
export namespace NodeProjectQueries {

    /**
     * Get all dependency requests from all scopes.
     */
    export function getAllDependencies(project: NodeProject): Dependency[] {
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
        project: NodeProject,
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
        project: NodeProject,
        packageName: string
    ): Dependency | undefined {
        return getAllDependencies(project).find(dep => dep.name === packageName);
    }

    /**
     * Get all dependency requests matching a predicate.
     */
    export function findDependencies(
        project: NodeProject,
        predicate: (dep: Dependency) => boolean
    ): Dependency[] {
        return getAllDependencies(project).filter(predicate);
    }

    /**
     * Resolve a dependency request to its installed version.
     * Returns undefined if resolutions are not available or the dependency is not resolved.
     */
    export function resolve(
        project: NodeProject,
        dependency: Dependency
    ): ResolvedDependency | undefined {
        return project.resolutions?.get(dependency);
    }

    /**
     * Find and resolve a dependency by name.
     * Returns the resolved dependency if found and resolutions are available.
     */
    export function findResolved(
        project: NodeProject,
        packageName: string
    ): ResolvedDependency | undefined {
        const dep = findDependency(project, packageName);
        return dep ? resolve(project, dep) : undefined;
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
 * Register RPC codec for NodeProject marker.
 * This handles serialization/deserialization for communication between JS and Java.
 */
RpcCodecs.registerCodec(NodeProjectMarkerKind, {
    async rpcReceive(before: NodeProject, q: RpcReceiveQueue): Promise<NodeProject> {
        const draft = createDraft(before);
        draft.id = await q.receive(before.id);
        draft.name = await q.receive(before.name);
        draft.version = await q.receive(before.version);
        draft.description = await q.receive(before.description);
        draft.packageJsonPath = await q.receive(before.packageJsonPath);

        draft.dependencies = (await q.receiveList(before.dependencies)) || [];
        draft.devDependencies = (await q.receiveList(before.devDependencies)) || [];
        draft.peerDependencies = (await q.receiveList(before.peerDependencies)) || [];
        draft.optionalDependencies = (await q.receiveList(before.optionalDependencies)) || [];
        draft.bundledDependencies = (await q.receiveList(before.bundledDependencies)) || [];

        // TODO: receive resolutions map when package-lock.json parsing is implemented

        draft.engines = await q.receive(before.engines);

        return finishDraft(draft) as NodeProject;
    },

    async rpcSend(after: NodeProject, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.name);
        await q.getAndSend(after, a => a.version);
        await q.getAndSend(after, a => a.description);
        await q.getAndSend(after, a => a.packageJsonPath);

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

        // TODO: send resolutions map when package-lock.json parsing is implemented

        await q.getAndSend(after, a => a.engines);
    }
});
