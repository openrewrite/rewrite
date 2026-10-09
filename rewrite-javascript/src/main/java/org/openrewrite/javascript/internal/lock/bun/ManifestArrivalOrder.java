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

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The dedupe in bun's {@code getOrPutResolvedPackageWithFindResult} (PackageManagerEnqueue.zig, bun 1.3.10) that
 * network timing decides: which of a name's edges, resolved from the packument in one install, ran first.
 */
final class ManifestArrivalOrder {

    /**
     * An edge resolved from its packument: its spec and the version {@code findBestVersion} picked before the dedupe.
     */
    static final class Resolved {
        final int dependencyId;
        final DependencyVersion version;
        final PackageManifest manifest;
        final PackageManifest.FindResult pick;
        final Resolution resolution;

        Resolved(int dependencyId, DependencyVersion version, PackageManifest manifest,
                 PackageManifest.FindResult pick) {
            this.dependencyId = dependencyId;
            this.version = version;
            this.manifest = manifest;
            this.pick = pick;
            this.resolution = Resolution.npm(pick.version, pick.packageVersion.tarballUrl);
        }
    }

    private ManifestArrivalOrder() {
    }

    /**
     * One line per name whose result depends on the order its edges ran, over the edges resolved in the install's
     * first pass; {@code packagesBefore} is the lockfile's package count when the install began.
     */
    static List<String> detect(Lockfile lockfile, int packagesBefore, List<Resolved> resolved) {
        Map<String, List<Resolved>> byName = new LinkedHashMap<>();
        for (Resolved edge : resolved) {
            if (!lockfile.dependencies.get(edge.dependencyId).behavior.isPeer()) {
                byName.computeIfAbsent(edge.manifest.name, k -> new ArrayList<>()).add(edge);
            }
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<Resolved>> entry : byName.entrySet()) {
            List<Resolved> edges = entry.getValue();
            pairs:
            for (int i = 0; i < edges.size(); i++) {
                for (int j = 0; j < edges.size(); j++) {
                    if (i != j && dependsOnOrder(lockfile, packagesBefore, edges.get(i), edges.get(j))) {
                        Resolved a = edges.get(Math.min(i, j));
                        Resolved b = edges.get(Math.max(i, j));
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

    /**
     * One line per name whose required peers nothing satisfied when the peers began and whose picks differ: the order
     * the requirers' manifests arrived in decides which pick the later peers take.
     */
    static List<String> detectPeers(Lockfile lockfile, List<Resolved> peers) {
        Map<String, List<Resolved>> byName = new LinkedHashMap<>();
        for (Resolved edge : peers) {
            byName.computeIfAbsent(edge.manifest.name, k -> new ArrayList<>()).add(edge);
        }
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<Resolved>> entry : byName.entrySet()) {
            List<Resolved> edges = entry.getValue();
            for (int i = 1; i < edges.size(); i++) {
                if (!edges.get(i).resolution.eql(edges.get(0).resolution)) {
                    Resolved a = edges.get(0);
                    Resolved b = edges.get(i);
                    out.add("manifest arrival order: peer " + entry.getKey() + " for " + requirer(lockfile, a) + " (" +
                            a.version.literal + ") and " + requirer(lockfile, b) + " (" + b.version.literal +
                            ") installed in one install");
                    break;
                }
            }
        }
        return out;
    }

    // run first, `edge` takes the first pre-existing package it accepts (getPackageID walks highest first) or appends
    // its pick; run after `other` appended its pick, it takes that pick when it accepts it and it walks earlier
    private static boolean dependsOnOrder(Lockfile lockfile, int packagesBefore, Resolved edge, Resolved other) {
        if (existing(lockfile, packagesBefore, other) != null || !accepts(edge, other.resolution)) {
            return false;
        }
        Resolution first = existing(lockfile, packagesBefore, edge);
        return first == null ? !other.resolution.eql(edge.resolution) : other.resolution.order(first) > 0;
    }

    @Nullable
    private static Resolution existing(Lockfile lockfile, int packagesBefore, Resolved edge) {
        List<Integer> ids = lockfile.packageIndex.get(Wyhash11.stringHash(edge.manifest.name));
        if (ids != null) {
            for (int id : ids) {
                if (id < packagesBefore && accepts(edge, lockfile.packages.get(id).resolution)) {
                    return lockfile.packages.get(id).resolution;
                }
            }
        }
        return null;
    }

    private static boolean accepts(Resolved edge, Resolution candidate) {
        return candidate.eql(edge.resolution) ||
               candidate.tag == Resolution.Tag.NPM && edge.version.tag == DependencyVersion.Tag.NPM &&
               edge.version.npm().version.satisfies(candidate.version);
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
