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
import org.openrewrite.javascript.internal.lock.RootRelativePath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of bun's src/install/lockfile/Package.zig {@code Diff.generate} (bun 1.3.10), for an install without update
 * requests, as the release that installs diffs ({@link BunRelease}).
 */
final class PackageDiff {

    static final class Summary {
        int add;
        int remove;
        int update;
        boolean overridesChanged;
        boolean catalogsChanged;
        final LinkedHashMap<Integer, Boolean> addedTrustedDependencies = new LinkedHashMap<>();
        final Set<Integer> removedTrustedDependencies = new LinkedHashSet<>();
        boolean patchedDependenciesChanged;

        /**
         * From 1.4.1, a workspace's version changed: no edge changed with it, but bun.lock records it.
         */
        boolean workspaceVersionsChanged;

        /**
         * From 1.4.0, the workspaces bun.lock lists whose package.json is gone, which a frozen install does not count
         * removed.
         */
        final List<Long> prunedWorkspaces = new ArrayList<>();

        /**
         * {@code changes_resolutions} (from 1.4.0): a dependency added, removed or updated, or the overrides or the
         * catalogs changed.
         */
        boolean changesResolutions() {
            return add > 0 || remove > 0 || update > 0 || overridesChanged || catalogsChanged;
        }

        boolean hasDiffs() {
            return changesResolutions() || !addedTrustedDependencies.isEmpty() ||
                   !removedTrustedDependencies.isEmpty() || patchedDependenciesChanged || workspaceVersionsChanged;
        }
    }

    private PackageDiff() {
    }

    /**
     * {@code idMapping}: for the root, per dependency of {@code to}, the index of the unchanged dependency of {@code
     * from} it keeps the resolution of, else {@link Package#INVALID_PACKAGE_ID}; {@code manifests} by location.
     */
    static Summary generate(BunOptions options, Log log, Lockfile fromLockfile, Lockfile toLockfile, Package from,
                            Package to, int @Nullable [] idMapping, Map<String, String> manifests) {
        Summary summary = new Summary();
        boolean isRoot = idMapping != null;
        List<Dependency> toDeps = toLockfile.dependenciesOf(to);
        List<Dependency> fromDeps = fromLockfile.dependenciesOf(from);
        List<Integer> fromResolutions = fromLockfile.resolutionsOf(from);
        int toI = 0;

        // from 1.4.0 the scoped rules count too
        summary.overridesChanged = OverrideMap.changed(fromLockfile.overrides, toLockfile.overrides, options.getRelease());

        if (isRoot) {
            summary.catalogsChanged = catalogsChanged(fromLockfile.catalogs, toLockfile.catalogs, options.getRelease());
        }

        trustedDependencies(fromLockfile, toLockfile, summary, options.getRelease());

        summary.patchedDependenciesChanged = patchedDependenciesChanged(fromLockfile, toLockfile);

        BunRelease release = options.getRelease();
        if (isRoot && release.diffsWorkspaceVersions()) {
            summary.workspaceVersionsChanged = workspaceVersionsChanged(fromLockfile, toLockfile);
        }

        List<Integer> missingWorkspaces = new ArrayList<>();
        List<Package> survivors = new ArrayList<>();
        for (int i = 0; i < fromDeps.size(); i++) {
            Dependency fromDep = fromDeps.get(i);
            int prevI = toI;
            boolean found = false;
            for (; toI < toDeps.size(); toI++) {
                if (matches(fromDep, toDeps.get(toI), release)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (toI = 0; toI < prevI; toI++) {
                    if (matches(fromDep, toDeps.get(toI), release)) {
                        found = true;
                        break;
                    }
                }
            }
            if (!found) {
                if (isRoot && fromDep.behavior.isWorkspace() && release.failsOnAWorkspaceMissingFromDisk() &&
                    workspaceIsMissingOnDisk(fromLockfile, fromDep.nameHash, manifests)) {
                    if (fromResolutions.get(i) < fromLockfile.packages.size()) {
                        missingWorkspaces.add(fromResolutions.get(i));
                    }
                    if (options.isFrozenLockfile() && !summary.overridesChanged && !summary.catalogsChanged) {
                        summary.prunedWorkspaces.add(fromDep.nameHash);
                        continue;
                    }
                }
                summary.remove++;
                continue;
            }
            int matched = toI;
            toI++;
            if (toDeps.get(matched).eql(fromDep, release)) {
                if (idMapping != null) {
                    boolean updateMapping = true;
                    // before 1.2.19 a member is diffed again where the dependency is a workspace spec
                    if (release.tellsDependencyGroupsApart() ? isRoot && fromDep.behavior.isWorkspace() :
                            toDeps.get(matched).version.tag == DependencyVersion.Tag.WORKSPACE) {
                        updateMapping = workspaceUnchanged(options, log, fromLockfile, toLockfile,
                                fromResolutions.get(i), fromDep, manifests, survivors);
                        toDeps = toLockfile.dependenciesOf(to);
                    }
                    if (updateMapping) {
                        idMapping[matched] = i;
                        continue;
                    }
                } else {
                    continue;
                }
            }
            // from 1.4.0 a changed literal keeps the locked resolution while it still satisfies the new range
            if (idMapping != null && release.keepsALockedVersionTheNewRangeAccepts()) {
                int fromResId = fromResolutions.get(i);
                if (fromResId < fromLockfile.packages.size()) {
                    Resolution fromPkgResolution = fromLockfile.packages.get(fromResId).resolution;
                    Dependency toDep = toDeps.get(matched);
                    if (toDep.version.tag == DependencyVersion.Tag.NPM && fromPkgResolution.tag == Resolution.Tag.NPM &&
                        toDep.version.npm().version.satisfies(fromPkgResolution.version)) {
                        // still counted an update, so that the root is replaced; only its resolution is kept
                        idMapping[matched] = i;
                    }
                }
            }
            summary.update++;
        }

        summary.add = Math.max(0, toDeps.size() - Math.max(0, fromDeps.size() -
                                                               (summary.remove + summary.prunedWorkspaces.size())));

        if (!missingWorkspaces.isEmpty()) {
            failIfSurvivorDependsOnMissing(fromLockfile, missingWorkspaces, toLockfile, to, survivors);
        }

        if (from.resolution.tag != Resolution.Tag.ROOT && !to.scripts.eql(from.scripts)) {
            for (int h = 0; h < Package.Scripts.NAMES.length; h++) {
                if (!to.scripts.hooks[h].equals(from.scripts.hooks[h])) {
                    summary.update++;
                }
            }
        }
        return summary;
    }

    /**
     * The dependency of {@code to} a dependency of {@code from} is diffed against: of the same name and behavior, or
     * before 1.2.19 of the same name unless only one of the two is a workspace alone.
     */
    private static boolean matches(Dependency from, Dependency to, BunRelease release) {
        if (from.nameHash != to.nameHash) {
            return false;
        }
        if (release.tellsDependencyGroupsApart()) {
            return from.behavior.eq(to.behavior);
        }
        return from.behavior.isWorkspaceOnly() == to.behavior.isWorkspaceOnly();
    }

    /**
     * @param survivors the workspaces read from disk, to which this one is added once read
     */
    private static boolean workspaceUnchanged(BunOptions options, Log log, Lockfile fromLockfile, Lockfile toLockfile,
                                              int fromResolution, Dependency fromDep, Map<String, String> manifests,
                                              List<Package> survivors) {
        String workspacePath = toLockfile.workspacePaths.get(fromDep.nameHash);
        if (workspacePath == null) {
            return false;
        }
        String source = manifests.get(RootRelativePath.normalize(workspacePath.replace('\\', '/')));
        if (source == null) {
            return false;
        }
        Json.Expr json;
        try {
            json = Json.parse(source);
        } catch (Json.SyntaxError e) {
            return false;
        }
        Package workspacePkg = new Package();
        workspacePkg.parseWithJSON(toLockfile, options, log,
                RootRelativePath.normalize(workspacePath.replace('\\', '/')), json,
                Features.WORKSPACE, manifests);
        survivors.add(workspacePkg);
        Package fromPkg = fromLockfile.packages.get(fromResolution);
        Summary diff = generate(options, log, fromLockfile, toLockfile, fromPkg, workspacePkg, null, manifests);
        // from 1.4.0 a trusted or patched dependency's change alone keeps the workspace
        return options.getRelease().keepsAWorkspaceWhoseDiffChangesNoResolution() ? !diff.changesResolutions() :
                !diff.hasDiffs();
    }

    /**
     * {@code workspace_versions_changed} (from 1.4.1): the workspaces' versions compared as bun.lock prints them, so
     * that one rewrite settles the file.
     */
    private static boolean workspaceVersionsChanged(Lockfile fromLockfile, Lockfile toLockfile) {
        if (fromLockfile.workspaceVersions.size() != toLockfile.workspaceVersions.size()) {
            return true;
        }
        for (Map.Entry<Long, SemverVersion> entry : toLockfile.workspaceVersions.entrySet()) {
            SemverVersion from = fromLockfile.workspaceVersions.get(entry.getKey());
            SemverVersion to = entry.getValue();
            if (from == null || from.major != to.major || from.minor != to.minor || from.patch != to.patch ||
                !from.pre.equals(to.pre) || !from.build.equals(to.build)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@code workspace_is_missing_on_disk} (from 1.4.0): bun.lock lists the workspace, and the project holds no
     * package.json where it says.
     */
    private static boolean workspaceIsMissingOnDisk(Lockfile lockfile, long workspaceNameHash,
                                                    Map<String, String> manifests) {
        String workspacePath = lockfile.workspacePaths.get(workspaceNameHash);
        return workspacePath != null &&
               !manifests.containsKey(RootRelativePath.normalize(workspacePath.replace('\\', '/')));
    }

    /**
     * {@code exit_if_survivor_depends_on_missing} (from 1.4.0): the root or a workspace read from disk that depends by
     * {@code workspace:} on a workspace missing from disk fails the install.
     */
    private static void failIfSurvivorDependsOnMissing(Lockfile fromLockfile, List<Integer> missing,
                                                       Lockfile toLockfile, Package toRoot, List<Package> survivors) {
        for (Dependency dep : toLockfile.dependenciesOf(toRoot)) {
            if (dep.behavior.isWorkspace()) {
                continue;
            }
            Package target = missingTarget(fromLockfile, missing, dep);
            if (target != null) {
                throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, target.name, "the root package depends on " +
                        "workspace \"" + target.name + "\" (" + target.resolution.value + "), which is listed in " +
                        "bun.lock but not on disk");
            }
        }
        for (Package survivor : survivors) {
            for (Dependency dep : toLockfile.dependenciesOf(survivor)) {
                Package target = missingTarget(fromLockfile, missing, dep);
                if (target != null) {
                    throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, target.name, "workspace \"" + survivor.name +
                            "\" depends on workspace \"" + target.name + "\" (" + target.resolution.value + "), " +
                            "which is listed in bun.lock but not on disk");
                }
            }
        }
    }

    private static @Nullable Package missingTarget(Lockfile fromLockfile, List<Integer> missing, Dependency dep) {
        if (dep.version.tag != DependencyVersion.Tag.WORKSPACE) {
            return null;
        }
        for (int id : missing) {
            Package pkg = fromLockfile.packages.get(id);
            if (pkg.nameHash == dep.nameHash) {
                return pkg;
            }
        }
        return null;
    }

    private static boolean catalogsChanged(CatalogMap from, CatalogMap to, BunRelease release) {
        if (from.defaultMap.size() != to.defaultMap.size()) {
            return true;
        }
        if (from.groups.size() != to.groups.size()) {
            return true;
        }
        from.sort();
        to.sort();
        if (!sameDeps(from.defaultMap, to.defaultMap, release)) {
            return true;
        }
        Iterator<Map.Entry<String, LinkedHashMap<String, Dependency>>> fromIt = from.groups.entrySet().iterator();
        Iterator<Map.Entry<String, LinkedHashMap<String, Dependency>>> toIt = to.groups.entrySet().iterator();
        while (fromIt.hasNext()) {
            Map.Entry<String, LinkedHashMap<String, Dependency>> f = fromIt.next();
            Map.Entry<String, LinkedHashMap<String, Dependency>> t = toIt.next();
            if (!f.getKey().equals(t.getKey())) {
                return true;
            }
            if (f.getValue().size() != t.getValue().size()) {
                return true;
            }
            if (!sameDeps(f.getValue(), t.getValue(), release)) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameDeps(Map<String, Dependency> from, Map<String, Dependency> to,
                                    BunRelease release) {
        Iterator<Map.Entry<String, Dependency>> fromIt = from.entrySet().iterator();
        Iterator<Map.Entry<String, Dependency>> toIt = to.entrySet().iterator();
        while (fromIt.hasNext() && toIt.hasNext()) {
            Map.Entry<String, Dependency> f = fromIt.next();
            Map.Entry<String, Dependency> t = toIt.next();
            if (!f.getKey().equals(t.getKey()) || !f.getValue().eql(t.getValue(), release)) {
                return false;
            }
        }
        return true;
    }

    // against the release's default list where package.json lists none
    private static void trustedDependencies(Lockfile fromLockfile, Lockfile toLockfile, Summary summary,
                                            BunRelease release) {
        Set<Integer> from = fromLockfile.trustedDependencies;
        Set<Integer> to = toLockfile.trustedDependencies;
        if (from == null && to == null) {
            return;
        }
        if (from != null && to != null) {
            for (int t : to) {
                if (!from.contains(t)) {
                    summary.addedTrustedDependencies.put(t, true);
                }
            }
            for (int f : from) {
                if (!to.contains(f)) {
                    summary.removedTrustedDependencies.add(f);
                }
            }
            return;
        }
        if (from != null) {
            Set<Integer> defaults = DefaultTrustedDependencies.of(release);
            for (int hash : defaults) {
                if (!from.contains(hash)) {
                    summary.addedTrustedDependencies.put(hash, false);
                }
            }
            for (int f : from) {
                if (!defaults.contains(f)) {
                    summary.removedTrustedDependencies.add(f);
                }
            }
            return;
        }
        for (int t : to) {
            summary.addedTrustedDependencies.put(t, true);
        }
    }

    private static boolean patchedDependenciesChanged(Lockfile fromLockfile, Lockfile toLockfile) {
        if (fromLockfile.patchedDependencies.size() != toLockfile.patchedDependencies.size()) {
            return true;
        }
        for (Map.Entry<Long, String> entry : toLockfile.patchedDependencies.entrySet()) {
            String val = fromLockfile.patchedDependencies.get(entry.getKey());
            if (val == null || !val.equals(entry.getValue())) {
                return true;
            }
        }
        for (Long key : fromLockfile.patchedDependencies.keySet()) {
            if (!toLockfile.patchedDependencies.containsKey(key)) {
                return true;
            }
        }
        return false;
    }
}
