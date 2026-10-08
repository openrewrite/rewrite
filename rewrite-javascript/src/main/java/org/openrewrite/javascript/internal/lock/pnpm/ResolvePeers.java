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
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * The data model of @pnpm/installing.deps-resolver src/resolvePeers.ts (pnpm 11.2.2): the dependency graph peers turn
 * the tree into, one node per dependency path (the package id and its peer suffix), its children by alias as
 * dependency paths, its peers and the peers it passes on.
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
        private int depth;
        private Map<String, String> children = new LinkedHashMap<>();
        private @Nullable Map<String, ResolveDependencies.PeerDependency> peerDependencies;
        private Set<String> transitivePeerDependencies = new LinkedHashSet<>();
        private Set<String> resolvedPeerNames = new LinkedHashSet<>();
        private boolean installable;
        private boolean isPure;
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
    }
}
