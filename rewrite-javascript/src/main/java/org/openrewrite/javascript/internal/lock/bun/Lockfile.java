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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.openrewrite.javascript.internal.lock.bun.Package.INVALID_PACKAGE_ID;

/** Port of bun's src/install/lockfile.zig (bun 1.3.10); package 0 is the root. */
public final class Lockfile {

    /**
     * What bun loads where it holds no bun.lock and rewrites besides it: a bun.lockb, or a pnpm-lock.yaml it migrates
     * (package-lock.json and yarn.lock first), rewriting package.json.
     */
    public static boolean rewritesAnotherFile(Predicate<String> holds) {
        return holds.test("bun.lockb") || !holds.test("package-lock.json") && !holds.test("yarn.lock") &&
                                          holds.test("pnpm-lock.yaml");
    }

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

    boolean isWorkspaceRootDependency(int id) {
        return packages.get(0).containsDependency(id);
    }

    /**
     * Whether a dependency belongs to the root or to a workspace member.
     */
    boolean isWorkspaceDependency(int id) {
        for (Package pkg : packages) {
            if ((pkg.resolution.tag == Resolution.Tag.WORKSPACE || pkg.resolution.tag == Resolution.Tag.ROOT) &&
                pkg.containsDependency(id)) {
                return true;
            }
        }
        return false;
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
        SemverQuery.Group npmVersion = version != null && version.tag == DependencyVersion.Tag.NPM ?
                version.npm().version : null;
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

    /**
     * {@code eql}: whether the hoisted trees place the same packages (by name, resolution, bin and scripts) at
     * the same paths, over the packages below {@code cutOffPkgId}.
     */
    boolean eql(Lockfile r, int cutOffPkgId) {
        if (hoistedDependencies.size() != r.hoistedDependencies.size()) {
            return false;
        }
        List<int[]> lBuf = placements(cutOffPkgId);
        List<int[]> rBuf = r.placements(cutOffPkgId);
        if (lBuf.size() != rBuf.size()) {
            return false;
        }
        lBuf.sort(placementOrder());
        rBuf.sort(r.placementOrder());
        for (int i = 0; i < lBuf.size(); i++) {
            Package l = packages.get(lBuf.get(i)[0]);
            Package rPkg = r.packages.get(rBuf.get(i)[0]);
            if (l.nameHash != rPkg.nameHash) {
                return false;
            }
            boolean uninitialized = l.resolution.tag == Resolution.Tag.UNINITIALIZED ||
                                    rPkg.resolution.tag == Resolution.Tag.UNINITIALIZED;
            if (uninitialized ? l.resolution.tag != rPkg.resolution.tag : !l.resolution.eql(rPkg.resolution)) {
                return false;
            }
            if (!l.bin.eql(rPkg.bin) || !l.scripts.eql(rPkg.scripts)) {
                return false;
            }
        }
        return true;
    }

    // [package id, tree id] of every placed dependency below the cut-off
    private List<int[]> placements(int cutOffPkgId) {
        List<int[]> out = new ArrayList<>();
        for (Tree tree : trees) {
            for (int depId : tree.dependencies) {
                if (depId == Tree.INVALID_DEPENDENCY_ID) {
                    continue;
                }
                int pkgId = resolutions.get(depId);
                if (pkgId == INVALID_PACKAGE_ID || pkgId >= cutOffPkgId) {
                    continue;
                }
                out.add(new int[]{pkgId, tree.id});
            }
        }
        return out;
    }

    private Comparator<int[]> placementOrder() {
        Map<Integer, String> paths = new HashMap<>();
        for (Tree tree : trees) {
            paths.put(tree.id, Tree.relativePathAndDepth(this, tree.id, tree.dependencies).relativePath);
        }
        return (l, r) -> {
            int byPath = Strings.order(paths.get(l[1]), paths.get(r[1]));
            return byPath != 0 ? byPath : Strings.order(packages.get(l[0]).name, packages.get(r[0]).name);
        };
    }
}
