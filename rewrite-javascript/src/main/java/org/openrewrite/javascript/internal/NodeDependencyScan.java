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
import org.openrewrite.json.tree.Json;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/** Shared per-project scan-state for the Node dependency {@code ScanningRecipe}s. */
public final class NodeDependencyScan {

    private NodeDependencyScan() {
    }

    public static final class Accumulator {
        public final Map<Path, ProjectState> projects = new HashMap<>();
        public final Map<Path, Path> lockToPackage = new HashMap<>();
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
        /** The package.json revision {@link #edit} was applied to when computing {@link #modifiedPackageJson}. */
        public @Nullable SourceFile editedFrom;
        public @Nullable Function<Json.Document, Json.Document> edit;
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

    /**
     * The modified package.json to return when visiting {@code visited}. Visiting the lock first computes
     * the edit from the package.json captured at scan time; when an earlier recipe in the same run has
     * changed the package.json since, the edit is applied again on top of that change rather than
     * reverting it.
     */
    public static SourceFile modifiedFor(ProjectState ps, SourceFile visited) {
        SourceFile modified = requireNonNull(ps.modifiedPackageJson);
        if (ps.edit == null || ps.editedFrom == visited) {
            return modified;
        }
        return PackageJsonHelper.reapplyEdit(visited, ps.edit, ps.regenResult);
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
     * Decide which catalog entries to edit. A manifest that says {@code catalog:} has delegated its
     * version to the catalog, so moving the entry is what it asked for, and moving it for every member
     * at once is the point of having one. An entry is edited when it exists and does not already hold
     * the constraint, and left alone with the skip reported otherwise.
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
                NodeCatalogs.CatalogEntry entry =
                        new NodeCatalogs.CatalogEntry(catalogName, skipped.getPackageName());
                acc.catalogEdits.computeIfAbsent(workspacePath, k -> new LinkedHashSet<>()).add(entry);
                // Every consumer's lock now disagrees with the entry, so each needs to answer for it.
                for (Path consumer : consumersOf(acc, workspacePath, entry)) {
                    ProjectState consumerPs = acc.projects.get(consumer);
                    if (!consumerPs.catalogEntriesEdited.contains(entry)) {
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

    /** The manifests under this workspace file that reference the entry, whose locks go stale with it. */
    private static List<Path> consumersOf(Accumulator acc, Path workspacePath, NodeCatalogs.CatalogEntry entry) {
        List<Path> consumers = new ArrayList<>();
        for (Map.Entry<Path, ProjectState> project : acc.projects.entrySet()) {
            ProjectState ps = project.getValue();
            if (!workspacePath.equals(governingWorkspaceFile(acc, project.getKey(), ps.capturedPackageJson))) {
                continue;
            }
            for (MatchedDependency skipped : ps.skippedProtocols) {
                if (entry.getPackageName().equals(skipped.getPackageName()) &&
                        entry.getCatalogName().equals(NodeCatalogs.catalogReference(skipped.getCurrentVersion()))) {
                    consumers.add(project.getKey());
                    break;
                }
            }
        }
        return consumers;
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
