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
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;
import static org.openrewrite.javascript.internal.lock.bun.Package.INVALID_PACKAGE_ID;

/** Port of bun's src/install/lockfile.zig (bun 1.3.10); package 0 is the root. */
public final class Lockfile {

    /**
     * What bun loads where it holds no bun.lock and rewrites besides it: a bun.lockb, or a pnpm-lock.yaml it migrates
     * (package-lock.json and yarn.lock first), rewriting package.json, from 1.2.23
     * ({@link BunRelease#migratesAPnpmLock()}).
     */
    public static boolean rewritesAnotherFile(Predicate<String> holds, BunRelease release) {
        return holds.test("bun.lockb") || release.migratesAPnpmLock() && !holds.test("package-lock.json") &&
                                          !holds.test("yarn.lock") && holds.test("pnpm-lock.yaml");
    }

    static final int TEXT_LOCKFILE_VERSION_CURRENT = 1;

    /**
     * From 1.4.0, the bun.lock version whose reading checks what a package's tarball and tag may be
     * ({@code Version::V2}).
     */
    static final int TEXT_LOCKFILE_VERSION_STRICT = 2;

    /**
     * From 1.4.0, the bun.lock version whose overrides may hold scoped rules, and the newest it reads
     * ({@code Version::V3}, {@code Version::CURRENT}).
     */
    static final int TEXT_LOCKFILE_VERSION_SCOPED_OVERRIDES = 3;

    /**
     * The version of the bun.lock read; for a new lock, the current one.
     */
    int textLockfileVersion;

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
     * From 1.4.0, the package count when the lock finished loading ({@code loaded_package_count}, set by
     * {@link #markLoadedPackages()}); 0 for a new lock, so that every package is one this install added.
     */
    int loadedPackageCount;

    /**
     * From 1.4.0, the packages added for a range of one exact version ({@code exact_pinned}).
     */
    final BitSet exactPinned = new BitSet();

    /**
     * The release that installs, whose order of dependency groups the sorts follow.
     */
    final BunRelease release;

    Lockfile() {
        this(BunRelease.PORTED);
    }

    Lockfile(BunRelease release) {
        this.release = release;
        this.textLockfileVersion = release.keepsTheLockfileVersionItRead() ? TEXT_LOCKFILE_VERSION_SCOPED_OVERRIDES :
                TEXT_LOCKFILE_VERSION_CURRENT;
    }

    /**
     * Sorts the dependencies of a node_modules folder: behavior, then name.
     */
    int depSort(int l, int r) {
        return Dependency.compare(dependencies.get(l), dependencies.get(r), release);
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
     * {@code is_trusted_folder_dependency} (1.4.3): whether the folder path of a dependency may leave its package's
     * directory, the root, a workspace or a {@code file:} package one of them depends on directly declaring it, or an
     * override or resolution naming it.
     */
    boolean isTrustedFolderDependency(int id) {
        if (isDependencyOfLocalPackage(id)) {
            return true;
        }
        Dependency dep = dependencies.get(id);
        return overridesContainName(dep.nameHash, dep.name);
    }

    // is_dependency_of_local_package: the first package whose dependencies hold it is the root, a workspace, or a
    // file: package the root or a workspace depends on directly
    private boolean isDependencyOfLocalPackage(int id) {
        for (int parentId = 0; parentId < packages.size(); parentId++) {
            Package parent = packages.get(parentId);
            if (parent.containsDependency(id)) {
                switch (parent.resolution.tag) {
                    case ROOT:
                    case WORKSPACE:
                        return true;
                    case FOLDER:
                        return isWorkspaceDeclaredPackage(parentId);
                    default:
                        return false;
                }
            }
        }
        return false;
    }

    // is_workspace_declared_package: the root or a workspace resolves a dependency to the package
    private boolean isWorkspaceDeclaredPackage(int id) {
        for (Package pkg : packages) {
            if ((pkg.resolution.tag == Resolution.Tag.WORKSPACE || pkg.resolution.tag == Resolution.Tag.ROOT) &&
                resolutionsOf(pkg).contains(id)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code OverrideMap::contains_name} (from 1.4.0): whether a plain override or resolution rule names the
     * dependency, its name compared as well as its hash.
     */
    boolean overridesContainName(long nameHash, String name) {
        Dependency rule = overrides.map.get(nameHash);
        return rule != null && rule.name.equals(name);
    }

    /**
     * {@code mark_loaded_packages}: the packages there are now are the lock's.
     */
    void markLoadedPackages() {
        loadedPackageCount = packages.size();
    }

    /**
     * {@code mark_exact_pin}: the package was added for a range of one exact version.
     */
    void markExactPin(int id) {
        exactPinned.set(id);
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
            if (existing.tag == Resolution.Tag.NPM && npmVersion != null && npmVersion.satisfies(existing.version) &&
                !(release.keepsARangeOffALowerMajorAddedThisInstall() && belowThePickOfAnotherMajor(id, resolution))) {
                return id;
            }
        }
        return null;
    }

    // get_package_id's order-independence guard: a package this install added, for no exact range, below the pick the
    // caller passes and of another major is the network order's, not a choice
    private boolean belowThePickOfAnotherMajor(int id, Resolution resolution) {
        if (id < loadedPackageCount || exactPinned.get(id) || resolution.tag != Resolution.Tag.NPM) {
            return false;
        }
        return isBelowAndOfAnotherMajor(requireNonNull(packages.get(id).resolution.version),
                requireNonNull(resolution.version), release);
    }

    /**
     * Whether a version orders below the pick and has another major.
     */
    static boolean isBelowAndOfAnotherMajor(SemverVersion version, SemverVersion pick, BunRelease release) {
        return version.order(pick, release) < 0 && version.major != pick.major;
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
            if (resolution.order(packages.get(ids.get(i)).resolution, release) > 0) {
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
     * configuration, for saving; from 1.4.0 again while a pass bound an optional peer late, which a reload has bound
     * up front.
     */
    void resolve() {
        boolean lateBoundOptionalPeer;
        do {
            lateBoundOptionalPeer = new Tree.Builder(this).hoist();
        } while (lateBoundOptionalPeer && release.rebindsTheOptionalPeersItDedupes());
    }

    /**
     * {@code linked_workspace_path} (from 1.4.1): the path of the workspace an npm range on the name links to instead
     * of the registry, the workspace of that name, when the range satisfies its version or is {@code *}, which links
     * even a workspace without a version; null for none.
     */
    static @Nullable String linkedWorkspacePath(boolean linkWorkspacePackages, Map<Long, String> workspacePaths,
                                                Map<Long, SemverVersion> workspaceVersions, long nameHash,
                                                SemverQuery.Group range) {
        if (!linkWorkspacePackages) {
            return null;
        }
        String path = workspacePaths.get(nameHash);
        if (path == null) {
            return null;
        }
        if (range.isStar()) {
            return path;
        }
        SemverVersion version = workspaceVersions.get(nameHash);
        return version != null && range.satisfies(version) ? path : null;
    }

    /**
     * {@code tag_workspace_links} (from 1.4.1): a loader rebuilds each dependency from its literal, so it ends by
     * giving an edge resolved to a workspace the shape a parse of its package.json gives it: a {@code workspace:} edge
     * takes the workspace's path, and an npm range becomes a workspace edge where {@link #linkedWorkspacePath}, asked
     * with the workspaces this lockfile records, links it. A range bound to a workspace any other way stays a range.
     */
    void tagWorkspaceLinks(boolean linkWorkspacePackages) {
        for (int i = 0; i < dependencies.size(); i++) {
            int pkgId = resolutions.get(i);
            if (pkgId >= packages.size()) {
                continue;
            }
            Resolution res = packages.get(pkgId).resolution;
            if (res.tag != Resolution.Tag.WORKSPACE) {
                continue;
            }
            Dependency dep = dependencies.get(i);
            boolean linked;
            switch (dep.version.tag) {
                case WORKSPACE:
                    linked = true;
                    break;
                case NPM: {
                    DependencyVersion.NpmInfo npm = dep.version.npm();
                    linked = linkedWorkspacePath(linkWorkspacePackages, workspacePaths, workspaceVersions,
                            Wyhash11.stringHash(npm.name), npm.version) != null;
                    break;
                }
                default:
                    linked = false;
                    break;
            }
            if (linked) {
                dep.version = dep.version.asWorkspace(res.value);
            }
        }
    }

    /**
     * {@code eql}: whether the hoisted trees place the same packages (by name, resolution, bin and scripts) at
     * the same paths, over the packages below {@code cutOffPkgId}.
     */
    boolean eql(Lockfile r, int cutOffPkgId) {
        return eql(placements(cutOffPkgId), r, r.placements(cutOffPkgId));
    }

    /**
     * {@code eql} from 1.4.0: every placement of each lockfile, one of {@code r} bound to a package past those it
     * loaded being a change.
     */
    boolean eqlLoaded(Lockfile r, int rLoadedPackageCount) {
        List<int[]> rBuf = r.placements(Integer.MAX_VALUE);
        for (int[] placement : rBuf) {
            if (placement[0] >= rLoadedPackageCount) {
                return false;
            }
        }
        return eql(placements(Integer.MAX_VALUE), r, rBuf);
    }

    private boolean eql(List<int[]> lBuf, Lockfile r, List<int[]> rBuf) {
        if (hoistedDependencies.size() != r.hoistedDependencies.size()) {
            return false;
        }
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
