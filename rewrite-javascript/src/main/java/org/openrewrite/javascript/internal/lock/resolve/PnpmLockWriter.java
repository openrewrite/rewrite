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
 * {@code packages} map carries one {@code name@version} per resolved node (its {@code resolution} integrity and
 * {@code engines}), and the {@code snapshots} map carries one {@code name@version} per node with its resolved
 * dependency edges. A directly-declared fork keeps both versions side by side with no nesting. Anything not yet
 * byte-verified — a peer/optional surface, a platform gate, a bin, a workspace — fails loud rather than guess.
 */
public final class PnpmLockWriter {

    public String write(ResolutionGraph graph) {
        ResolutionGraph.Importer root = singleRootImporter(graph);
        Map<String, String> declared = prodDeclared(root);

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
                sb.append("      ").append(name).append(":\n")
                        .append("        specifier: ").append(declared.get(name)).append('\n')
                        .append("        version: ").append(resolved).append('\n');
            }
        }

        // packages
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
            sb.append("\n  ").append(key).append(":\n")
                    .append("    resolution: {integrity: ").append(integrity).append("}\n");
            if (notEmpty(m.getEngines())) {
                requireQuotableEngines(m.getName(), m.getEngines());
                sb.append("    engines: ").append(renderEngines(m.getEngines())).append('\n');
            }
        }

        // snapshots
        sb.append("\nsnapshots:\n");
        for (String key : nodeKeys) {
            ResolvedNode node = graph.getNodes().get(key);
            sb.append("\n  ").append(key).append(':');
            Map<String, String> edges = node.getResolvedEdges();
            if (edges.isEmpty()) {
                sb.append(" {}\n");
            } else {
                sb.append("\n    dependencies:\n");
                for (String dep : sortedKeys(edges)) {
                    sb.append("      ").append(dep).append(": ").append(edges.get(dep)).append('\n');
                }
            }
        }
        return sb.toString();
    }

    /**
     * The clean-closure/fork writer reproduces only the entry fields the goldens pin exactly. A manifest carrying
     * any other lock-surfaced field (a peer/optional surface, a platform gate, a bin, an install script, a bundle)
     * reshapes the {@code packages}/{@code snapshots} entry in a way not yet byte-verified, so it defers.
     */
    private static void requireEmittable(VersionManifest m) {
        deferIf(m, "optionalDependencies", notEmpty(m.getOptionalDependencies()));
        deferIf(m, "peerDependencies", notEmpty(m.getPeerDependencies()));
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
