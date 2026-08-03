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

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.EntryMetadata;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.internal.lock.resolve.ResolutionGraph;
import org.openrewrite.javascript.internal.lock.resolve.ResolvedNode;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.*;

/**
 * Diffs a freshly resolved {@link ResolutionGraph} against the existing {@code bun.lock} (JSONC) and expresses
 * the difference as {@link PackageEdit}s for {@link BunLockPatcher} — so untouched entries keep their bytes and
 * only what the resolution actually changed is rewritten. bun records npm-style hoisted placement as
 * {@code packages} tuples keyed {@code name} (top-level) or {@code parent/name} (nested one level under the
 * package that requires the conflicting version). Nodes are matched to installed keys slot-by-slot (the root's
 * declared version claims the top slot, then exact version matches, then a lone leftover pair is an in-place
 * move); an unmatched node takes a free top slot, or nests under its unique top-level requirer when the top
 * slot holds another version and that version is the directly-declared one; unmatched lock keys become
 * removals. A difference the patcher cannot express byte-exactly fails loud rather than guess.
 */
final class BunLockDiff {

    /** bun.lock is JSONC (trailing commas, comments). */
    private static final ObjectMapper JSON = JsonMapper.builder()
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)
            .build();
    private static final List<String> DECLARED_SCOPES =
            Arrays.asList("dependencies", "devDependencies", "optionalDependencies");

    private BunLockDiff() {
    }

    static List<PackageEdit> diff(ResolutionGraph graph, String existingLock) {
        ResolutionGraph.Importer root = singleRootImporter(graph);
        requireNoRootPeers(root);
        Lock lock = Lock.parse(existingLock);
        requireRootWorkspaceMirrors(root, lock);
        requireScopeMembershipsCurrent(graph, root, lock);

        Map<String, String> bindings = bindNodesToKeys(graph, root, lock);
        Set<String> fresh = placeUnbound(graph, root, bindings);

        List<PackageEdit> edits = new ArrayList<>();
        boolean prunes = false;
        for (Map.Entry<String, ResolvedNode> e : graph.getNodes().entrySet()) {
            String nodeKey = e.getKey();
            String slot = slotOfNode(nodeKey);
            ResolvedNode node = e.getValue();
            String key = bindings.get(nodeKey);
            PackageEdit edit = fresh.contains(nodeKey) ?
                    addEdit(root, slot, node, key) :
                    boundEdit(root, lock, slot, node, key);
            if (edit != null) {
                prunes |= edit.isPrunesOrphans();
                edits.add(edit);
            }
        }
        edits.addAll(removalEdits(root, lock, bindings, prunes));
        return edits;
    }

    // --- matching ----------------------------------------------------------

    /**
     * Bind graph nodes to installed keys slot-by-slot: the root's declared version claims the top slot,
     * remaining versions match keys holding exactly that version, and a lone leftover pair binds as an
     * in-place move. Anything else is ambiguous and fails loud.
     */
    private static Map<String, String> bindNodesToKeys(ResolutionGraph graph, ResolutionGraph.Importer root,
                                                       Lock lock) {
        Map<String, List<String>> graphSlots = new LinkedHashMap<>();
        for (String nodeKey : graph.getNodes().keySet()) {
            graphSlots.computeIfAbsent(slotOfNode(nodeKey), k -> new ArrayList<>()).add(nodeKey);
        }

        Map<String, String> bindings = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : graphSlots.entrySet()) {
            String slot = e.getKey();
            List<String> nodeKeys = new ArrayList<>(e.getValue());
            List<String> lockKeys = new ArrayList<>(lock.keysBySlot.getOrDefault(slot, Collections.emptyList()));

            String declared = root.getResolved().get(slot);
            if (declared != null && lockKeys.remove(slot)) {
                nodeKeys.remove(ResolutionGraph.key(slot, declared));
                bindings.put(ResolutionGraph.key(slot, declared), slot);
            }
            for (Iterator<String> it = nodeKeys.iterator(); it.hasNext(); ) {
                String nodeKey = it.next();
                String version = versionOfNode(nodeKey);
                List<String> matches = new ArrayList<>();
                for (String lockKey : lockKeys) {
                    if (version.equals(lock.tuples.get(lockKey).version)) {
                        matches.add(lockKey);
                    }
                }
                if (matches.size() > 1) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                            slot + "@" + version + " is installed at multiple places (" + matches + ")");
                }
                if (matches.size() == 1) {
                    bindings.put(nodeKey, matches.get(0));
                    lockKeys.remove(matches.get(0));
                    it.remove();
                }
            }
            if (nodeKeys.size() == 1 && lockKeys.size() == 1) {
                bindings.put(nodeKeys.get(0), lockKeys.get(0));
            } else if (!nodeKeys.isEmpty() && !lockKeys.isEmpty()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                        slot + " resolves to versions whose installed places cannot be matched unambiguously");
            }
            // Leftover graph versions place fresh; leftover lock keys become removals.
        }
        return bindings;
    }

    /**
     * Place every unbound node the way bun does: the single version of a name — or a fork's directly-declared
     * version — sits top-level under its bare name, and a fork's other version nests as {@code parent/name}
     * under its unique top-level requirer. A shape bun has not been byte-verified on (a three-version fork, a
     * transitive-only fork the lock did not decide, a multi-requirer or nested-requirer nest) fails loud.
     * Returns the freshly placed node keys.
     */
    private static Set<String> placeUnbound(ResolutionGraph graph, ResolutionGraph.Importer root,
                                            Map<String, String> bindings) {
        Map<String, Set<String>> versionsByName = new LinkedHashMap<>();
        for (String nodeKey : graph.getNodes().keySet()) {
            versionsByName.computeIfAbsent(slotOfNode(nodeKey), k -> new LinkedHashSet<>()).add(versionOfNode(nodeKey));
        }
        Set<String> occupiedKeys = new HashSet<>(bindings.values());
        Set<String> fresh = new LinkedHashSet<>();

        List<String> nestLater = new ArrayList<>();
        for (String nodeKey : graph.getNodes().keySet()) {
            if (bindings.containsKey(nodeKey)) {
                continue;
            }
            String name = slotOfNode(nodeKey);
            String version = versionOfNode(nodeKey);
            Set<String> versions = versionsByName.get(name);
            if (versions.size() > 2) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " resolves to " + versions.size() + " versions (only a two-version fork is placed)");
            }
            String declared = root.getResolved().get(name);
            if (versions.size() == 1 || version.equals(declared)) {
                if (!occupiedKeys.add(name)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            name + "@" + version + " belongs top-level but the slot is taken");
                }
                bindings.put(nodeKey, name);
                fresh.add(nodeKey);
            } else if (declared != null && versions.contains(declared)) {
                nestLater.add(nodeKey);
            } else {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " forks but neither version is directly declared (top slot not decidable)");
            }
        }
        for (String nodeKey : nestLater) {
            nestUnderRequirer(graph, bindings, occupiedKeys, fresh, nodeKey);
        }

        for (String nodeKey : graph.getNodes().keySet()) {
            if (!bindings.containsKey(nodeKey)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slotOfNode(nodeKey),
                        nodeKey + " resolved but was not placed");
            }
        }
        return fresh;
    }

    private static void nestUnderRequirer(ResolutionGraph graph, Map<String, String> bindings,
                                          Set<String> occupiedKeys, Set<String> fresh, String nodeKey) {
        String name = slotOfNode(nodeKey);
        String version = versionOfNode(nodeKey);
        ResolvedNode parent = null;
        for (ResolvedNode candidate : graph.getNodes().values()) {
            if (version.equals(candidate.getResolvedEdges().get(name))) {
                if (parent != null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            name + "@" + version + " is required by multiple packages (nested placement ambiguous)");
                }
                parent = candidate;
            }
        }
        if (parent == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + "@" + version + " has no requiring package (cannot place its nested copy)");
        }
        if (!parent.getName().equals(bindings.get(ResolutionGraph.key(parent.getName(), parent.getVersion())))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + "@" + version + " nests under " + parent.getName() + " which is not top-level");
        }
        String key = parent.getName() + "/" + name;
        if (!occupiedKeys.add(key)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name, key + " is already installed");
        }
        bindings.put(nodeKey, key);
        fresh.add(nodeKey);
    }

    // --- edits -------------------------------------------------------------

    private static PackageEdit addEdit(ResolutionGraph.Importer root, String slot, ResolvedNode node,
                                       String key) {
        VersionManifest m = node.getManifest();
        if (!slot.equals(m.getName())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                    slot + " aliases " + m.getName() + "; a fresh alias tuple cannot yet be patched in");
        }
        requireTupleExpressible(m);
        List<String> optionalPeers = optionalPeers(m);
        VersionManifest.Dist dist = m.getDist();
        if (dist == null || dist.getIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, slot,
                    slot + "@" + m.getVersion() + " has no registry integrity");
        }
        EntryMetadata metadata = null;
        if (notEmpty(m.getPeerDependencies())) {
            metadata = EntryMetadata.builder()
                    .peerDependencies(m.getPeerDependencies())
                    .peerDependenciesMeta(optionalPeers == null ? null : m.getPeerDependenciesMeta())
                    .build();
        }
        return PackageEdit.builder()
                .name(slot)
                .oldVersion("")
                .newVersion(m.getVersion())
                .newIntegrity(dist.getIntegrity())
                .newDependencies(notEmpty(m.getDependencies()) ? m.getDependencies() : null)
                .scope(declaringScope(root, slot))
                .importerDir(null)
                .kind(PackageEdit.Kind.ADD)
                .nestedUnder(key.equals(slot) ? null : key.substring(0, key.length() - slot.length() - 1))
                .metadata(metadata)
                .build();
    }

    /**
     * The edit for a node the lock already installs: nothing when key, version and declared constraint all
     * match; a promotion or constraint re-pin when just the importer edge changed; an in-place move (bump for
     * a root-declared dependency, forced move for a transitive) when the version changed, provided every
     * tuple-metadata surface beyond {@code dependencies} carries over unchanged.
     */
    private static @Nullable PackageEdit boundEdit(ResolutionGraph.Importer root, Lock lock, String slot,
                                                   ResolvedNode node, String key) {
        Tuple entry = lock.tuples.get(key);
        String oldVersion = entry.version;
        boolean moves = !node.getVersion().equals(oldVersion);
        String scope = declaringScope(root, slot);
        String declared = node.getVersion().equals(root.getResolved().get(slot)) ? declaredRange(root, slot) : null;
        boolean promotion = !moves && declared != null && !lock.rootScopeHas(scope, slot);
        boolean constraintChanged = declared != null && !declared.equals(lock.rootScopeValue(scope, slot));

        if (!moves && !promotion && !constraintChanged) {
            return null;
        }
        PackageEdit.PackageEditBuilder edit = PackageEdit.builder()
                .name(slot)
                .oldVersion(oldVersion)
                .newVersion(node.getVersion())
                .scope(scope)
                .importerDir(null);
        if (promotion) {
            // Declared now but installed already: only the workspace edge is written; the tuple stays.
            return edit.kind(PackageEdit.Kind.PROMOTION).build();
        }
        if (!moves) {
            return edit.build();
        }

        VersionManifest m = node.getManifest();
        requireTupleExpressible(m);
        requireMetadataCarriesOver(slot, entry, m);
        VersionManifest.Dist dist = m.getDist();
        if (dist == null || dist.getIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, slot,
                    slot + "@" + m.getVersion() + " has no registry integrity");
        }
        EdgeDelta delta = edgeDelta(entry, m);
        return edit.kind(declared != null ? PackageEdit.Kind.BUMP : PackageEdit.Kind.FORCED_MOVE)
                .newIntegrity(dist.getIntegrity())
                .newDependencies(notEmpty(m.getDependencies()) ? m.getDependencies() : null)
                .prunesOrphans(delta.dropped)
                .addsDependencyEdges(delta.added)
                .build();
    }

    /** How a moved entry's dependency edge set changed: drops orphan-prune, gains graft fresh subtrees. */
    private static EdgeDelta edgeDelta(Tuple entry, VersionManifest m) {
        Set<String> oldEdges = fieldKeys(entry.metadata.get("dependencies"));
        Set<String> newEdges = m.getDependencies() == null ? Collections.emptySet() : m.getDependencies().keySet();
        return new EdgeDelta(!oldEdges.containsAll(newEdges), !newEdges.containsAll(oldEdges));
    }

    private static final class EdgeDelta {
        final boolean added;
        final boolean dropped;

        EdgeDelta(boolean added, boolean dropped) {
            this.added = added;
            this.dropped = dropped;
        }
    }

    // --- expressibility guards ---------------------------------------------

    /**
     * bun's text tuple carries {@code dependencies} and a peer surface ({@code peerDependencies} verbatim, its
     * optional peers flattened into {@code optionalPeers}). A manifest with any other field bun folds into the
     * tuple metadata reshapes the entry in a way not yet byte-verified, so it defers rather than guess.
     */
    private static void requireTupleExpressible(VersionManifest m) {
        deferIfSurface(m, "optionalDependencies", notEmpty(m.getOptionalDependencies()));
        deferIfSurface(m, "bin", m.getBin() != null);
        deferIfSurface(m, "os", m.getOs() != null);
        deferIfSurface(m, "cpu", m.getCpu() != null);
        deferIfSurface(m, "libc", m.getLibc() != null);
        deferIfSurface(m, "bundleDependencies", m.getBundleDependencies() != null);
    }

    private static void deferIfSurface(VersionManifest m, String field, boolean present) {
        if (present) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, m.getName(),
                    m.getName() + "@" + m.getVersion() + " declares " + field + " (entry shape not yet patched)");
        }
    }

    /**
     * On a version change the patcher rewrites only the tuple's locator, integrity and {@code dependencies}
     * map; every other metadata member must carry over byte-identical, so a peer-surface change or any member
     * beyond those three defers.
     */
    private static void requireMetadataCarriesOver(String slot, Tuple entry, VersionManifest m) {
        Map<String, String> oldPeers = stringMap(entry.metadata.get("peerDependencies"));
        Map<String, String> newPeers = m.getPeerDependencies() == null ? Collections.emptyMap() : m.getPeerDependencies();
        if (!oldPeers.equals(newPeers)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                    slot + " changed its peerDependencies on upgrade (not yet patched)");
        }
        List<String> oldOptionalPeers = stringList(entry.metadata.get("optionalPeers"));
        List<String> newOptionalPeers = optionalPeers(m);
        if (!Objects.equals(oldOptionalPeers, newOptionalPeers)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                    slot + " changed its optional peers on upgrade (not yet patched)");
        }
        for (String member : fieldKeys(entry.metadata)) {
            if (!"dependencies".equals(member) && !"peerDependencies".equals(member) && !"optionalPeers".equals(member)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                        slot + " carries " + member + " in its lock entry (a version change is not yet patched)");
            }
        }
    }

    /**
     * The names bun flattens into the tuple's {@code optionalPeers} array: peers flagged optional in
     * {@code peerDependenciesMeta}, ASCII-sorted. A meta entry that marks a name optional without declaring it
     * a peer is a shape bun has not been byte-verified on, so it defers.
     */
    private static @Nullable List<String> optionalPeers(VersionManifest m) {
        JsonNode meta = m.getPeerDependenciesMeta();
        if (meta == null || !meta.isObject() || meta.size() == 0) {
            return null;
        }
        Map<String, String> peers = m.getPeerDependencies();
        List<String> optional = new ArrayList<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = meta.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> e = it.next();
            if (e.getValue().path("optional").asBoolean(false)) {
                if (peers == null || !peers.containsKey(e.getKey())) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, m.getName(),
                            m.getName() + " marks " + e.getKey() + " optional but does not declare it as a peer");
                }
                optional.add(e.getKey());
            }
        }
        optional.sort(null);
        return optional.isEmpty() ? null : optional;
    }

    // --- removals ----------------------------------------------------------

    /**
     * Installed keys the resolution no longer reaches. A root-declared dependency that disappeared is a removal
     * edit (the patcher drops the workspace edge, the tuple, and the orphaned subtree); an undeclared leftover
     * rides an orphan-pruning bump's GC, and with no such bump nothing can prove it collectable, so it defers.
     */
    private static List<PackageEdit> removalEdits(ResolutionGraph.Importer root, Lock lock,
                                                  Map<String, String> bindings, boolean prunes) {
        Set<String> boundKeys = new HashSet<>(bindings.values());
        List<PackageEdit> removals = new ArrayList<>();
        for (String key : lock.installedKeys) {
            if (boundKeys.contains(key)) {
                continue;
            }
            String slot = lock.tuples.get(key).name;
            String declaredScope = null;
            for (String scope : DECLARED_SCOPES) {
                if (lock.rootScopeHas(scope, slot)) {
                    declaredScope = scope;
                    break;
                }
            }
            if (declaredScope != null && key.equals(slot) && root.getResolved().get(slot) == null) {
                removals.add(PackageEdit.builder()
                        .name(slot)
                        .oldVersion(lock.tuples.get(key).version)
                        .newVersion(null)
                        .scope(declaredScope)
                        .importerDir(null)
                        .build());
            } else if (!prunes) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                        slot + " is installed but no longer resolved, and no edit prunes it");
            }
        }
        return removals;
    }

    // --- lock model --------------------------------------------------------

    /** One installed {@code packages} tuple: its locator name/version and metadata object (element 2). */
    private static final class Tuple {
        final String name;
        final String version;
        final JsonNode metadata;

        Tuple(String name, String version, JsonNode metadata) {
            this.name = name;
            this.version = version;
            this.metadata = metadata;
        }
    }

    /** The existing lock's installed entries, read once: keys, per-key tuple, per-slot keys, root workspace. */
    private static final class Lock {
        final JsonNode rootWorkspace;
        final List<String> installedKeys = new ArrayList<>();
        final Map<String, Tuple> tuples = new LinkedHashMap<>();
        final Map<String, List<String>> keysBySlot = new LinkedHashMap<>();

        private Lock(JsonNode rootWorkspace) {
            this.rootWorkspace = rootWorkspace;
        }

        static Lock parse(String existingLock) {
            JsonNode root;
            try {
                root = JSON.readTree(existingLock);
            } catch (Exception e) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, null, "bun.lock could not be parsed");
            }
            JsonNode lockfileVersion = root.get("lockfileVersion");
            if (lockfileVersion != null && lockfileVersion.isInt() && lockfileVersion.asInt() != 1) {
                throw new EngineFailure(Reason.UNSUPPORTED_LOCKFILE_VERSION, null,
                        "bun.lock lockfileVersion " + lockfileVersion.asInt() + " is not supported (need 1)");
            }
            JsonNode configVersion = root.get("configVersion");
            if (configVersion != null && configVersion.isInt() && configVersion.asInt() != 1) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "bun.lock configVersion " + configVersion.asInt() + " is not supported (need 1)");
            }
            JsonNode packages = root.path("packages");
            if (!packages.isObject()) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, null, "bun.lock has no packages map");
            }
            Lock lock = new Lock(root.path("workspaces").path(""));
            packages.fields().forEachRemaining(f -> {
                String key = f.getKey();
                JsonNode tuple = f.getValue();
                if (!tuple.isArray() || tuple.size() < 4 || !tuple.get(0).isTextual() || !tuple.get(2).isObject()) {
                    throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, key,
                            key + " is not a registry package tuple");
                }
                String locator = tuple.get(0).asText();
                int at = locator.lastIndexOf('@');
                if (at <= 0 || locator.indexOf(':', at) >= 0) {
                    throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, key,
                            key + " does not resolve to a registry version: " + locator);
                }
                String name = locator.substring(0, at);
                if (!slotOfKey(key).equals(name)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, key,
                            key + " aliases " + name + " (aliased tuples are not yet patched)");
                }
                lock.installedKeys.add(key);
                lock.tuples.put(key, new Tuple(name, locator.substring(at + 1), tuple.get(2)));
                lock.keysBySlot.computeIfAbsent(name, k -> new ArrayList<>()).add(key);
            });
            return lock;
        }

        boolean rootScopeHas(String scope, String name) {
            return rootWorkspace.path(scope).has(name);
        }

        @Nullable
        String rootScopeValue(String scope, String name) {
            JsonNode value = rootWorkspace.path(scope).get(name);
            return value == null ? null : value.asText();
        }
    }

    // --- structural guards -------------------------------------------------

    private static ResolutionGraph.Importer singleRootImporter(ResolutionGraph graph) {
        if (graph.getImporters().size() != 1 || !graph.getImporters().get(0).getDir().isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "only a single root importer can be diffed against the lock");
        }
        return graph.getImporters().get(0);
    }

    private static void requireNoRootPeers(ResolutionGraph.Importer root) {
        if (!root.getDeclared().getOrDefault("peerDependencies", Collections.emptyMap()).isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "the manifest declares peerDependencies (bun workspace peer scopes are not yet patched)");
        }
    }

    private static void requireRootWorkspaceMirrors(ResolutionGraph.Importer root, Lock lock) {
        String lockName = lock.rootWorkspace.path("name").asText(null);
        if (lockName != null && root.getName() != null && !lockName.equals(root.getName())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "the manifest's name changed; the workspace entry cannot yet be rewritten");
        }
    }

    /**
     * The workspace scopes mirror the manifest. A dependency that moved between scopes, or one no longer
     * declared but still resolved (its workspace edge alone would have to go), is a reshape the patcher does
     * not express yet; a fully-removed dependency falls through to {@link #removalEdits}.
     */
    private static void requireScopeMembershipsCurrent(ResolutionGraph graph, ResolutionGraph.Importer root,
                                                       Lock lock) {
        Set<String> resolvedNames = new HashSet<>();
        for (String nodeKey : graph.getNodes().keySet()) {
            resolvedNames.add(slotOfNode(nodeKey));
        }
        for (String scope : DECLARED_SCOPES) {
            for (String name : fieldKeys(lock.rootWorkspace.get(scope))) {
                String declaredScope = manifestScope(root, name);
                if (scope.equals(declaredScope)) {
                    continue;
                }
                if (declaredScope != null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            name + " moved from " + scope + " to " + declaredScope + " (not yet patched)");
                }
                if (resolvedNames.contains(name)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            name + " is no longer declared but still resolved (dropping just its workspace edge is not yet patched)");
                }
            }
        }
        if (!fieldKeys(lock.rootWorkspace.get("peerDependencies")).isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "the lock's workspace declares peerDependencies (not yet patched)");
        }
    }

    // --- small helpers -----------------------------------------------------

    private static String slotOfNode(String nodeKey) {
        return nodeKey.substring(0, nodeKey.lastIndexOf('@'));
    }

    private static String versionOfNode(String nodeKey) {
        return nodeKey.substring(nodeKey.lastIndexOf('@') + 1);
    }

    /** The package name a {@code packages} key addresses: its last path segment, {@code @scope/}-aware. */
    private static String slotOfKey(String key) {
        int lastSlash = key.lastIndexOf('/');
        if (lastSlash < 0) {
            return key;
        }
        int prevSlash = key.lastIndexOf('/', lastSlash - 1);
        String tail = key.substring(prevSlash + 1);
        return tail.startsWith("@") ? tail : key.substring(lastSlash + 1);
    }

    /** The scope the root importer declares {@code name} in, or {@code null} for a transitive. */
    private static @Nullable String manifestScope(ResolutionGraph.Importer root, String name) {
        for (String scope : DECLARED_SCOPES) {
            if (root.getDeclared().getOrDefault(scope, Collections.emptyMap()).containsKey(name)) {
                return scope;
            }
        }
        return null;
    }

    /** The declaring scope for an edit, defaulting to {@code dependencies} for a transitive. */
    private static String declaringScope(ResolutionGraph.Importer root, String name) {
        String scope = manifestScope(root, name);
        return scope != null ? scope : "dependencies";
    }

    private static @Nullable String declaredRange(ResolutionGraph.Importer root, String name) {
        for (Map.Entry<String, Map<String, String>> scope : root.getDeclared().entrySet()) {
            if (scope.getValue().containsKey(name)) {
                return scope.getValue().get(name);
            }
        }
        return null;
    }

    private static Set<String> fieldKeys(@Nullable JsonNode obj) {
        if (obj == null || !obj.isObject()) {
            return Collections.emptySet();
        }
        Set<String> keys = new LinkedHashSet<>();
        obj.fieldNames().forEachRemaining(keys::add);
        return keys;
    }

    private static Map<String, String> stringMap(@Nullable JsonNode obj) {
        if (obj == null || !obj.isObject()) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new LinkedHashMap<>();
        obj.fields().forEachRemaining(f -> map.put(f.getKey(), f.getValue().asText()));
        return map;
    }

    private static @Nullable List<String> stringList(@Nullable JsonNode node) {
        if (node == null || !node.isArray() || node.size() == 0) {
            return null;
        }
        List<String> values = new ArrayList<>();
        for (JsonNode v : node) {
            values.add(v.asText());
        }
        return values;
    }

    private static boolean notEmpty(@Nullable Map<String, String> m) {
        return m != null && !m.isEmpty();
    }
}
