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
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.BunJson;
import org.openrewrite.javascript.internal.lock.EngineFailure;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.*;

/**
 * Serializes a {@link ResolutionGraph} to {@code bun.lock} text (JSONC), byte-for-byte identical to what a real
 * {@code bun install --lockfile-only} would write. It computes bun's hoisted layout from the graph (every package
 * top-level for a clean closure; the conflicting version of a directly-declared fork nested under its requiring
 * parent as a {@code "parent/name"} tuple) and renders each entry through {@link BunJson}. A satisfied
 * {@code peerDependencies} surface is reproduced (its ranges verbatim, its optional peers flattened into bun's
 * {@code optionalPeers} array). Anything else it cannot reproduce byte-exact — a workspace, a dev/optional surface,
 * a manifest field bun surfaces into the tuple — fails loud rather than emit a wrong lock.
 */
public final class BunLockWriter {

    private static final String UNIT = "  ";
    /** Importer scopes in the order bun writes them; only {@code dependencies} survives the prod-only gate today. */
    private static final List<String> SCOPE_ORDER =
            Arrays.asList("dependencies", "devDependencies", "optionalDependencies", "peerDependencies");

    public String write(ResolutionGraph graph, int lockfileVersion, int configVersion) {
        if (lockfileVersion != 1) {
            throw new EngineFailure(Reason.UNSUPPORTED_LOCKFILE_VERSION, null,
                    "bun.lock lockfileVersion " + lockfileVersion + " is not supported (need 1)");
        }
        if (configVersion != 1) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "bun.lock configVersion " + configVersion + " is not supported (need 1)");
        }
        ResolutionGraph.Importer root = singleRootImporter(graph);
        List<Entry> entries = layout(graph, root);

        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append(UNIT).append("\"lockfileVersion\": ").append(lockfileVersion).append(",\n");
        sb.append(UNIT).append("\"configVersion\": ").append(configVersion).append(",\n");
        appendWorkspaces(sb, root);
        appendPackages(sb, entries);
        sb.append("}\n");
        return sb.toString();
    }

    // --- workspaces (single root importer) --------------------------------

    private static void appendWorkspaces(StringBuilder sb, ResolutionGraph.Importer root) {
        sb.append(UNIT).append("\"workspaces\": {\n");
        sb.append(UNIT).append(UNIT).append("\"\": {\n");
        if (root.getName() == null && root.getDeclared().isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "root importer has neither a name nor a dependency scope (not yet reproduced)");
        }
        String i3 = UNIT + UNIT + UNIT;
        if (root.getName() != null) {
            sb.append(i3).append("\"name\": ").append(BunJson.quote(root.getName())).append(",\n");
        }
        for (String scope : SCOPE_ORDER) {
            Map<String, String> deps = root.getDeclared().get(scope);
            if (deps == null || deps.isEmpty()) {
                continue;
            }
            sb.append(i3).append(BunJson.quote(scope)).append(": {\n");
            List<String> names = new ArrayList<>(deps.keySet());
            names.sort(null); // bun sorts each scope by name (ASCII)
            for (String name : names) {
                sb.append(i3).append(UNIT).append(BunJson.quote(name)).append(": ")
                        .append(BunJson.quote(deps.get(name))).append(",\n");
            }
            sb.append(i3).append("},\n");
        }
        sb.append(UNIT).append(UNIT).append("},\n");
        sb.append(UNIT).append("},\n");
    }

    // --- packages ---------------------------------------------------------

    private static void appendPackages(StringBuilder sb, List<Entry> entries) {
        sb.append(UNIT).append("\"packages\": {");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                sb.append("\n"); // bun separates entries with a blank line
            }
            Entry e = entries.get(i);
            sb.append("\n").append(UNIT).append(UNIT)
                    .append(BunJson.quote(e.key)).append(": ").append(tuple(e.node)).append(",");
        }
        sb.append("\n").append(UNIT).append("}\n");
    }

    private static String tuple(ResolvedNode node) {
        VersionManifest m = node.getManifest();
        requireEmittable(m);
        VersionManifest.Dist dist = m.getDist();
        String integrity = dist == null ? null : dist.getIntegrity();
        if (integrity == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, m.getName(),
                    m.getName() + "@" + m.getVersion() + " has no integrity");
        }
        return BunJson.renderTuple(m.getName(), m.getVersion(), m.getDependencies(),
                m.getPeerDependencies(), optionalPeers(m), integrity);
    }

    /**
     * bun's text tuple carries {@code dependencies} and a satisfied {@code peerDependencies} surface (its ranges
     * verbatim, its optional peers flattened into {@code optionalPeers}). A manifest with any other field bun folds
     * into the tuple metadata (an optional-dependency surface, a platform gate, a bin) reshapes the entry in a way
     * not yet byte-verified, so it defers rather than guess. Fields bun keeps out of the text lock (engines,
     * license, funding, deprecated, install scripts) are intentionally ignored.
     */
    private static void requireEmittable(VersionManifest m) {
        deferIf(m, "optionalDependencies", notEmpty(m.getOptionalDependencies()));
        deferIf(m, "bin", m.getBin() != null);
        deferIf(m, "os", m.getOs() != null);
        deferIf(m, "cpu", m.getCpu() != null);
        deferIf(m, "libc", m.getLibc() != null);
        deferIf(m, "bundleDependencies", m.getBundleDependencies() != null);
    }

    /**
     * bun records optional peers not as npm's verbatim {@code peerDependenciesMeta} object but as a flat
     * {@code optionalPeers} array of the peer names flagged {@code optional: true}, ASCII-sorted. A meta entry that
     * marks a name optional without declaring it a peer is a shape bun has not been byte-verified on, so it defers.
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

    private static void deferIf(VersionManifest m, String field, boolean present) {
        if (present) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, m.getName(),
                    m.getName() + "@" + m.getVersion() + " declares " + field + " (entry shape not yet reproduced)");
        }
    }

    // --- bun hoisting layout ----------------------------------------------

    /**
     * bun places every package top-level under its bare name. When a name resolves to two versions (a fork), the
     * importer's directly-declared version wins the top slot and the other nests as {@code "<parent>/<name>"}
     * under its unique requiring top-level parent — the shape bun writes for a directly-declared fork. Top-level
     * entries come first, ASCII-sorted; nested entries follow. Anything more (a transitive-only fork, three
     * versions, an ambiguous requirer) defers.
     */
    private static List<Entry> layout(ResolutionGraph graph, ResolutionGraph.Importer root) {
        Map<String, List<ResolvedNode>> byName = new TreeMap<>();
        for (ResolvedNode node : graph.getNodes().values()) {
            byName.computeIfAbsent(node.getName(), k -> new ArrayList<>()).add(node);
        }

        Map<String, ResolvedNode> topLevel = new LinkedHashMap<>();   // name -> its hoisted node (ASCII order)
        Map<String, ResolvedNode> nested = new LinkedHashMap<>();     // name -> its nested (forked) node
        for (Map.Entry<String, List<ResolvedNode>> e : byName.entrySet()) {
            String name = e.getKey();
            List<ResolvedNode> versions = e.getValue();
            if (versions.size() == 1) {
                topLevel.put(name, versions.get(0));
            } else if (versions.size() == 2) {
                String declared = root.getResolved().get(name);
                ResolvedNode top = declared == null ? null : find(versions, declared);
                if (top == null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            name + " forks but its hoisted version is not the directly-declared one (ambiguous)");
                }
                topLevel.put(name, top);
                nested.put(name, versions.get(0) == top ? versions.get(1) : versions.get(0));
            } else {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                        name + " resolves to " + versions.size() + " versions (only a single fork is reproduced)");
            }
        }

        List<Entry> entries = new ArrayList<>();
        for (Map.Entry<String, ResolvedNode> e : topLevel.entrySet()) {
            entries.add(new Entry(e.getKey(), e.getValue()));
        }
        List<Entry> nestedEntries = new ArrayList<>();
        for (Map.Entry<String, ResolvedNode> e : nested.entrySet()) {
            String parent = uniqueParent(topLevel, e.getKey(), e.getValue().getVersion());
            nestedEntries.add(new Entry(parent + "/" + e.getKey(), e.getValue()));
        }
        nestedEntries.sort(Comparator.comparing(en -> en.key));
        entries.addAll(nestedEntries);
        return entries;
    }

    /** The single top-level package whose edge requires {@code name@version}; ambiguity or absence defers. */
    private static String uniqueParent(Map<String, ResolvedNode> topLevel, String name, String version) {
        String parent = null;
        for (ResolvedNode candidate : topLevel.values()) {
            if (version.equals(candidate.getResolvedEdges().get(name))) {
                if (parent != null) {
                    throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                            name + "@" + version + " is required by multiple parents (ambiguous nest)");
                }
                parent = candidate.getName();
            }
        }
        if (parent == null) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, name,
                    name + "@" + version + " has no requiring top-level parent (cannot place nest)");
        }
        return parent;
    }

    private static @Nullable ResolvedNode find(List<ResolvedNode> nodes, String version) {
        for (ResolvedNode node : nodes) {
            if (node.getVersion().equals(version)) {
                return node;
            }
        }
        return null;
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

    private static final class Entry {
        final String key;
        final ResolvedNode node;

        Entry(String key, ResolvedNode node) {
            this.key = key;
            this.node = node;
        }
    }
}
