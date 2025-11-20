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

import static org.openrewrite.rpc.RpcReceiveQueue.toEnum;

/**
 * Contains metadata about a Node.js project, parsed from package.json.
 * Attached as a marker to JS.CompilationUnit to provide dependency context for recipes.
 * <p>
 * Similar to GradleProject marker, this allows recipes to:
 * - Query project dependencies
 * - Check if specific packages are in use
 * - Modify dependencies programmatically
 * - Understand the project structure
 */
@Value
@With
public class NodeProject implements Marker, RpcCodec<NodeProject> {
    UUID id;

    // Project metadata from package.json
    @Nullable String name;
    @Nullable String version;
    @Nullable String description;
    String packageJsonPath;

    // Dependencies organized by scope
    List<NodeDependency> dependencies;
    List<NodeDependency> devDependencies;
    List<NodeDependency> peerDependencies;
    List<NodeDependency> optionalDependencies;
    List<NodeDependency> bundledDependencies;

    // Additional metadata
    @Nullable Map<String, String> scripts;
    @Nullable Map<String, String> engines;
    @Nullable Repository repository;

    @Override
    public void rpcSend(NodeProject after, RpcSendQueue q) {
        q.getAndSend(after, NodeProject::getId);
        q.getAndSend(after, NodeProject::getName);
        q.getAndSend(after, NodeProject::getVersion);
        q.getAndSend(after, NodeProject::getDescription);
        q.getAndSend(after, NodeProject::getPackageJsonPath);

        // Send dependency arrays with asRef to enable deduplication
        q.getAndSendListAsRef(after, NodeProject::getDependencies,
                dep -> dep.getName() + "@" + dep.getVersion() + ":" + dep.getScope(),
                dep -> dep.rpcSend(dep, q));
        q.getAndSendListAsRef(after, NodeProject::getDevDependencies,
                dep -> dep.getName() + "@" + dep.getVersion() + ":" + dep.getScope(),
                dep -> dep.rpcSend(dep, q));
        q.getAndSendListAsRef(after, NodeProject::getPeerDependencies,
                dep -> dep.getName() + "@" + dep.getVersion() + ":" + dep.getScope(),
                dep -> dep.rpcSend(dep, q));
        q.getAndSendListAsRef(after, NodeProject::getOptionalDependencies,
                dep -> dep.getName() + "@" + dep.getVersion() + ":" + dep.getScope(),
                dep -> dep.rpcSend(dep, q));
        q.getAndSendListAsRef(after, NodeProject::getBundledDependencies,
                dep -> dep.getName() + "@" + dep.getVersion() + ":" + dep.getScope(),
                dep -> dep.rpcSend(dep, q));

        q.getAndSend(after, NodeProject::getScripts);
        q.getAndSend(after, NodeProject::getEngines);
        q.getAndSend(after, NodeProject::getRepository, repo -> repo.rpcSend(repo, q));
    }

    @Override
    public NodeProject rpcReceive(NodeProject before, RpcReceiveQueue q) {
        return before
                .withId(q.receiveAndGet(before.id, UUID::fromString))
                .withName(q.receive(before.name))
                .withVersion(q.receive(before.version))
                .withDescription(q.receive(before.description))
                .withPackageJsonPath(q.receive(before.packageJsonPath))
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
                .withScripts(q.receive(before.scripts))
                .withEngines(q.receive(before.engines))
                .withRepository(q.receive(before.repository, repo -> repo.rpcReceive(repo, q)));
    }

    /**
     * Scope/section of package.json where a dependency is declared.
     */
    public enum DependencyScope {
        DEPENDENCIES("dependencies"),
        DEV_DEPENDENCIES("devDependencies"),
        PEER_DEPENDENCIES("peerDependencies"),
        OPTIONAL_DEPENDENCIES("optionalDependencies"),
        BUNDLED_DEPENDENCIES("bundledDependencies");

        private final String value;

        DependencyScope(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Represents a Node.js dependency from package.json.
     * Analogous to Maven's GroupArtifactVersion or Gradle's Dependency.
     * <p>
     * This class is designed to be extensible for future transitive dependency support.
     * Uses asRef() when creating instances to enable reference deduplication during RPC serialization.
     */
    @Value
    @With
    public static class NodeDependency implements RpcCodec<NodeDependency> {
        String name;                          // Package name (e.g., "react")
        String version;                       // Version constraint (e.g., "^18.2.0")
        @Nullable String resolved;            // Actual resolved version (from package-lock.json if available)
        DependencyScope scope;                // Which section this came from

        // Fields reserved for future transitive dependency support
        // Initially these will be null/empty
        @Nullable List<NodeDependency> dependencies;  // Transitive dependencies (not populated initially)
        @Nullable Integer depth;                       // Depth in dependency tree (0 for direct)
        @Nullable List<String> requestedBy;           // Package names that requested this dependency

        @Override
        public void rpcSend(NodeDependency after, RpcSendQueue q) {
            q.getAndSend(after, NodeDependency::getName);
            q.getAndSend(after, NodeDependency::getVersion);
            q.getAndSend(after, NodeDependency::getResolved);
            q.getAndSend(after, NodeDependency::getScope);
            q.getAndSend(after, NodeDependency::getDepth);

            // Future: send transitive dependencies
            if (after.dependencies != null) {
                q.getAndSendListAsRef(after, NodeDependency::getDependencies,
                        dep -> dep.getName() + "@" + dep.getVersion() + ":" + dep.getScope(),
                        dep -> dep.rpcSend(dep, q));
            } else {
                q.getAndSend(after, d -> null);
            }

            q.getAndSend(after, NodeDependency::getRequestedBy);
        }

        @Override
        public NodeDependency rpcReceive(NodeDependency before, RpcReceiveQueue q) {
            NodeDependency result = before
                    .withName(q.receive(before.name))
                    .withVersion(q.receive(before.version))
                    .withResolved(q.receive(before.resolved))
                    .withScope(q.receiveAndGet(before.scope, toEnum(DependencyScope.class)))
                    .withDepth(q.receive(before.depth));

            // Future: receive transitive dependencies
            List<NodeDependency> deps = q.receiveList(before.dependencies,
                    dep -> rpcReceive(dep, q));

            return result
                    .withDependencies(deps)
                    .withRequestedBy(q.receive(before.requestedBy));
        }
    }

    /**
     * Repository information from package.json.
     */
    @Value
    @With
    public static class Repository implements RpcCodec<Repository> {
        String type;
        String url;

        @Override
        public void rpcSend(Repository after, RpcSendQueue q) {
            q.getAndSend(after, Repository::getType);
            q.getAndSend(after, Repository::getUrl);
        }

        @Override
        public Repository rpcReceive(Repository before, RpcReceiveQueue q) {
            return before
                    .withType(q.receive(before.type))
                    .withUrl(q.receive(before.url));
        }
    }
}
