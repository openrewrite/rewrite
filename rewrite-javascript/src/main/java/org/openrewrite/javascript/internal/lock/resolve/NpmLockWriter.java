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
package org.openrewrite.javascript.internal.lock.resolve;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.EngineFailure;
import org.openrewrite.javascript.internal.lock.NpmJson;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.*;

/**
 * Serializes a {@link ResolutionGraph} to {@code package-lock.json} text, byte-for-byte identical to what a real
 * {@code npm install --package-lock-only} would write. It computes npm's hoisted {@code node_modules} layout from
 * the graph (every package top-level for a clean closure; the conflicting version of a fork nested under its
 * requiring parent), builds the {@code packages} map (and, for lockfileVersion 2, the legacy {@code dependencies}
 * tree), and renders through {@link NpmJson} so the key order and pretty-print match npm exactly. A satisfied
 * {@code peerDependencies} surface is reproduced (recorded verbatim, its provider flagged {@code peer: true}),
 * and dev/optional dependencies are placed and flagged ({@code dev}/{@code optional}/{@code devOptional}) per
 * npm's reachability; anything else it cannot reproduce byte-exact — a workspace, a manifest field it does not
 * model — fails loud rather than emit a wrong lock.
 */
public final class NpmLockWriter {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String UNIT = "  ";
    private static final String NM = "node_modules/";

    public String write(ResolutionGraph graph, int lockfileVersion) {
        if (lockfileVersion != 2 && lockfileVersion != 3) {
            throw new EngineFailure(Reason.UNSUPPORTED_LOCKFILE_VERSION, null,
                    "package-lock.json lockfileVersion " + lockfileVersion + " is not supported (need 2 or 3)");
        }
        ResolutionGraph.Importer root = singleRootImporter(graph);
        Map<String, String> placements = hoist(graph, root);

        ObjectNode lock = JSON.createObjectNode();
        if (root.getName() != null) {
            lock.put("name", root.getName());
        }
        if (root.getVersion() != null) {
            lock.put("version", root.getVersion());
        }
        lock.put("lockfileVersion", lockfileVersion);
        lock.put("requires", true);

        Set<String> peerProviders = peerProviderKeys(graph);
        ObjectNode packages = lock.putObject("packages");
        packages.set("", rootEntry(root));
        for (Map.Entry<String, ResolvedNode> e : graph.getNodes().entrySet()) {
            String key = placements.get(e.getKey());
            if (key == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, e.getValue().getName(),
                        e.getValue().getName() + "@" + e.getValue().getVersion() + " resolved but was not placed");
            }
            packages.set(key, packageEntry(e.getValue(), peerProviders.contains(e.getKey())));
        }

        if (lockfileVersion == 2) {
            lock.set("dependencies", legacyTree(graph, placements, peerProviders));
        }
        return NpmJson.render(lock, "", UNIT) + "\n";
    }

    // --- importer entry ---------------------------------------------------

    private static ObjectNode rootEntry(ResolutionGraph.Importer root) {
        ObjectNode entry = JSON.createObjectNode();
        if (root.getName() != null) {
            entry.put("name", root.getName());
        }
        if (root.getVersion() != null) {
            entry.put("version", root.getVersion());
        }
        // Each declared scope is mirrored verbatim; render sorts the maps as npm's serializer does.
        for (Map.Entry<String, Map<String, String>> scope : root.getDeclared().entrySet()) {
            entry.set(scope.getKey(), stringMapNode(scope.getValue()));
        }
        return entry;
    }

    // --- package entry ----------------------------------------------------

    private ObjectNode packageEntry(ResolvedNode node, boolean peer) {
        VersionManifest m = node.getManifest();
        requireEmittable(m);

        ObjectNode entry = JSON.createObjectNode();
        entry.put("version", m.getVersion());

        VersionManifest.Dist dist = m.getDist();
        String resolved = dist == null ? null : dist.getTarball();
        String integrity = dist == null ? null : dist.getIntegrity();
        if (resolved == null || integrity == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, m.getName(),
                    m.getName() + "@" + m.getVersion() + " has no registry locator (resolved/integrity)");
        }
        entry.put("resolved", resolved);
        entry.put("integrity", integrity);

        JsonNode license = m.getLicense();
        if (license != null) {
            if (!license.isTextual()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, m.getName(),
                        m.getName() + " has a non-string license (object/array form not yet reproduced)");
            }
            entry.put("license", license.asText());
        } else if (m.getLicenseString() != null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, m.getName(),
                    m.getName() + " carries a legacy licenses array (not yet reproduced)");
        }

        applyDepFlags(entry, node);
        // A node with any incoming peer edge is flagged, even when it is also a regular/top-level dependency.
        if (peer) {
            entry.put("peer", true);
        }
        if (notEmpty(m.getDependencies())) {
            entry.set("dependencies", stringMapNode(m.getDependencies()));
        }
        if (notEmpty(m.getOptionalDependencies())) {
            entry.set("optionalDependencies", stringMapNode(m.getOptionalDependencies()));
        }
        if (notEmpty(m.getEngines())) {
            entry.set("engines", stringMapNode(m.getEngines()));
        }
        if (notEmpty(m.getPeerDependencies())) {
            entry.set("peerDependencies", stringMapNode(m.getPeerDependencies()));
        }
        JsonNode peerMeta = m.getPeerDependenciesMeta();
        if (peerMeta != null && peerMeta.isObject() && peerMeta.size() > 0) {
            entry.set("peerDependenciesMeta", peerMeta.deepCopy());
        }
        return entry;
    }

    /**
     * npm's {@code dev}/{@code optional}/{@code devOptional} entry flags. npm writes {@code dev} and
     * {@code optional} independently (a node reachable only through both a dev path and an optional path carries
     * both) and {@code devOptional} only when the node is neither purely dev nor purely optional.
     */
    private static void applyDepFlags(ObjectNode entry, ResolvedNode node) {
        if (node.isDev()) {
            entry.put("dev", true);
        }
        if (node.isOptional()) {
            entry.put("optional", true);
        }
        if (node.isDevOptional() && !node.isDev() && !node.isOptional()) {
            entry.put("devOptional", true);
        }
    }

    /**
     * The writer reproduces only the entry fields the corpus goldens pin exactly. {@code peerDependencies} and
     * {@code peerDependenciesMeta} are recorded verbatim (the graph proved the peers already satisfied), and
     * {@code optionalDependencies} are recorded like {@code dependencies}. A manifest carrying any other
     * lock-surfaced field (a platform gate, a bin, an install script, a bundle) reshapes the entry in a way not
     * yet byte-verified, so it defers rather than guess.
     */
    private static void requireEmittable(VersionManifest m) {
        // peerDependenciesMeta is only reproduced alongside the peerDependencies it annotates; a meta-only shape
        // (no peers) is unusual and not byte-verified, so it defers.
        JsonNode peerMeta = m.getPeerDependenciesMeta();
        deferIf(m, "peerDependenciesMeta",
                peerMeta != null && peerMeta.isObject() && peerMeta.size() > 0 && !notEmpty(m.getPeerDependencies()));
        deferIf(m, "bin", m.getBin() != null);
        deferIf(m, "os", m.getOs() != null);
        deferIf(m, "cpu", m.getCpu() != null);
        deferIf(m, "libc", m.getLibc() != null);
        deferIf(m, "funding", m.getFunding() != null);
        deferIf(m, "deprecated", m.getDeprecated() != null);
        deferIf(m, "hasInstallScript", Boolean.TRUE.equals(m.getHasInstallScript()));
        deferIf(m, "hasShrinkwrap", Boolean.TRUE.equals(m.getHasShrinkwrap()));
        deferIf(m, "bundleDependencies", m.getBundleDependencies() != null);
        deferIf(m, "acceptDependencies", notEmpty(m.getAcceptDependencies()));
    }

    private static void deferIf(VersionManifest m, String field, boolean present) {
        if (present) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, m.getName(),
                    m.getName() + "@" + m.getVersion() + " declares " + field + " (entry shape not yet reproduced)");
        }
    }

    // --- npm hoisting layout ----------------------------------------------

    /**
     * Compute each node's {@code packages} key by npm's hoisting: a package is placed at the highest
     * {@code node_modules} on its requirer's path whose name slot is free or already holds the same version;
     * a conflicting version nests one level deeper. Importer-direct dependencies claim the top level first.
     */
    private Map<String, String> hoist(ResolutionGraph graph, ResolutionGraph.Importer root) {
        Map<String, String> placements = new LinkedHashMap<>();          // nodeKey -> packages key
        Map<String, Map<String, String>> shelf = new HashMap<>();        // nm-prefix -> (name -> version)
        Set<String> visited = new HashSet<>();
        Deque<String[]> queue = new ArrayDeque<>();                      // {nodeKey, locationKey}

        for (Map.Entry<String, String> direct : root.getResolved().entrySet()) {
            resolveEdge(graph, "", direct.getKey(), direct.getValue(), placements, shelf, visited, queue);
        }
        while (!queue.isEmpty()) {
            String[] cur = queue.poll();
            ResolvedNode node = graph.getNodes().get(cur[0]);
            for (Map.Entry<String, String> edge : node.getResolvedEdges().entrySet()) {
                resolveEdge(graph, cur[1], edge.getKey(), edge.getValue(), placements, shelf, visited, queue);
            }
        }
        requireUnambiguousForks(graph, placements, root);
        return placements;
    }

    private void resolveEdge(ResolutionGraph graph, String fromLocation, String depName, String depVersion,
                             Map<String, String> placements, Map<String, Map<String, String>> shelf,
                             Set<String> visited, Deque<String[]> queue) {
        for (String prefix : chainTopToBottom(fromLocation)) {
            Map<String, String> at = shelf.computeIfAbsent(prefix, k -> new HashMap<>());
            String existing = at.get(depName);
            if (existing == null) {
                String key = prefix + depName;
                String nodeKey = ResolutionGraph.key(depName, depVersion);
                String prior = placements.get(nodeKey);
                if (prior != null && !prior.equals(key)) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, depName,
                            depName + "@" + depVersion + " would be placed at both " + prior + " and " + key);
                }
                at.put(depName, depVersion);
                placements.put(nodeKey, key);
                if (visited.add(nodeKey)) {
                    queue.add(new String[]{nodeKey, key});
                }
                return;
            }
            if (existing.equals(depVersion)) {
                return; // deduped against an ancestor placement
            }
            // a different version occupies this level; nest one deeper
        }
        throw new EngineFailure(Reason.RESOLUTION_REQUIRED, depName,
                depName + "@" + depVersion + " could not be placed (no free node_modules on the path)");
    }

    /** The {@code node_modules} prefixes visible to a package at {@code locationKey}, shallowest first. */
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

    /**
     * A fork is reproduced byte-exact only when its hoisted (top-level) version is the importer's directly
     * declared one — so the winner of the top slot is unambiguous. A fork whose top-level version is transitive
     * would depend on npm's tie-breaking across the whole tree and defers.
     */
    private static void requireUnambiguousForks(ResolutionGraph graph, Map<String, String> placements,
                                                ResolutionGraph.Importer root) {
        Map<String, Set<String>> versionsByName = new LinkedHashMap<>();
        for (ResolvedNode node : graph.getNodes().values()) {
            versionsByName.computeIfAbsent(node.getName(), k -> new LinkedHashSet<>()).add(node.getVersion());
        }
        for (Map.Entry<String, Set<String>> e : versionsByName.entrySet()) {
            if (e.getValue().size() < 2) {
                continue;
            }
            String topKey = NM + e.getKey();
            String topVersion = null;
            for (String version : e.getValue()) {
                if (topKey.equals(placements.get(ResolutionGraph.key(e.getKey(), version)))) {
                    topVersion = version;
                }
            }
            if (topVersion == null || !topVersion.equals(root.getResolved().get(e.getKey()))) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, e.getKey(),
                        e.getKey() + " forks but its hoisted version is not the directly-declared one (ambiguous)");
            }
        }
    }

    // --- lockfileVersion 2 legacy tree ------------------------------------

    /**
     * The lockfileVersion 2 legacy {@code dependencies} tree. Reproduced byte-exact only for a flat closure (no
     * fork): one minimal {@code version}/{@code resolved}/{@code integrity} entry per name, keyed by bare name,
     * carrying a {@code peer} flag for a peer provider and a {@code requires} constraint map. npm omits peer edges
     * from {@code requires} but still emits it (as {@code {}}) whenever the node has any edge, so a peer-only node
     * gets an empty {@code requires}. A fork would nest the legacy tree, which defers.
     */
    private ObjectNode legacyTree(ResolutionGraph graph, Map<String, String> placements, Set<String> peerProviders) {
        ObjectNode legacy = JSON.createObjectNode();
        for (Map.Entry<String, ResolvedNode> e : graph.getNodes().entrySet()) {
            ResolvedNode node = e.getValue();
            String key = placements.get(e.getKey());
            if (!(NM + node.getName()).equals(key)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, node.getName(),
                        node.getName() + " is nested; the lockfileVersion 2 legacy tree of a fork is not yet reproduced");
            }
            VersionManifest m = node.getManifest();
            if (notEmpty(m.getOptionalDependencies())) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, node.getName(),
                        node.getName() + " declares optionalDependencies; the lockfileVersion 2 legacy tree of an " +
                                "optional-bearing package is not yet reproduced");
            }
            VersionManifest.Dist dist = m.getDist();
            ObjectNode entry = JSON.createObjectNode();
            entry.put("version", m.getVersion());
            entry.put("resolved", dist == null ? null : dist.getTarball());
            entry.put("integrity", dist == null ? null : dist.getIntegrity());
            applyDepFlags(entry, node);
            if (peerProviders.contains(e.getKey())) {
                entry.put("peer", true);
            }
            if (notEmpty(m.getDependencies())) {
                entry.set("requires", stringMapNode(m.getDependencies()));
            } else if (notEmpty(m.getPeerDependencies())) {
                entry.set("requires", JSON.createObjectNode());
            }
            legacy.set(node.getName(), entry);
        }
        return legacy;
    }

    /**
     * The node keys npm flags {@code peer: true}: a node has an incoming peer edge when some resolved package
     * declares its name in a <em>non-optional</em> {@code peerDependencies}. An optional peer (per
     * {@code peerDependenciesMeta}) confers no flag even when the provider is present, so it is skipped. The graph
     * proved each such peer resolves to a single satisfying version, so the provider is that one node.
     */
    private static Set<String> peerProviderKeys(ResolutionGraph graph) {
        Map<String, Set<String>> versionsByName = new LinkedHashMap<>();
        for (ResolvedNode node : graph.getNodes().values()) {
            versionsByName.computeIfAbsent(node.getName(), k -> new LinkedHashSet<>()).add(node.getVersion());
        }
        Set<String> providers = new LinkedHashSet<>();
        for (ResolvedNode node : graph.getNodes().values()) {
            Map<String, String> peers = node.getManifest().getPeerDependencies();
            if (peers == null) {
                continue;
            }
            JsonNode meta = node.getManifest().getPeerDependenciesMeta();
            for (String peerName : peers.keySet()) {
                if (isOptionalPeer(meta, peerName)) {
                    continue;
                }
                Set<String> versions = versionsByName.get(peerName);
                if (versions != null && versions.size() == 1) {
                    providers.add(ResolutionGraph.key(peerName, versions.iterator().next()));
                }
            }
        }
        return providers;
    }

    private static boolean isOptionalPeer(@Nullable JsonNode meta, String peer) {
        if (meta == null) {
            return false;
        }
        JsonNode entry = meta.get(peer);
        return entry != null && entry.path("optional").asBoolean(false);
    }

    // --- helpers ----------------------------------------------------------

    private static ObjectNode stringMapNode(Map<String, String> map) {
        ObjectNode node = JSON.createObjectNode();
        for (Map.Entry<String, String> e : map.entrySet()) {
            node.put(e.getKey(), e.getValue());
        }
        return node;
    }

    private static boolean notEmpty(@Nullable Map<String, String> m) {
        return m != null && !m.isEmpty();
    }

    private static ResolutionGraph.Importer singleRootImporter(ResolutionGraph graph) {
        if (graph.getImporters().size() != 1) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "only a single-importer project is reproduced (found " + graph.getImporters().size() + ")");
        }
        ResolutionGraph.Importer root = graph.getImporters().get(0);
        if (!root.getDir().isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "only a root importer is reproduced (found importer dir '" + root.getDir() + "')");
        }
        return root;
    }
}
