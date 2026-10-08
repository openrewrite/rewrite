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
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The data model of @pnpm/installing.deps-resolver src/resolveDependencies.ts and src/getNonDevWantedDependencies.ts
 * (pnpm 11.2.2): the dependencies a manifest wants, the package each resolves to, the edge that reached it, and the
 * dependency tree they form before peers are resolved.
 */
final class ResolveDependencies {

    private ResolveDependencies() {
    }

    /**
     * A dependency a manifest wants, by alias.
     */
    @Getter
    @Setter
    @RequiredArgsConstructor
    static final class WantedDependency {
        private final String alias;
        private final String bareSpecifier;
        private final boolean dev;
        private final boolean optional;
        private @Nullable Boolean injected;
        private @Nullable String saveCatalogName;
    }

    /**
     * A peer a package declares: its range, and whether {@code peerDependenciesMeta} makes it optional.
     */
    @Value
    static class PeerDependency {
        String version;

        @Nullable
        Boolean optional;
    }

    /**
     * {@code NodeId}: a leaf's is its package id, every other node's a number from {@code nextNodeId}.
     */
    @Value
    static class NodeId {
        Object id;

        static NodeId of(String pkgId) {
            return new NodeId(pkgId);
        }

        static NodeId of(long counter) {
            return new NodeId(counter);
        }

        @Override
        public String toString() {
            return String.valueOf(id);
        }
    }

    /**
     * One {@code name@version} as resolved: its resolution, its normalized peers and the manifest fields the lock
     * records.
     */
    @Getter
    @Setter
    @RequiredArgsConstructor
    static final class ResolvedPackage {
        private final String id;
        private boolean isLeaf;
        private Map<String, @Nullable Object> resolution;
        private boolean prod;
        private boolean dev;
        private boolean optional;
        private final String name;
        private final String version;
        private Map<String, PeerDependency> peerDependencies = new LinkedHashMap<>();
        private Set<String> optionalDependencies = new LinkedHashSet<>();
        private boolean hasBin;
        private boolean hasBundledDependencies;
        private boolean prepare;
        private String pkgIdWithPatchHash;
        private @Nullable Map<String, @Nullable Object> patch;

        /**
         * {@code additionalInfo}: deprecated, bundleDependencies, bundledDependencies, engines, cpu, os, libc.
         */
        private Map<String, @Nullable Object> additionalInfo = new LinkedHashMap<>();
    }

    /**
     * The catalog entry an importer dependency resolved through.
     */
    @Value
    static class CatalogLookup {
        String catalogName;
        String specifier;
        String userSpecifiedBareSpecifier;
    }

    /**
     * {@code PkgAddress}: one resolved edge, or ({@link #isLinkedDependency}) a workspace link.
     */
    @Getter
    @Setter
    @RequiredArgsConstructor
    static final class PkgAddress {
        private final String alias;
        private final String pkgId;
        private boolean optional;
        private Map<String, @Nullable Object> pkg;
        private @Nullable String normalizedBareSpecifier;
        private @Nullable CatalogLookup catalogLookup;
        private @Nullable NodeId nodeId;
        private boolean depIsLinked;
        private boolean isNew;
        private boolean isLinkedDependency;
        private @Nullable String resolvedVia;
        private boolean installable;
        private @Nullable String version;
        private boolean updated;
        private @Nullable String rootDir;
        private Map<String, PeerDependency> missingPeers = new LinkedHashMap<>();
        private @Nullable String publishedAt;
        private @Nullable String saveCatalogName;

        /**
         * A link's: whether it is a dev dependency, its directory resolution, and its name.
         */
        private boolean dev;
        private @Nullable Map<String, @Nullable Object> resolution;
        private @Nullable String name;
    }

    /**
     * {@code DependenciesTreeNode}: the children by alias (computed when first asked for), whether the package is
     * installable, the resolved package, and the depth it was first reached at ({@code -1} for a node whose
     * package is only named).
     */
    @Getter
    @Setter
    @RequiredArgsConstructor
    static final class DependenciesTreeNode {
        private @Nullable Supplier<Map<String, NodeId>> lazyChildren;
        private @Nullable Map<String, NodeId> children;
        private boolean installable;
        private final ResolvedPackage resolvedPackage;
        private final int depth;

        Map<String, NodeId> children() {
            if (children == null) {
                children = lazyChildren == null ? new LinkedHashMap<>() : lazyChildren.get();
                lazyChildren = null;
            }
            return children;
        }
    }
}
