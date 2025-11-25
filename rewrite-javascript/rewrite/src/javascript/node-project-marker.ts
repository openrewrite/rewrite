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
 */
export function createNodeProjectMarker(
    packageJsonPath: string,
    packageJsonContent: any
): NodeProject {
    // Cache for deduplicating dependencies with the same name+versionConstraint.
    const dependencyCache = new Map<string, Dependency>();

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

    return {
        kind: NodeProjectMarkerKind,
        id: randomId(),
        name: packageJsonContent.name,
        version: packageJsonContent.version,
        description: packageJsonContent.description,
        packageJsonPath,
        dependencies: parseDependencies(packageJsonContent.dependencies),
        devDependencies: parseDependencies(packageJsonContent.devDependencies),
        peerDependencies: parseDependencies(packageJsonContent.peerDependencies),
        optionalDependencies: parseDependencies(packageJsonContent.optionalDependencies),
        bundledDependencies: parseBundledDependencies(
            packageJsonContent.bundledDependencies || packageJsonContent.bundleDependencies
        ),
        resolutions: undefined, // Not populated until package-lock.json is parsed
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
