/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.javascript.internal.lock.pnpm;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Value;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The data model of @pnpm/installing.deps-resolver src/resolvePeers.ts (pnpm 11.2.2).
 */
final class ResolvePeers {

    private ResolvePeers() {
    }

    /**
     * {@code DependenciesGraphNode}: a {@link ResolveDependencies.ResolvedPackage} at a dependency path.
     */
    @Getter
    @Setter
    @RequiredArgsConstructor
    static final class DependenciesGraphNode {
        private final String depPath;
        private final ResolveDependencies.ResolvedPackage resolvedPackage;
        private String modules = "";
        private String dir = "";
        private int depth;

        /**
         * The children by alias as tree nodes, until {@code resolvePeers} turns them into dependency paths.
         */
        private Map<String, ResolveDependencies.NodeId> childrenNodeIds = new LinkedHashMap<>();
        private Map<String, String> children = new LinkedHashMap<>();
        private @Nullable Map<String, ResolveDependencies.PeerDependency> peerDependencies;
        private Set<String> transitivePeerDependencies = new LinkedHashSet<>();
        private Set<String> resolvedPeerNames = new LinkedHashSet<>();
        private boolean installable;
        private @Nullable Boolean isBuilt;
        private boolean isPure;
        private @Nullable Boolean requiresBuild;
    }

    /**
     * One of a project's top dependencies that were already installed.
     */
    @Value
    static class TopParent {
        String name;
        String version;

        @Nullable
        String alias;
    }

    /**
     * {@code ProjectToResolve}: an importer's direct dependencies by alias, and its top parents.
     */
    @Getter
    @RequiredArgsConstructor
    static final class ProjectToResolve {
        private final String id;
        private final String rootDir;
        private final Map<String, ResolveDependencies.NodeId> directNodeIdsByAlias;
        private final List<TopParent> topParents;
    }
}
