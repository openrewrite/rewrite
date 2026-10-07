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
package org.openrewrite.javascript.internal.lock.bun;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * What bun's {@code getOrPutResolvedPackageWithFindResult} (PackageManagerEnqueue.zig, bun 1.3.10) leaves to
 * network timing: it reuses any package of the name the edge's range accepts, and {@code runTasks} resolves edges
 * as their requirers' manifests arrive. Two edges of one name resolved from the packument in one install, whose
 * picks differ while one pick satisfies the other's range, end as one package or two depending on which arrives
 * first. The engine reports such a name instead of choosing an order.
 */
final class ManifestArrivalOrder {

    /**
     * An edge resolved from its packument in this install: its spec after overrides, catalogs and aliases, and the
     * version {@code findBestVersion} picked for it before the dedupe against existing packages.
     */
    static final class Resolved {
        final int dependencyId;
        final DependencyVersion version;
        final PackageManifest manifest;
        final PackageManifest.FindResult pick;

        Resolved(int dependencyId, DependencyVersion version, PackageManifest manifest,
                 PackageManifest.FindResult pick) {
            this.dependencyId = dependencyId;
            this.version = version;
            this.manifest = manifest;
            this.pick = pick;
        }
    }

    private ManifestArrivalOrder() {
    }

    /**
     * One line per name whose result depends on arrival order, over the edges resolved in the install's first pass
     * (peers are resolved after it, in their own queue). {@code packagesBefore} is how many packages the lockfile
     * held when the install began: an edge one of those is or satisfies is deduped to it whatever the order.
     */
    static List<String> detect(Lockfile lockfile, int packagesBefore, List<Resolved> resolved) {
        Map<String, List<Resolved>> byName = new LinkedHashMap<>();
        for (Resolved edge : resolved) {
            if (lockfile.dependencies.get(edge.dependencyId).behavior.isPeer() ||
                settled(lockfile, packagesBefore, edge)) {
                continue;
            }
            byName.computeIfAbsent(edge.manifest.name, k -> new ArrayList<>()).add(edge);
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<Resolved>> entry : byName.entrySet()) {
            List<Resolved> edges = entry.getValue();
            pairs:
            for (int i = 0; i < edges.size(); i++) {
                for (int j = i + 1; j < edges.size(); j++) {
                    Resolved a = edges.get(i);
                    Resolved b = edges.get(j);
                    if (a.pick.version.eql(b.pick.version)) {
                        continue;
                    }
                    if (accepts(a, b.pick.version) || accepts(b, a.pick.version)) {
                        out.add("manifest arrival order: " + entry.getKey() + " for " + requirer(lockfile, a) + " (" +
                                a.version.literal + ") and " + requirer(lockfile, b) + " (" + b.version.literal +
                                ") resolved in one install");
                        break pairs;
                    }
                }
            }
        }
        return out;
    }

    // getPackageID over the packages that predate the install: the pick itself, or an npm version the range accepts
    private static boolean settled(Lockfile lockfile, int packagesBefore, Resolved edge) {
        List<Integer> ids = lockfile.packageIndex.get(Wyhash11.stringHash(edge.manifest.name));
        if (ids == null) {
            return false;
        }
        Resolution resolution = Resolution.npm(edge.pick.version, edge.pick.packageVersion.tarballUrl);
        for (int id : ids) {
            if (id >= packagesBefore) {
                continue;
            }
            Resolution existing = lockfile.packages.get(id).resolution;
            if (existing.eql(resolution) || existing.tag == Resolution.Tag.NPM && accepts(edge, existing.version)) {
                return true;
            }
        }
        return false;
    }

    private static boolean accepts(Resolved edge, SemverVersion version) {
        return edge.version.tag == DependencyVersion.Tag.NPM && edge.version.npm().version.satisfies(version);
    }

    private static String requirer(Lockfile lockfile, Resolved edge) {
        for (Package pkg : lockfile.packages) {
            if (pkg.containsDependency(edge.dependencyId)) {
                return pkg.resolution.tag == Resolution.Tag.ROOT ? pkg.name : pkg.name + "@" + pkg.resolution;
            }
        }
        return lockfile.dependencies.get(edge.dependencyId).name;
    }
}
