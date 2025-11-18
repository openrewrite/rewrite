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

/**
 * Scope/section of package.json where a dependency is declared.
 */
export enum DependencyScope {
    Dependencies = "dependencies",
    DevDependencies = "devDependencies",
    PeerDependencies = "peerDependencies",
    OptionalDependencies = "optionalDependencies",
    BundledDependencies = "bundledDependencies"
}

/**
 * Represents a Node.js dependency from package.json.
 * Analogous to Maven's GroupArtifactVersion or Gradle's Dependency.
 *
 * This interface is designed to be extensible for future transitive dependency support.
 * Use asRef() when creating instances to enable reference deduplication during RPC serialization.
 */
export interface NodeDependency {
    readonly name: string;           // Package name (e.g., "react")
    readonly version: string;         // Version constraint (e.g., "^18.2.0")
    readonly resolved?: string;       // Actual resolved version (from package-lock.json if available)
    readonly scope: DependencyScope; // Which section this came from

    // Fields reserved for future transitive dependency support
    // Initially these will be undefined/empty
    readonly dependencies?: NodeDependency[];  // Transitive dependencies (not populated initially)
    readonly depth?: number;                    // Depth in dependency tree (0 for direct)
    readonly requestedBy?: string[];           // Package names that requested this dependency
}

/**
 * Repository information from package.json.
 */
export interface Repository {
    readonly type: string;
    readonly url: string;
}

/**
 * Contains metadata about a Node.js project, parsed from package.json.
 * Attached as a marker to JS.CompilationUnit to provide dependency context for recipes.
 *
 * Similar to GradleProject marker, this allows recipes to:
 * - Query project dependencies
 * - Check if specific packages are in use
 * - Modify dependencies programmatically
 * - Understand the project structure
 */
export interface NodeProject extends Marker {
    readonly kind: typeof NodeProjectMarkerKind;
    readonly id: UUID;

    // Project metadata from package.json
    readonly name?: string;           // Project name
    readonly version?: string;        // Project version
    readonly description?: string;    // Project description
    readonly packageJsonPath: string; // Path to the package.json file

    // Dependencies organized by scope
    readonly dependencies: NodeDependency[];          // Regular dependencies
    readonly devDependencies: NodeDependency[];       // Development dependencies
    readonly peerDependencies: NodeDependency[];      // Peer dependencies
    readonly optionalDependencies: NodeDependency[];  // Optional dependencies
    readonly bundledDependencies: NodeDependency[];   // Bundled dependencies

    // Additional metadata
    readonly scripts?: Record<string, string>;        // npm scripts
    readonly engines?: Record<string, string>;        // Node/npm version requirements
    readonly repository?: Repository;                 // Repository info
}

/**
 * Creates a NodeProject marker from a package.json file.
 * Should be called during parsing to attach to JS.CompilationUnit.
 *
 * All NodeDependency instances are wrapped with asRef() to enable
 * reference deduplication during RPC serialization.
 */
export function createNodeProjectMarker(
    packageJsonPath: string,
    packageJsonContent: any
): NodeProject {

    function parseDependencies(
        deps: Record<string, string> | undefined,
        scope: DependencyScope
    ): NodeDependency[] {
        if (!deps) return [];
        return Object.entries(deps).map(([name, version]) => asRef({
            name,
            version,
            scope,
            depth: 0,  // Mark as direct dependency
            dependencies: undefined,  // No transitives initially
            requestedBy: undefined
        }));
    }

    return {
        kind: NodeProjectMarkerKind,
        id: randomId(),
        name: packageJsonContent.name,
        version: packageJsonContent.version,
        description: packageJsonContent.description,
        packageJsonPath,
        dependencies: parseDependencies(
            packageJsonContent.dependencies,
            DependencyScope.Dependencies
        ),
        devDependencies: parseDependencies(
            packageJsonContent.devDependencies,
            DependencyScope.DevDependencies
        ),
        peerDependencies: parseDependencies(
            packageJsonContent.peerDependencies,
            DependencyScope.PeerDependencies
        ),
        optionalDependencies: parseDependencies(
            packageJsonContent.optionalDependencies,
            DependencyScope.OptionalDependencies
        ),
        bundledDependencies: parseDependencies(
            packageJsonContent.bundledDependencies || packageJsonContent.bundleDependencies,
            DependencyScope.BundledDependencies
        ),
        scripts: packageJsonContent.scripts,
        engines: packageJsonContent.engines,
        repository: packageJsonContent.repository
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
     * Get all dependencies from all scopes.
     */
    export function getAllDependencies(project: NodeProject): NodeDependency[] {
        return [
            ...project.dependencies,
            ...project.devDependencies,
            ...project.peerDependencies,
            ...project.optionalDependencies,
            ...project.bundledDependencies
        ];
    }

    /**
     * Check if project has a specific dependency, optionally filtered by scope.
     */
    export function hasDependency(
        project: NodeProject,
        packageName: string,
        scope?: DependencyScope
    ): boolean {
        const deps = scope
            ? getDependenciesByScope(project, scope)
            : getAllDependencies(project);
        return deps.some(dep => dep.name === packageName);
    }

    /**
     * Get all dependencies for a specific scope.
     */
    export function getDependenciesByScope(
        project: NodeProject,
        scope: DependencyScope
    ): NodeDependency[] {
        switch (scope) {
            case DependencyScope.Dependencies:
                return project.dependencies;
            case DependencyScope.DevDependencies:
                return project.devDependencies;
            case DependencyScope.PeerDependencies:
                return project.peerDependencies;
            case DependencyScope.OptionalDependencies:
                return project.optionalDependencies;
            case DependencyScope.BundledDependencies:
                return project.bundledDependencies;
        }
    }

    /**
     * Find a specific dependency by name across all scopes.
     */
    export function findDependency(
        project: NodeProject,
        packageName: string
    ): NodeDependency | undefined {
        return getAllDependencies(project).find(dep => dep.name === packageName);
    }

    /**
     * Get all dependencies matching a predicate.
     */
    export function findDependencies(
        project: NodeProject,
        predicate: (dep: NodeDependency) => boolean
    ): NodeDependency[] {
        return getAllDependencies(project).filter(predicate);
    }
}

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

        // Receive dependency arrays with asRef support
        draft.dependencies = (await q.receiveList(before.dependencies, async (dep) => {
            return asRef(await receiveNodeDependency(dep, q));
        })) || [];
        draft.devDependencies = (await q.receiveList(before.devDependencies, async (dep) => {
            return asRef(await receiveNodeDependency(dep, q));
        })) || [];
        draft.peerDependencies = (await q.receiveList(before.peerDependencies, async (dep) => {
            return asRef(await receiveNodeDependency(dep, q));
        })) || [];
        draft.optionalDependencies = (await q.receiveList(before.optionalDependencies, async (dep) => {
            return asRef(await receiveNodeDependency(dep, q));
        })) || [];
        draft.bundledDependencies = (await q.receiveList(before.bundledDependencies, async (dep) => {
            return asRef(await receiveNodeDependency(dep, q));
        })) || [];

        draft.scripts = await q.receive(before.scripts);
        draft.engines = await q.receive(before.engines);
        draft.repository = await q.receive(before.repository);

        return finishDraft(draft) as NodeProject;
    },

    async rpcSend(after: NodeProject, q: RpcSendQueue): Promise<void> {
        await q.getAndSend(after, a => a.id);
        await q.getAndSend(after, a => a.name);
        await q.getAndSend(after, a => a.version);
        await q.getAndSend(after, a => a.description);
        await q.getAndSend(after, a => a.packageJsonPath);

        // Send dependency arrays with asRef to enable deduplication
        await q.getAndSendList(after, a => a.dependencies.map(d => asRef(d)),
            dep => `${dep.name}@${dep.version}:${dep.scope}`,
            async (dep) => await sendNodeDependency(dep, q));
        await q.getAndSendList(after, a => a.devDependencies.map(d => asRef(d)),
            dep => `${dep.name}@${dep.version}:${dep.scope}`,
            async (dep) => await sendNodeDependency(dep, q));
        await q.getAndSendList(after, a => a.peerDependencies.map(d => asRef(d)),
            dep => `${dep.name}@${dep.version}:${dep.scope}`,
            async (dep) => await sendNodeDependency(dep, q));
        await q.getAndSendList(after, a => a.optionalDependencies.map(d => asRef(d)),
            dep => `${dep.name}@${dep.version}:${dep.scope}`,
            async (dep) => await sendNodeDependency(dep, q));
        await q.getAndSendList(after, a => a.bundledDependencies.map(d => asRef(d)),
            dep => `${dep.name}@${dep.version}:${dep.scope}`,
            async (dep) => await sendNodeDependency(dep, q));

        await q.getAndSend(after, a => a.scripts);
        await q.getAndSend(after, a => a.engines);
        await q.getAndSend(after, a => a.repository);
    }
});

/**
 * Helper function to receive a NodeDependency from RPC.
 */
async function receiveNodeDependency(before: NodeDependency, q: RpcReceiveQueue): Promise<NodeDependency> {
    const draft = createDraft(before);
    draft.name = await q.receive(before.name);
    draft.version = await q.receive(before.version);
    draft.resolved = await q.receive(before.resolved);
    draft.scope = await q.receive(before.scope);
    draft.depth = await q.receive(before.depth);

    // Future: receive transitive dependencies
    draft.dependencies = before.dependencies ? (await q.receiveList(before.dependencies, async (dep) => {
        return asRef(await receiveNodeDependency(dep, q));
    })) || [] : undefined;

    draft.requestedBy = await q.receive(before.requestedBy);

    return finishDraft(draft) as NodeDependency;
}

/**
 * Helper function to send a NodeDependency via RPC.
 */
async function sendNodeDependency(after: NodeDependency, q: RpcSendQueue): Promise<void> {
    await q.getAndSend(after, a => a.name);
    await q.getAndSend(after, a => a.version);
    await q.getAndSend(after, a => a.resolved);
    await q.getAndSend(after, a => a.scope);
    await q.getAndSend(after, a => a.depth);

    // Future: send transitive dependencies
    if (after.dependencies) {
        await q.getAndSendList(after, a => (a.dependencies || []).map(d => asRef(d)),
            dep => `${dep.name}@${dep.version}:${dep.scope}`,
            async (dep) => await sendNodeDependency(dep, q));
    } else {
        await q.getAndSend(after, () => undefined);
    }

    await q.getAndSend(after, a => a.requestedBy);
}
