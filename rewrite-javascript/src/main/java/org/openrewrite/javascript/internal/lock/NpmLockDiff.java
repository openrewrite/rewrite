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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.LockEditSet.EntryMetadata;
import org.openrewrite.javascript.internal.lock.LockEditSet.PackageEdit;
import org.openrewrite.javascript.internal.lock.resolve.ResolutionGraph;
import org.openrewrite.javascript.internal.lock.resolve.ResolvedNode;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.*;

/**
 * Diffs a freshly resolved {@link ResolutionGraph} against the existing {@code package-lock.json} and expresses
 * the difference as {@link PackageEdit}s for {@link NpmLockPatcher} — so untouched entries keep their bytes and
 * only what the resolution actually changed is rewritten. Entries are matched to installed keys slot-by-slot
 * (the root importer's declared version claims the top-level slot, then exact version matches, then a lone
 * leftover pair is an in-place move); unmatched graph nodes hoist into free slots exactly as npm would place
 * them (nesting a conflicting version one level under its requirer) and become adds, while unmatched lock keys
 * become removals. A difference the patcher cannot express byte-exactly — a relocation, a deeper nest, an
 * ambiguous slot-to-version correspondence — fails loud rather than guess.
 */
final class NpmLockDiff {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String NM = "node_modules/";
    private static final List<String> DECLARED_SCOPES =
            Arrays.asList("dependencies", "devDependencies", "optionalDependencies");

    private NpmLockDiff() {
    }

    static List<PackageEdit> diff(ResolutionGraph graph, String existingLock) {
        ResolutionGraph.Importer root = singleRootImporter(graph);
        Lock lock = Lock.parse(existingLock);
        requireRootEntryMirrors(root, lock);

        Map<String, String> bindings = bindNodesToKeys(graph, root, lock);
        Set<String> fresh = hoistUnbound(graph, root, bindings);
        requireReproducibleFreshPlacements(graph, root, bindings, fresh);

        Set<String> peerProviders = peerProviderKeys(graph);
        List<PackageEdit> edits = new ArrayList<>();
        boolean prunes = false;
        for (Map.Entry<String, ResolvedNode> e : graph.getNodes().entrySet()) {
            String nodeKey = e.getKey();
            String slot = slotOf(nodeKey);
            ResolvedNode node = e.getValue();
            String key = bindings.get(nodeKey);
            boolean peer = peerProviders.contains(nodeKey);
            PackageEdit edit = fresh.contains(nodeKey) ?
                    addEdit(root, slot, node, key, peer) :
                    boundEdit(root, lock, slot, node, key, peer);
            if (edit != null) {
                prunes |= edit.isPrunesOrphans();
                edits.add(edit);
            }
        }

        edits.addAll(removalEdits(graph, root, lock, bindings, prunes));
        return edits;
    }

    // --- matching ----------------------------------------------------------

    /**
     * Bind graph nodes to installed keys slot-by-slot: the root importer's declared version claims the
     * top-level slot, remaining versions match keys holding exactly that version, and a lone leftover pair
     * binds as an in-place move. Anything else is ambiguous and fails loud.
     */
    private static Map<String, String> bindNodesToKeys(ResolutionGraph graph, ResolutionGraph.Importer root,
                                                       Lock lock) {
        Map<String, List<String>> graphSlots = new LinkedHashMap<>();
        for (String nodeKey : graph.getNodes().keySet()) {
            graphSlots.computeIfAbsent(slotOf(nodeKey), k -> new ArrayList<>()).add(nodeKey);
        }

        Map<String, String> bindings = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> e : graphSlots.entrySet()) {
            String slot = e.getKey();
            List<String> nodeKeys = new ArrayList<>(e.getValue());
            List<String> lockKeys = new ArrayList<>(lock.keysBySlot.getOrDefault(slot, Collections.emptyList()));

            // The root's directly-declared version claims the top-level slot, as npm hoists root directs.
            String declared = root.getResolved().get(slot);
            String topKey = NM + slot;
            if (declared != null && lockKeys.remove(topKey)) {
                nodeKeys.remove(ResolutionGraph.key(slot, declared));
                bindings.put(ResolutionGraph.key(slot, declared), topKey);
            }
            for (Iterator<String> it = nodeKeys.iterator(); it.hasNext(); ) {
                String nodeKey = it.next();
                String version = versionOf(nodeKey);
                List<String> matches = new ArrayList<>();
                for (String lockKey : lockKeys) {
                    if (version.equals(lock.versions.get(lockKey))) {
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
     * Place every unbound node by npm's hoisting walk over a shelf pre-seeded with the bound placements: a
     * package lands at the highest {@code node_modules} on its requirer's path whose slot is free or already
     * holds the same version, nesting one level deeper past a conflict. Returns the freshly placed node keys.
     */
    private static Set<String> hoistUnbound(ResolutionGraph graph, ResolutionGraph.Importer root,
                                            Map<String, String> bindings) {
        Map<String, Map<String, String>> shelf = new HashMap<>();
        Set<String> visited = new HashSet<>();
        Deque<String[]> queue = new ArrayDeque<>();
        for (Map.Entry<String, String> b : bindings.entrySet()) {
            String key = b.getValue();
            shelf.computeIfAbsent(prefixOf(key), k -> new HashMap<>()).put(slotOf(key), versionOf(b.getKey()));
            visited.add(b.getKey());
            queue.add(new String[]{b.getKey(), key});
        }
        Set<String> fresh = new LinkedHashSet<>();

        for (Map.Entry<String, String> direct : new TreeMap<>(root.getResolved()).entrySet()) {
            resolveEdge(graph, "", direct.getKey(), direct.getValue(), bindings, shelf, visited, queue, fresh);
        }
        drainQueue(graph, bindings, shelf, visited, queue, fresh);
        seedAutoInstalledPeers(graph, bindings, shelf, visited, queue, fresh);
        drainQueue(graph, bindings, shelf, visited, queue, fresh);

        for (Map.Entry<String, ResolvedNode> e : graph.getNodes().entrySet()) {
            if (!bindings.containsKey(e.getKey())) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slotOf(e.getKey()),
                        e.getKey() + " resolved but was not placed");
            }
        }
        return fresh;
    }

    private static void drainQueue(ResolutionGraph graph, Map<String, String> bindings,
                                   Map<String, Map<String, String>> shelf, Set<String> visited,
                                   Deque<String[]> queue, Set<String> fresh) {
        while (!queue.isEmpty()) {
            String[] cur = queue.poll();
            ResolvedNode node = graph.getNodes().get(cur[0]);
            for (Map.Entry<String, String> edge : node.getResolvedEdges().entrySet()) {
                resolveEdge(graph, cur[1], edge.getKey(), edge.getValue(), bindings, shelf, visited, queue, fresh);
            }
        }
    }

    private static void resolveEdge(ResolutionGraph graph, String fromLocation, String depName, String depVersion,
                                    Map<String, String> bindings, Map<String, Map<String, String>> shelf,
                                    Set<String> visited, Deque<String[]> queue, Set<String> fresh) {
        for (String prefix : chainTopToBottom(fromLocation)) {
            Map<String, String> at = shelf.computeIfAbsent(prefix, k -> new HashMap<>());
            String existing = at.get(depName);
            if (existing == null) {
                String key = prefix + depName;
                String nodeKey = ResolutionGraph.key(depName, depVersion);
                String prior = bindings.get(nodeKey);
                if (prior != null && !prior.equals(key)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, depName,
                            depName + "@" + depVersion + " would be placed at both " + prior + " and " + key);
                }
                at.put(depName, depVersion);
                bindings.put(nodeKey, key);
                fresh.add(nodeKey);
                if (visited.add(nodeKey)) {
                    queue.add(new String[]{nodeKey, key});
                }
                return;
            }
            if (existing.equals(depVersion)) {
                return; // deduped against an existing or ancestor placement
            }
            // a different version occupies this level; nest one deeper
        }
        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, depName,
                depName + "@" + depVersion + " could not be placed (no free node_modules on the path)");
    }

    /**
     * An auto-installed peer is a provider the dependency graph never reaches, so the traversal leaves it
     * unplaced; npm hoists it to the top level.
     */
    private static void seedAutoInstalledPeers(ResolutionGraph graph, Map<String, String> bindings,
                                               Map<String, Map<String, String>> shelf, Set<String> visited,
                                               Deque<String[]> queue, Set<String> fresh) {
        Set<String> peerProviders = peerProviderKeys(graph);
        for (Map.Entry<String, ResolvedNode> e : graph.getNodes().entrySet()) {
            if (!bindings.containsKey(e.getKey()) && peerProviders.contains(e.getKey())) {
                resolveEdge(graph, "", slotOf(e.getKey()), e.getValue().getVersion(),
                        bindings, shelf, visited, queue, fresh);
            }
        }
    }

    /** The {@code node_modules} prefixes visible from {@code locationKey}, shallowest first. */
    private static List<String> chainTopToBottom(String locationKey) {
        List<String> deepestFirst = new ArrayList<>();
        String own = locationKey.isEmpty() ? NM : locationKey + "/" + NM;
        deepestFirst.add(own);
        String cursor = locationKey;
        while (!cursor.isEmpty()) {
            int nm = cursor.lastIndexOf(NM);
            String parentDir = nm < 0 ? "" : cursor.substring(0, nm);
            deepestFirst.add(parentDir.isEmpty() ? NM : parentDir + NM);
            cursor = nm < 0 ? "" : trimTrailingSlash(cursor.substring(0, nm));
        }
        Collections.reverse(deepestFirst);
        return deepestFirst;
    }

    private static String trimTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }

    // --- fresh-placement guards --------------------------------------------

    /**
     * Placements the lock already held are ground truth and need no proof; only freshly contended placements
     * do. A fresh copy must sit at most one level deep (the patcher's reach). When two versions of one name are
     * both fresh, the from-scratch fork rules apply: each requires a single top-level requirer whose name order
     * is collation-unambiguous, so the hoisted winner provably matches npm's. A fresh member of any fork also
     * proves its peers are satisfied by single top-level dependency-reached providers.
     */
    private static void requireReproducibleFreshPlacements(ResolutionGraph graph, ResolutionGraph.Importer root,
                                                           Map<String, String> bindings, Set<String> fresh) {
        Map<String, Set<String>> versionsByName = new LinkedHashMap<>();
        for (String nodeKey : graph.getNodes().keySet()) {
            versionsByName.computeIfAbsent(slotOf(nodeKey), k -> new LinkedHashSet<>()).add(versionOf(nodeKey));
        }
        Map<String, List<String>> freshByName = new LinkedHashMap<>();
        for (String nodeKey : fresh) {
            String key = bindings.get(nodeKey);
            if (depthOf(key) > 2) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slotOf(nodeKey),
                        slotOf(nodeKey) + " places more than one level deep (" + key + ")");
            }
            freshByName.computeIfAbsent(slotOf(nodeKey), k -> new ArrayList<>()).add(nodeKey);
        }
        for (Map.Entry<String, List<String>> e : freshByName.entrySet()) {
            String name = e.getKey();
            Set<String> versions = versionsByName.get(name);
            if (versions.size() < 2) {
                continue;
            }
            if (versions.size() > 2) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " forks into " + versions.size() + " versions (only a two-version fork is reproduced)");
            }
            for (String nodeKey : e.getValue()) {
                requireSatisfiedForkMemberPeers(graph, root, bindings, versionsByName, graph.getNodes().get(nodeKey));
            }
            if (e.getValue().size() >= 2) {
                requireReproducibleFreshFork(graph, bindings, name, versions);
            }
        }
    }

    /**
     * Both fork members are fresh, so the lock decided nothing: reproduce npm's winner only in the cleanest
     * shape — each version required by exactly one top-level package, the two requirer names ordering the same
     * under {@code compareTo} and npm's {@code localeCompare} — and verify the walk placed them that way.
     */
    private static void requireReproducibleFreshFork(ResolutionGraph graph, Map<String, String> bindings,
                                                     String name, Set<String> versions) {
        Map<String, ResolvedNode> requirerByVersion = new LinkedHashMap<>();
        for (ResolvedNode node : graph.getNodes().values()) {
            String required = node.getResolvedEdges().get(name);
            if (required != null && versions.contains(required) && requirerByVersion.putIfAbsent(required, node) != null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + "@" + required + " has multiple requirers (only a single-requirer fork is reproduced)");
            }
        }
        if (requirerByVersion.size() != versions.size()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " forks but a version has no single top-level requirer (not reproduced)");
        }
        for (ResolvedNode requirer : requirerByVersion.values()) {
            if (!(NM + requirer.getName()).equals(bindings.get(ResolutionGraph.key(requirer.getName(), requirer.getVersion())))) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " forks under nested requirer " + requirer.getName() + " (not reproduced)");
            }
        }
        List<Map.Entry<String, ResolvedNode>> pairs = new ArrayList<>(requirerByVersion.entrySet());
        String nameA = pairs.get(0).getValue().getName();
        String nameB = pairs.get(1).getValue().getName();
        if (!unambiguousOrder(nameA, nameB)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " forks with a collation-ambiguous requirer order (" + nameA + ", " + nameB + ")");
        }
        boolean aFirst = nameA.compareTo(nameB) < 0;
        String hoistedVersion = pairs.get(aFirst ? 0 : 1).getKey();
        Map.Entry<String, ResolvedNode> nested = pairs.get(aFirst ? 1 : 0);
        String expectedNest = NM + nested.getValue().getName() + "/" + NM + name;
        if (!(NM + name).equals(bindings.get(ResolutionGraph.key(name, hoistedVersion))) ||
                !expectedNest.equals(bindings.get(ResolutionGraph.key(name, nested.getKey())))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " forks into a layout that cannot yet be patched byte-exact");
        }
    }

    /**
     * Whether {@code a} and {@code b} order the same under {@code compareTo} and npm's {@code localeCompare('en')}:
     * true only when they first differ at two ASCII-lowercase letters, so the decision is a plain letter
     * comparison. A prefix relation or any non-letter deciding character could reorder under ICU collation.
     */
    private static boolean unambiguousOrder(String a, String b) {
        int n = Math.min(a.length(), b.length());
        for (int i = 0; i < n; i++) {
            char ca = a.charAt(i);
            char cb = b.charAt(i);
            if (ca != cb) {
                return ca >= 'a' && ca <= 'z' && cb >= 'a' && cb <= 'z';
            }
        }
        return false;
    }

    /**
     * A peer-bearing fork member serializes its peers the same in or out of the fork only when each non-optional
     * peer resolves to a single version placed top-level and reached by a real dependency edge; an optional peer
     * may be absent. Anything else reshapes the layout, so it defers.
     */
    private static void requireSatisfiedForkMemberPeers(ResolutionGraph graph, ResolutionGraph.Importer root,
                                                        Map<String, String> bindings,
                                                        Map<String, Set<String>> versionsByName, ResolvedNode member) {
        Map<String, String> peers = member.getManifest().getPeerDependencies();
        if (peers == null) {
            return;
        }
        JsonNode meta = member.getManifest().getPeerDependenciesMeta();
        String who = member.getName() + "@" + member.getVersion();
        for (String peerName : peers.keySet()) {
            Set<String> provided = versionsByName.get(peerName);
            if (provided == null || provided.isEmpty()) {
                if (isOptionalPeer(meta, peerName)) {
                    continue;
                }
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, member.getName(),
                        who + " forks and its peer " + peerName + " is unsatisfied (fork member with an unsatisfied peer)");
            }
            if (provided.size() != 1) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, member.getName(),
                        who + " forks and its peer " + peerName + " itself forks (fork member with a forked peer)");
            }
            String peerVersion = provided.iterator().next();
            if (!(NM + peerName).equals(bindings.get(ResolutionGraph.key(peerName, peerVersion))) ||
                    !dependencyReachable(graph, root, peerName, peerVersion)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, member.getName(),
                        who + " forks and its peer " + peerName + " is nested or auto-installed (not reproduced)");
            }
        }
    }

    /** Whether some importer or resolved dependency edge reaches {@code name@version}. */
    private static boolean dependencyReachable(ResolutionGraph graph, ResolutionGraph.Importer root,
                                               String name, String version) {
        if (version.equals(root.getResolved().get(name))) {
            return true;
        }
        for (ResolvedNode node : graph.getNodes().values()) {
            if (version.equals(node.getResolvedEdges().get(name))) {
                return true;
            }
        }
        return false;
    }

    // --- edits -------------------------------------------------------------

    private static PackageEdit addEdit(ResolutionGraph.Importer root, String slot, ResolvedNode node,
                                       String key, boolean peer) {
        VersionManifest m = node.getManifest();
        if (!slot.equals(m.getName())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                    slot + " aliases " + m.getName() + "; a fresh alias entry cannot yet be patched in");
        }
        EntryMetadata metadata = addMetadata(node, peer);
        VersionManifest.Dist dist = m.getDist();
        if (dist == null || dist.getTarball() == null || dist.getIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, slot,
                    slot + "@" + m.getVersion() + " has no registry locator (resolved/integrity)");
        }
        return PackageEdit.builder()
                .name(slot)
                .oldVersion("")
                .newVersion(m.getVersion())
                .newResolved(dist.getTarball())
                .newIntegrity(dist.getIntegrity())
                .newShasum(dist.getShasum())
                .newDependencies(notEmpty(m.getDependencies()) ? m.getDependencies() : null)
                .newOptionalDependencies(notEmpty(m.getOptionalDependencies()) ? m.getOptionalDependencies() : null)
                .scope(declaringScope(root, slot))
                .importerDir(null)
                .kind(PackageEdit.Kind.ADD)
                .nestedUnder(nestedUnderOf(key))
                .metadata(metadata)
                .build();
    }

    /**
     * Full entry metadata for a fresh add, mirroring what npm serializes on a new entry. A manifest surface the
     * patcher has no byte-verified serialization for defers.
     */
    private static @Nullable EntryMetadata addMetadata(ResolvedNode node, boolean peer) {
        VersionManifest m = node.getManifest();
        String who = m.getName() + "@" + m.getVersion();
        if (m.getBin() != null && !m.getBin().isObject()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, m.getName(), who + " has a non-object bin");
        }
        if (m.getFunding() != null && !m.getFunding().isTextual()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, m.getName(),
                    who + " declares funding in a non-string form npm reshapes (entry shape not yet patched)");
        }
        if (m.getLicense() != null && !m.getLicense().isTextual() ||
                m.getLicense() == null && m.getLicenseString() != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, m.getName(),
                    who + " has a non-string license (not yet serialized)");
        }
        requireNoUnpatchedSurfaces(m);
        boolean dev = node.isDev();
        boolean optional = node.isOptional();
        boolean devOptional = node.isDevOptional() && !dev && !optional;
        JsonNode peerMeta = m.getPeerDependenciesMeta();
        return EntryMetadata.builder()
                .dev(dev ? Boolean.TRUE : null)
                .optional(optional ? Boolean.TRUE : null)
                .devOptional(devOptional ? Boolean.TRUE : null)
                .peer(peer ? Boolean.TRUE : null)
                .license(m.getLicenseString())
                .deprecated(m.getDeprecated())
                .engines(notEmpty(m.getEngines()) ? m.getEngines() : null)
                .os(m.getOs())
                .cpu(m.getCpu())
                .libc(m.getLibc())
                .hasInstallScript(Boolean.TRUE.equals(m.getHasInstallScript()) ? Boolean.TRUE : null)
                .bin(m.getBin())
                .funding(normalizeFunding(m.getFunding()))
                .peerDependencies(notEmpty(m.getPeerDependencies()) ? m.getPeerDependencies() : null)
                .peerDependenciesMeta(nonEmptyObject(peerMeta) ? peerMeta : null)
                .build();
    }

    /**
     * The edit for a node the lock already installs: nothing when key, version, flags and declared constraint
     * all match; a flags-only or constraint-only bump when just those changed; an in-place move (bump for a
     * root-declared dependency, forced move for a transitive) when the version changed, carrying the metadata
     * delta between the old entry and the new manifest.
     */
    private static @Nullable PackageEdit boundEdit(ResolutionGraph.Importer root, Lock lock, String slot,
                                                   ResolvedNode node, String key, boolean peer) {
        JsonNode entry = lock.entries.get(key);
        String oldVersion = lock.versions.get(key);
        boolean moves = !node.getVersion().equals(oldVersion);
        if (entry.path("link").asBoolean(false)) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, slot, slot + " is a workspace link entry");
        }
        if (moves && depthOf(key) > 2) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                    slot + " moves at a nesting depth the patcher cannot reach (" + key + ")");
        }

        EntryMetadata metadata = moves ? moveMetadata(node, entry, peer) : flagsDelta(node, entry, peer);
        String scope = declaringScope(root, slot);
        String declared = declaredRange(root, slot);
        boolean promotion = !moves && declared != null && !inRootScope(lock, scope, slot);
        boolean constraintChanged = declared != null &&
                !declared.equals(rootScopeValue(lock, scope, slot));

        if (!moves && metadata == null && !constraintChanged && !promotion) {
            return null;
        }
        PackageEdit.PackageEditBuilder edit = PackageEdit.builder()
                .name(slot)
                .oldVersion(oldVersion)
                .newVersion(node.getVersion())
                .scope(scope)
                .importerDir(null)
                .nestedUnder(nestedUnderOf(key))
                .metadata(metadata);
        if (promotion) {
            // Declared now but installed already: the importer edge is written (and any stale scope membership
            // dropped) while the entry stays, its flags exact-set by the metadata.
            return edit.kind(PackageEdit.Kind.PROMOTION)
                    .metadata(metadata != null ? metadata :
                            flagsMetadata(node, peer))
                    .build();
        }
        if (!moves) {
            return edit.build();
        }

        VersionManifest m = node.getManifest();
        VersionManifest.Dist dist = m.getDist();
        if (dist == null || dist.getTarball() == null || dist.getIntegrity() == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, slot,
                    slot + "@" + m.getVersion() + " has no registry locator (resolved/integrity)");
        }
        EdgeDelta delta = edgeDelta(slot, entry, m);
        return edit.kind(declared != null ? PackageEdit.Kind.BUMP : PackageEdit.Kind.FORCED_MOVE)
                .newResolved(dist.getTarball())
                .newIntegrity(dist.getIntegrity())
                .newShasum(dist.getShasum())
                .newDependencies(notEmpty(m.getDependencies()) ? m.getDependencies() : null)
                .prunesOrphans(delta.dropped)
                .addsDependencyEdges(delta.added)
                .build();
    }

    /** How a moved entry's dependency edge set changed: drops orphan-prune, gains graft the full new map. */
    private static EdgeDelta edgeDelta(String slot, JsonNode entry, VersionManifest m) {
        Set<String> oldEdges = fieldKeys(entry.get("dependencies"));
        Set<String> newEdges = m.getDependencies() == null ? Collections.emptySet() : m.getDependencies().keySet();
        Set<String> oldOptional = fieldKeys(entry.get("optionalDependencies"));
        Set<String> newOptional = m.getOptionalDependencies() == null ?
                Collections.emptySet() : m.getOptionalDependencies().keySet();
        if (!oldOptional.equals(newOptional)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                    slot + " changed its optionalDependencies on upgrade (not yet patched)");
        }
        boolean added = !oldEdges.containsAll(newEdges);
        boolean dropped = !newEdges.containsAll(oldEdges);
        return new EdgeDelta(added, dropped);
    }

    private static final class EdgeDelta {
        final boolean added;
        final boolean dropped;

        EdgeDelta(boolean added, boolean dropped) {
            this.added = added;
            this.dropped = dropped;
        }
    }

    /**
     * The metadata delta between the installed entry and the moved-to manifest. Surfaces with an in-place
     * writer (engines, license, deprecated, funding, peers, flags) become the delta; a change to any other
     * lock-surfaced field (platform gates, bin, install scripts) defers.
     */
    private static @Nullable EntryMetadata moveMetadata(ResolvedNode node, JsonNode entry, boolean peer) {
        VersionManifest m = node.getManifest();
        String slot = m.getName();
        requireNoUnpatchedSurfaces(m);
        EntryMetadata.EntryMetadataBuilder b = EntryMetadata.builder();
        boolean any = false;

        Map<String, String> oldEngines = stringMap(entry.get("engines"));
        Map<String, String> newEngines = m.getEngines() == null ? Collections.emptyMap() : m.getEngines();
        if (!oldEngines.equals(newEngines)) {
            b.engines(newEngines.isEmpty() ? null : newEngines).enginesChanged(true);
            any = true;
        }
        String oldLicense = entry.path("license").isTextual() ? entry.get("license").asText() : null;
        String newLicense = m.getLicenseString();
        if (!Objects.equals(oldLicense, newLicense)) {
            if (newLicense == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                        slot + " drops its license field on upgrade (not yet patched)");
            }
            b.license(newLicense);
            any = true;
        }
        String oldDeprecated = entry.path("deprecated").isTextual() ? entry.get("deprecated").asText() : null;
        if (!Objects.equals(oldDeprecated, m.getDeprecated())) {
            if (m.getDeprecated() == null || oldDeprecated == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                        slot + " gains or drops a deprecated field on upgrade (not yet patched)");
            }
            b.deprecated(m.getDeprecated());
            any = true;
        }
        JsonNode newFunding = normalizeFunding(m.getFunding());
        if (!Objects.equals(entry.get("funding"), newFunding)) {
            if (m.getFunding() != null && !m.getFunding().isTextual()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                        slot + " declares funding in a non-string form npm reshapes (not yet patched)");
            }
            b.funding(newFunding).fundingChanged(true);
            any = true;
        }
        if (!Objects.equals(entry.get("bin"), m.getBin())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                    slot + " changed its bin on upgrade (not yet patched)");
        }
        requireUnchanged(slot, "os", entry.get("os"), m.getOs());
        requireUnchanged(slot, "cpu", entry.get("cpu"), m.getCpu());
        requireUnchanged(slot, "libc", entry.get("libc"), m.getLibc());
        if (entry.path("hasInstallScript").asBoolean(false) != Boolean.TRUE.equals(m.getHasInstallScript())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                    slot + " changed hasInstallScript on upgrade (not yet patched)");
        }

        Map<String, String> oldPeers = stringMap(entry.get("peerDependencies"));
        Map<String, String> newPeers = m.getPeerDependencies() == null ? Collections.emptyMap() : m.getPeerDependencies();
        if (!oldPeers.equals(newPeers)) {
            b.peerDependencies(newPeers.isEmpty() ? null : newPeers).peerDependenciesChanged(true);
            any = true;
        }
        JsonNode oldMeta = entry.get("peerDependenciesMeta");
        JsonNode newMeta = nonEmptyObject(m.getPeerDependenciesMeta()) ? m.getPeerDependenciesMeta() : null;
        if (!Objects.equals(oldMeta, newMeta)) {
            if (newMeta != null) {
                for (Iterator<String> it = newMeta.fieldNames(); it.hasNext(); ) {
                    String peerName = it.next();
                    if (!newPeers.containsKey(peerName)) {
                        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                                slot + " declares peerDependenciesMeta for the undeclared peer " + peerName +
                                        " (not yet patched)");
                    }
                }
            }
            b.peerDependenciesMeta(newMeta).peerDependenciesMetaChanged(true);
            any = true;
        }

        EntryMetadata flags = flagsDelta(node, entry, peer);
        if (flags != null) {
            b.dev(flags.getDev()).optional(flags.getOptional())
                    .devOptional(flags.getDevOptional()).peer(flags.getPeer()).flagsChanged(true);
            any = true;
        }
        return any ? b.build() : null;
    }

    /** Manifest surfaces with no verified lock serialization at all; any presence defers. */
    private static void requireNoUnpatchedSurfaces(VersionManifest m) {
        String who = m.getName() + "@" + m.getVersion();
        deferIfSurface(m, "bundleDependencies", m.getBundleDependencies() != null, who);
        deferIfSurface(m, "hasShrinkwrap", Boolean.TRUE.equals(m.getHasShrinkwrap()), who);
        deferIfSurface(m, "acceptDependencies", notEmpty(m.getAcceptDependencies()), who);
        deferIfSurface(m, "workspaces", m.getWorkspaces() != null, who);
    }

    private static void deferIfSurface(VersionManifest m, String field, boolean present, String who) {
        if (present) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, m.getName(),
                    who + " declares " + field + " (entry shape not yet patched)");
        }
    }

    private static void requireUnchanged(String slot, String field, @Nullable JsonNode oldValue,
                                         @Nullable List<String> newValue) {
        List<String> old = null;
        if (oldValue != null && oldValue.isArray()) {
            old = new ArrayList<>();
            for (JsonNode v : oldValue) {
                old.add(v.asText());
            }
        }
        if (!Objects.equals(old, newValue)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, slot,
                    slot + " changed its " + field + " on upgrade (not yet patched)");
        }
    }

    /** The exact-set flags metadata when the entry's flags differ from the resolution's, else {@code null}. */
    private static @Nullable EntryMetadata flagsDelta(ResolvedNode node, JsonNode entry, boolean peer) {
        boolean dev = node.isDev();
        boolean optional = node.isOptional();
        boolean devOptional = node.isDevOptional() && !dev && !optional;
        if (entry.path("dev").asBoolean(false) == dev &&
                entry.path("optional").asBoolean(false) == optional &&
                entry.path("devOptional").asBoolean(false) == devOptional &&
                entry.path("peer").asBoolean(false) == peer) {
            return null;
        }
        return flagsMetadata(node, peer);
    }

    private static EntryMetadata flagsMetadata(ResolvedNode node, boolean peer) {
        boolean dev = node.isDev();
        boolean optional = node.isOptional();
        boolean devOptional = node.isDevOptional() && !dev && !optional;
        return EntryMetadata.builder()
                .flagsChanged(true)
                .dev(dev ? Boolean.TRUE : null)
                .optional(optional ? Boolean.TRUE : null)
                .devOptional(devOptional ? Boolean.TRUE : null)
                .peer(peer ? Boolean.TRUE : null)
                .build();
    }

    // --- removals ----------------------------------------------------------

    /**
     * Installed keys the resolution no longer reaches. A root-declared dependency that disappeared is a removal
     * edit (the patcher drops the entry, the importer edge, and the orphaned subtree); an undeclared leftover
     * rides an orphan-pruning bump's GC, and with no such bump nothing can prove it collectable, so it defers.
     */
    private static List<PackageEdit> removalEdits(ResolutionGraph graph, ResolutionGraph.Importer root,
                                                  Lock lock, Map<String, String> bindings, boolean prunes) {
        Set<String> boundKeys = new HashSet<>(bindings.values());
        List<PackageEdit> removals = new ArrayList<>();
        for (String key : lock.installedKeys) {
            if (boundKeys.contains(key)) {
                continue;
            }
            String slot = slotOf(key);
            String declaredScope = null;
            for (String scope : DECLARED_SCOPES) {
                if (inRootScope(lock, scope, slot)) {
                    declaredScope = scope;
                    break;
                }
            }
            if (declaredScope != null && depthOf(key) == 1 && root.getResolved().get(slot) == null) {
                removals.add(PackageEdit.builder()
                        .name(slot)
                        .oldVersion(lock.versions.get(key))
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

    // --- peers -------------------------------------------------------------

    /**
     * The node keys npm flags {@code peer: true}: any node whose name a resolved package — or the library root
     * itself — requires in a non-optional {@code peerDependencies}.
     */
    private static Set<String> peerProviderKeys(ResolutionGraph graph) {
        Map<String, Set<String>> versionsByName = new LinkedHashMap<>();
        for (String nodeKey : graph.getNodes().keySet()) {
            versionsByName.computeIfAbsent(slotOf(nodeKey), k -> new LinkedHashSet<>()).add(versionOf(nodeKey));
        }
        Set<String> providers = new LinkedHashSet<>();
        for (ResolvedNode node : graph.getNodes().values()) {
            Map<String, String> peers = node.getManifest().getPeerDependencies();
            if (peers == null) {
                continue;
            }
            JsonNode meta = node.getManifest().getPeerDependenciesMeta();
            for (String peerName : peers.keySet()) {
                if (!isOptionalPeer(meta, peerName)) {
                    addProvider(providers, versionsByName, peerName);
                }
            }
        }
        for (ResolutionGraph.Importer importer : graph.getImporters()) {
            Map<String, String> rootPeers = importer.getDeclared().get("peerDependencies");
            if (rootPeers != null) {
                for (String peerName : rootPeers.keySet()) {
                    addProvider(providers, versionsByName, peerName);
                }
            }
        }
        return providers;
    }

    private static void addProvider(Set<String> providers, Map<String, Set<String>> versionsByName, String peerName) {
        Set<String> versions = versionsByName.get(peerName);
        if (versions != null && versions.size() == 1) {
            providers.add(ResolutionGraph.key(peerName, versions.iterator().next()));
        }
    }

    private static boolean isOptionalPeer(@Nullable JsonNode meta, String peer) {
        if (meta == null) {
            return false;
        }
        JsonNode entry = meta.get(peer);
        return entry != null && entry.path("optional").asBoolean(false);
    }

    // --- lock model --------------------------------------------------------

    /** The existing lock's installed entries, read once: keys, per-key version, per-slot keys, root entry. */
    private static final class Lock {
        final JsonNode rootEntry;
        final List<String> installedKeys = new ArrayList<>();
        final Map<String, JsonNode> entries = new LinkedHashMap<>();
        final Map<String, String> versions = new LinkedHashMap<>();
        final Map<String, List<String>> keysBySlot = new LinkedHashMap<>();

        private Lock(JsonNode rootEntry) {
            this.rootEntry = rootEntry;
        }

        static Lock parse(String existingLock) {
            JsonNode packages;
            try {
                packages = JSON.readTree(existingLock).path("packages");
            } catch (Exception e) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, null, "package-lock.json could not be parsed");
            }
            if (!packages.isObject()) {
                throw new EngineFailure(Reason.MALFORMED_LOCK, null, "package-lock.json has no packages map");
            }
            Lock lock = new Lock(packages.path(""));
            packages.fields().forEachRemaining(f -> {
                String key = f.getKey();
                if (!key.contains(NM)) {
                    return; // importer entry
                }
                lock.installedKeys.add(key);
                lock.entries.put(key, f.getValue());
                lock.versions.put(key, f.getValue().path("version").asText(null));
                lock.keysBySlot.computeIfAbsent(slotOf(key), k -> new ArrayList<>()).add(key);
            });
            return lock;
        }
    }

    private static void requireRootEntryMirrors(ResolutionGraph.Importer root, Lock lock) {
        String lockName = lock.rootEntry.path("name").asText(null);
        String lockVersion = lock.rootEntry.path("version").asText(null);
        if ((lockName != null && root.getName() != null && !lockName.equals(root.getName())) ||
                (lockVersion != null && root.getVersion() != null && !lockVersion.equals(root.getVersion()))) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "the manifest's name/version changed; the root lock entry cannot yet be rewritten");
        }
    }

    private static ResolutionGraph.Importer singleRootImporter(ResolutionGraph graph) {
        if (graph.getImporters().size() != 1 || !graph.getImporters().get(0).getDir().isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "only a single root importer can be diffed against the lock");
        }
        return graph.getImporters().get(0);
    }

    // --- small helpers -----------------------------------------------------

    /** The slot (directory) name of a node key ({@code slot@version}) or installed key. */
    private static String slotOf(String key) {
        if (key.contains(NM)) {
            return key.substring(key.lastIndexOf(NM) + NM.length());
        }
        return key.substring(0, key.lastIndexOf('@'));
    }

    private static String versionOf(String nodeKey) {
        return nodeKey.substring(nodeKey.lastIndexOf('@') + 1);
    }

    /** The key's prefix up to and including its last {@code node_modules/}. */
    private static String prefixOf(String key) {
        return key.substring(0, key.lastIndexOf(NM) + NM.length());
    }

    /** How many {@code node_modules} levels deep an installed key sits (1 = top-level). */
    private static int depthOf(String key) {
        int depth = 0;
        for (int i = key.indexOf(NM); i >= 0; i = key.indexOf(NM, i + 1)) {
            depth++;
        }
        return depth;
    }

    /** The dependent slot a one-level-nested key sits under, or {@code null} top-level. */
    private static @Nullable String nestedUnderOf(String key) {
        if (depthOf(key) < 2) {
            return null;
        }
        return key.substring(NM.length(), key.lastIndexOf("/" + NM));
    }

    /** The scope the root importer declares {@code slot} in, defaulting to {@code dependencies} for a transitive. */
    private static String declaringScope(ResolutionGraph.Importer root, String slot) {
        for (Map.Entry<String, Map<String, String>> scope : root.getDeclared().entrySet()) {
            if (scope.getValue().containsKey(slot) && !"peerDependencies".equals(scope.getKey())) {
                return scope.getKey();
            }
        }
        return "dependencies";
    }

    private static @Nullable String declaredRange(ResolutionGraph.Importer root, String slot) {
        for (Map.Entry<String, Map<String, String>> scope : root.getDeclared().entrySet()) {
            if (!"peerDependencies".equals(scope.getKey()) && scope.getValue().containsKey(slot)) {
                return scope.getValue().get(slot);
            }
        }
        return null;
    }

    private static boolean inRootScope(Lock lock, String scope, String slot) {
        return lock.rootEntry.path(scope).has(slot);
    }

    private static @Nullable String rootScopeValue(Lock lock, String scope, String slot) {
        JsonNode value = lock.rootEntry.path(scope).get(slot);
        return value == null ? null : value.asText();
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

    /** npm normalizes a string {@code funding} to the object form {@code {url}}. */
    private static @Nullable JsonNode normalizeFunding(@Nullable JsonNode funding) {
        if (funding == null) {
            return null;
        }
        if (funding.isTextual()) {
            return JSON.createObjectNode().put("url", funding.asText());
        }
        return funding;
    }

    private static boolean nonEmptyObject(@Nullable JsonNode node) {
        return node != null && node.isObject() && node.size() > 0;
    }

    private static boolean notEmpty(@Nullable Map<String, String> m) {
        return m != null && !m.isEmpty();
    }
}
