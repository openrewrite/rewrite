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
 * Computes the exact go.mod require set {@code go mod tidy} would write, by
 * walking the PACKAGE import graph (not just the module graph). {@link
 * #neededModules} gives the modules that provide a package in {@code all};
 * {@link #tidyRequireSet} additionally adds the go1.17+ pruning-completeness
 * roots — test-transitive modules the pruned graph would under-select — mirroring
 * {@code cmd/go/internal/modload.tidyPrunedRoots}. No {@code go} execution.
 */
public final class Tidy {

    private Tidy() {
    }

    public static RequireSet neededModules(List<String> mainImports, String mainModulePath,
                                           ResolveResult res, ModSource src, boolean separateIndirect) {
        RequireSet rs = new RequireSet();
        List<ResolveResult.Module> mods = nonMain(res);

        Map<String, String> needed = new HashMap<>();
        Set<String> direct = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();

        for (String imp : mainImports) {
            if (isStdlibImport(imp) || isLocal(imp, mainModulePath)) {
                continue;
            }
            ModVer mv = moduleOf(mods, imp);
            if (mv == null) {
                rs.complete = false;
                continue;
            }
            direct.add(mv.path);
            needed.put(mv.path, mv.version);
            queue.add(imp);
        }

        while (!queue.isEmpty()) {
            String imp = queue.poll();
            if (!visited.add(imp)) {
                continue;
            }
            if (isStdlibImport(imp) || isLocal(imp, mainModulePath)) {
                continue;
            }
            ModVer mv = moduleOf(mods, imp);
            if (mv == null) {
                // Maps to no build-list module: not part of the real build (e.g. a
                // //go:build ignore generator or an unselected platform). Diagnostic
                // only — not a missing dependency.
                rs.unresolved.add(imp);
                continue;
            }
            needed.put(mv.path, mv.version);

            List<String> deps = packageImports(src, mv.path, mv.version, imp);
            if (deps == null) {
                rs.complete = false;
                rs.missingDirs.add(imp);
                continue;
            }
            for (String dep : deps) {
                if (isStdlibImport(dep) || isLocal(dep, mainModulePath) || visited.contains(dep)) {
                    continue;
                }
                queue.add(dep);
            }
        }

        // For go < 1.17, omit indirect modules implied by another needed module's
        // go.mod (already pinned by the graph). go >= 1.17 records the full set.
        Set<String> implied = new HashSet<>();
        if (!separateIndirect) {
            for (ResolveResult.Edge e : res.graph()) {
                if (!e.fromPath.isEmpty() && !e.fromPath.equals(mainModulePath) && needed.containsKey(e.fromPath)) {
                    implied.add(e.toPath);
                }
            }
        }

        for (Map.Entry<String, String> en : needed.entrySet()) {
            String mod = en.getKey();
            if (direct.contains(mod)) {
                rs.direct.put(mod, en.getValue());
            } else if (!implied.contains(mod)) {
                rs.indirect.put(mod, en.getValue());
            }
        }
        return rs;
    }

    public static RequireSet tidyRequireSet(List<String> mainImports, String mainModulePath, String mainGoMod,
                                            ResolveResult res, ModSource src, boolean separateIndirect) {
        RequireSet base = neededModules(mainImports, mainModulePath, res, src, separateIndirect);
        if (!separateIndirect || !base.complete) {
            return base;
        }

        Map<String, String> loaded = new HashMap<>();
        List<ResolveResult.Module> mods = new ArrayList<>();
        for (ResolveResult.Module m : res.buildList()) {
            if (!m.main) {
                loaded.put(m.path, m.version);
                mods.add(m);
            }
        }

        // Current root set: everything NeededModules already requires.
        Map<String, String> roots = new HashMap<>();
        roots.putAll(base.direct);
        roots.putAll(base.indirect);

        ReqIndex idx = new ReqIndex(res, mainGoMod, src);

        // Walk the import graph FRONTIER BY FRONTIER (increasing import-stack
        // depth). At each frontier recompute the pruned selection under the roots
        // so far, then promote any frontier module the pruned graph under-selects.
        // This ordering pins a shallow module's deeper requirements before they are
        // examined, so they are not wrongly promoted. Test imports are deferred one
        // frontier deeper (go's separate `<pkg>.test` node).
        Set<String> queued = new HashSet<>();
        List<QItem> queue = new ArrayList<>();
        for (String imp : mainImports) {
            enq(imp, false, mainModulePath, queued, queue);
        }

        while (!queue.isEmpty()) {
            Map<String, String> sel = prunedSelectInMemory(roots, idx);
            List<QItem> frontier = queue;
            queue = new ArrayList<>();
            for (QItem it : frontier) {
                ModVer mv = moduleOf(mods, it.path);
                if (mv == null) {
                    continue;
                }
                PkgImports pi = packageImportsWithTests(src, mv.path, mv.version, it.path);
                if (pi != null) {
                    if (it.isTest) {
                        for (String d : pi.testImports) {
                            enq(d, false, mainModulePath, queued, queue);
                        }
                    } else {
                        for (String d : pi.imports) {
                            enq(d, false, mainModulePath, queued, queue);
                        }
                        enq(it.path, true, mainModulePath, queued, queue); // the test node, one frontier deeper
                    }
                }
                if (!roots.containsKey(mv.path)) {
                    String want = loaded.get(mv.path);
                    String have = sel.get(mv.path);
                    if (want != null && !want.isEmpty() && (have == null || GoSemver.compare(have, want) < 0)) {
                        roots.put(mv.path, want);
                    }
                }
            }
        }

        // Reclassify: direct stays direct; every other root is indirect.
        RequireSet out = new RequireSet();
        out.unresolved.addAll(base.unresolved);
        out.missingDirs.addAll(base.missingDirs);
        for (Map.Entry<String, String> e : roots.entrySet()) {
            if (base.direct.containsKey(e.getKey())) {
                out.direct.put(e.getKey(), e.getValue());
            } else {
                out.indirect.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    // ---- helpers -----------------------------------------------------------

    private static List<ResolveResult.Module> nonMain(ResolveResult res) {
        List<ResolveResult.Module> mods = new ArrayList<>();
        for (ResolveResult.Module m : res.buildList()) {
            if (!m.main) {
                mods.add(m);
            }
        }
        return mods;
    }

    /** The longest build-list module path that is a prefix of importPath, or null. */
    private static @Nullable ModVer moduleOf(List<ResolveResult.Module> mods, String importPath) {
        String best = "";
        String bestVer = "";
        for (ResolveResult.Module m : mods) {
            if (importPath.equals(m.path) || importPath.startsWith(m.path + "/")) {
                if (m.path.length() > best.length()) {
                    best = m.path;
                    bestVer = m.version;
                }
            }
        }
        return best.isEmpty() ? null : new ModVer(best, bestVer);
    }

    private static boolean isLocal(String importPath, String mainModulePath) {
        return importPath.equals(mainModulePath) || importPath.startsWith(mainModulePath + "/");
    }

    /** A standard-library import: no dot in its first path segment. */
    static boolean isStdlibImport(String importPath) {
        int i = importPath.indexOf('/');
        String first = i >= 0 ? importPath.substring(0, i) : importPath;
        return !first.contains(".");
    }

    /** Non-test imports of a package, or null if its sources could not be read. */
    private static @Nullable List<String> packageImports(ModSource src, String mod, String version, String importPath) {
        Map<String, byte[]> files = src.packageGoFiles(mod, version, importPath);
        if (files == null) {
            return null;
        }
        Set<String> set = new HashSet<>();
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            if (e.getKey().endsWith("_test.go")) {
                continue;
            }
            for (String p : GoImports.parse(new String(e.getValue(), StandardCharsets.UTF_8))) {
                if (!p.isEmpty()) {
                    set.add(p);
                }
            }
        }
        return new ArrayList<>(set);
    }

    /** A package's ordinary and test imports, separately; null if unreadable. */
    private static @Nullable PkgImports packageImportsWithTests(ModSource src, String mod, String version, String importPath) {
        Map<String, byte[]> files = src.packageGoFiles(mod, version, importPath);
        if (files == null) {
            return null;
        }
        Set<String> imp = new HashSet<>();
        Set<String> test = new HashSet<>();
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            Set<String> target = e.getKey().endsWith("_test.go") ? test : imp;
            for (String p : GoImports.parse(new String(e.getValue(), StandardCharsets.UTF_8))) {
                if (!p.isEmpty()) {
                    target.add(p);
                }
            }
        }
        return new PkgImports(new ArrayList<>(imp), new ArrayList<>(test));
    }

    private static void enq(String path, boolean isTest, String mainModulePath, Set<String> queued, List<QItem> queue) {
        if (isStdlibImport(path) || isLocal(path, mainModulePath)) {
            return;
        }
        String k = isTest ? path + " t" : path;
        if (queued.add(k)) {
            queue.add(new QItem(path, isTest));
        }
    }

    private static final class QItem {
        final String path;
        final boolean isTest;

        QItem(String path, boolean isTest) {
            this.path = path;
            this.isTest = isTest;
        }
    }

    private static final class PkgImports {
        final List<String> imports;
        final List<String> testImports;

        PkgImports(List<String> imports, List<String> testImports) {
            this.imports = imports;
            this.testImports = testImports;
        }
    }

    /**
     * MVS-selected version of every module under the go1.17+ pruned graph rooted
     * at {@code roots}, reading requirements from {@code idx}. Mirrors {@link
     * Resolver} exactly: loaded modules' requires become nodes, but recursion
     * continues only through unpruned (go &lt; 1.17) modules.
     */
    static Map<String, String> prunedSelectInMemory(Map<String, String> roots, ReqIndex idx) {
        Map<String, String> present = new HashMap<>();
        Set<String> loadPath = new HashSet<>();
        Set<String> enqueued = new HashSet<>();
        Deque<ModVer> queue = new ArrayDeque<>();

        for (Map.Entry<String, String> e : roots.entrySet()) {
            ModVer m = new ModVer(e.getKey(), e.getValue());
            setNode(present, loadPath, enqueued, queue, m);
            loadPath.add(m.path);
            enqueue(enqueued, queue, m);
        }

        while (!queue.isEmpty()) {
            ModVer cur = queue.poll();
            if (!cur.version.equals(present.get(cur.path))) {
                continue;
            }
            ReqIndex.Reqs r = idx.requires(cur.path, cur.version);
            boolean unpruned = Resolver.goUnpruned(r.goVersion);
            for (ModVer req : r.reqs) {
                setNode(present, loadPath, enqueued, queue, req);
                if (unpruned) {
                    loadPath.add(req.path);
                    enqueue(enqueued, queue, req);
                }
            }
        }
        return present;
    }

    private static void enqueue(Set<String> enqueued, Deque<ModVer> queue, ModVer m) {
        if (enqueued.add(m.path + "@" + m.version)) {
            queue.add(m);
        }
    }

    private static void setNode(Map<String, String> present, Set<String> loadPath, Set<String> enqueued,
                                Deque<ModVer> queue, ModVer m) {
        String v = present.get(m.path);
        if (v == null || GoSemver.compare(m.version, v) > 0) {
            present.put(m.path, m.version);
            if (loadPath.contains(m.path)) {
                enqueue(enqueued, queue, m);
            }
        }
    }
}
