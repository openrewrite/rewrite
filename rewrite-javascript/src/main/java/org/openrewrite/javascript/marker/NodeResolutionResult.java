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
package org.openrewrite.javascript.marker;

import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.marker.Marker;
import org.openrewrite.rpc.RpcCodec;
import org.openrewrite.rpc.RpcReceiveQueue;
import org.openrewrite.rpc.RpcSendQueue;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Collections.emptyList;

/**
 * Contains metadata about a Node.js project, parsed from package.json and package-lock.json.
 * Attached as a marker to JS.CompilationUnit to provide dependency context for recipes.
 * <p>
 * Similar to MavenResolutionResult marker, this allows recipes to:
 * - Query project dependencies
 * - Check if specific packages are in use
 * - Modify dependencies programmatically
 * - Understand the project structure
 * <p>
 * The model separates requests (Dependency) from resolutions (ResolvedDependency):
 * - The dependency arrays contain Dependency objects (what was requested)
 * - The resolvedDependencies list contains what was actually installed
 */
@Value
@With
public class NodeResolutionResult implements Marker, RpcCodec<NodeResolutionResult> {
    UUID id;

    // Project metadata from package.json
    @Nullable String name;
    @Nullable String version;
    @Nullable String description;
    String path;

    // Paths to workspace package.json files (only populated on workspace root)
    @Nullable List<String> workspacePackagePaths;

    // Dependency requests organized by scope (from package.json)
    List<Dependency> dependencies;
    List<Dependency> devDependencies;
    List<Dependency> peerDependencies;
    List<Dependency> optionalDependencies;
    List<Dependency> bundledDependencies;

    // Resolved dependencies from package-lock.json - what was actually installed
    // Use getResolvedDependency() helper to look up by name
    List<ResolvedDependency> resolvedDependencies;

    // The package manager used by the project (npm, yarn, pnpm, etc.)
    @Nullable PackageManager packageManager;

    // Node/npm version requirements
    @Nullable Map<String, String> engines;

    /**
     * Look up a resolved dependency by package name.
     *
     * @param packageName The name of the package to look up
     * @return The resolved dependency, or null if not found
     */
    public @Nullable ResolvedDependency getResolvedDependency(String packageName) {
        if (resolvedDependencies == null) {
            return null;
        }
        return resolvedDependencies.stream()
                .filter(r -> r.getName().equals(packageName))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void rpcSend(NodeResolutionResult after, RpcSendQueue q) {
        q.getAndSend(after, NodeResolutionResult::getId);
        q.getAndSend(after, NodeResolutionResult::getName);
        q.getAndSend(after, NodeResolutionResult::getVersion);
        q.getAndSend(after, NodeResolutionResult::getDescription);
        q.getAndSend(after, NodeResolutionResult::getPath);
        q.getAndSend(after, NodeResolutionResult::getWorkspacePackagePaths);

        q.getAndSendListAsRef(after, NodeResolutionResult::getDependencies,
                dep -> dep.getName() + "@" + dep.getVersionConstraint(),
                dep -> dep.rpcSend(dep, q));
        q.getAndSendListAsRef(after, NodeResolutionResult::getDevDependencies,
                dep -> dep.getName() + "@" + dep.getVersionConstraint(),
                dep -> dep.rpcSend(dep, q));
        q.getAndSendListAsRef(after, NodeResolutionResult::getPeerDependencies,
                dep -> dep.getName() + "@" + dep.getVersionConstraint(),
                dep -> dep.rpcSend(dep, q));
        q.getAndSendListAsRef(after, NodeResolutionResult::getOptionalDependencies,
                dep -> dep.getName() + "@" + dep.getVersionConstraint(),
                dep -> dep.rpcSend(dep, q));
        q.getAndSendListAsRef(after, NodeResolutionResult::getBundledDependencies,
                dep -> dep.getName() + "@" + dep.getVersionConstraint(),
                dep -> dep.rpcSend(dep, q));
        q.getAndSendListAsRef(after, NodeResolutionResult::getResolvedDependencies,
                resolved -> resolved.getName() + "@" + resolved.getVersion(),
                resolved -> resolved.rpcSend(resolved, q));

        q.getAndSend(after, NodeResolutionResult::getPackageManager);
        q.getAndSend(after, NodeResolutionResult::getEngines);
    }

    @Override
    public NodeResolutionResult rpcReceive(NodeResolutionResult before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.id, UUID::fromString))
                .withName(q.receive(before.name))
                .withVersion(q.receive(before.version))
                .withDescription(q.receive(before.description))
                .withPath(q.receive(before.path))
                .withWorkspacePackagePaths(q.receive(before.workspacePackagePaths))
                .withDependencies(q.receiveList(before.dependencies,
                        dep -> dep.rpcReceive(dep, q)))
                .withDevDependencies(q.receiveList(before.devDependencies,
                        dep -> dep.rpcReceive(dep, q)))
                .withPeerDependencies(q.receiveList(before.peerDependencies,
                        dep -> dep.rpcReceive(dep, q)))
                .withOptionalDependencies(q.receiveList(before.optionalDependencies,
                        dep -> dep.rpcReceive(dep, q)))
                .withBundledDependencies(q.receiveList(before.bundledDependencies,
                        dep -> dep.rpcReceive(dep, q)))
                .withResolvedDependencies(q.receiveList(before.resolvedDependencies,
                        resolved -> resolved.rpcReceive(resolved, q)))
                .withPackageManager(q.receiveAndGet(before.packageManager, s -> s == null ? null : PackageManager.valueOf(s.toString())))
                .withEngines(q.receive(before.engines));
    }

    /**
     * Represents a dependency request as declared in package.json.
     * This is what a package asks for (name + version constraint).
     * <p>
     * When the same name+versionConstraint appears multiple times, the same
     * Dependency instance is reused. This enables reference deduplication
     * during RPC serialization.
     */
    @Value
    @With
    public static class Dependency implements RpcCodec<Dependency> {
        String name;              // Package name (e.g., "react")
        String versionConstraint; // Version constraint (e.g., "^18.2.0")

        @Override
        public void rpcSend(Dependency after, RpcSendQueue q) {
            q.getAndSend(after, Dependency::getName);
            q.getAndSend(after, Dependency::getVersionConstraint);
        }

        @Override
        public Dependency rpcReceive(Dependency before, RpcReceiveQueue q) {
            return before
                    .withName(q.receive(before.name))
                    .withVersionConstraint(q.receive(before.versionConstraint));
        }
    }

    /**
     * Represents a resolved dependency from package-lock.json.
     * This is what was actually installed (name + resolved version + its own dependencies).
     * <p>
     * Each ResolvedDependency's dependency arrays contain Dependency objects (requests),
     * which can be looked up in NodeResolutionResult.resolvedDependencies to find their resolved versions.
     */
    @Value
    @With
    public static class ResolvedDependency implements RpcCodec<ResolvedDependency> {
        String name;    // Package name (e.g., "react")
        String version; // Actual resolved version (e.g., "18.3.1")

        // This package's own dependency requests
        @Nullable List<Dependency> dependencies;
        @Nullable List<Dependency> devDependencies;
        @Nullable List<Dependency> peerDependencies;
        @Nullable List<Dependency> optionalDependencies;

        // Node/npm version requirements for this package
        @Nullable Map<String, String> engines;

        // SPDX license identifier (e.g., "MIT", "Apache-2.0")
        @Nullable String license;

        @Override
        public void rpcSend(ResolvedDependency after, RpcSendQueue q) {
            q.getAndSend(after, ResolvedDependency::getName);
            q.getAndSend(after, ResolvedDependency::getVersion);
            q.getAndSendListAsRef(after, r -> r.getDependencies() != null ? r.getDependencies() : emptyList(),
                    dep -> dep.getName() + "@" + dep.getVersionConstraint(),
                    dep -> dep.rpcSend(dep, q));
            q.getAndSendListAsRef(after, r -> r.getDevDependencies() != null ? r.getDevDependencies() : emptyList(),
                    dep -> dep.getName() + "@" + dep.getVersionConstraint(),
                    dep -> dep.rpcSend(dep, q));
            q.getAndSendListAsRef(after, r -> r.getPeerDependencies() != null ? r.getPeerDependencies() : emptyList(),
                    dep -> dep.getName() + "@" + dep.getVersionConstraint(),
                    dep -> dep.rpcSend(dep, q));
            q.getAndSendListAsRef(after, r -> r.getOptionalDependencies() != null ? r.getOptionalDependencies() : emptyList(),
                    dep -> dep.getName() + "@" + dep.getVersionConstraint(),
                    dep -> dep.rpcSend(dep, q));
            q.getAndSend(after, ResolvedDependency::getEngines);
            q.getAndSend(after, ResolvedDependency::getLicense);
        }

        @Override
        public ResolvedDependency rpcReceive(ResolvedDependency before, RpcReceiveQueue q) {
            return before
                    .withName(q.receive(before.name))
                    .withVersion(q.receive(before.version))
                    .withDependencies(q.receiveList(before.dependencies,
                            dep -> dep.rpcReceive(dep, q)))
                    .withDevDependencies(q.receiveList(before.devDependencies,
                            dep -> dep.rpcReceive(dep, q)))
                    .withPeerDependencies(q.receiveList(before.peerDependencies,
                            dep -> dep.rpcReceive(dep, q)))
                    .withOptionalDependencies(q.receiveList(before.optionalDependencies,
                            dep -> dep.rpcReceive(dep, q)))
                    .withEngines(q.receive(before.engines))
                    .withLicense(q.receive(before.license));
        }
    }

    /**
     * Represents the package manager used by a Node.js project.
     */
    public enum PackageManager {
        Npm,
        YarnClassic,
        YarnBerry,
        Pnpm,
        Bun
    }
}
