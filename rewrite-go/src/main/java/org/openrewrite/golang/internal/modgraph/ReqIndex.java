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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Memoizes each module version's direct requirements (post-replace) and {@code
 * go} directive. Seeded for free from an already-resolved graph — whose edges
 * cover every LOADED module — and lazily fetches the go.mod of any module that
 * pruning left unloaded (only the handful of pruned modules promoted to roots).
 * This lets {@link Tidy#prunedSelectInMemory} run a pruned MVS with no
 * per-iteration re-resolution.
 */
final class ReqIndex {

    private final Map<String, List<ModVer>> edges = new HashMap<>(); // path@version -> requires (post-replace)
    private final Map<String, String> gover = new HashMap<>();       // path@version -> go directive
    private final Set<String> known = new HashSet<>();               // keys whose edges are fully populated
    private final Map<String, ModVer> replace = new HashMap<>();     // main module's version replacements
    private final ModSource src;

    /** A module version's direct requirements and its {@code go} directive. */
    static final class Reqs {
        final List<ModVer> reqs;
        final @Nullable String goVersion;

        Reqs(List<ModVer> reqs, @Nullable String goVersion) {
            this.reqs = reqs;
            this.goVersion = goVersion;
        }
    }

    ReqIndex(ResolveResult res, String mainGoMod, ModSource src) {
        this.src = src;

        // Main module's version replacements (local-path replaces are skipped; they
        // already mark the resolution incomplete upstream).
        GoModFile mf = GoModFile.parse(mainGoMod);
        for (GoModFile.Replace r : mf.replaces()) {
            if (r.newVersion == null) {
                continue;
            }
            ModVer nv = new ModVer(r.newPath, r.newVersion);
            replace.put(r.oldPath, nv);
            replace.put(r.oldPath + "@" + (r.oldVersion == null ? "" : r.oldVersion), nv);
        }
        // Seed edges from the resolved graph. Every LOADED module contributes all of
        // its require edges here (already post-replace), so its key is fully known.
        for (ResolveResult.Edge e : res.graph()) {
            String key = e.fromPath + "@" + e.fromVersion;
            edges.computeIfAbsent(key, k -> new ArrayList<>()).add(new ModVer(e.toPath, e.toVersion));
            known.add(key);
        }
        for (ResolveResult.Module m : res.buildList()) {
            gover.put(m.path + "@" + m.version, m.goVersion);
        }
    }

    /** Direct requirements + go directive, fetching/memoizing the go.mod on a miss. */
    Reqs requires(String path, String version) {
        String key = path + "@" + version;
        if (known.contains(key)) {
            return new Reqs(edges.getOrDefault(key, Collections.emptyList()), gover.get(key));
        }
        known.add(key); // memoize even on miss, so a failed fetch isn't retried
        byte[] b = src.goMod(path, version);
        if (b == null) {
            return new Reqs(Collections.emptyList(), gover.get(key));
        }
        GoModFile df = GoModFile.parse(new String(b, StandardCharsets.UTF_8));
        if (df.goVersion() != null) {
            gover.put(key, df.goVersion());
        }
        List<ModVer> reqs = new ArrayList<>();
        for (GoModFile.Require r : df.requires()) {
            reqs.add(applyReplace(new ModVer(r.path, r.version)));
        }
        edges.put(key, reqs);
        return new Reqs(reqs, gover.get(key));
    }

    private ModVer applyReplace(ModVer m) {
        ModVer nv = replace.get(m.path + "@" + m.version);
        if (nv != null) {
            return nv;
        }
        nv = replace.get(m.path);
        if (nv != null) {
            return nv;
        }
        return m;
    }
}
