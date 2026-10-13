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
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.Rejected;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.openrewrite.javascript.internal.lock.bun.Package.INVALID_PACKAGE_ID;

/**
 * Port of bun's src/install/lockfile/Tree.zig (bun 1.3.10) for the {@code resolvable} method, with
 * {@code Lockfile.hoist}.
 */
final class Tree {

    static final int INVALID_ID = Integer.MAX_VALUE;
    static final int ROOT_DEP_ID = INVALID_PACKAGE_ID - 1;
    static final int INVALID_DEPENDENCY_ID = Integer.MAX_VALUE;

    int id = INVALID_ID;
    int dependencyId = INVALID_DEPENDENCY_ID;
    int parent = INVALID_ID;
    List<Integer> dependencies = new ArrayList<>();

    static final class DependencyLoop extends IllegalStateException {
        DependencyLoop(String message) {
            super(message);
        }
    }

    String folderName(List<Dependency> deps) {
        if (dependencyId == INVALID_DEPENDENCY_ID) {
            return "";
        }
        return deps.get(dependencyId).name;
    }

    /**
     * One hoisting decision.
     */
    private static final class HoistResult {
        enum Kind {
            DEPENDENCY_LOOP, HOISTED, RESOLVE, RESOLVE_REPLACE, RESOLVE_LATER,
            /**
             * From 1.4.0: hoisted, and the optional peer's slot now points at the package it deduped onto.
             */
            REBIND,
            PLACEMENT
        }

        static final HoistResult DEPENDENCY_LOOP = new HoistResult(Kind.DEPENDENCY_LOOP, 0, 0, false);
        static final HoistResult HOISTED = new HoistResult(Kind.HOISTED, 0, 0, false);
        static final HoistResult RESOLVE_LATER = new HoistResult(Kind.RESOLVE_LATER, 0, 0, false);

        final Kind kind;
        final int id;
        final int depId;
        final boolean bundled;

        HoistResult(Kind kind, int id, int depId, boolean bundled) {
            this.kind = kind;
            this.id = id;
            this.depId = depId;
            this.bundled = bundled;
        }

        static HoistResult resolve(int packageId) {
            return new HoistResult(Kind.RESOLVE, packageId, 0, false);
        }

        static HoistResult resolveReplace(int treeId, int depId) {
            return new HoistResult(Kind.RESOLVE_REPLACE, treeId, depId, false);
        }

        static HoistResult rebind(int packageId) {
            return new HoistResult(Kind.REBIND, packageId, 0, false);
        }

        static HoistResult placement(int treeId, boolean bundled) {
            return new HoistResult(Kind.PLACEMENT, treeId, 0, bundled);
        }
    }

    private static final class FillItem {
        final int treeId;
        final int dependencyId;
        final int hoistRootId;

        FillItem(int treeId, int dependencyId, int hoistRootId) {
            this.treeId = treeId;
            this.dependencyId = dependencyId;
            this.hoistRootId = hoistRootId;
        }
    }

    /**
     * {@code Builder(.resolvable)}: hoists every dependency, resolved or an optional peer that may resolve later.
     */
    static final class Builder {
        private final Lockfile lockfile;

        /**
         * What the hoisting logs. bun hoists into the install's log, which a {@code --lockfile-only} install prints
         * and clears before it saves, so that nothing here changes what it writes or how it ends.
         */
        final Log log = new Log();

        private final List<Tree> trees = new ArrayList<>();
        private final List<List<Integer>> dependencyLists = new ArrayList<>();
        private final Deque<FillItem> queue = new ArrayDeque<>();
        private final Map<Long, LinkedHashSet<Integer>> pendingOptionalPeers = new LinkedHashMap<>();

        /**
         * An optional peer got bound after its dependent was placed ({@link Lockfile#resolve}).
         */
        private boolean lateBoundOptionalPeer;

        Builder(Lockfile lockfile) {
            this.lockfile = lockfile;
        }

        /**
         * Sets the lockfile's trees and hoisted dependencies; whether an optional peer got bound late.
         */
        boolean hoist() {
            new Tree().processSubtree(ROOT_DEP_ID, INVALID_ID, this);
            while (!queue.isEmpty()) {
                FillItem item = queue.poll();
                trees.get(item.treeId).processSubtree(item.dependencyId, item.hoistRootId, this);
            }
            clean();
            return lateBoundOptionalPeer;
        }

        private void clean() {
            List<Integer> depIds = new ArrayList<>();
            for (int i = 0; i < trees.size(); i++) {
                Tree tree = trees.get(i);
                List<Integer> kept = new ArrayList<>();
                for (int depId : dependencyLists.get(i)) {
                    if (lockfile.resolutions.get(depId) == INVALID_PACKAGE_ID) {
                        continue;
                    }
                    kept.add(depId);
                    depIds.add(depId);
                }
                tree.dependencies = kept;
            }
            lockfile.trees = trees;
            lockfile.hoistedDependencies = depIds;
        }

        private String describe(int packageId) {
            Package pkg = lockfile.packages.get(packageId);
            return pkg.name + "@" + pkg.resolution;
        }
    }

    void processSubtree(int dependencyId, int hoistRootId, Builder builder) {
        int parentPkgId = dependencyId == ROOT_DEP_ID ? 0 : builder.lockfile.resolutions.get(dependencyId);
        Package parentPkg = builder.lockfile.packages.get(parentPkgId);
        if (parentPkg.dependenciesLen == 0) {
            return;
        }
        Tree next = new Tree();
        next.parent = this.id;
        next.id = builder.trees.size();
        next.dependencyId = dependencyId;
        builder.trees.add(next);
        builder.dependencyLists.add(new ArrayList<>());

        List<Integer> sortBuf = new ArrayList<>(parentPkg.dependenciesLen);
        for (int depId = parentPkg.dependenciesOff; depId < parentPkg.dependenciesEnd(); depId++) {
            sortBuf.add(depId);
        }
        sortBuf.sort(builder.lockfile::depSort);

        for (int depId : sortBuf) {
            int pkgId = builder.lockfile.resolutions.get(depId);
            Dependency dependency = builder.lockfile.dependencies.get(depId);
            // from 1.4.0 a name that would leave node_modules is skipped; an empty one has no folder to leave
            if (!dependency.name.isEmpty() && builder.lockfile.release.skipsAnUnsafeDependencyName() &&
                !Dependency.isSafeInstallFolderName(dependency.name)) {
                builder.log.addError("Invalid dependency name \"" + dependency.name + "\"");
                continue;
            }
            HoistResult hoisted;
            if (dependency.behavior.isBundled()) {
                hoisted = HoistResult.placement(next.id, true);
            } else if (pkgId == INVALID_PACKAGE_ID) {
                if (!dependency.behavior.isOptionalPeer()) {
                    continue;
                }
                hoisted = next.hoistDependency(true, hoistRootId, pkgId, dependency, parentPkg, builder);
            } else if (builder.lockfile.packages.get(pkgId).resolution.tag == Resolution.Tag.FOLDER) {
                BunRelease release = builder.lockfile.release;
                if (release.dedupesAPeerResolvedToAFolderPackage() && dependency.behavior.isPeer() &&
                    next.hoistDependency(true, hoistRootId, pkgId, dependency, parentPkg, builder).kind ==
                    HoistResult.Kind.HOISTED) {
                    hoisted = HoistResult.HOISTED;
                } else {
                    // folder packages never hoist, so a cycle between them would nest forever
                    int holder = holderAbove(next.id, pkgId, dependency, builder);
                    if (holder != INVALID_ID) {
                        if (release.skipsAFolderPackageADirectoryAboveHolds()) {
                            continue;
                        }
                        if (onlyFolderPackagesBelow(holder, next.id, builder)) {
                            throw Rejected.crashes(Reason.RESOLUTION_REQUIRED, null, "it never finishes: its " +
                                    "hoisting places the folder package " + builder.describe(pkgId) + " inside " +
                                    "itself again and again, as folder packages never hoist");
                        }
                    }
                    hoisted = HoistResult.placement(next.id, false);
                }
            } else {
                hoisted = next.hoistDependency(true, hoistRootId, pkgId, dependency, parentPkg, builder);
            }

            switch (hoisted.kind) {
                case DEPENDENCY_LOOP:
                case HOISTED:
                    continue;
                case REBIND:
                    builder.lockfile.resolutions.set(depId, hoisted.id);
                    break;
                case RESOLVE: {
                    builder.lockfile.resolutions.set(depId, hoisted.id);
                    LinkedHashSet<Integer> peers = builder.pendingOptionalPeers.remove(dependency.nameHash);
                    if (peers != null) {
                        for (int unresolvedDepId : peers) {
                            builder.lockfile.resolutions.set(unresolvedDepId, hoisted.id);
                        }
                    }
                    break;
                }
                case RESOLVE_REPLACE: {
                    builder.lateBoundOptionalPeer = true;
                    builder.lockfile.resolutions.set(hoisted.depId, pkgId);
                    LinkedHashSet<Integer> peers = builder.pendingOptionalPeers.remove(dependency.nameHash);
                    if (peers != null) {
                        for (int unresolvedDepId : peers) {
                            builder.lockfile.resolutions.set(unresolvedDepId, pkgId);
                        }
                    }
                    List<Integer> placed = builder.dependencyLists.get(hoisted.id);
                    for (int i = 0; i < placed.size(); i++) {
                        if (placed.get(i) == hoisted.depId) {
                            placed.set(i, depId);
                        }
                    }
                    if (pkgId != INVALID_PACKAGE_ID && builder.lockfile.packages.get(pkgId).dependenciesLen > 0) {
                        builder.queue.add(new FillItem(hoisted.id, depId, hoistRootId));
                    }
                    break;
                }
                case RESOLVE_LATER:
                    builder.pendingOptionalPeers.computeIfAbsent(dependency.nameHash, k -> new LinkedHashSet<>())
                            .add(depId);
                    break;
                default: {
                    builder.dependencyLists.get(hoisted.id).add(depId);
                    if (pkgId != INVALID_PACKAGE_ID && builder.lockfile.packages.get(pkgId).dependenciesLen > 0) {
                        builder.queue.add(new FillItem(hoisted.id, depId, hoisted.bundled ? hoisted.id : hoistRootId));
                    }
                    break;
                }
            }
        }

        if (builder.dependencyLists.get(next.id).isEmpty()) {
            builder.trees.remove(builder.trees.size() - 1);
            builder.dependencyLists.remove(builder.dependencyLists.size() - 1);
        }
    }

    // the tree, this one or one above it, whose dependency is the same package under the same name; else INVALID_ID
    private static int holderAbove(int treeId, int packageId, Dependency dependency, Builder builder) {
        for (int id = treeId; id != INVALID_ID; id = builder.trees.get(id).parent) {
            int ancestorDepId = builder.trees.get(id).dependencyId;
            if (ancestorDepId < builder.lockfile.dependencies.size() &&
                builder.lockfile.resolutions.get(ancestorDepId) == packageId &&
                builder.lockfile.dependencies.get(ancestorDepId).nameHash == dependency.nameHash) {
                return id;
            }
        }
        return INVALID_ID;
    }

    // whether every tree from this one up to, not including, the holder is a folder package's, each placed whatever is
    // above it, so that a copy of the holder placed here repeats the same chain below it without end
    private static boolean onlyFolderPackagesBelow(int holder, int treeId, Builder builder) {
        for (int id = treeId; id != holder; id = builder.trees.get(id).parent) {
            int packageId = builder.lockfile.resolutions.get(builder.trees.get(id).dependencyId);
            if (builder.lockfile.packages.get(packageId).resolution.tag != Resolution.Tag.FOLDER) {
                return false;
            }
        }
        return true;
    }

    // 1: dedupe (hoisted), 2: place here or higher, 3: stay at this relative place (dependency loop)
    // inputDepRange: the package whose dependencies are being placed
    private HoistResult hoistDependency(boolean asDefined, int hoistRootId, int packageId, Dependency dependency,
                                        Package inputDepRange, Builder builder) {
        BunRelease release = builder.lockfile.release;
        List<Integer> thisDependencies = builder.dependencyLists.get(this.id);
        for (int depId : thisDependencies) {
            Dependency dep = builder.lockfile.dependencies.get(depId);
            if (dep.nameHash != dependency.nameHash) {
                continue;
            }
            int resId = builder.lockfile.resolutions.get(depId);
            if (resId == INVALID_PACKAGE_ID && packageId == INVALID_PACKAGE_ID) {
                return HoistResult.RESOLVE_LATER;
            }
            if (resId == INVALID_PACKAGE_ID) {
                return HoistResult.resolveReplace(this.id, depId);
            }
            if (packageId == INVALID_PACKAGE_ID) {
                return HoistResult.resolve(resId);
            }
            if (resId == packageId) {
                return HoistResult.HOISTED;
            }
            if (release.dedupesAPackagesEdgesToOneName()) {
                // the same package lists this name in another dependency group
                if (inputDepRange.containsDependency(depId)) {
                    return HoistResult.HOISTED;
                }
            } else if (asDefined && dep.behavior.isDev() != dependency.behavior.isDev()) {
                return HoistResult.HOISTED;
            }
            if (dependency.behavior.isPeer()) {
                // from 1.4.0 an optional peer's binding follows the dedupe, in the tree being saved
                HoistResult dedupe = dependency.behavior.isOptionalPeer() &&
                                     release.rebindsTheOptionalPeersItDedupes() ?
                        HoistResult.rebind(resId) : HoistResult.HOISTED;
                DependencyVersion peerRange = release.dedupesACatalogPeerByItsRange() ?
                        builder.lockfile.catalogs.resolveRange(dependency) : dependency.version;
                if (peerRange.tag == DependencyVersion.Tag.NPM) {
                    Resolution resolution = builder.lockfile.packages.get(resId).resolution;
                    if (resolution.tag == Resolution.Tag.NPM &&
                        peerRange.npm().version.satisfies(resolution.version)) {
                        return dedupe;
                    }
                }
                if (builder.lockfile.isWorkspaceRootDependency(depId)) {
                    return dedupe;
                }
            }
            if (asDefined && !dep.behavior.isPeer() && !release.dedupesAPackagesEdgesToOneName()) {
                throw new DependencyLoop("Package \"" + builder.describe(packageId) + "\" has a dependency loop\n" +
                                         "  Resolution: \"" + builder.describe(resId) + "\"\n" +
                                         "  Dependency: \"" + dependency.name + "@" + dependency.version.literal +
                                         "\"");
            }
            return HoistResult.DEPENDENCY_LOOP;
        }

        if (this.parent != INVALID_ID && this.id != hoistRootId) {
            HoistResult id = builder.trees.get(this.parent)
                    .hoistDependency(false, hoistRootId, packageId, dependency, inputDepRange, builder);
            if (!asDefined || id.kind != HoistResult.Kind.DEPENDENCY_LOOP) {
                return id;
            }
        }
        return HoistResult.placement(this.id, false);
    }

    /**
     * {@code Iterator(.pkg_path)}: every tree with dependencies, in id order, with its path of folder names joined
     * by {@code /} and its depth.
     */
    static final class Iterator {
        private final Lockfile lockfile;
        private int treeId;

        Iterator(Lockfile lockfile) {
            this.lockfile = lockfile;
        }

        void reset() {
            treeId = 0;
        }

        @Nullable
        Next next() {
            List<Tree> trees = lockfile.trees;
            if (treeId >= trees.size()) {
                return null;
            }
            while (trees.get(treeId).dependencies.isEmpty()) {
                treeId++;
                if (treeId >= trees.size()) {
                    return null;
                }
            }
            int current = treeId;
            Tree tree = trees.get(current);
            treeId++;
            return relativePathAndDepth(lockfile, current, tree.dependencies);
        }
    }

    static final class Next {
        final String relativePath;
        final List<Integer> dependencies;
        final int treeId;
        final int depth;

        Next(String relativePath, List<Integer> dependencies, int treeId, int depth) {
            this.relativePath = relativePath;
            this.dependencies = dependencies;
            this.treeId = treeId;
            this.depth = depth;
        }
    }

    static Next relativePathAndDepth(Lockfile lockfile, int treeId, List<Integer> dependencies) {
        List<Tree> trees = lockfile.trees;
        Tree tree = trees.get(treeId);
        int depth = 0;
        StringBuilder path = new StringBuilder();
        if (tree.id > 0) {
            List<Integer> chain = new ArrayList<>();
            chain.add(0);
            int parentId = tree.id;
            while (parentId > 0 && parentId < trees.size()) {
                chain.add(parentId);
                parentId = trees.get(parentId).parent;
            }
            int len = chain.size() - 1;
            depth = len;
            while (len > 0) {
                if (len != depth) {
                    path.append('/');
                }
                path.append(trees.get(chain.get(len)).folderName(lockfile.dependencies));
                len--;
            }
        }
        return new Next(path.toString(), dependencies, treeId, depth);
    }
}
