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
package org.openrewrite.golang.internal.modgraph;

import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds the module graph and (pruned) build list for a main module, mirroring
 * the go1.17+ pruned module graph: every loaded module's requirements become
 * build-list NODES, but a module's requirements are only RECURSED into when that
 * module is unpruned (its {@code go} directive is &lt; 1.17). The resulting build
 * list matches {@code go list -m all}. Performs no process execution — all
 * dependency go.mods come from the {@link ModSource}.
 */
public final class Resolver {

    private Resolver() {
    }

    private static final class MV {
        final String path;
        final String version;

        MV(String path, String version) {
            this.path = path;
            this.version = version;
        }
    }

    public static ResolveResult resolve(byte[] mainGoMod, ModSource src) {
        ResolveResult res = new ResolveResult();
        GoModFile mf = GoModFile.parse(new String(mainGoMod, StandardCharsets.UTF_8));

        // Main-module version replacements, keyed by "path" and "path@version".
        Map<String, MV> replace = new HashMap<>();
        for (GoModFile.Replace r : mf.replaces()) {
            if (r.newVersion == null) { // local filesystem replace — can't resolve from a source
                res.complete = false;
                continue;
            }
            MV nv = new MV(r.newPath, r.newVersion);
            replace.put(r.oldPath, nv);
            replace.put(r.oldPath + "@" + (r.oldVersion == null ? "" : r.oldVersion), nv);
        }

        String mainPath = mf.modulePath() == null ? "" : mf.modulePath();
        String mainGo = mf.goVersion() == null ? "" : mf.goVersion();

        Map<String, String> present = new HashMap<>();      // path -> MVS-selected version
        Map<String, String> goVersionAt = new HashMap<>();  // path@version -> go directive
        Set<String> goModLoaded = new HashSet<>();          // path@version we fetched the go.mod for
        Set<String> loadPath = new HashSet<>();             // paths we recurse into
        Set<String> enqueued = new HashSet<>();
        Deque<MV> loadQueue = new ArrayDeque<>();

        Node node = new Node(present, loadPath, enqueued, loadQueue);

        // Roots: the main module's requirements.
        for (GoModFile.Require r : mf.requires()) {
            MV to = applyReplace(replace, r.path, r.version);
            res.graph.add(new ResolveResult.Edge(mainPath, "", to.path, to.version, r.indirect));
            node.setNode(to);
            node.markLoad(to);
        }

        while (!loadQueue.isEmpty()) {
            MV cur = loadQueue.poll();
            if (!cur.version.equals(present.get(cur.path))) {
                continue; // superseded by a higher selected version
            }
            String key = cur.path + "@" + cur.version;
            byte[] b = src.goMod(cur.path, cur.version);
            if (b == null) {
                res.complete = false;
                continue;
            }
            GoModFile df = GoModFile.parse(new String(b, StandardCharsets.UTF_8));
            goModLoaded.add(key);
            String goV = df.goVersion() == null ? "" : df.goVersion();
            goVersionAt.put(key, goV);
            boolean unpruned = goUnpruned(goV);
            for (GoModFile.Require r : df.requires()) {
                MV to = applyReplace(replace, r.path, r.version);
                res.graph.add(new ResolveResult.Edge(cur.path, cur.version, to.path, to.version, r.indirect));
                node.setNode(to);              // every requirement of a loaded module is a node
                if (unpruned) {
                    node.markLoad(to);         // recurse only through unpruned (go<1.17) modules
                }
            }
        }

        // Assemble the build list. For a leaf node not recursed into, fetch its
        // go.mod once to recover the `go` directive (the pruning pass needs it).
        res.buildList.add(new ResolveResult.Module(mainPath, "", mainGo, true));
        for (Map.Entry<String, String> e : present.entrySet()) {
            String path = e.getKey();
            String version = e.getValue();
            String key = path + "@" + version;
            String goV = goVersionAt.get(key);
            if (goV == null && !goModLoaded.contains(key)) {
                byte[] b = src.goMod(path, version);
                if (b != null) {
                    String dv = GoModFile.parse(new String(b, StandardCharsets.UTF_8)).goVersion();
                    if (dv != null) {
                        goV = dv;
                    }
                }
            }
            res.buildList.add(new ResolveResult.Module(path, version, goV == null ? "" : goV, false));
        }
        return res;
    }

    private static MV applyReplace(Map<String, MV> replace, String path, String version) {
        MV nv = replace.get(path + "@" + version);
        if (nv != null) {
            return nv;
        }
        nv = replace.get(path);
        if (nv != null) {
            return nv;
        }
        return new MV(path, version);
    }

    /**
     * Whether a module with the given {@code go} directive is UNPRUNED
     * (go &lt; 1.17), meaning its transitive requirements are part of the graph.
     * An empty/invalid directive is treated as unpruned (pre-1.16 behavior).
     */
    static boolean goUnpruned(@Nullable String v) {
        if (v == null || v.isEmpty()) {
            return true;
        }
        String[] parts = v.split("\\.", 3);
        if (parts.length < 2) {
            return true;
        }
        try {
            int maj = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            return maj < 1 || (maj == 1 && min < 17);
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    /** Bundles the MVS bookkeeping closures (setNode / markLoad) over shared state. */
    private static final class Node {
        final Map<String, String> present;
        final Set<String> loadPath;
        final Set<String> enqueued;
        final Deque<MV> loadQueue;

        Node(Map<String, String> present, Set<String> loadPath, Set<String> enqueued, Deque<MV> loadQueue) {
            this.present = present;
            this.loadPath = loadPath;
            this.enqueued = enqueued;
            this.loadQueue = loadQueue;
        }

        void enqueueLoad(MV m) {
            String k = m.path + "@" + m.version;
            if (enqueued.add(k)) {
                loadQueue.add(m);
            }
        }

        // Record a build-list node at its highest seen version; re-enqueue a
        // load-path whose version is raised (simple iterative MVS).
        void setNode(MV m) {
            String v = present.get(m.path);
            if (v == null || GoSemver.compare(m.version, v) > 0) {
                present.put(m.path, m.version);
                if (loadPath.contains(m.path)) {
                    enqueueLoad(m);
                }
            }
        }

        void markLoad(MV m) {
            loadPath.add(m.path);
            enqueueLoad(m);
        }
    }
}
