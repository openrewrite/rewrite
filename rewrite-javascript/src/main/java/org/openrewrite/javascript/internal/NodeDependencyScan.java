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
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
        /** Only the manifests that reference a catalog, as package name to catalog name. */
        public final Map<Path, Map<String, String>> catalogRefs = new HashMap<>();
        /** Workspace files that can declare catalogs, by path. */
        public final Map<Path, Yaml.Documents> workspaceFiles = new HashMap<>();
        /** The catalog entries judged safe to edit in place, by the workspace file declaring them. */
        public final Map<Path, Set<NodeCatalogs.CatalogEntry>> catalogEdits = new HashMap<>();
        /** Set whenever the inputs to {@link #decideCatalogEdits} change, so the verdict is recomputed. */
        public boolean catalogEditsStale = true;
    }

    public static final class ProjectState {
        public @Nullable SourceFile capturedPackageJson;
        public @Nullable String capturedLockContent;
        public @Nullable SourceFile modifiedPackageJson;
        public @Nullable Set<String> scopesContainingPackage;
        public @Nullable List<MatchedDependency> matchedDeps;
        /** Matched dependencies left alone because their version position holds a specifier protocol. */
        public final List<MatchedDependency> skippedProtocols = new ArrayList<>();
        /** Catalog entries this manifest consumes that the recipe edited, leaving its lock stale. */
        public final List<NodeCatalogs.CatalogEntry> catalogEntriesEdited = new ArrayList<>();
        public LockFileRegeneration.@Nullable Result regenResult;
        public boolean failureRecorded;
        public boolean protocolsReported;
        public boolean catalogStalenessReported;
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
        // Called once per source file, so a repository with catalogs must not pay for the whole verdict
        // on every file it contains. Without a workspace file there is nothing to decide at all, and
        // otherwise the verdict stands until something it reads changes.
        if (acc.workspaceFiles.isEmpty() || !acc.catalogEditsStale) {
            return;
        }
        acc.catalogEditsStale = false;
        acc.catalogEdits.clear();
        Map<Path, Boolean> complete = new HashMap<>();
        for (ProjectState ps : acc.projects.values()) {
            ps.catalogEntriesEdited.clear();
        }
        for (Map.Entry<Path, ProjectState> project : acc.projects.entrySet()) {
            ProjectState ps = project.getValue();
            if (ps.skippedProtocols.isEmpty()) {
                continue;
            }
            Path workspacePath = governingWorkspaceFile(acc, project.getKey(), ps.capturedPackageJson);
            if (workspacePath == null) {
                continue;
            }
            // Depends only on the workspace, so it is decided once rather than per dependency.
            if (!complete.computeIfAbsent(workspacePath, w -> everyDeclaredMemberWasScanned(acc, w))) {
                continue;
            }
            Yaml.Documents workspaceFile = acc.workspaceFiles.get(workspacePath);
            for (MatchedDependency skipped : ps.skippedProtocols) {
                String catalogName = NodeCatalogs.catalogReference(skipped.getCurrentVersion());
                if (catalogName == null) {
                    continue;
                }
                // An entry already holding the constraint is not an edit: recording one would leave every
                // consumer answering for a lock that nothing made stale.
                String current = NodeCatalogs.findEntry(workspaceFile, catalogName, skipped.getPackageName());
                if (current == null || newVersion.equals(current)) {
                    continue;
                }
                List<Path> consumers = consumersOf(acc, workspacePath, catalogName, skipped.getPackageName());
                if (!everyConsumerIsUpgraded(acc, consumers, catalogName, skipped.getPackageName())) {
                    continue;
                }
                NodeCatalogs.CatalogEntry entry =
                        new NodeCatalogs.CatalogEntry(catalogName, skipped.getPackageName());
                acc.catalogEdits.computeIfAbsent(workspacePath, k -> new LinkedHashSet<>()).add(entry);
                // Every consumer's lock now disagrees with the entry, so each needs to answer for it.
                for (Path consumer : consumers) {
                    ProjectState consumerPs = acc.projects.get(consumer);
                    if (consumerPs != null && !consumerPs.catalogEntriesEdited.contains(entry)) {
                        consumerPs.catalogEntriesEdited.add(entry);
                    }
                }
            }
        }
    }

    /** True when this reference was followed into a catalog entry rather than left alone. */
    public static boolean isFollowedIntoCatalog(Accumulator acc, Path packageJsonPath, MatchedDependency skipped) {
        String catalogName = NodeCatalogs.catalogReference(skipped.getCurrentVersion());
        if (catalogName == null) {
            return false;
        }
        ProjectState ps = acc.projects.get(packageJsonPath);
        Path workspacePath = governingWorkspaceFile(acc, packageJsonPath,
                ps == null ? null : ps.capturedPackageJson);
        Set<NodeCatalogs.CatalogEntry> edits =
                workspacePath == null ? null : acc.catalogEdits.get(workspacePath);
        return edits != null &&
                edits.contains(new NodeCatalogs.CatalogEntry(catalogName, skipped.getPackageName()));
    }

    /**
     * A member the workspace declares but whose manifest never reached the scan may well reference an
     * entry; nothing here can tell, so no entry under that workspace can be promised safe.
     */
    private static boolean everyDeclaredMemberWasScanned(Accumulator acc, Path workspacePath) {
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
        return true;
    }

    private static boolean everyConsumerIsUpgraded(Accumulator acc, List<Path> consumers,
                                                   String catalogName, String packageName) {
        for (Path consumer : consumers) {
            ProjectState ps = acc.projects.get(consumer);
            if (ps == null || !isUpgrading(ps, catalogName, packageName)) {
                return false;
            }
        }
        return true;
    }

    /** The manifests under a workspace file whose JSON references the given catalog entry. */
    private static List<Path> consumersOf(Accumulator acc, Path workspacePath,
                                          String catalogName, String packageName) {
        Path root = workspacePath.getParent();
        List<Path> consumers = new ArrayList<>();
        for (Map.Entry<Path, Map<String, String>> manifest : acc.catalogRefs.entrySet()) {
            if (isUnder(manifest.getKey(), root) &&
                    catalogName.equals(manifest.getValue().get(packageName))) {
                consumers.add(manifest.getKey());
            }
        }
        return consumers;
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

    /**
     * The nearest workspace file at or above a manifest, of the kind this manifest's package manager
     * keeps catalogs in. Catalogs are scoped to their own workspace, and a repository can hold both a
     * `pnpm-workspace.yaml` and a `.yarnrc.yml`, so the marker decides which is read.
     */
    private static @Nullable Path governingWorkspaceFile(Accumulator acc, Path manifestPath,
                                                         @Nullable SourceFile packageJson) {
        NodeResolutionResult marker = packageJson == null ? null :
                packageJson.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
        String wanted = marker == null ? null : NodeCatalogs.workspaceFileFor(marker.getPackageManager());
        if (wanted == null) {
            return null;
        }
        Path nearest = null;
        int nearestDepth = -1;
        for (Path candidate : acc.workspaceFiles.keySet()) {
            if (!wanted.equals(candidate.getFileName().toString())) {
                continue;
            }
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
