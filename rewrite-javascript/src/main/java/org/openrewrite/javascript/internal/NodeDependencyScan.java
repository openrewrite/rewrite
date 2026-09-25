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
package org.openrewrite.javascript.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Shared per-project scan-state for the Node dependency {@code ScanningRecipe}s. */
public final class NodeDependencyScan {

    private NodeDependencyScan() {
    }

    public static final class Accumulator {
        public final Map<Path, ProjectState> projects = new HashMap<>();
        public final Map<Path, Path> lockToPackage = new HashMap<>();
        /** Every {@code package.json} the scan saw, whether or not it carried a resolution result. */
        public final Set<Path> manifests = new HashSet<>();
        /** Per manifest, the catalog each dependency references: package name to catalog name. */
        public final Map<Path, Map<String, String>> catalogRefs = new HashMap<>();
        /** Workspace files that can declare catalogs, by path. */
        public final Map<Path, SourceFile> workspaceFiles = new HashMap<>();
        /** The catalog entries judged safe to edit in place, by the workspace file declaring them. */
        public final Map<Path, Map<NodeCatalogs.CatalogEntry, String>> catalogEdits = new HashMap<>();
    }

    public static final class ProjectState {
        public @Nullable SourceFile capturedPackageJson;
        public @Nullable String capturedLockContent;
        public @Nullable SourceFile modifiedPackageJson;
        public @Nullable Set<String> scopesContainingPackage;
        public @Nullable List<MatchedDependency> matchedDeps;
        /** Matched dependencies left alone because their version position holds a specifier protocol. */
        public final List<MatchedDependency> skippedProtocols = new ArrayList<>();
        public LockFileRegeneration.@Nullable Result regenResult;
        public boolean failureRecorded;
        public boolean protocolsReported;
    }

    /**
     * Link each workspace-member {@code package.json} to the ancestor root lock, so editing a member
     * regenerates that lock. A member with its own sibling lock keeps it.
     */
    public static void linkWorkspaceMembers(Accumulator acc) {
        for (Map.Entry<Path, Path> e : acc.lockToPackage.entrySet()) {
            ProjectState root = acc.projects.get(e.getValue());
            if (root == null || root.capturedPackageJson == null || root.capturedLockContent == null) {
                continue;
            }
            for (Path member : PackageJsonHelper.workspaceMemberPaths(root.capturedPackageJson)) {
                ProjectState memberPs = acc.projects.get(member);
                if (memberPs != null && memberPs.capturedLockContent == null) {
                    memberPs.capturedLockContent = root.capturedLockContent;
                }
            }
        }
    }

    /** The manifests that can regenerate this lock: the sibling manifest first, then any workspace members it covers. */
    public static List<Path> lockImporters(Accumulator acc, Path packagePath, ProjectState rootPs) {
        List<Path> importers = new ArrayList<>();
        importers.add(packagePath);
        if (rootPs.capturedPackageJson != null) {
            for (Path member : PackageJsonHelper.workspaceMemberPaths(rootPs.capturedPackageJson)) {
                if (!member.equals(packagePath) && acc.projects.containsKey(member)) {
                    importers.add(member);
                }
            }
        }
        return importers;
    }

    /**
     * Decide which catalog entries may be edited in place rather than followed by overwriting the
     * reference in a manifest. An entry is safe only when every consumer of it is one the recipe is
     * upgrading, because moving the entry moves all of them. Anything that cannot be accounted for
     * disqualifies it: a member the workspace declares whose manifest never reached the scan, or a
     * manifest that references the entry but produced no match. The caller then leaves both the
     * manifest and the entry alone, which keeps every member in step at the cost of the upgrade.
     */
    public static void decideCatalogEdits(Accumulator acc, String newVersion) {
        acc.catalogEdits.clear();
        for (Map.Entry<Path, ProjectState> project : acc.projects.entrySet()) {
            ProjectState ps = project.getValue();
            if (ps.skippedProtocols.isEmpty()) {
                continue;
            }
            Path workspacePath = governingWorkspaceFile(acc, project.getKey());
            if (workspacePath == null) {
                continue;
            }
            SourceFile workspaceFile = acc.workspaceFiles.get(workspacePath);
            for (MatchedDependency skipped : ps.skippedProtocols) {
                String catalogName = NodeCatalogs.catalogReference(skipped.getCurrentVersion());
                if (catalogName == null ||
                        NodeCatalogs.findEntry(workspaceFile, catalogName, skipped.getPackageName()) == null ||
                        !everyConsumerIsUpgraded(acc, workspacePath, catalogName, skipped.getPackageName())) {
                    continue;
                }
                acc.catalogEdits.computeIfAbsent(workspacePath, k -> new LinkedHashMap<>())
                        .put(new NodeCatalogs.CatalogEntry(catalogName, skipped.getPackageName()), newVersion);
            }
        }
    }

    /** True when this reference was followed into a catalog entry rather than left alone. */
    public static boolean isFollowedIntoCatalog(Accumulator acc, Path packageJsonPath, MatchedDependency skipped) {
        String catalogName = NodeCatalogs.catalogReference(skipped.getCurrentVersion());
        if (catalogName == null) {
            return false;
        }
        Path workspacePath = governingWorkspaceFile(acc, packageJsonPath);
        Map<NodeCatalogs.CatalogEntry, String> edits =
                workspacePath == null ? null : acc.catalogEdits.get(workspacePath);
        return edits != null &&
                edits.containsKey(new NodeCatalogs.CatalogEntry(catalogName, skipped.getPackageName()));
    }

    private static boolean everyConsumerIsUpgraded(Accumulator acc, Path workspacePath,
                                                   String catalogName, String packageName) {
        Path root = workspacePath.getParent();
        for (ProjectState ps : acc.projects.values()) {
            if (ps.capturedPackageJson == null) {
                continue;
            }
            for (Path member : PackageJsonHelper.workspaceMemberPaths(ps.capturedPackageJson)) {
                if (isUnder(member, root) && !acc.manifests.contains(member)) {
                    return false;
                }
            }
        }
        for (Map.Entry<Path, Map<String, String>> consumer : acc.catalogRefs.entrySet()) {
            if (!isUnder(consumer.getKey(), root) ||
                    !catalogName.equals(consumer.getValue().get(packageName))) {
                continue;
            }
            ProjectState ps = acc.projects.get(consumer.getKey());
            if (ps == null || !isUpgrading(ps, catalogName, packageName)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isUpgrading(ProjectState ps, String catalogName, String packageName) {
        for (MatchedDependency skipped : ps.skippedProtocols) {
            if (packageName.equals(skipped.getPackageName()) &&
                    catalogName.equals(NodeCatalogs.catalogReference(skipped.getCurrentVersion()))) {
                return true;
            }
        }
        return false;
    }

    /** The nearest workspace file at or above a manifest; catalogs are scoped to their own workspace. */
    private static @Nullable Path governingWorkspaceFile(Accumulator acc, Path manifestPath) {
        Path nearest = null;
        int nearestDepth = -1;
        for (Path candidate : acc.workspaceFiles.keySet()) {
            Path root = candidate.getParent();
            int depth = root == null ? 0 : root.getNameCount();
            if (isUnder(manifestPath, root) && depth > nearestDepth) {
                nearest = candidate;
                nearestDepth = depth;
            }
        }
        return nearest;
    }

    private static boolean isUnder(Path path, @Nullable Path root) {
        return root == null || path.startsWith(root);
    }
}
