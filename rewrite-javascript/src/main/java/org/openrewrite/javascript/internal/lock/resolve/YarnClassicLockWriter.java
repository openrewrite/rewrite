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

import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.EngineFailure;
import org.openrewrite.javascript.internal.lock.YarnLock;
import org.openrewrite.javascript.internal.registry.VersionManifest;

import java.util.*;

/**
 * Serializes a {@link ResolutionGraph} to classic {@code yarn.lock} (v1) text, byte-for-byte identical to what a
 * real {@code yarn install} would write. yarn v1 is a flat resolution map (no hoisting): one block per resolved
 * {@code name@version}, keyed by every {@code name@range} selector that resolved to it (merged, {@code sortAlpha}
 * order); a fork is simply two flat blocks. Each block carries {@code version}, {@code resolved}
 * ({@code registry.yarnpkg.com/…#<sha1>}), {@code integrity}, and a {@code dependencies:} section of the declared
 * ranges. Blocks are ordered by {@code sortAlpha} of their first selector. Serialization goes through
 * {@link YarnLock} so the quoting/ordering match yarn's own {@code _stringify}. A satisfied {@code peerDependencies}
 * surface is reproduced by omission — yarn v1 records no peers in the lock, so the declarer's block is byte-identical
 * to one with no peers. Anything not byte-verified — a missing locator, an empty selector set, a workspace importer,
 * an {@code optionalDependencies} section — fails loud rather than emit a wrong lock.
 */
public final class YarnClassicLockWriter {

    private final boolean mirrorToYarnpkg;

    public YarnClassicLockWriter(boolean mirrorToYarnpkg) {
        this.mirrorToYarnpkg = mirrorToYarnpkg;
    }

    public String write(ResolutionGraph graph) {
        singleRootImporter(graph);
        Map<String, Set<String>> selectorsByNode = collectSelectors(graph);

        // Each block carries its unquoted joined descriptor key — yarn's own _stringify sorts blocks by that.
        List<String[]> keyed = new ArrayList<>();
        for (Map.Entry<String, ResolvedNode> e : graph.getNodes().entrySet()) {
            Set<String> ranges = selectorsByNode.get(e.getKey());
            if (ranges == null || ranges.isEmpty()) {
                throw new EngineFailure(Reason.RESOLUTION_REQUIRED, e.getValue().getName(),
                        e.getValue().getName() + "@" + e.getValue().getVersion() + " resolved but no selector requires it");
            }
            List<String> selectors = sortedSelectors(e.getValue().getName(), ranges);
            keyed.add(new String[]{String.join(", ", selectors), block(e.getValue(), selectors)});
        }
        keyed.sort((a, b) -> YarnLock.sortAlpha(a[0], b[0]));

        List<String> blocks = new ArrayList<>();
        for (String[] k : keyed) {
            blocks.add(k[1]);
        }
        return YarnLock.HEADER + String.join("\n\n", blocks) + "\n";
    }

    private static List<String> sortedSelectors(String name, Set<String> ranges) {
        List<String> selectors = new ArrayList<>();
        for (String range : ranges) {
            selectors.add(name + "@" + range);
        }
        selectors.sort(YarnLock::sortAlpha);
        return selectors;
    }

    /**
     * Map each resolved node to the set of ranges that resolved to it: an importer's declared range whose
     * resolution is this node, plus every requirer's dependency range whose edge resolves to this node.
     */
    private static Map<String, Set<String>> collectSelectors(ResolutionGraph graph) {
        Map<String, Set<String>> byNode = new LinkedHashMap<>();
        for (ResolutionGraph.Importer imp : graph.getImporters()) {
            for (Map<String, String> scope : imp.getDeclared().values()) {
                for (Map.Entry<String, String> dep : scope.entrySet()) {
                    String version = imp.getResolved().get(dep.getKey());
                    if (version != null) {
                        add(byNode, dep.getKey(), version, dep.getValue());
                    }
                }
            }
        }
        for (ResolvedNode node : graph.getNodes().values()) {
            Map<String, String> declared = node.getManifest().getDependencies();
            if (declared == null) {
                continue;
            }
            for (Map.Entry<String, String> edge : node.getResolvedEdges().entrySet()) {
                String range = declared.get(edge.getKey());
                if (range != null) {
                    add(byNode, edge.getKey(), edge.getValue(), range);
                }
            }
        }
        return byNode;
    }

    private static void add(Map<String, Set<String>> byNode, String name, String version, String range) {
        byNode.computeIfAbsent(ResolutionGraph.key(name, version), k -> new LinkedHashSet<>()).add(range);
    }

    private String block(ResolvedNode node, List<String> selectors) {
        VersionManifest m = node.getManifest();
        requireEmittable(m);

        StringBuilder header = new StringBuilder();
        for (int i = 0; i < selectors.size(); i++) {
            if (i > 0) {
                header.append(", ");
            }
            header.append(YarnLock.maybeWrap(selectors.get(i)));
        }

        StringBuilder sb = new StringBuilder();
        sb.append(header).append(":\n");
        sb.append("  version ").append(YarnLock.maybeWrap(m.getVersion())).append('\n');
        sb.append("  resolved ").append(YarnLock.maybeWrap(resolved(m))).append('\n');
        sb.append("  integrity ").append(YarnLock.maybeWrap(integrity(m)));
        appendDeps(sb, m.getDependencies());
        return sb.toString();
    }

    private String resolved(VersionManifest m) {
        VersionManifest.Dist dist = m.getDist();
        String tarball = dist == null ? null : dist.getTarball();
        String shasum = dist == null ? null : dist.getShasum();
        if (tarball == null || shasum == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, m.getName(),
                    m.getName() + "@" + m.getVersion() + " has no registry locator (tarball/shasum)");
        }
        return YarnLock.mirrorHost(tarball, mirrorToYarnpkg) + "#" + shasum;
    }

    private static String integrity(VersionManifest m) {
        VersionManifest.Dist dist = m.getDist();
        String integrity = dist == null ? null : dist.getIntegrity();
        if (integrity == null) {
            throw new EngineFailure(Reason.UNSUPPORTED_ENTRY_TYPE, m.getName(),
                    m.getName() + "@" + m.getVersion() + " has no integrity");
        }
        return integrity;
    }

    /** yarn's {@code dependencies:} section: the declared ranges, {@code sortAlpha}-ordered by name. */
    private static void appendDeps(StringBuilder sb, Map<String, String> deps) {
        if (deps == null || deps.isEmpty()) {
            return;
        }
        List<String> names = new ArrayList<>(deps.keySet());
        names.sort(YarnLock::sortAlpha);
        sb.append("\n  dependencies:");
        for (String name : names) {
            sb.append("\n    ").append(YarnLock.maybeWrap(name)).append(' ').append(YarnLock.maybeWrap(deps.get(name)));
        }
    }

    /**
     * The clean/merged/fork writer reproduces only the block fields the goldens pin exactly. yarn v1 never records
     * the peer surface (a real {@code yarn install} writes no {@code peerDependencies:} into any block — it does not
     * install peers), so a satisfied-peer manifest emits a block byte-identical to one with no peers and needs no
     * guard. A resolved package whose own manifest declares {@code optionalDependencies}, by contrast, gets an
     * {@code optionalDependencies:} block whose shape is not yet byte-verified, so it still defers. (An importer's
     * optionalDependencies are ordinary unmarked entries — yarn v1 marks no scope — and resolve fine.)
     */
    private static void requireEmittable(VersionManifest m) {
        if (notEmpty(m.getOptionalDependencies())) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, m.getName(),
                    m.getName() + "@" + m.getVersion() + " declares optionalDependencies (block shape not yet reproduced)");
        }
    }

    private static boolean notEmpty(Map<String, String> map) {
        return map != null && !map.isEmpty();
    }

    private static void singleRootImporter(ResolutionGraph graph) {
        if (graph.getImporters().size() != 1) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "only a single-importer project is reproduced (found " + graph.getImporters().size() + ")");
        }
        if (!graph.getImporters().get(0).getDir().isEmpty()) {
            throw new EngineFailure(Reason.RESOLUTION_REQUIRED, null,
                    "only a root importer is reproduced (found importer dir '" + graph.getImporters().get(0).getDir() + "')");
        }
    }
}
