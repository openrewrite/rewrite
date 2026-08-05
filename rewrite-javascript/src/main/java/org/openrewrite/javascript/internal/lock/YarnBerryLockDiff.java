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
import org.openrewrite.javascript.internal.registry.VersionManifest;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

/**
 * Diffs a freshly resolved {@link ResolutionGraph} against the existing Yarn Berry {@code yarn.lock} and expresses
 * the difference as {@link PackageEdit}s for {@link YarnBerryLockPatcher}. Berry's lock is flat — one entry per
 * resolved {@code (name, version)} keyed by its merged {@code name@npm:range} descriptor(s) — so matching is by
 * name: an entry whose version and descriptor set both agree with the resolution is untouched (its recorded
 * checksum kept, no tarball fetched); a version that moved on a single-range descriptor is an in-place bump or
 * forced move (its checksum reproduced afterwards); fresh nodes become adds; entries the resolution no longer
 * reaches are removed through the importer edge and the orphan GC. A merged descriptor that would need re-heading,
 * a fork, or any other shape the patcher cannot express byte-exact fails loud.
 */
final class YarnBerryLockDiff {

    private static final List<String> DECLARED_SCOPES =
            Arrays.asList("dependencies", "devDependencies", "optionalDependencies");

    private YarnBerryLockDiff() {
    }

    static List<PackageEdit> diff(ResolutionGraph graph, String existingLock) {
        ResolutionGraph.Importer root = singleRootImporter(graph);
        Lock lock = Lock.parse(existingLock);
        Map<String, Set<String>> descriptors = collectDescriptors(graph);

        Map<String, ResolvedNode> byName = new LinkedHashMap<>();
        for (Map.Entry<String, ResolvedNode> e : graph.getNodes().entrySet()) {
            String name = nameOf(e.getKey());
            if (byName.put(name, e.getValue()) != null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " forks; the yarn berry fork layout is not yet patched");
            }
            if (!name.equals(e.getValue().getName())) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " aliases " + e.getValue().getName() + "; a berry alias entry is not yet patched");
            }
        }

        List<PackageEdit> edits = new ArrayList<>();
        boolean prunes = false;
        for (Map.Entry<String, ResolvedNode> e : byName.entrySet()) {
            String name = e.getKey();
            ResolvedNode node = e.getValue();
            Set<String> targetRanges = descriptors.get(ResolutionGraph.key(name, node.getVersion()));
            if (targetRanges == null || targetRanges.isEmpty()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + "@" + node.getVersion() + " resolved but no descriptor requires it");
            }
            Lock.Entry entry = lock.byName.get(name);
            PackageEdit edit = entry == null ?
                    addEdit(root, node, targetRanges) :
                    boundEdit(root, node, entry, targetRanges);
            if (edit != null) {
                prunes |= edit.isPrunesOrphans();
                edits.add(edit);
            }
        }

        for (Map.Entry<String, Lock.Entry> e : lock.byName.entrySet()) {
            if (byName.containsKey(e.getKey())) {
                continue;
            }
            String name = e.getKey();
            if (lock.workspaceDeps.containsKey(name)) {
                // Declared before, gone now: dropping the importer edge lets the orphan GC reap the subtree.
                edits.add(PackageEdit.builder()
                        .name(name)
                        .oldVersion(e.getValue().version)
                        .newVersion(null)
                        .scope("dependencies")
                        .importerDir(null)
                        .build());
            } else if (!prunes) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " is locked but no longer resolved, and no edit prunes it");
            }
        }
        return edits;
    }

    private static PackageEdit addEdit(ResolutionGraph.Importer root, ResolvedNode node, Set<String> targetRanges) {
        VersionManifest m = node.getManifest();
        requireEmittable(m);
        if (targetRanges.size() > 1) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, m.getName(),
                    m.getName() + " is required at multiple ranges; a merged berry descriptor is not yet patched");
        }
        VersionManifest.Dist dist = m.getDist();
        if (dist == null || dist.getTarball() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, m.getName(),
                    m.getName() + "@" + m.getVersion() + " has no registry tarball to checksum");
        }
        return PackageEdit.builder()
                .name(m.getName())
                .oldVersion("")
                .newVersion(m.getVersion())
                .newResolved(dist.getTarball())
                .newDependencies(notEmpty(m.getDependencies()) ? m.getDependencies() : null)
                .metadata(peerMetadata(m))
                .scope(declaringScope(root, m.getName()))
                .importerDir(null)
                .kind(PackageEdit.Kind.ADD)
                .build();
    }

    /**
     * A name the lock already resolves. Same version and descriptor set: untouched. A single-range descriptor
     * whose version moved: an in-place bump (declared) or forced move (transitive), the checksum reproduced
     * afterwards. A re-headed or merged descriptor defers.
     */
    private static @Nullable PackageEdit boundEdit(ResolutionGraph.Importer root, ResolvedNode node,
                                                   Lock.Entry entry, Set<String> targetRanges) {
        String name = node.getName();
        boolean moves = !node.getVersion().equals(entry.version);
        if (!moves && entry.ranges.equals(targetRanges)) {
            requirePeersUnchanged(name, node.getManifest(), entry);
            return null;
        }
        if (entry.ranges.size() > 1 || targetRanges.size() > 1) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + "'s merged berry descriptor cannot yet be re-headed");
        }
        String oldRange = entry.ranges.iterator().next();
        String newRange = targetRanges.iterator().next();
        VersionManifest m = node.getManifest();
        requireEmittable(m);
        requirePeersUnchanged(name, m, entry);
        String declared = declaredRange(root, name);
        VersionManifest.Dist dist = m.getDist();
        if (moves && (dist == null || dist.getTarball() == null)) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name,
                    name + "@" + m.getVersion() + " has no registry tarball to checksum");
        }
        Map<String, String> newDeps = m.getDependencies() == null ? Collections.emptyMap() : m.getDependencies();
        boolean dropped = !newDeps.keySet().containsAll(entry.depNames);
        if (moves && !entry.depNames.containsAll(newDeps.keySet())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " gained a dependency edge on upgrade (not yet patched)");
        }
        PackageEdit.PackageEditBuilder edit = PackageEdit.builder()
                .name(name)
                .oldVersion(entry.version)
                .newVersion(node.getVersion())
                .newResolved(moves ? dist.getTarball() : null)
                .newDependencies(moves && !newDeps.isEmpty() ? newDeps : null)
                .oldConstraint(oldRange)
                .scope(declaringScope(root, name))
                .importerDir(null)
                .prunesOrphans(moves && dropped);
        if (declared != null && declared.equals(newRange)) {
            // The patcher re-heads the descriptor to the edited manifest's declared range and repins the importer.
            return edit.kind(PackageEdit.Kind.BUMP).build();
        }
        return edit.kind(PackageEdit.Kind.FORCED_MOVE).newConstraint(newRange).build();
    }

    /** The berry bump rewrites resolution and dependencies but never the peer blocks; a peer delta defers. */
    private static void requirePeersUnchanged(String name, VersionManifest m, Lock.Entry entry) {
        Map<String, String> peers = m.getPeerDependencies() == null ? Collections.emptyMap() : m.getPeerDependencies();
        if (!peers.equals(entry.peerDependencies)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + "'s peerDependencies changed (berry peer rewrite not yet patched)");
        }
    }

    private static @Nullable EntryMetadata peerMetadata(VersionManifest m) {
        boolean any = notEmpty(m.getPeerDependencies()) ||
                (m.getPeerDependenciesMeta() != null && m.getPeerDependenciesMeta().size() > 0);
        if (!any) {
            return null;
        }
        return EntryMetadata.builder()
                .peerDependencies(notEmpty(m.getPeerDependencies()) ? m.getPeerDependencies() : null)
                .peerDependenciesMeta(m.getPeerDependenciesMeta())
                .build();
    }

    /** The entry fields berry serializes that are not yet byte-verified; a fresh or moved node defers on them. */
    private static void requireEmittable(VersionManifest m) {
        deferIf(m, "optionalDependencies", notEmpty(m.getOptionalDependencies()));
        deferIf(m, "bin", m.getBin() != null);
        deferIf(m, "os", m.getOs() != null);
        deferIf(m, "cpu", m.getCpu() != null);
        deferIf(m, "libc", m.getLibc() != null);
        deferIf(m, "bundleDependencies", m.getBundleDependencies() != null);
    }

    private static void deferIf(VersionManifest m, String field, boolean present) {
        if (present) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, m.getName(),
                    m.getName() + "@" + m.getVersion() + " declares " + field + " (berry entry shape not yet patched)");
        }
    }

    /**
     * For each resolved node, the merged set of ranges — the importer's declared one and every transitive
     * requirer's — that resolved to it.
     */
    private static Map<String, Set<String>> collectDescriptors(ResolutionGraph graph) {
        Map<String, Set<String>> byNode = new TreeMap<>();
        for (ResolutionGraph.Importer imp : graph.getImporters()) {
            for (Map.Entry<String, Map<String, String>> scope : imp.getDeclared().entrySet()) {
                if ("peerDependencies".equals(scope.getKey())) {
                    continue;
                }
                for (Map.Entry<String, String> dep : scope.getValue().entrySet()) {
                    add(byNode, dep.getKey(), imp.getResolved().get(dep.getKey()), dep.getValue());
                }
            }
        }
        for (ResolvedNode node : graph.getNodes().values()) {
            Map<String, String> deps = node.getManifest().getDependencies();
            if (deps != null) {
                for (Map.Entry<String, String> dep : deps.entrySet()) {
                    add(byNode, dep.getKey(), node.getResolvedEdges().get(dep.getKey()), dep.getValue());
                }
            }
        }
        return byNode;
    }

    private static void add(Map<String, Set<String>> byNode, String name, @Nullable String version, String range) {
        if (version != null) {
            byNode.computeIfAbsent(ResolutionGraph.key(name, version), k -> new TreeSet<>()).add(range);
        }
    }

    private static String declaringScope(ResolutionGraph.Importer root, String name) {
        for (String scope : DECLARED_SCOPES) {
            Map<String, String> deps = root.getDeclared().get(scope);
            if (deps != null && deps.containsKey(name)) {
                return scope;
            }
        }
        return "dependencies";
    }

    private static @Nullable String declaredRange(ResolutionGraph.Importer root, String name) {
        for (String scope : DECLARED_SCOPES) {
            Map<String, String> deps = root.getDeclared().get(scope);
            if (deps != null && deps.containsKey(name)) {
                return deps.get(name);
            }
        }
        return null;
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
                        "importer declares " + scope + " (scope not yet patched for yarn berry)");
            }
        }
        return root;
    }

    private static String nameOf(String key) {
        return key.substring(0, key.lastIndexOf('@'));
    }

    private static boolean notEmpty(@Nullable Map<String, String> m) {
        return m != null && !m.isEmpty();
    }

    // --- lock model --------------------------------------------------------

    /** The existing lock's registry entries by name, plus the workspace importer's merged dependency map. */
    private static final class Lock {
        final Map<String, Entry> byName = new LinkedHashMap<>();
        final Map<String, String> workspaceDeps = new LinkedHashMap<>();

        static final class Entry {
            final String version;
            final Set<String> ranges;
            final Set<String> depNames;
            final Map<String, String> peerDependencies;

            Entry(String version, Set<String> ranges, Set<String> depNames, Map<String, String> peerDependencies) {
                this.version = version;
                this.ranges = ranges;
                this.depNames = depNames;
                this.peerDependencies = peerDependencies;
            }
        }

        @SuppressWarnings("unchecked")
        static Lock parse(String existingLock) {
            Map<String, Object> doc;
            try {
                doc = (Map<String, Object>) new Yaml().load(existingLock);
            } catch (Exception e) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, null, "yarn.lock could not be parsed");
            }
            if (doc == null || !(doc.get("__metadata") instanceof Map)) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, null, "yarn.lock has no __metadata block");
            }
            Lock lock = new Lock();
            for (Map.Entry<String, Object> e : doc.entrySet()) {
                String key = e.getKey();
                if ("__metadata".equals(key) || !(e.getValue() instanceof Map)) {
                    continue;
                }
                Map<?, ?> body = (Map<?, ?>) e.getValue();
                String resolution = String.valueOf(body.get("resolution"));
                if (resolution.contains("@workspace:")) {
                    Object deps = body.get("dependencies");
                    if (deps instanceof Map) {
                        for (Map.Entry<?, ?> dep : ((Map<?, ?>) deps).entrySet()) {
                            lock.workspaceDeps.put(String.valueOf(dep.getKey()), String.valueOf(dep.getValue()));
                        }
                    }
                    continue;
                }
                int npm = resolution.lastIndexOf("@npm:");
                if (npm <= 0) {
                    throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, key,
                            key + " does not resolve to a registry entry");
                }
                String name = resolution.substring(0, npm);
                String version = resolution.substring(npm + "@npm:".length());
                Set<String> ranges = new TreeSet<>();
                for (String descriptor : key.split(", ")) {
                    int at = descriptor.lastIndexOf("@npm:");
                    if (at <= 0) {
                        throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, name,
                                name + " has a non-registry descriptor " + descriptor);
                    }
                    ranges.add(descriptor.substring(at + "@npm:".length()));
                }
                Set<String> depNames = new LinkedHashSet<>();
                Object deps = body.get("dependencies");
                if (deps instanceof Map) {
                    for (Object dep : ((Map<?, ?>) deps).keySet()) {
                        depNames.add(String.valueOf(dep));
                    }
                }
                Map<String, String> peers = new LinkedHashMap<>();
                Object peerDeps = body.get("peerDependencies");
                if (peerDeps instanceof Map) {
                    for (Map.Entry<?, ?> peer : ((Map<?, ?>) peerDeps).entrySet()) {
                        peers.put(String.valueOf(peer.getKey()), String.valueOf(peer.getValue()));
                    }
                }
                if (lock.byName.put(name, new Entry(version, ranges, depNames, peers)) != null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            name + " is locked at multiple versions; the berry fork layout is not yet patched");
                }
            }
            return lock;
        }
    }
}
