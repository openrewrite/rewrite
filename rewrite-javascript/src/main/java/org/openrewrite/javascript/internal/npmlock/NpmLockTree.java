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
package org.openrewrite.javascript.internal.npmlock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Graph operations over a {@code package-lock.json} v3 {@code packages} map. The map
 * records a physical {@code node_modules} tree; an entry's dependency on {@code name}
 * resolves to the nearest {@code node_modules/name} walking up from the entry's own
 * location — nested copies shadow top-level ones.
 */
final class NpmLockTree {

    /** An outgoing requirement edge: {@code from}'s manifest asks for {@code name@range}. */
    static final class Edge {
        final String name;
        final String range;
        final boolean dev;
        final boolean optional;
        final boolean peer;

        Edge(String name, String range, boolean dev, boolean optional, boolean peer) {
            this.name = name;
            this.range = range;
            this.dev = dev;
            this.optional = optional;
            this.peer = peer;
        }
    }

    /** Mutable dep flags per entry, mirroring arborist's node flags. */
    static final class Flags {
        boolean dev = true;
        boolean optional = true;
        boolean devOptional = true;
        boolean peer = true;
    }

    private final ObjectNode packages;

    NpmLockTree(ObjectNode packages) {
        this.packages = packages;
    }

    @Nullable ObjectNode entry(String pathKey) {
        JsonNode node = packages.get(pathKey);
        return node instanceof ObjectNode ? (ObjectNode) node : null;
    }

    List<String> paths() {
        List<String> out = new ArrayList<>();
        packages.fieldNames().forEachRemaining(out::add);
        return out;
    }

    /** The nearest visible location providing {@code name} from {@code fromPath}, or null. */
    @Nullable String resolve(String fromPath, String name) {
        String scope = fromPath;
        while (true) {
            String candidate = scope.isEmpty() ? "node_modules/" + name : scope + "/node_modules/" + name;
            if (packages.has(candidate)) {
                return candidate;
            }
            if (scope.isEmpty()) {
                return null;
            }
            int idx = scope.lastIndexOf("/node_modules/");
            scope = idx < 0 ? "" : scope.substring(0, idx);
        }
    }

    /**
     * Outgoing edges of the entry at {@code pathKey}, in arborist's load order —
     * peer first, then prod, optional, and (root only) dev, with a later scope
     * replacing an earlier same-name edge, so the effective precedence is
     * dev &gt; optional &gt; prod &gt; peer. A root declaring the same package in both
     * {@code peerDependencies} and {@code devDependencies} gets a dev edge, which
     * is why npm records such entries as {@code "dev": true}.
     */
    List<Edge> edges(String pathKey) {
        ObjectNode entry = entry(pathKey);
        if (entry == null) {
            return new ArrayList<>();
        }
        Map<String, Edge> byName = new LinkedHashMap<>();
        JsonNode peerMeta = entry.get("peerDependenciesMeta");
        forEachDep(entry, "peerDependencies", (name, range) -> {
            boolean peerOptional = peerMeta != null &&
                    peerMeta.path(name).path("optional").asBoolean(false);
            byName.put(name, new Edge(name, range, false, peerOptional, true));
        });
        forEachDep(entry, "dependencies", (name, range) ->
                byName.put(name, new Edge(name, range, false, false, false)));
        forEachDep(entry, "optionalDependencies", (name, range) ->
                byName.put(name, new Edge(name, range, false, true, false)));
        if (pathKey.isEmpty()) {
            forEachDep(entry, "devDependencies", (name, range) ->
                    byName.put(name, new Edge(name, range, true, false, false)));
        }
        return new ArrayList<>(byName.values());
    }

    private static void forEachDep(ObjectNode entry, String field,
                                   java.util.function.BiConsumer<String, String> consumer) {
        JsonNode deps = entry.get(field);
        if (deps != null && deps.isObject()) {
            deps.fields().forEachRemaining(e -> consumer.accept(e.getKey(), e.getValue().asText("")));
        }
    }

    /**
     * arborist's {@code calc-dep-flags}: all flags start true (root all-false) and are
     * unset walking edges to a fixed point; whatever survives is provable ("still
     * flagged optional" means "only reachable via optional edges"). Returns flags for
     * every reachable entry; unreachable entries are absent, which makes this double
     * as the orphan sweep.
     */
    Map<String, Flags> calcDepFlags() {
        Map<String, Flags> flags = new HashMap<>();
        Flags rootFlags = new Flags();
        rootFlags.dev = rootFlags.optional = rootFlags.devOptional = rootFlags.peer = false;
        flags.put("", rootFlags);

        Deque<String> queue = new ArrayDeque<>();
        queue.push("");
        Set<String> seen = new HashSet<>();
        while (!queue.isEmpty()) {
            String path = queue.pop();
            seen.add(path);
            Flags from = flags.get(path);
            for (Edge edge : edges(path)) {
                String toPath = resolve(path, edge.name);
                if (toPath == null) {
                    continue;
                }
                Flags to = flags.computeIfAbsent(toPath, k -> new Flags());
                boolean changed = !seen.contains(toPath);
                if (to.dev && !from.dev && !edge.dev) {
                    to.dev = false;
                    changed = true;
                }
                if (to.optional && !from.optional && !edge.optional) {
                    to.optional = false;
                    changed = true;
                }
                if (to.devOptional && !from.devOptional && !from.dev && !from.optional &&
                        !edge.dev && !edge.optional) {
                    to.devOptional = false;
                    changed = true;
                }
                if (to.peer && !from.peer && !edge.peer) {
                    to.peer = false;
                    changed = true;
                }
                if (changed) {
                    queue.push(toPath);
                }
            }
        }
        for (Map.Entry<String, Flags> e : flags.entrySet()) {
            Flags f = e.getValue();
            if (f.devOptional && (f.dev || f.optional)) {
                f.devOptional = false;
            }
        }
        return flags;
    }

    /**
     * Apply computed flags to an entry's {@code dev}/{@code optional}/{@code peer}/
     * {@code devOptional} fields exactly as {@code Shrinkwrap.metaFromNode} writes them.
     */
    static void applyFlags(ObjectNode entry, Flags flags) {
        entry.remove("dev");
        entry.remove("optional");
        entry.remove("devOptional");
        entry.remove("peer");
        if (flags.peer) {
            entry.put("peer", true);
        }
        if (flags.dev) {
            entry.put("dev", true);
        }
        if (flags.optional) {
            entry.put("optional", true);
        }
        if (flags.devOptional && !flags.dev && !flags.optional) {
            entry.put("devOptional", true);
        }
    }

    static boolean sameFlags(ObjectNode entry, Flags flags) {
        return flagMatches(entry, "dev", flags.dev) &&
                flagMatches(entry, "optional", flags.optional) &&
                flagMatches(entry, "peer", flags.peer) &&
                flagMatches(entry, "devOptional", flags.devOptional && !flags.dev && !flags.optional);
    }

    private static boolean flagMatches(ObjectNode entry, String field, boolean expected) {
        return entry.path(field).asBoolean(false) == expected;
    }

    /** The package name of a lock path key: whatever follows the last {@code node_modules/}. */
    static @Nullable String nameOf(String pathKey) {
        int marker = pathKey.lastIndexOf("node_modules/");
        if (marker < 0) {
            return null;
        }
        String tail = pathKey.substring(marker + "node_modules/".length());
        return tail.isEmpty() ? null : tail;
    }
}
