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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.EngineFailure;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.*;

/**
 * Serializes a {@link ResolutionGraph} to {@code pnpm-lock.yaml} text (lockfileVersion 9), byte-for-byte identical
 * to what a real {@code pnpm install --lockfile-only} writes. pnpm is content-addressed — no hoisting — so layout
 * is trivial: the {@code importers} entry lists each declared dependency's specifier and resolved version, the
 * {@code packages} map carries one {@code name@version} per resolved node (its {@code resolution} integrity,
 * {@code engines} and verbatim {@code peerDependencies}), and the {@code snapshots} map carries one entry per node
 * with its resolved dependency edges. A satisfied peer surface is reproduced through pnpm's peer-specific snapshot
 * keys: a node with resolved peers is keyed {@code name@version(peer@version)...} in {@code snapshots} (and
 * referenced that way from its dependents), the peer materializing as a normal snapshot dependency. A directly
 * declared fork keeps both versions side by side with no nesting. Anything not yet byte-verified — an optional peer,
 * a transitive peer ({@code transitivePeerDependencies}), a platform gate, a bin, a workspace — fails loud.
 */
public final class PnpmLockWriter {

    public String write(ResolutionGraph graph) {
        ResolutionGraph.Importer root = singleRootImporter(graph);
        Map<String, String> declared = prodDeclared(root);
        PeerLayout peers = new PeerLayout(graph);

        List<String> nodeKeys = new ArrayList<>(graph.getNodes().keySet());
        Collections.sort(nodeKeys);

        StringBuilder sb = new StringBuilder();
        sb.append("lockfileVersion: '9.0'\n");
        sb.append("\nsettings:\n")
                .append("  autoInstallPeers: true\n")
                .append("  excludeLinksFromLockfile: false\n");

        // importers
        sb.append("\nimporters:\n");
        sb.append("\n  .:");
        if (declared.isEmpty()) {
            sb.append(" {}\n");
        } else {
            sb.append("\n    dependencies:\n");
            for (String name : sortedKeys(declared)) {
                String resolved = root.getResolved().get(name);
                if (resolved == null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            name + " declared but not resolved");
                }
                requirePlainSpecifier(name, declared.get(name));
                sb.append("      ").append(yamlToken(name)).append(":\n")
                        .append("        specifier: ").append(declared.get(name)).append('\n')
                        .append("        version: ").append(peers.reference(ResolutionGraph.key(name, resolved))).append('\n');
            }
        }

        // packages (bare name@version keys, sorted)
        sb.append("\npackages:\n");
        for (String key : nodeKeys) {
            ResolvedNode node = graph.getNodes().get(key);
            VersionManifest m = node.getManifest();
            requireEmittable(m);
            String integrity = m.getDist() == null ? null : m.getDist().getIntegrity();
            if (integrity == null) {
                throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, m.getName(),
                        m.getName() + "@" + m.getVersion() + " has no registry integrity");
            }
            sb.append("\n  ").append(yamlToken(key)).append(":\n")
                    .append("    resolution: {integrity: ").append(integrity).append("}\n");
            if (notEmpty(m.getEngines())) {
                requireQuotableEngines(m.getName(), m.getEngines());
                sb.append("    engines: ").append(renderEngines(m.getEngines())).append('\n');
            }
            if (notEmpty(m.getPeerDependencies())) {
                Map<String, String> peerRanges = m.getPeerDependencies();
                sb.append("    peerDependencies:\n");
                for (String peerName : sortedKeys(peerRanges)) {   // pnpm sorts the peer block by name
                    sb.append("      ").append(yamlToken(peerName)).append(": ")
                            .append(yamlToken(peerRanges.get(peerName))).append('\n');
                }
            }
        }

        // snapshots (peer-suffixed keys, sorted by the suffixed key)
        sb.append("\nsnapshots:\n");
        TreeMap<String, String> bySnapshotKey = new TreeMap<>();
        for (String key : nodeKeys) {
            bySnapshotKey.put(peers.snapshotKey(key), key);
        }
        for (Map.Entry<String, String> e : bySnapshotKey.entrySet()) {
            Map<String, String> deps = peers.snapshotDependencies(e.getValue());
            sb.append("\n  ").append(yamlToken(e.getKey())).append(':');
            if (deps.isEmpty()) {
                sb.append(" {}\n");
            } else {
                sb.append("\n    dependencies:\n");
                for (String dep : sortedKeys(deps)) {
                    sb.append("      ").append(yamlToken(dep)).append(": ").append(deps.get(dep)).append('\n');
                }
            }
        }
        return sb.toString();
    }

    /**
     * The clean-closure/fork writer reproduces only the entry fields the goldens pin exactly. Verbatim
     * {@code peerDependencies} are emitted (the graph proved the peers already satisfied); {@code peerDependenciesMeta}
     * (an optional peer — which reshapes the snapshot and can leak into {@code transitivePeerDependencies}) and any
     * other lock-surfaced field (a platform gate, a bin, an install script, a bundle) defer.
     */
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
                    m.getName() + "@" + m.getVersion() + " declares " + field + " (entry shape not yet reproduced)");
        }
    }

    /**
     * pnpm's peer-specific snapshot keys and their propagation. A node's key is
     * {@code name@version(providerRef)(providerRef2)...}: one suffix per peer it declares (each satisfied by a single
     * resolved node, guaranteed by the graph builder), sorted by the rendered reference, and a provider that itself
     * has peers renders its own suffix recursively. Only the "every peer is directly declared where it is needed"
     * shape is reproduced: a peer that reaches a node from its subtree without being declared there is a transitive
     * peer (pnpm's {@code transitivePeerDependencies}), and an optional/absent/forked peer likewise reshapes the
     * layout — all fail loud rather than emit a wrong lock.
     */
    private static final class PeerLayout {
        private final ResolutionGraph graph;
        private final Map<String, String> versionByName = new HashMap<>();
        private final Set<String> forkedNames = new HashSet<>();
        private final Map<String, String> suffixCache = new HashMap<>();

        PeerLayout(ResolutionGraph graph) {
            this.graph = graph;
            for (ResolvedNode node : graph.getNodes().values()) {
                if (versionByName.put(node.getName(), node.getVersion()) != null) {
                    forkedNames.add(node.getName());
                }
            }
            requireNoTransitivePeers();
            // Warm the suffix cache so a cycle or an unsatisfiable peer defers before any bytes are emitted.
            for (String key : graph.getNodes().keySet()) {
                snapshotKey(key);
            }
        }

        /** The {@code snapshots}/{@code packages}-referenced key: {@code name@version} with the peer suffix. */
        String snapshotKey(String nodeKey) {
            return nodeKey + suffix(nodeKey);
        }

        /** The importer/edge form: the resolved version with the peer suffix (the snapshot key without the name). */
        String reference(String nodeKey) {
            return graph.getNodes().get(nodeKey).getVersion() + suffix(nodeKey);
        }

        private String suffix(String nodeKey) {
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
                        node.getName() + " is in a peer-dependency cycle (not yet reproduced)");
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

        /** The unique resolved node providing a peer; an absent or forked peer is not reproducible here. */
        private String providerKey(ResolvedNode consumer, String peerName) {
            if (forkedNames.contains(peerName)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, consumer.getName(),
                        consumer.getName() + " peer " + peerName + " resolves to multiple versions (peer fork not yet reproduced)");
            }
            String version = versionByName.get(peerName);
            if (version == null) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, consumer.getName(),
                        consumer.getName() + " peer " + peerName + " is not installed (optional/auto-install peer not yet reproduced)");
            }
            return ResolutionGraph.key(peerName, version);
        }

        /** Regular dependency edges merged with satisfied peer edges, each valued by its target's peer reference. */
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
                                node.getName() + " lists " + peerName + " as both a dependency and a peer (not yet reproduced)");
                    }
                    deps.put(peerName, reference(providerKey(node, peerName)));
                }
            }
            return deps;
        }

        /**
         * Fail loud if any node would carry a {@code transitivePeerDependencies} list — a peer required somewhere in
         * its subtree that the node neither declares itself nor resolves through a dependency of its own.
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
                                        " (pnpm transitivePeerDependencies not yet reproduced)");
                    }
                }
            }
        }

        /** The peers a node still needs from above: its own declared peers plus its children's, minus what it provides. */
        private Set<String> peerContext(String nodeKey, Map<String, Set<String>> cache, Set<String> visiting) {
            Set<String> cached = cache.get(nodeKey);
            if (cached != null) {
                return cached;
            }
            ResolvedNode node = graph.getNodes().get(nodeKey);
            if (!visiting.add(nodeKey)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, node.getName(),
                        node.getName() + " is in a peer-dependency cycle (not yet reproduced)");
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

    /** Only a pure {@code dependencies} closure is reproduced; a dev/optional importer scope defers. */
    private static Map<String, String> prodDeclared(ResolutionGraph.Importer root) {
        for (String scope : root.getDeclared().keySet()) {
            if (!"dependencies".equals(scope)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                        "importer declares " + scope + " (only prod dependencies are resolved today)");
            }
        }
        return root.getDeclared().getOrDefault("dependencies", Collections.emptyMap());
    }

    /** pnpm single-quotes engine ranges; a value it would leave bare would be over-quoted by the renderer, so defer. */
    private static void requireQuotableEngines(String name, Map<String, String> engines) {
        for (String value : engines.values()) {
            if (isPlainYamlScalar(value)) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " engine constraint '" + value + "' is not single-quoted by pnpm; not yet reproduced");
            }
        }
    }

    private static String renderEngines(Map<String, String> engines) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> engine : engines.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(engine.getKey()).append(": '").append(engine.getValue()).append('\'');
        }
        return sb.append('}').toString();
    }

    private static void requirePlainSpecifier(String name, String specifier) {
        if (!isPlainYamlScalar(specifier)) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + " specifier '" + specifier + "' needs YAML quoting; not yet reproduced");
        }
    }

    /** A YAML key/scalar as pnpm renders it: bare when plain, single-quoted otherwise (e.g. a scoped {@code @name}). */
    private static String yamlToken(String s) {
        return isPlainYamlScalar(s) ? s : "'" + s + "'";
    }

    /** Whether {@code s} is a YAML plain scalar pnpm would emit unquoted (no leading indicator, no {@code :}/{@code #}). */
    private static boolean isPlainYamlScalar(String s) {
        if (s.isEmpty()) {
            return false;
        }
        if ("!&*?|>%@`\"'#,[]{}:- ".indexOf(s.charAt(0)) >= 0) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ':' || c == '#' || c == '\n' || c == '\t') {
                return false;
            }
        }
        return true;
    }

    private static List<String> sortedKeys(Map<String, String> map) {
        List<String> keys = new ArrayList<>(map.keySet());
        Collections.sort(keys);
        return keys;
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
