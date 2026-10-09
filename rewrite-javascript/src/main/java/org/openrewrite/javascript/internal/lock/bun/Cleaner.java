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

    private Cleaner(Lockfile old) {
        this.old = old;
        this.lockfile = new Lockfile();
        this.mapping = new int[old.packages.size()];
        Arrays.fill(mapping, INVALID_PACKAGE_ID);
    }

    static Lockfile clean(Lockfile old) {
        Cleaner cloner = new Cleaner(old);
        Lockfile lockfile = cloner.lockfile;
        lockfile.overrides = old.overrides.cloneMap();
        lockfile.catalogs = old.catalogs.cloneMap();

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
            lockfile.dependencies.add(oldDep.cloneDependency());
            lockfile.resolutions.add(INVALID_PACKAGE_ID);
        }

        for (int i = 0; i < oldResolutions.size(); i++) {
            int oldResolution = oldResolutions.get(i);
            if (oldResolution >= maxPackageId) {
                lockfile.resolutions.set(prevLen + i, INVALID_PACKAGE_ID);
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
