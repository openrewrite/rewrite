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
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.EngineFailure;
import org.openrewrite.javascript.internal.registry.VersionManifest;
import org.openrewrite.semver.NodeSemver;

import java.util.*;

import static org.openrewrite.javascript.internal.LockFileRegeneration.Reason.RESOLUTION_REQUIRED;

/**
 * Builds the {@link ResolutionGraph} for the npm resolution of a closure: every package resolves to a single
 * version (the highest satisfying every requirer). Regular, dev, and optional dependencies (importer-declared or
 * transitive) are all resolved and placed; each node is then classified {@code dev}/{@code optional}/
 * {@code devOptional} by npm's reachability rules for the serializers to mark. A manifest may declare
 * {@code peerDependencies} as long as every non-optional peer is already satisfied by a resolved node (a
 * top-level dependency or a normal dependency of some node) at a version its range admits — the peer is then a
 * constraint already met and adds no node. A missing non-optional peer (npm would auto-install it), an optional
 * peer present at a non-satisfying version, or a peer resolving to more than one version fails loud. Version and
 * constraint decisions are delegated entirely to node-semver.
 */
public final class NpmGraphBuilder {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> ROOT_SCOPES =
            Arrays.asList("dependencies", "devDependencies", "optionalDependencies");

    private final Registry registry;

    public NpmGraphBuilder(Registry registry) {
        this.registry = registry;
    }

    public ResolutionGraph build(Map<String, String> importerManifests) {
        List<ImporterDecl> declared = new ArrayList<>();
        Set<String> directDepNames = new LinkedHashSet<>();
        for (Map.Entry<String, String> e : importerManifests.entrySet()) {
            ImporterDecl decl = parseImporter(e.getKey(), e.getValue());
            declared.add(decl);
            for (Map<String, String> scope : decl.scopes.values()) {
                directDepNames.addAll(scope.keySet());
            }
        }

        Map<String, Set<String>> chosen = new LinkedHashMap<>();      // name -> selected versions (>1 = fork)
        Map<String, VersionManifest> manifests = new LinkedHashMap<>();  // nodeKey -> manifest
        Map<String, Map<String, String>> nodeEdges = new LinkedHashMap<>();          // nodeKey -> regular edges
        Map<String, Map<String, String>> nodeOptionalEdges = new LinkedHashMap<>();  // nodeKey -> optional edges
        Deque<String[]> work = new ArrayDeque<>();                    // {name, version} awaiting edge resolution

        // Phase 1: importer direct deps select their versions first, so a directly-declared version wins the
        // hoisted slot over any conflicting transitive requirement of the same name.
        for (ImporterDecl decl : declared) {
            for (Map<String, String> scope : decl.scopes.values()) {
                for (Map.Entry<String, String> dep : scope.entrySet()) {
                    select(dep.getKey(), dep.getValue(), directDepNames, chosen, manifests, work);
                }
            }
        }
        // Phase 2: BFS every resolved node's regular and optional edges (dedup to an already-chosen satisfying
        // version, else fork). Optional dependencies are resolved and placed like regular ones; only their flag
        // classification (below) differs.
        while (!work.isEmpty()) {
            String[] cur = work.poll();
            String nodeKey = ResolutionGraph.key(cur[0], cur[1]);
            VersionManifest manifest = manifests.get(nodeKey);
            nodeEdges.put(nodeKey, resolveEdges(manifest.getDependencies(), directDepNames, chosen, manifests, work));
            nodeOptionalEdges.put(nodeKey,
                    resolveEdges(manifest.getOptionalDependencies(), directDepNames, chosen, manifests, work));
        }
        verifyPeersSatisfied(manifests, chosen);

        List<ResolutionGraph.Importer> importers = new ArrayList<>();
        for (ImporterDecl decl : declared) {
            Map<String, String> importerResolved = new LinkedHashMap<>();
            for (Map<String, String> scope : decl.scopes.values()) {
                for (Map.Entry<String, String> dep : scope.entrySet()) {
                    importerResolved.put(dep.getKey(),
                            NodeSemver.maxSatisfying(chosen.getOrDefault(dep.getKey(), Collections.emptySet()), dep.getValue()));
                }
            }
            importers.add(new ResolutionGraph.Importer(decl.dir, decl.name, decl.version, decl.scopes, importerResolved));
        }

        DepFlags flags = classifyFlags(manifests.keySet(), importers, nodeEdges, nodeOptionalEdges);

        Map<String, ResolvedNode> nodes = new LinkedHashMap<>();
        for (Map.Entry<String, VersionManifest> e : manifests.entrySet()) {
            String nodeKey = e.getKey();
            Map<String, String> edges = new LinkedHashMap<>(nodeEdges.getOrDefault(nodeKey, Collections.emptyMap()));
            edges.putAll(nodeOptionalEdges.getOrDefault(nodeKey, Collections.emptyMap()));
            nodes.put(nodeKey, new ResolvedNode(e.getValue(), edges,
                    flags.dev.contains(nodeKey), flags.optional.contains(nodeKey), flags.devOptional.contains(nodeKey)));
        }
        return new ResolutionGraph(importers, nodes);
    }

    private Map<String, String> resolveEdges(@Nullable Map<String, String> declaredEdges, Set<String> directDepNames,
                                             Map<String, Set<String>> chosen, Map<String, VersionManifest> manifests,
                                             Deque<String[]> work) {
        Map<String, String> edges = new LinkedHashMap<>();
        if (declaredEdges != null) {
            for (Map.Entry<String, String> dep : declaredEdges.entrySet()) {
                edges.put(dep.getKey(), select(dep.getKey(), dep.getValue(), directDepNames, chosen, manifests, work));
            }
        }
        return edges;
    }

    /**
     * Resolve a single {@code (name, range)} requirement, deduping to an already-chosen version when one
     * satisfies. A range no chosen version satisfies selects a fresh version; when that means a <em>second</em>
     * version of an already-resolved name (a fork), it is allowed only for a directly-declared dependency — whose
     * declared version wins the hoisted slot — and otherwise defers.
     */
    private String select(String name, String range, Set<String> directDepNames,
                          Map<String, Set<String>> chosen, Map<String, VersionManifest> manifests,
                          Deque<String[]> work) {
        String deduped = NodeSemver.maxSatisfying(chosen.getOrDefault(name, Collections.emptySet()), range);
        if (deduped != null) {
            return deduped;
        }
        if (chosen.containsKey(name) && !directDepNames.contains(name)) {
            throw new EngineFailure(RESOLUTION_REQUIRED, name,
                    name + " required at " + range + " forks from " + chosen.get(name) + " (transitive fork)");
        }
        String version = NodeSemver.maxSatisfying(registry.versions(name), range);
        if (version == null) {
            throw new EngineFailure(RESOLUTION_REQUIRED, name, "no version of " + name + " satisfies " + range);
        }
        String key = ResolutionGraph.key(name, version);
        if (!manifests.containsKey(key)) {
            VersionManifest manifest = registry.manifest(name, version);
            manifests.put(key, manifest);
            chosen.computeIfAbsent(name, k -> new LinkedHashSet<>()).add(version);
            work.add(new String[]{name, version});
        }
        return version;
    }

    /**
     * npm's dev/optional/devOptional reachability, computed as a monotone flag-clearing fixpoint. Every node
     * starts a candidate for all three flags; each edge relaxation clears the flag a path fails to preserve
     * (a node stays {@code dev} only where every incoming path carries dev, and likewise for optional; a node
     * is {@code devOptional} where every path is dev-or-optional). An importer-declared {@code devDependencies}
     * edge is a dev edge, an {@code optionalDependencies} edge (importer or transitive) is an optional edge,
     * every other edge is plain. Serializers translate the surviving flags into their own marking.
     */
    private static DepFlags classifyFlags(Set<String> nodeKeys, List<ResolutionGraph.Importer> importers,
                                          Map<String, Map<String, String>> nodeEdges,
                                          Map<String, Map<String, String>> nodeOptionalEdges) {
        List<Edge> edges = new ArrayList<>();
        for (ResolutionGraph.Importer importer : importers) {
            for (Map.Entry<String, Map<String, String>> scope : importer.getDeclared().entrySet()) {
                boolean dev = "devDependencies".equals(scope.getKey());
                boolean optional = "optionalDependencies".equals(scope.getKey());
                for (String name : scope.getValue().keySet()) {
                    String version = importer.getResolved().get(name);
                    if (version != null) {
                        edges.add(new Edge(null, ResolutionGraph.key(name, version), dev, optional));
                    }
                }
            }
        }
        addNodeEdges(edges, nodeEdges, false);
        addNodeEdges(edges, nodeOptionalEdges, true);

        Set<String> dev = new HashSet<>(nodeKeys);
        Set<String> optional = new HashSet<>(nodeKeys);
        Set<String> devOptional = new HashSet<>(nodeKeys);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Edge e : edges) {
                boolean srcDev = e.src != null && dev.contains(e.src);
                boolean srcOptional = e.src != null && optional.contains(e.src);
                boolean srcDevOptional = e.src != null && devOptional.contains(e.src);
                if (!(srcDev || e.dev) && dev.remove(e.dst)) {
                    changed = true;
                }
                if (!(srcOptional || e.optional) && optional.remove(e.dst)) {
                    changed = true;
                }
                if (!(srcDevOptional || e.dev || e.optional) && devOptional.remove(e.dst)) {
                    changed = true;
                }
            }
        }
        return new DepFlags(dev, optional, devOptional);
    }

    private static void addNodeEdges(List<Edge> edges, Map<String, Map<String, String>> nodeEdges, boolean optional) {
        for (Map.Entry<String, Map<String, String>> node : nodeEdges.entrySet()) {
            for (Map.Entry<String, String> edge : node.getValue().entrySet()) {
                edges.add(new Edge(node.getKey(), ResolutionGraph.key(edge.getKey(), edge.getValue()), false, optional));
            }
        }
    }

    private static final class Edge {
        final @Nullable String src;  // null = the (flag-free) importer root
        final String dst;
        final boolean dev;
        final boolean optional;

        Edge(@Nullable String src, String dst, boolean dev, boolean optional) {
            this.src = src;
            this.dst = dst;
            this.dev = dev;
            this.optional = optional;
        }
    }

    private static final class DepFlags {
        final Set<String> dev;
        final Set<String> optional;
        final Set<String> devOptional;

        DepFlags(Set<String> dev, Set<String> optional, Set<String> devOptional) {
            this.dev = dev;
            this.optional = optional;
            this.devOptional = devOptional;
        }
    }

    /**
     * Every {@code peerDependencies} declaration must already be met by the resolved closure. A non-optional peer
     * with no provider would make npm auto-install one (a new node); a peer present but not admitted by the range,
     * or resolved to more than one version, would reshape the layout. All of those defer. An optional peer
     * (per {@code peerDependenciesMeta}) may be absent.
     */
    private static void verifyPeersSatisfied(Map<String, VersionManifest> manifests, Map<String, Set<String>> chosen) {
        for (VersionManifest m : manifests.values()) {
            Map<String, String> peers = m.getPeerDependencies();
            if (peers == null || peers.isEmpty()) {
                continue;
            }
            JsonNode meta = m.getPeerDependenciesMeta();
            for (Map.Entry<String, String> peer : peers.entrySet()) {
                String peerName = peer.getKey();
                String range = peer.getValue();
                Set<String> resolved = chosen.getOrDefault(peerName, Collections.emptySet());
                if (resolved.isEmpty()) {
                    if (isOptionalPeer(meta, peerName)) {
                        continue;
                    }
                    throw new EngineFailure(RESOLUTION_REQUIRED, m.getName(),
                            m.getName() + " peer " + peerName + " is not installed (peer auto-install not yet resolved)");
                }
                if (resolved.size() > 1) {
                    throw new EngineFailure(RESOLUTION_REQUIRED, m.getName(), m.getName() + " peer " + peerName +
                            " resolves to multiple versions " + resolved + " (peer fork not yet resolved)");
                }
                String v = resolved.iterator().next();
                if (!NodeSemver.validRange(range) || !NodeSemver.satisfies(v, range)) {
                    throw new EngineFailure(RESOLUTION_REQUIRED, m.getName(), m.getName() + " peer " + peerName + "@" +
                            v + " does not satisfy " + range + " (peer re-resolution not yet resolved)");
                }
            }
        }
    }

    private static boolean isOptionalPeer(@Nullable JsonNode meta, String peer) {
        if (meta == null) {
            return false;
        }
        JsonNode entry = meta.get(peer);
        return entry != null && entry.path("optional").asBoolean(false);
    }

    private static ImporterDecl parseImporter(String dir, String manifestJson) {
        Map<String, Map<String, String>> scopes = new LinkedHashMap<>();
        try {
            JsonNode root = JSON.readTree(manifestJson);
            String name = root.hasNonNull("name") ? root.get("name").asText() : null;
            String version = root.hasNonNull("version") ? root.get("version").asText() : null;
            for (String scope : ROOT_SCOPES) {
                JsonNode node = root.get(scope);
                if (node != null && node.isObject()) {
                    Map<String, String> deps = new LinkedHashMap<>();
                    node.fields().forEachRemaining(f -> deps.put(f.getKey(), f.getValue().asText()));
                    if (!deps.isEmpty()) {
                        scopes.put(scope, deps);
                    }
                }
            }
            return new ImporterDecl(dir, name, version, scopes);
        } catch (EngineFailure ef) {
            throw ef;
        } catch (Exception e) {
            throw new EngineFailure(RESOLUTION_REQUIRED, null, "could not parse importer manifest");
        }
    }

    private static final class ImporterDecl {
        final String dir;
        final @Nullable String name;
        final @Nullable String version;
        final Map<String, Map<String, String>> scopes;

        ImporterDecl(String dir, @Nullable String name, @Nullable String version,
                     Map<String, Map<String, String>> scopes) {
            this.dir = dir;
            this.name = name;
            this.version = version;
            this.scopes = scopes;
        }
    }
}
