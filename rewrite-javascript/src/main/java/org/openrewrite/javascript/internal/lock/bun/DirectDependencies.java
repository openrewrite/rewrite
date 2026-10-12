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
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

import static org.openrewrite.javascript.internal.lock.bun.Package.INVALID_PACKAGE_ID;

/**
 * Port of bun's src/install/update_transitive.rs {@code DirectDependencies} and {@code redirect}, with dedupe.rs
 * {@code effective_version} (bun 1.4.0, {@link BunRelease#redirectsDependentsOfAMovedDirectDependency()}): the root's
 * and the workspaces' dependency rows as bun.lock held them, taken before the root's diff enqueues them again, and the
 * edges that follow a row's move once the install has resolved.
 */
final class DirectDependencies {

    static final DirectDependencies NONE = new DirectDependencies();

    // (owner package id, first row, row count)
    private final List<int[]> owners = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();

    private static final class Row {
        final long nameHash;
        final Behavior behavior;
        final int resolved;

        Row(long nameHash, Behavior behavior, int resolved) {
            this.nameHash = nameHash;
            this.behavior = behavior;
            this.resolved = resolved;
        }

        boolean same(Dependency dep) {
            return nameHash == dep.nameHash && behavior.eq(dep.behavior);
        }
    }

    private DirectDependencies() {
    }

    static DirectDependencies snapshot(Lockfile lockfile) {
        DirectDependencies out = new DirectDependencies();
        for (int owner = 0; owner < lockfile.packages.size(); owner++) {
            Package pkg = lockfile.packages.get(owner);
            if (pkg.resolution.tag != Resolution.Tag.ROOT && pkg.resolution.tag != Resolution.Tag.WORKSPACE) {
                continue;
            }
            int start = out.rows.size();
            for (int i = pkg.dependenciesOff; i < pkg.dependenciesEnd(); i++) {
                Dependency dep = lockfile.dependencies.get(i);
                out.rows.add(new Row(dep.nameHash, dep.behavior, lockfile.resolutions.get(i)));
            }
            out.owners.add(new int[]{owner, start, out.rows.size() - start});
        }
        return out;
    }

    /**
     * {@code redirect_dependents}: edges still resolving to the previous package of a direct dependency that moved
     * follow it when their range allows.
     */
    void redirectDependents(Lockfile lockfile) {
        if (owners.isEmpty() || lockfile.loadedPackageCount == 0) {
            return;
        }
        redirect(lockfile, movedPairs(lockfile));
    }

    // (old, new) for each row now resolving to another npm package of the same name, a current row matched to the
    // snapshot's by index while they agree, then to the first unclaimed row of the same name and behavior
    private List<int[]> movedPairs(Lockfile lockfile) {
        int packagesLen = lockfile.packages.size();
        List<int[]> pairs = new ArrayList<>();
        for (int[] owner : owners) {
            if (owner[0] >= packagesLen) {
                continue;
            }
            List<Row> ownerRows = rows.subList(owner[1], owner[1] + owner[2]);
            Package pkg = lockfile.packages.get(owner[0]);
            BitSet claimed = null;
            for (int i = 0; i < pkg.dependenciesLen; i++) {
                Dependency dep = lockfile.dependencies.get(pkg.dependenciesOff + i);
                int now = lockfile.resolutions.get(pkg.dependenciesOff + i);
                int index;
                if (claimed == null && i < ownerRows.size() && ownerRows.get(i).same(dep)) {
                    index = i;
                } else {
                    if (claimed == null) {
                        // every row before the first miss was a same-index hit
                        claimed = new BitSet(ownerRows.size());
                        claimed.set(0, Math.min(i, ownerRows.size()));
                    }
                    int hit = -1;
                    if (i < ownerRows.size() && !claimed.get(i) && ownerRows.get(i).same(dep)) {
                        hit = i;
                    } else {
                        for (int k = 0; k < ownerRows.size(); k++) {
                            if (!claimed.get(k) && ownerRows.get(k).same(dep)) {
                                hit = k;
                                break;
                            }
                        }
                    }
                    if (hit < 0) {
                        continue;
                    }
                    claimed.set(hit);
                    index = hit;
                }
                int old = ownerRows.get(index).resolved;
                if (old == now || old >= packagesLen || now >= packagesLen ||
                    lockfile.packages.get(now).resolution.tag != Resolution.Tag.NPM ||
                    lockfile.packages.get(old).nameHash != lockfile.packages.get(now).nameHash) {
                    continue;
                }
                pairs.add(new int[]{old, now});
            }
        }
        return pairs;
    }

    // every edge still resolving to an old package whose range accepts its new npm package is re-pointed at it
    private static void redirect(Lockfile lockfile, List<int[]> pairs) {
        if (pairs.isEmpty()) {
            return;
        }
        int[] newOf = new int[lockfile.packages.size()];
        Arrays.fill(newOf, INVALID_PACKAGE_ID);
        for (int[] pair : pairs) {
            if (newOf[pair[0]] == INVALID_PACKAGE_ID) {
                newOf[pair[0]] = pair[1];
            }
        }
        List<int[]> moved = new ArrayList<>();
        for (int j = 0; j < lockfile.resolutions.size(); j++) {
            int target = lockfile.resolutions.get(j);
            if (target >= newOf.length || newOf[target] == INVALID_PACKAGE_ID) {
                continue;
            }
            int to = newOf[target];
            Dependency dep = lockfile.dependencies.get(j);
            if (dep.behavior.isBundled()) {
                continue;
            }
            DependencyVersion range = effectiveVersion(lockfile, j, dep);
            if (range == null || range.tag != DependencyVersion.Tag.NPM) {
                continue;
            }
            if (range.npm().version.satisfies(lockfile.packages.get(to).resolution.version)) {
                moved.add(new int[]{j, to});
            }
        }
        for (int[] move : moved) {
            lockfile.resolutions.set(move[0], move[1]);
        }
    }

    // dedupe.rs effective_version: what the edge resolves with, after an override (not for a workspace edge or an npm:
    // alias) and its catalog; null where the catalog has no entry
    private static @Nullable DependencyVersion effectiveVersion(Lockfile lockfile, int depId, Dependency dep) {
        DependencyVersion version = dep.version;
        if (!dep.behavior.isWorkspace() &&
            (dep.version.tag != DependencyVersion.Tag.NPM || !dep.version.npm().isAlias)) {
            DependencyVersion override = lockfile.overrides.get(lockfile, depId, dep.nameHash);
            if (override != null) {
                version = override;
            }
        }
        if (version.tag == DependencyVersion.Tag.CATALOG) {
            Dependency catalogDep = lockfile.catalogs.get(version.string(), dep.name, lockfile.release);
            return catalogDep == null ? null : catalogDep.version;
        }
        return version;
    }
}
