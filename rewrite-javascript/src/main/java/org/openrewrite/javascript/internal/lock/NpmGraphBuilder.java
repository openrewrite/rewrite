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
import org.openrewrite.javascript.internal.registry.NodeRegistryException;
import org.openrewrite.javascript.internal.registry.VersionManifest;
import org.openrewrite.semver.Semver;

import java.util.*;

import static org.openrewrite.javascript.internal.LockFileRegeneration.Reason.RESOLUTION_REQUIRED;
import static org.openrewrite.semver.Semver.Ecosystem.NODE;

/**
 * Builds the {@link ResolutionGraph} for the npm resolution of a closure: every package resolves to a single
 * version (the highest satisfying every requirer). Regular, dev, and optional dependencies (importer-declared or
 * transitive) are all resolved and placed; each node is then classified {@code dev}/{@code optional}/
 * {@code devOptional} by npm's reachability rules for the serializers to mark. A manifest may declare
 * {@code peerDependencies} as long as every non-optional peer is already satisfied by a resolved node (a
 * top-level dependency or a normal dependency of some node) at a version its range admits — the peer is then a
 * constraint already met and adds no node. A missing non-optional peer is npm's auto-install: when it is enabled
 * (npm only; see {@link #autoInstallPeers}) and the slice is cleanest — an all-prod closure, the peer
 * a single pure-leaf version required by a single package — the peer is added as a top-level node; every other
 * missing-peer shape fails loud. An {@code npm:<name>@<range>} alias resolves its real package but is keyed and
 * placed by the alias name, reproduced only when self-contained (no un-aliased copy of the same package, no peer
 * entanglement). Version and constraint decisions are delegated entirely to node-semver.
 */
public final class NpmGraphBuilder {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final List<String> ROOT_SCOPES =
            Arrays.asList("dependencies", "devDependencies", "optionalDependencies");

    private final Registry registry;

    /**
     * npm 7+ auto-installs a missing non-optional peer. Only the npm serialization places such a node byte-exact, so
     * it is enabled solely for npm; the other package managers share this builder but keep the classic
     * deferral (their locks would not reproduce an auto-installed peer node).
     */
    private final boolean autoInstallPeers;

    /**
     * Versions already present in the existing lock, keyed by tree-slot name (an alias seeds under its alias name).
     * Real installs are incremental: a locked version still satisfying a range is kept rather than re-resolved to
     * the registry maximum. Empty seeds a fresh resolution.
     */
    private final Map<String, Set<String>> lockedVersions;

    public NpmGraphBuilder(Registry registry) {
        this(registry, false);
    }

    public NpmGraphBuilder(Registry registry, boolean autoInstallPeers) {
        this(registry, autoInstallPeers, Collections.emptyMap());
    }

    public NpmGraphBuilder(Registry registry, boolean autoInstallPeers, Map<String, Set<String>> lockedVersions) {
        this.registry = registry;
        this.autoInstallPeers = autoInstallPeers;
        this.lockedVersions = lockedVersions;
    }

    public ResolutionGraph build(Map<String, String> importerManifests) {
        List<ImporterDecl> declared = new ArrayList<>();
        for (Map.Entry<String, String> e : importerManifests.entrySet()) {
            declared.add(parseImporter(e.getKey(), e.getValue()));
        }

        Map<String, Set<String>> chosen = new LinkedHashMap<>();      // name -> selected versions (>1 = fork)
        Map<String, VersionManifest> manifests = new LinkedHashMap<>();  // nodeKey -> manifest
        Map<String, Map<String, String>> nodeEdges = new LinkedHashMap<>();          // nodeKey -> regular edges
        Map<String, Map<String, String>> nodeOptionalEdges = new LinkedHashMap<>();  // nodeKey -> optional edges
        Deque<String[]> work = new ArrayDeque<>();                    // {name, version} awaiting edge resolution

        // Phase 1: importer direct deps select their versions first, so a compatible transitive dedupes to a
        // directly-declared version rather than resolving its own.
        for (ImporterDecl decl : declared) {
            for (Map<String, String> scope : decl.scopes.values()) {
                for (Map.Entry<String, String> dep : scope.entrySet()) {
                    selectDep(dep.getKey(), dep.getValue(), chosen, manifests, work);
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
            nodeEdges.put(nodeKey, resolveEdges(manifest.getDependencies(), chosen, manifests, work));
            nodeOptionalEdges.put(nodeKey,
                    resolveEdges(manifest.getOptionalDependencies(), chosen, manifests, work));
        }
        requireResolvableAliases(deriveAliases(manifests), chosen, manifests);
        Set<String> autoInstalledPeers = resolvePeers(manifests, chosen, declared);

        List<ResolutionGraph.Importer> importers = new ArrayList<>();
        for (ImporterDecl decl : declared) {
            Map<String, String> importerResolved = new LinkedHashMap<>();
            for (Map<String, String> scope : decl.scopes.values()) {
                for (Map.Entry<String, String> dep : scope.entrySet()) {
                    importerResolved.put(dep.getKey(), resolvedVersionOf(dep.getKey(), dep.getValue(), chosen));
                }
            }
            // peerDependencies trails the resolved scopes so the writer mirrors npm's root-entry field order.
            Map<String, Map<String, String>> declaredScopes = new LinkedHashMap<>(decl.scopes);
            if (!decl.peers.isEmpty()) {
                declaredScopes.put("peerDependencies", decl.peers);
            }
            importers.add(new ResolutionGraph.Importer(decl.dir, decl.name, decl.version, declaredScopes, importerResolved));
        }

        DepFlags flags = classifyFlags(manifests.keySet(), importers, nodeEdges, nodeOptionalEdges);
        // An auto-installed peer is unreachable from the dependency graph, so the reachability fixpoint leaves it a
        // candidate for every flag; in the all-prod closure it is gated to, it carries none.
        flags.dev.removeAll(autoInstalledPeers);
        flags.optional.removeAll(autoInstalledPeers);
        flags.devOptional.removeAll(autoInstalledPeers);

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

    private Map<String, String> resolveEdges(@Nullable Map<String, String> declaredEdges,
                                             Map<String, Set<String>> chosen, Map<String, VersionManifest> manifests,
                                             Deque<String[]> work) {
        Map<String, String> edges = new LinkedHashMap<>();
        if (declaredEdges != null) {
            for (Map.Entry<String, String> dep : declaredEdges.entrySet()) {
                edges.put(dep.getKey(), selectDep(dep.getKey(), dep.getValue(), chosen, manifests, work));
            }
        }
        return edges;
    }

    /**
     * Resolve a single {@code (name, range)} requirement, deduping to an already-chosen version when one
     * satisfies. A range no chosen version satisfies selects a fresh version; when that means a <em>second</em>
     * version of an already-resolved name it is kept as a fork (both directly-declared and transitive forks
     * proceed), and the consumer decides which layouts it can reproduce byte-exact.
     */
    private String select(String name, String range,
                          Map<String, Set<String>> chosen, Map<String, VersionManifest> manifests,
                          Deque<String[]> work) {
        String deduped = Semver.maxSatisfying(chosen.getOrDefault(name, Collections.emptySet()), range, NODE);
        if (deduped != null) {
            return deduped;
        }
        String version = lockedSatisfying(name, range);
        if (version == null) {
            version = Semver.maxSatisfying(registry.versions(name), range, NODE);
        }
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

    /** The highest already-locked version of {@code name} that {@code range} admits, or {@code null}. */
    private @Nullable String lockedSatisfying(String name, String range) {
        return Semver.maxSatisfying(lockedVersions.getOrDefault(name, Collections.emptySet()), range, NODE);
    }

    /**
     * Resolve one declared dependency value. An {@code npm:<name>@<range>} alias installs the real package
     * {@code <name>} under the declared directory name, so it routes to {@link #selectAlias}; anything else is a
     * plain requirement for {@link #select}. An alias whose target is not a registry range (a git/file/url/dist-tag
     * spec) defers.
     */
    private String selectDep(String name, String spec, Map<String, Set<String>> chosen,
                             Map<String, VersionManifest> manifests, Deque<String[]> work) {
        if (spec.startsWith("npm:")) {
            Alias alias = parseAlias(spec);
            if (alias == null) {
                throw new EngineFailure(RESOLUTION_REQUIRED, name,
                        name + " aliases " + spec + " (only a registry-range alias is resolved)");
            }
            return selectAlias(name, alias.realName, alias.range, chosen, manifests, work);
        }
        return select(name, spec, chosen, manifests, work);
    }

    /**
     * Resolve an alias's real package but key it by the alias name ({@code aliasName@version}), so its tree slot
     * and dedup follow the alias while its (real) manifest drives the entry. The real package's own dependencies
     * resolve normally under their real names.
     */
    private String selectAlias(String aliasName, String realName, String range, Map<String, Set<String>> chosen,
                               Map<String, VersionManifest> manifests, Deque<String[]> work) {
        String deduped = Semver.maxSatisfying(chosen.getOrDefault(aliasName, Collections.emptySet()), range, NODE);
        if (deduped != null) {
            return deduped;
        }
        String version = lockedSatisfying(aliasName, range);
        if (version == null) {
            version = Semver.maxSatisfying(registry.versions(realName), range, NODE);
        }
        if (version == null) {
            throw new EngineFailure(RESOLUTION_REQUIRED, realName, "no version of " + realName + " satisfies " + range);
        }
        String key = ResolutionGraph.key(aliasName, version);
        if (!manifests.containsKey(key)) {
            manifests.put(key, registry.manifest(realName, version));
            chosen.computeIfAbsent(aliasName, k -> new LinkedHashSet<>()).add(version);
            work.add(new String[]{aliasName, version});
        }
        return version;
    }

    /** The version a directly-declared dependency resolved to, reading an alias spec's range rather than its literal. */
    private static String resolvedVersionOf(String name, String spec, Map<String, Set<String>> chosen) {
        String range = spec;
        if (spec.startsWith("npm:")) {
            Alias alias = parseAlias(spec);
            if (alias != null) {
                range = alias.range;
            }
        }
        return Semver.maxSatisfying(chosen.getOrDefault(name, Collections.emptySet()), range, NODE);
    }

    /** Parse an {@code npm:<name>@<range>} alias, or {@code null} when the target is not a registry range. */
    private static @Nullable Alias parseAlias(String spec) {
        String body = spec.substring("npm:".length());
        int at = body.lastIndexOf('@');
        if (at <= 0) {
            return null;
        }
        String range = body.substring(at + 1);
        if (range.isEmpty() || !Semver.validate(range, null, NODE).isValid()) {
            return null;
        }
        return new Alias(body.substring(0, at), range);
    }

    /**
     * Recover every resolved alias node — one whose graph key name differs from its manifest name — as
     * {@code aliasName -> realName}, so the reproducibility guard can inspect them without threading extra state.
     */
    private static Map<String, String> deriveAliases(Map<String, VersionManifest> manifests) {
        Map<String, String> aliases = new LinkedHashMap<>();
        for (Map.Entry<String, VersionManifest> e : manifests.entrySet()) {
            String key = e.getKey();
            String slot = key.substring(0, key.lastIndexOf('@'));
            if (!slot.equals(e.getValue().getName())) {
                aliases.put(slot, e.getValue().getName());
            }
        }
        return aliases;
    }

    /**
     * Only a self-contained alias is reproduced byte-exact: the real package must not also resolve un-aliased (nor
     * be aliased more than once), and it must not entangle the peer machinery (which keys by real name). An alias
     * that forks with a non-aliased copy, whose real name is required as a peer, or that itself declares peers,
     * defers with the classic message.
     */
    private static void requireResolvableAliases(Map<String, String> aliases, Map<String, Set<String>> chosen,
                                                 Map<String, VersionManifest> manifests) {
        if (aliases.isEmpty()) {
            return;
        }
        Map<String, Integer> targetCount = new LinkedHashMap<>();
        for (String realName : aliases.values()) {
            targetCount.merge(realName, 1, Integer::sum);
        }
        Set<String> aliasedReal = new HashSet<>(aliases.values());
        for (Map.Entry<String, String> alias : aliases.entrySet()) {
            String realName = alias.getValue();
            if (chosen.containsKey(realName) || targetCount.get(realName) > 1) {
                throw new EngineFailure(RESOLUTION_REQUIRED, realName, alias.getKey() + " aliases " + realName +
                        " which also resolves un-aliased (alias fork not yet resolved)");
            }
        }
        for (VersionManifest m : manifests.values()) {
            Map<String, String> peers = m.getPeerDependencies();
            if (peers == null) {
                continue;
            }
            for (String peerName : peers.keySet()) {
                if (aliasedReal.contains(peerName)) {
                    throw new EngineFailure(RESOLUTION_REQUIRED, peerName, peerName +
                            " is aliased but also required as a peer (alias-peer entanglement not yet resolved)");
                }
            }
        }
        for (Map.Entry<String, String> alias : aliases.entrySet()) {
            for (String version : chosen.getOrDefault(alias.getKey(), Collections.emptySet())) {
                VersionManifest m = manifests.get(ResolutionGraph.key(alias.getKey(), version));
                if (m != null && notEmpty(m.getPeerDependencies())) {
                    throw new EngineFailure(RESOLUTION_REQUIRED, alias.getValue(), alias.getValue() +
                            " (aliased) declares peerDependencies (alias-with-peers not yet resolved)");
                }
            }
        }
    }

    private static final class Alias {
        final String realName;
        final String range;

        Alias(String realName, String range) {
            this.realName = realName;
            this.range = range;
        }
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
     * Resolve every {@code peerDependencies} declaration — both a resolved node's and a library root importer's own.
     * A peer already met by the resolved closure adds no node (verified in place). A non-optional peer with no
     * provider is npm's auto-install: when {@link #autoInstallPeers} is enabled and the slice is the cleanest one —
     * an all-prod closure, each missing peer a single pure-leaf version required by a single package (a node or the
     * root) — the peer is added as a top-level node the serializer flags {@code peer: true}. Every other missing-peer
     * shape (auto-install disabled, a dev/optional closure, a non-leaf peer, an interacting multi-requirer peer, an
     * unfetchable peer, or a peer present at a non-satisfying / forked version) fails loud with the classic deferral
     * so no serializer emits a peer layout it cannot reproduce. An optional peer (per {@code peerDependenciesMeta})
     * may be absent; the root importer carries no meta here (that shape defers), so its peers are all required.
     *
     * @return the node keys of the auto-installed peers (they carry no dev/optional flag in the all-prod closure).
     */
    private Set<String> resolvePeers(Map<String, VersionManifest> manifests, Map<String, Set<String>> chosen,
                                     List<ImporterDecl> declared) {
        List<String[]> missing = new ArrayList<>();  // {requirer, peerName, range}
        for (VersionManifest m : new ArrayList<>(manifests.values())) {
            Map<String, String> peers = m.getPeerDependencies();
            if (peers == null) {
                continue;
            }
            JsonNode meta = m.getPeerDependenciesMeta();
            for (Map.Entry<String, String> peer : peers.entrySet()) {
                resolvePeer(m.getName(), peer.getKey(), peer.getValue(), meta, chosen, missing);
            }
        }
        for (ImporterDecl decl : declared) {
            for (Map.Entry<String, String> peer : decl.peers.entrySet()) {
                resolvePeer(rootRequirer(decl), peer.getKey(), peer.getValue(), null, chosen, missing);
            }
        }
        return missing.isEmpty() ? Collections.emptySet() : installMissingPeers(missing, manifests, chosen, declared);
    }

    /**
     * Classify one {@code (requirer, peer, range)}: an unmet non-optional peer is collected for auto-install (or
     * defers when disabled), a present peer must resolve to a single satisfying version, and an optional absent peer
     * is skipped.
     */
    private void resolvePeer(String requirer, String peerName, String range, @Nullable JsonNode meta,
                             Map<String, Set<String>> chosen, List<String[]> missing) {
        Set<String> resolved = chosen.getOrDefault(peerName, Collections.emptySet());
        if (resolved.isEmpty()) {
            if (isOptionalPeer(meta, peerName)) {
                return;
            }
            if (!autoInstallPeers) {
                throw peerNotInstalled(requirer, peerName);
            }
            missing.add(new String[]{requirer, peerName, range});
            return;
        }
        if (resolved.size() > 1) {
            throw new EngineFailure(RESOLUTION_REQUIRED, requirer, requirer + " peer " + peerName +
                    " resolves to multiple versions " + resolved + " (peer fork not yet resolved)");
        }
        String v = resolved.iterator().next();
        if (!Semver.validate(range, null, NODE).isValid() || !Semver.satisfies(v, range, NODE)) {
            throw new EngineFailure(RESOLUTION_REQUIRED, requirer, requirer + " peer " + peerName + "@" +
                    v + " does not satisfy " + range + " (peer re-resolution not yet resolved)");
        }
    }

    private static String rootRequirer(ImporterDecl decl) {
        return decl.name != null ? decl.name : "root";
    }

    /**
     * Add each missing non-optional peer as a top-level node (npm 7+ auto-install), gated to the clean slice
     * the npm serialization reproduces byte-exact. Any un-clean condition defers with the classic peer message.
     */
    private Set<String> installMissingPeers(List<String[]> missing, Map<String, VersionManifest> manifests,
                                            Map<String, Set<String>> chosen, List<ImporterDecl> declared) {
        String[] first = missing.get(0);
        // Only an all-prod closure with each peer required by a single package is reproduced; the peer inheriting a
        // dev/optional flag, or an interacting (multi-requirer) peer, defers.
        Set<String> requestedPeers = new LinkedHashSet<>();
        boolean clean = closureIsAllProd(declared, manifests);
        for (String[] miss : missing) {
            clean &= requestedPeers.add(miss[1]);
        }
        if (!clean) {
            throw peerNotInstalled(first[0], first[1]);
        }
        Set<String> autoInstalled = new LinkedHashSet<>();
        for (String[] miss : missing) {
            VersionManifest peerManifest = resolveLeafPeer(miss[1], miss[2]);
            if (peerManifest == null) {
                throw peerNotInstalled(miss[0], miss[1]);
            }
            String key = ResolutionGraph.key(miss[1], peerManifest.getVersion());
            manifests.put(key, peerManifest);
            chosen.computeIfAbsent(miss[1], k -> new LinkedHashSet<>()).add(peerManifest.getVersion());
            autoInstalled.add(key);
        }
        return autoInstalled;
    }

    /**
     * The single pure-leaf version of {@code peerName} admitted by {@code range}, or {@code null} when the peer is
     * unfetchable, has no satisfying version, or carries dependencies/peers of its own (all of which defer).
     */
    private @Nullable VersionManifest resolveLeafPeer(String peerName, String range) {
        VersionManifest m;
        try {
            String version = lockedSatisfying(peerName, range);
            if (version == null) {
                version = Semver.maxSatisfying(registry.versions(peerName), range, NODE);
            }
            if (version == null) {
                return null;
            }
            m = registry.manifest(peerName, version);
        } catch (NodeRegistryException e) {
            return null;
        }
        boolean leaf = !notEmpty(m.getDependencies()) && !notEmpty(m.getOptionalDependencies()) &&
                !notEmpty(m.getPeerDependencies());
        return leaf ? m : null;
    }

    private static EngineFailure peerNotInstalled(String requirer, String peerName) {
        return new EngineFailure(RESOLUTION_REQUIRED, requirer,
                requirer + " peer " + peerName + " is not installed (peer auto-install not yet resolved)");
    }

    /** No importer declares dev/optional scopes and no resolved manifest declares optionalDependencies. */
    private static boolean closureIsAllProd(List<ImporterDecl> declared, Map<String, VersionManifest> manifests) {
        for (ImporterDecl decl : declared) {
            if (decl.scopes.containsKey("devDependencies") || decl.scopes.containsKey("optionalDependencies")) {
                return false;
            }
        }
        for (VersionManifest m : manifests.values()) {
            if (notEmpty(m.getOptionalDependencies())) {
                return false;
            }
        }
        return true;
    }

    private static boolean notEmpty(@Nullable Map<String, String> m) {
        return m != null && !m.isEmpty();
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
            // A library root's own peerDependencies are kept apart from the resolved scopes: they never select a
            // node directly (Phase 1), they auto-install through resolvePeers like any transitive peer.
            Map<String, String> peers = new LinkedHashMap<>();
            JsonNode peerNode = root.get("peerDependencies");
            if (peerNode != null && peerNode.isObject()) {
                peerNode.fields().forEachRemaining(f -> peers.put(f.getKey(), f.getValue().asText()));
            }
            return new ImporterDecl(dir, name, version, scopes, peers);
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
        final Map<String, String> peers;

        ImporterDecl(String dir, @Nullable String name, @Nullable String version,
                     Map<String, Map<String, String>> scopes, Map<String, String> peers) {
            this.dir = dir;
            this.name = name;
            this.version = version;
            this.scopes = scopes;
            this.peers = peers;
        }
    }
}
