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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.openrewrite.javascript.internal.lock.bun.Package.INVALID_PACKAGE_ID;

/**
 * Port of bun's src/install/lockfile.zig (bun 1.3.10): the packages, the dependency buffer and the resolution
 * buffer beside it (dependency id to package id), the package index (name hash to the ids of that name, highest
 * resolution first), workspaces, trusted and patched dependencies, overrides, catalogs, and the hoisted tree the
 * last {@link #hoist} built. Package 0 is the root.
 */
final class Lockfile {

    static final int TEXT_LOCKFILE_VERSION_CURRENT = 1;

    int textLockfileVersion = TEXT_LOCKFILE_VERSION_CURRENT;

    final List<Package> packages = new ArrayList<>();
    final List<Dependency> dependencies = new ArrayList<>();
    final List<Integer> resolutions = new ArrayList<>();
    List<Tree> trees = new ArrayList<>();
    List<Integer> hoistedDependencies = new ArrayList<>();

    /**
     * Name hash to package ids, highest resolution first; not for iterating.
     */
    final Map<Long, List<Integer>> packageIndex = new HashMap<>();

    LinkedHashMap<Long, String> workspacePaths = new LinkedHashMap<>();
    LinkedHashMap<Long, SemverVersion> workspaceVersions = new LinkedHashMap<>();

    /**
     * Truncated (u32) name hashes; null when neither the lock nor package.json lists any.
     */
    @Nullable LinkedHashSet<Integer> trustedDependencies;
    LinkedHashMap<Long, String> patchedDependencies = new LinkedHashMap<>();
    OverrideMap overrides = new OverrideMap();
    CatalogMap catalogs = new CatalogMap();

    @Nullable Integer savedConfigVersion;

    /**
     * Sorts the dependencies of a node_modules folder: behavior, then name.
     */
    int depSort(int l, int r) {
        return Dependency.compare(dependencies.get(l), dependencies.get(r));
    }

    @Nullable
    Package rootPackage() {
        return packages.isEmpty() ? null : packages.get(0);
    }

    boolean isEmpty() {
        return packages.isEmpty() || packages.size() == 1 && packages.get(0).dependenciesLen == 0;
    }

    List<Dependency> dependenciesOf(Package pkg) {
        return dependencies.subList(pkg.dependenciesOff, pkg.dependenciesEnd());
    }

    List<Integer> resolutionsOf(Package pkg) {
        return resolutions.subList(pkg.dependenciesOff, pkg.dependenciesEnd());
    }

    @Nullable
    DependencyVersion resolveCatalogDependency(Dependency dep) {
        if (dep.version.tag != DependencyVersion.Tag.CATALOG) {
            return dep.version;
        }
        Dependency catalogDep = catalogs.get(dep.version.string(), dep.name);
        return catalogDep == null ? null : catalogDep.version;
    }

    boolean isWorkspaceRootDependency(int id) {
        return packages.get(0).containsDependency(id);
    }

    /**
     * An existing package that is the resolution, or one whose npm version the range satisfies, highest first.
     */
    @Nullable
    Integer getPackageID(long nameHash, @Nullable DependencyVersion version, Resolution resolution) {
        List<Integer> entry = packageIndex.get(nameHash);
        if (entry == null) {
            return null;
        }
        SemverQuery.Group npmVersion = version != null && version.tag == DependencyVersion.Tag.NPM ? version.npm().version : null;
        for (int id : entry) {
            Resolution existing = packages.get(id).resolution;
            if (existing.eql(resolution)) {
                return id;
            }
            if (existing.tag == Resolution.Tag.NPM && npmVersion != null && npmVersion.satisfies(existing.version)) {
                return id;
            }
        }
        return null;
    }

    int appendPackageDedupe(Package pkg) {
        List<Integer> entry = packageIndex.get(pkg.nameHash);
        if (entry == null) {
            int newId = packages.size();
            pkg.meta.id = newId;
            packages.add(pkg);
            List<Integer> ids = new ArrayList<>(1);
            ids.add(newId);
            packageIndex.put(pkg.nameHash, ids);
            return newId;
        }
        for (int existingId : entry) {
            if (pkg.resolution.eql(packages.get(existingId).resolution)) {
                pkg.meta.id = existingId;
                return existingId;
            }
        }
        int newId = packages.size();
        pkg.meta.id = newId;
        packages.add(pkg);
        insertSorted(entry, newId);
        return newId;
    }

    void getOrPutID(int id, long nameHash) {
        List<Integer> entry = packageIndex.get(nameHash);
        if (entry == null) {
            List<Integer> ids = new ArrayList<>(1);
            ids.add(id);
            packageIndex.put(nameHash, ids);
            return;
        }
        insertSorted(entry, id);
    }

    // before the first id this one orders above, else last: highest resolution first, ties in insertion order
    private void insertSorted(List<Integer> ids, int id) {
        Resolution resolution = packages.get(id).resolution;
        for (int i = 0; i < ids.size(); i++) {
            if (resolution.order(packages.get(ids.get(i)).resolution) > 0) {
                ids.add(i, id);
                return;
            }
        }
        ids.add(id);
    }

    Package appendPackageWithID(Package pkg, int id) {
        pkg.meta.id = id;
        packages.add(pkg);
        getOrPutID(id, pkg.nameHash);
        return pkg;
    }

    Package appendPackage(Package pkg) {
        return appendPackageWithID(pkg, packages.size());
    }

    /**
     * {@code resolve}: the hoisting that keeps every dependency so the tree is resolvable whatever the
     * configuration, for saving.
     */
    void resolve() {
        Tree.Builder builder = new Tree.Builder(this);
        builder.hoist();
    }
}
