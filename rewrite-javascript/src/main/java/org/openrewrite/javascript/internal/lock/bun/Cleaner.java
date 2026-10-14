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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.openrewrite.javascript.internal.lock.bun.Package.INVALID_PACKAGE_ID;

/**
 * Port of bun's src/install/lockfile.zig {@code cleanWithLogger}, {@code Cloner} and Package.zig {@code clone}
 * (bun 1.3.10) for an install without update requests.
 */
final class Cleaner {

    private final Lockfile old;
    private final Lockfile lockfile;
    private final int[] mapping;
    private final List<int[]> cloneQueue = new ArrayList<>();

    /**
     * From 1.4.0, whether an optional peer's resolution keeps its target alive ({@code keep_optional_peer_targets});
     * where not, the peers are bound in {@link #flush} to what survived ({@code optional_peers}).
     */
    private final boolean keepOptionalPeerTargets;
    private final List<int[]> optionalPeers = new ArrayList<>();

    private Cleaner(Lockfile old, boolean keepOptionalPeerTargets) {
        this.old = old;
        this.lockfile = new Lockfile(old.release);
        this.mapping = new int[old.packages.size()];
        Arrays.fill(mapping, INVALID_PACKAGE_ID);
        this.keepOptionalPeerTargets = keepOptionalPeerTargets || !old.release.dropsWhatOnlyOptionalPeersHold();
    }

    /**
     * The clean of an install that changes no resolution.
     */
    static Lockfile clean(Lockfile old) {
        return clean(old, true);
    }

    /**
     * @param keepOptionalPeerTargets whether the install is frozen or changes no resolution
     *                                ({@link PackageDiff.Summary#changesResolutions}), so that from 1.4.0 a package
     *                                only optional peers resolve to survives
     */
    static Lockfile clean(Lockfile old, boolean keepOptionalPeerTargets) {
        Cleaner cloner = new Cleaner(old, keepOptionalPeerTargets);
        Lockfile lockfile = cloner.lockfile;
        lockfile.overrides = old.overrides.cloneMap(old.release);
        lockfile.catalogs = old.catalogs.cloneMap(old.release);

        Package root = old.rootPackage();
        if (root == null) {
            throw new IllegalStateException("NoPackage");
        }
        cloner.clonePackage(root);

        if (!old.workspacePaths.isEmpty() || !old.workspaceVersions.isEmpty()) {
            List<Map.Entry<Long, String>> paths = new ArrayList<>(old.workspacePaths.entrySet());
            paths.sort((a, b) -> Strings.order(a.getValue(), b.getValue()));
            LinkedHashMap<Long, String> sortedPaths = new LinkedHashMap<>();
            for (Map.Entry<Long, String> e : paths) {
                sortedPaths.put(e.getKey(), e.getValue());
            }
            old.workspacePaths = sortedPaths;
            lockfile.workspacePaths = new LinkedHashMap<>(sortedPaths);
            lockfile.workspaceVersions = new LinkedHashMap<>(old.workspaceVersions);
        }

        cloner.flush();

        lockfile.trustedDependencies = old.trustedDependencies;
        if (old.release.keepsTheLockfileVersionItRead()) {
            lockfile.textLockfileVersion = old.textLockfileVersion;
        }
        lockfile.patchedDependencies = new LinkedHashMap<>(old.patchedDependencies);
        return lockfile;
    }

    private void flush() {
        int maxPackageId = old.packages.size();
        while (!cloneQueue.isEmpty()) {
            int[] toClone = cloneQueue.remove(cloneQueue.size() - 1);
            int oldResolution = toClone[0];
            int resolveId = toClone[1];
            int mapped = mapping[oldResolution];
            if (mapped < maxPackageId) {
                lockfile.resolutions.set(resolveId, mapped);
                continue;
            }
            lockfile.resolutions.set(resolveId, clonePackage(old.packages.get(oldResolution)));
        }
        // the reader binds these before it hoists; the hoist below has to see the same bindings
        for (int[] pending : optionalPeers) {
            int mapped = mapping[pending[0]];
            if (mapped < maxPackageId) {
                lockfile.resolutions.set(pending[1], mapped);
            }
        }
        if (!lockfile.packages.isEmpty()) {
            lockfile.resolve();
        }
    }

    private int clonePackage(Package pkg) {
        List<Dependency> oldDependencies = old.dependenciesOf(pkg);
        List<Integer> oldResolutions = old.resolutionsOf(pkg);
        int prevLen = lockfile.dependencies.size();
        int end = prevLen + oldDependencies.size();
        int maxPackageId = old.packages.size();

        int id = lockfile.packages.size();
        Package newPackage = new Package();
        newPackage.name = pkg.name;
        newPackage.nameHash = pkg.nameHash;
        newPackage.bin = pkg.bin;
        newPackage.meta = pkg.meta.copy(id);
        newPackage.resolution = pkg.resolution;
        newPackage.scripts = pkg.scripts.copy();
        newPackage.dependenciesOff = prevLen;
        newPackage.dependenciesLen = end - prevLen;
        lockfile.appendPackageWithID(newPackage, id);

        mapping[pkg.meta.id] = newPackage.meta.id;

        for (Dependency oldDep : oldDependencies) {
            lockfile.dependencies.add(oldDep.cloneDependency(old.release));
            lockfile.resolutions.add(INVALID_PACKAGE_ID);
        }

        for (int i = 0; i < oldResolutions.size(); i++) {
            int oldResolution = oldResolutions.get(i);
            if (oldResolution >= maxPackageId) {
                lockfile.resolutions.set(prevLen + i, INVALID_PACKAGE_ID);
                continue;
            }
            // a peer slot must not keep its target alive
            if (oldDependencies.get(i).behavior.isOptionalPeer() && !keepOptionalPeerTargets) {
                optionalPeers.add(new int[]{oldResolution, newPackage.dependenciesOff + i});
                continue;
            }
            int mapped = mapping[oldResolution];
            if (mapped < maxPackageId) {
                lockfile.resolutions.set(prevLen + i, mapped);
            } else {
                cloneQueue.add(new int[]{oldResolution, newPackage.dependenciesOff + i});
            }
        }
        return newPackage.meta.id;
    }
}
