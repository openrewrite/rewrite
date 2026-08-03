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
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.EntryMetadata;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.internal.lock.resolve.ResolutionGraph;
import org.openrewrite.javascript.internal.lock.resolve.ResolvedNode;
import org.openrewrite.javascript.internal.registry.VersionManifest;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/**
 * Diffs a freshly resolved {@link ResolutionGraph} against the existing {@code pnpm-lock.yaml} (lockfileVersion 9)
 * and expresses the difference as {@link PackageEdit}s for {@link PnpmLockPatcher}. pnpm is content-addressed —
 * no hoisting — so matching is by {@code name@version} key: a key the lock already holds is untouched (no edit,
 * no metadata verification), a lone leftover version pair is an in-place move, a fresh node becomes an add (a
 * declared re-resolution beside a retained version is pnpm's content-fork), and a key the resolution no longer
 * reaches is removed or rides an orphan-pruning move's GC. Peer-dependent entries are keyed with pnpm's
 * peer-suffix ({@code name@version(provider@version)…}), recomputed from the graph; an entry whose recorded
 * suffix disagrees, or any shape the patcher cannot express byte-exact, fails loud rather than guess.
 */
final class PnpmLockDiff {

    private static final List<String> DECLARED_SCOPES =
            Arrays.asList("dependencies", "devDependencies", "optionalDependencies");

    private PnpmLockDiff() {
    }

    static List<PackageEdit> diff(ResolutionGraph graph, String existingLock) {
        ResolutionGraph.Importer root = singleRootImporter(graph);
        Lock lock = Lock.parse(existingLock);
        PeerLayout layout = new PeerLayout(graph);

        Map<String, List<String>> graphByName = new LinkedHashMap<>();
        for (String nodeKey : graph.getNodes().keySet()) {
            graphByName.computeIfAbsent(nameOf(nodeKey), k -> new ArrayList<>()).add(nodeKey);
        }

        List<PackageEdit> edits = new ArrayList<>();
        boolean prunes = false;
        Set<String> matchedLockKeys = new HashSet<>();
        for (Map.Entry<String, List<String>> e : graphByName.entrySet()) {
            String name = e.getKey();
            List<String> nodeKeys = new ArrayList<>(e.getValue());
            List<String> lockKeys = new ArrayList<>(lock.keysByName.getOrDefault(name, Collections.emptyList()));

            // Exact version matches are retained entries.
            for (Iterator<String> it = nodeKeys.iterator(); it.hasNext(); ) {
                String nodeKey = it.next();
                if (lockKeys.remove(nodeKey)) {
                    matchedLockKeys.add(nodeKey);
                    it.remove();
                    PackageEdit edit = retainedEdit(graph, root, lock, layout, nodeKey);
                    if (edit != null) {
                        edits.add(edit);
                    }
                }
            }
            if (nodeKeys.size() == 1 && lockKeys.size() == 1) {
                String lockKey = lockKeys.remove(0);
                matchedLockKeys.add(lockKey);
                PackageEdit edit = moveEdit(graph, root, lock, layout, nodeKeys.remove(0), lockKey);
                prunes |= edit.isPrunesOrphans();
                edits.add(edit);
            } else if (!nodeKeys.isEmpty() && !lockKeys.isEmpty()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " resolves to versions whose lock entries cannot be matched unambiguously");
            }
            for (String nodeKey : nodeKeys) {
                edits.add(freshEdit(graph, root, lock, layout, nodeKey));
            }
        }

        edits.addAll(removalEdits(lock, matchedLockKeys, prunes));
        return edits;
    }

    /**
     * A node the lock already holds at its resolved version: nothing to edit unless the declared specifier
     * changed (a constraint-only bump) — and the recorded peer suffix and optional marking must still agree
     * with the resolution, else the closure reshaped in a way the patcher cannot express.
     */
    private static @Nullable PackageEdit retainedEdit(ResolutionGraph graph, ResolutionGraph.Importer root,
                                                      Lock lock, PeerLayout layout, String nodeKey) {
        String name = nameOf(nodeKey);
        String lockSnapKey = lock.snapshotKeyOf(nodeKey);
        if (lockSnapKey == null || !lockSnapKey.equals(layout.snapshotKey(nodeKey))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + "'s peer resolution changed (snapshot key " + lockSnapKey + " vs " +
                            layout.snapshotKey(nodeKey) + ")");
        }
        ResolvedNode node = graph.getNodes().get(nodeKey);
        if (lock.optionalSnapshots.contains(lockSnapKey) != node.isOptional()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + "'s optional marking changed (not yet patched)");
        }

        String version = node.getVersion();
        if (!version.equals(root.getResolved().get(name))) {
            return null; // transitive: nothing declared to reconcile
        }
        String declaredScope = declaringScope(root, name);
        Lock.ImporterDep dep = lock.importerDeps.get(name);
        if (dep == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " is declared but has no importer entry (not yet patched)");
        }
        if (!declaredScope.equals(dep.scope)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " moved between dependency scopes (not yet patched)");
        }
        String declaredRange = root.getDeclared().get(declaredScope).get(name);
        if (declaredRange.equals(dep.specifier)) {
            return null;
        }
        // Constraint-only bump: the patcher re-pins specifier and version; a peer-suffixed importer reference
        // defers in its own pre-checks.
        return PackageEdit.builder()
                .name(name)
                .oldVersion(version)
                .newVersion(version)
                .scope(declaredScope)
                .oldConstraint(dep.specifier)
                .importerDir(null)
                .build();
    }

    /** An in-place move of the one lock entry for this name to the newly resolved version. */
    private static PackageEdit moveEdit(ResolutionGraph graph, ResolutionGraph.Importer root, Lock lock,
                                        PeerLayout layout, String nodeKey, String lockKey) {
        String name = nameOf(nodeKey);
        ResolvedNode node = graph.getNodes().get(nodeKey);
        VersionManifest m = node.getManifest();
        requireEmittable(m);
        requireOptionalMarkable(node);
        if (!layout.suffix(nodeKey).isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " is peer-dependent; its keyed move is not yet patched");
        }
        String integrity = m.getDist() == null ? null : m.getDist().getIntegrity();
        if (integrity == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name,
                    name + "@" + m.getVersion() + " has no registry integrity");
        }
        Set<String> oldEdges = lock.snapshotDepNames.getOrDefault(lockKey, Collections.emptySet());
        Map<String, String> newEdges = layout.snapshotDependencies(nodeKey);
        if (!oldEdges.containsAll(newEdges.keySet())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " gained a dependency edge on upgrade (not yet patched)");
        }
        boolean dropped = !newEdges.keySet().containsAll(oldEdges);
        return PackageEdit.builder()
                .name(name)
                .oldVersion(versionOf(lockKey))
                .newVersion(node.getVersion())
                .newIntegrity(integrity)
                .newDependencies(newEdges.isEmpty() ? null : newEdges)
                .metadata(notEmpty(m.getEngines()) ? EntryMetadata.builder().engines(m.getEngines()).build() : null)
                .scope(declaringScope(root, name))
                .importerDir(null)
                .kind(node.getVersion().equals(root.getResolved().get(name)) ?
                        PackageEdit.Kind.BUMP : PackageEdit.Kind.FORCED_MOVE)
                .prunesOrphans(dropped)
                .build();
    }

    /**
     * A brand-new {@code name@version}: an add, or — when the importer already declared this name (whose old
     * version another dependent retains) — pnpm's content-fork, which retargets the importer edge and leaves
     * the retained entries alone.
     */
    private static PackageEdit freshEdit(ResolutionGraph graph, ResolutionGraph.Importer root, Lock lock,
                                         PeerLayout layout, String nodeKey) {
        String name = nameOf(nodeKey);
        ResolvedNode node = graph.getNodes().get(nodeKey);
        VersionManifest m = node.getManifest();
        if (!name.equals(m.getName())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " aliases " + m.getName() + "; a pnpm alias entry is not yet patched");
        }
        requireEmittable(m);
        requireOptionalMarkable(node);
        String integrity = m.getDist() == null ? null : m.getDist().getIntegrity();
        if (integrity == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name,
                    name + "@" + m.getVersion() + " has no registry integrity");
        }
        String suffix = layout.suffix(nodeKey);
        Map<String, String> snapshotDeps = layout.snapshotDependencies(nodeKey);
        boolean declared = node.getVersion().equals(root.getResolved().get(name));
        boolean forked = declared && lock.importerDeps.containsKey(name);
        return PackageEdit.builder()
                .name(name)
                .oldVersion("")
                .newVersion(node.getVersion())
                .newVersionRef(suffix.isEmpty() ? null : node.getVersion() + suffix)
                .newIntegrity(integrity)
                .newDependencies(snapshotDeps.isEmpty() ? null : snapshotDeps)
                .metadata(freshMetadata(node))
                .scope(declaringScope(root, name))
                .importerDir(null)
                .kind(forked ? PackageEdit.Kind.CONTENT_FORK : PackageEdit.Kind.ADD)
                .build();
    }

    private static @Nullable EntryMetadata freshMetadata(ResolvedNode node) {
        VersionManifest m = node.getManifest();
        boolean any = notEmpty(m.getEngines()) || notEmpty(m.getPeerDependencies()) || node.isOptional();
        if (!any) {
            return null;
        }
        return EntryMetadata.builder()
                .engines(notEmpty(m.getEngines()) ? m.getEngines() : null)
                .peerDependencies(notEmpty(m.getPeerDependencies()) ? m.getPeerDependencies() : null)
                .optional(node.isOptional() ? Boolean.TRUE : null)
                .build();
    }

    /**
     * Lock keys the resolution no longer reaches: a declared-before dependency that disappeared is a removal
     * edit; an undeclared leftover rides an orphan-pruning move's GC, and with none nothing proves it
     * collectable, so it defers.
     */
    private static List<PackageEdit> removalEdits(Lock lock, Set<String> matched, boolean prunes) {
        List<PackageEdit> removals = new ArrayList<>();
        for (String key : lock.packageKeys) {
            if (matched.contains(key)) {
                continue;
            }
            String name = nameOf(key);
            Lock.ImporterDep dep = lock.importerDeps.get(name);
            if (dep != null) {
                removals.add(PackageEdit.builder()
                        .name(name)
                        .oldVersion(versionOf(key))
                        .newVersion(null)
                        .scope(dep.scope)
                        .importerDir(null)
                        .build());
            } else if (!prunes) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " is locked but no longer resolved, and no edit prunes it");
            }
        }
        return removals;
    }

    /** The manifest surfaces pnpm serializes in ways not yet byte-verified; a fresh or moved node defers on them. */
    private static void requireEmittable(VersionManifest m) {
        deferIf(m, "optionalDependencies", notEmpty(m.getOptionalDependencies()));
        deferIf(m, "peerDependenciesMeta", m.getPeerDependenciesMeta() != null);
        deferIf(m, "bin", m.getBin() != null);
        deferIf(m, "os", m.getOs() != null);
        deferIf(m, "cpu", m.getCpu() != null);
        deferIf(m, "libc", m.getLibc() != null);
        deferIf(m, "deprecated", m.getDeprecated() != null);
        deferIf(m, "hasInstallScript", Boolean.TRUE.equals(m.getHasInstallScript()));
        deferIf(m, "hasShrinkwrap", Boolean.TRUE.equals(m.getHasShrinkwrap()));
        deferIf(m, "bundleDependencies", m.getBundleDependencies() != null);
    }

    private static void deferIf(VersionManifest m, String field, boolean present) {
        if (present) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, m.getName(),
                    m.getName() + "@" + m.getVersion() + " declares " + field + " (entry shape not yet patched)");
        }
    }

    /** pnpm marks a snapshot {@code optional: true} iff optional-reachable; a purely dev-optional node defers. */
    private static void requireOptionalMarkable(ResolvedNode node) {
        if (node.isDevOptional() && !node.isOptional() && !node.isDev()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, node.getName(),
                    node.getName() + " is dev-optional (pnpm snapshot marking not yet patched)");
        }
    }

    private static String declaringScope(ResolutionGraph.Importer root, String name) {
        for (Map.Entry<String, Map<String, String>> scope : root.getDeclared().entrySet()) {
            if (DECLARED_SCOPES.contains(scope.getKey()) && scope.getValue().containsKey(name)) {
                return scope.getKey();
            }
        }
        return "dependencies";
    }

    private static ResolutionGraph.Importer singleRootImporter(ResolutionGraph graph) {
        if (graph.getImporters().size() != 1 || !graph.getImporters().get(0).getDir().isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "only a single root importer can be diffed against the lock");
        }
        ResolutionGraph.Importer root = graph.getImporters().get(0);
        for (String scope : root.getDeclared().keySet()) {
            if (!DECLARED_SCOPES.contains(scope)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "importer declares " + scope + " (scope not yet patched for pnpm)");
            }
        }
        return root;
    }

    private static String nameOf(String key) {
        return key.substring(0, key.lastIndexOf('@'));
    }

    private static String versionOf(String key) {
        return key.substring(key.lastIndexOf('@') + 1);
    }

    private static boolean notEmpty(@Nullable Map<String, String> m) {
        return m != null && !m.isEmpty();
    }

    // --- lock model --------------------------------------------------------

    /** The existing lock, read once: package keys, snapshot keys by bare key, importer declarations. */
    private static final class Lock {
        final List<String> packageKeys = new ArrayList<>();
        final Map<String, List<String>> keysByName = new LinkedHashMap<>();
        final Map<String, String> snapshotKeys = new LinkedHashMap<>();      // bare -> full (suffixed) key
        final Set<String> optionalSnapshots = new LinkedHashSet<>();
        final Map<String, Set<String>> snapshotDepNames = new LinkedHashMap<>();  // bare -> dep names
        final Map<String, ImporterDep> importerDeps = new LinkedHashMap<>();

        static final class ImporterDep {
            final String scope;
            final String specifier;

            ImporterDep(String scope, String specifier) {
                this.scope = scope;
                this.specifier = specifier;
            }
        }

        @Nullable String snapshotKeyOf(String bare) {
            return snapshotKeys.get(bare);
        }

        @SuppressWarnings("unchecked")
        static Lock parse(String existingLock) {
            Map<String, Object> doc;
            try {
                doc = (Map<String, Object>) new Yaml().load(existingLock);
            } catch (Exception e) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, null, "pnpm-lock.yaml could not be parsed");
            }
            if (doc == null || !String.valueOf(doc.get("lockfileVersion")).startsWith("9")) {
                throw new EngineFailure(Reason.UNSUPPORTED_LOCKFILE_VERSION, null,
                        "only pnpm lockfileVersion 9 locks can be diffed");
            }
            Lock lock = new Lock();
            Object importers = doc.get("importers");
            if (importers instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) importers;
                if (map.size() != 1 || !map.containsKey(".")) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                            "only a single-importer pnpm lock can be diffed");
                }
                Object rootScopes = map.get(".");
                if (rootScopes instanceof Map) {
                    for (String scope : DECLARED_SCOPES) {
                        Object deps = ((Map<?, ?>) rootScopes).get(scope);
                        if (deps instanceof Map) {
                            for (Map.Entry<?, ?> dep : ((Map<?, ?>) deps).entrySet()) {
                                Object body = dep.getValue();
                                String specifier = body instanceof Map ?
                                        String.valueOf(((Map<?, ?>) body).get("specifier")) : "";
                                lock.importerDeps.put(String.valueOf(dep.getKey()),
                                        new ImporterDep(scope, specifier));
                            }
                        }
                    }
                }
            }
            Object packages = doc.get("packages");
            if (packages instanceof Map) {
                for (Object key : ((Map<?, ?>) packages).keySet()) {
                    String k = String.valueOf(key);
                    lock.packageKeys.add(k);
                    lock.keysByName.computeIfAbsent(nameOf(k), x -> new ArrayList<>()).add(k);
                }
            }
            Object snapshots = doc.get("snapshots");
            if (snapshots instanceof Map) {
                for (Map.Entry<?, ?> e : ((Map<?, ?>) snapshots).entrySet()) {
                    String full = String.valueOf(e.getKey());
                    int paren = full.indexOf('(');
                    String bare = paren < 0 ? full : full.substring(0, paren);
                    if (lock.snapshotKeys.put(bare, full) != null) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, nameOf(bare),
                                nameOf(bare) + " has multiple peer-suffixed snapshots (not yet diffed)");
                    }
                    if (e.getValue() instanceof Map) {
                        Map<?, ?> body = (Map<?, ?>) e.getValue();
                        if (Boolean.TRUE.equals(body.get("optional"))) {
                            lock.optionalSnapshots.add(full);
                        }
                        Set<String> depNames = new LinkedHashSet<>();
                        for (String depScope : Arrays.asList("dependencies", "optionalDependencies")) {
                            Object deps = body.get(depScope);
                            if (deps instanceof Map) {
                                for (Object dep : ((Map<?, ?>) deps).keySet()) {
                                    depNames.add(String.valueOf(dep));
                                }
                            }
                        }
                        lock.snapshotDepNames.put(bare, depNames);
                    }
                }
            }
            return lock;
        }
    }

    // --- pnpm peer-suffix layout (ported from the deleted whole-file serializer) ---

    /**
     * pnpm's peer-specific snapshot keys and their propagation. A node's key is
     * {@code name@version(providerRef)(providerRef2)…}: one suffix per peer it declares (each satisfied by a
     * single resolved node), sorted by the rendered reference, a provider with peers rendering its own suffix
     * recursively. Only the "every peer is directly declared where it is needed" shape is expressible: a peer
     * reaching a node from its subtree undeclared is pnpm's {@code transitivePeerDependencies}, and an
     * optional/absent/forked peer likewise reshapes the layout — all fail loud.
     */
    static final class PeerLayout {
        private final ResolutionGraph graph;
        private final Map<String, String> versionByName = new HashMap<>();
        private final Set<String> forkedNames = new HashSet<>();
        private final Map<String, String> suffixCache = new HashMap<>();

        PeerLayout(ResolutionGraph graph) {
            this.graph = graph;
            for (Map.Entry<String, ResolvedNode> e : graph.getNodes().entrySet()) {
                String name = nameOf(e.getKey());
                if (versionByName.put(name, e.getValue().getVersion()) != null) {
                    forkedNames.add(name);
                }
            }
            requireNoTransitivePeers();
            for (String key : graph.getNodes().keySet()) {
                snapshotKey(key);
            }
        }

        /** The snapshots key: {@code name@version} with the peer suffix. */
        String snapshotKey(String nodeKey) {
            return nodeKey + suffix(nodeKey);
        }

        /** The importer/edge reference: the resolved version with the peer suffix. */
        String reference(String nodeKey) {
            return graph.getNodes().get(nodeKey).getVersion() + suffix(nodeKey);
        }

        String suffix(String nodeKey) {
            String cached = suffixCache.get(nodeKey);
            return cached != null ? cached : computeSuffix(nodeKey, new LinkedHashSet<>());
        }

        private String computeSuffix(String nodeKey, Set<String> visiting) {
            String cached = suffixCache.get(nodeKey);
            if (cached != null) {
                return cached;
            }
            ResolvedNode node = graph.getNodes().get(nodeKey);
            if (!visiting.add(nodeKey)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, node.getName(),
                        node.getName() + " is in a peer-dependency cycle (not yet patched)");
            }
            List<String> refs = new ArrayList<>();
            Map<String, String> peers = node.getManifest().getPeerDependencies();
            if (peers != null) {
                for (String peerName : peers.keySet()) {
                    String providerKey = providerKey(node, peerName);
                    refs.add(providerKey + computeSuffix(providerKey, visiting));
                }
            }
            Collections.sort(refs);
            StringBuilder sb = new StringBuilder();
            for (String ref : refs) {
                sb.append('(').append(ref).append(')');
            }
            visiting.remove(nodeKey);
            String result = sb.toString();
            suffixCache.put(nodeKey, result);
            return result;
        }

        /** The unique resolved node providing a peer; an absent or forked peer is not expressible here. */
        private String providerKey(ResolvedNode consumer, String peerName) {
            if (forkedNames.contains(peerName)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, consumer.getName(),
                        consumer.getName() + " peer " + peerName + " resolves to multiple versions (peer fork not yet patched)");
            }
            String version = versionByName.get(peerName);
            if (version == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, consumer.getName(),
                        consumer.getName() + " peer " + peerName + " is not installed (optional/auto-install peer not yet patched)");
            }
            return ResolutionGraph.key(peerName, version);
        }

        /** Regular dependency edges merged with satisfied peer edges, each valued by its target's reference. */
        Map<String, String> snapshotDependencies(String nodeKey) {
            ResolvedNode node = graph.getNodes().get(nodeKey);
            Map<String, String> deps = new LinkedHashMap<>();
            for (Map.Entry<String, String> edge : node.getResolvedEdges().entrySet()) {
                deps.put(edge.getKey(), reference(ResolutionGraph.key(edge.getKey(), edge.getValue())));
            }
            Map<String, String> peers = node.getManifest().getPeerDependencies();
            if (peers != null) {
                for (String peerName : peers.keySet()) {
                    if (deps.containsKey(peerName)) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, node.getName(),
                                node.getName() + " lists " + peerName + " as both a dependency and a peer (not yet patched)");
                    }
                    deps.put(peerName, reference(providerKey(node, peerName)));
                }
            }
            return deps;
        }

        /**
         * Fail loud if any node would carry {@code transitivePeerDependencies} — a peer required in its subtree
         * that it neither declares nor resolves through a dependency of its own.
         */
        private void requireNoTransitivePeers() {
            Map<String, Set<String>> cache = new HashMap<>();
            for (String nodeKey : graph.getNodes().keySet()) {
                ResolvedNode node = graph.getNodes().get(nodeKey);
                Set<String> declared = declaredPeerNames(node);
                for (String peer : peerContext(nodeKey, cache, new LinkedHashSet<>())) {
                    if (!declared.contains(peer)) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, node.getName(),
                                node.getName() + " has a transitive peer " + peer +
                                        " (pnpm transitivePeerDependencies not yet patched)");
                    }
                }
            }
        }

        /** The peers a node still needs from above: its own plus its children's, minus what it provides. */
        private Set<String> peerContext(String nodeKey, Map<String, Set<String>> cache, Set<String> visiting) {
            Set<String> cached = cache.get(nodeKey);
            if (cached != null) {
                return cached;
            }
            ResolvedNode node = graph.getNodes().get(nodeKey);
            if (!visiting.add(nodeKey)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, node.getName(),
                        node.getName() + " is in a peer-dependency cycle (not yet patched)");
            }
            Set<String> provided = node.getResolvedEdges().keySet();
            Set<String> context = new LinkedHashSet<>();
            for (Map.Entry<String, String> edge : node.getResolvedEdges().entrySet()) {
                for (String peer : peerContext(ResolutionGraph.key(edge.getKey(), edge.getValue()), cache, visiting)) {
                    if (!provided.contains(peer)) {
                        context.add(peer);
                    }
                }
            }
            context.addAll(declaredPeerNames(node));
            visiting.remove(nodeKey);
            cache.put(nodeKey, context);
            return context;
        }

        private static Set<String> declaredPeerNames(ResolvedNode node) {
            Map<String, String> peers = node.getManifest().getPeerDependencies();
            return peers == null ? Collections.emptySet() : peers.keySet();
        }
    }
}
