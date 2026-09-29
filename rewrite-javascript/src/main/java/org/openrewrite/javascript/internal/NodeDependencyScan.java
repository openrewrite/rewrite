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
import org.openrewrite.ExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.json.tree.Json;
import org.openrewrite.yaml.tree.Yaml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
        public final Map<Path, Yaml.Documents> workspaceFiles = new HashMap<>();
        /** The entries to edit, by the workspace file declaring them. */
        public final Map<Path, Set<NodeCatalogs.CatalogEntry>> catalogEdits = new HashMap<>();
        /** Entries a catalog does declare, edited or already current; the rest were not followed. */
        public final Map<Path, Set<NodeCatalogs.CatalogEntry>> catalogsResolved = new HashMap<>();
        /** Cleared once the verdict is computed; set again by anything the verdict reads. */
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
        /** Entries this manifest consumes that were edited, so its lock is now stale. */
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
     * Every declaration of {@code packageName} that holds a specifier protocol and would be overridden
     * by writing into {@code manifestPath}, keyed by the manifest declaring it, {@code manifestPath}
     * itself first.
     * <p>
     * The workspace members {@code manifestPath} governs are walked too, because an override is honoured
     * only in the root manifest and then applies to the whole dependency graph. A root entry therefore
     * wins over a member's reference workspace-wide, for pnpm {@code pnpm.overrides}, npm
     * {@code overrides} and Yarn {@code resolutions} alike.
     * <p>
     * Documents are read rather than markers, because a marker is free to report a resolved version
     * where the manifest holds a reference, and an override written on that basis silently wins over a
     * constraint nobody meant to replace. The root's own document says nothing about a member, so there
     * is no backstop there the way {@code declaredProtocolReference} is one for the same manifest.
     */
    public static Map<Path, List<MatchedDependency>> findProtocolReferences(
            Accumulator acc, ExecutionContext ctx, Path manifestPath, String packageName) {
        Map<Path, List<MatchedDependency>> references = new LinkedHashMap<>();
        collectProtocolReferences(acc, ctx, manifestPath, packageName, references);
        ProjectState ps = acc.projects.get(manifestPath);
        if (ps != null && ps.capturedPackageJson != null) {
            for (Path member : PackageJsonHelper.workspaceMemberPaths(ps.capturedPackageJson)) {
                if (!member.equals(manifestPath)) {
                    collectProtocolReferences(acc, ctx, member, packageName, references);
                }
            }
        }
        return references;
    }

    private static void collectProtocolReferences(Accumulator acc, ExecutionContext ctx, Path manifestPath,
                                                  String packageName,
                                                  Map<Path, List<MatchedDependency>> into) {
        Json.Document doc = documentFor(acc, ctx, manifestPath);
        if (doc == null) {
            return;
        }
        List<MatchedDependency> declarations = PackageJsonHelper.findProtocolDeclarations(doc, packageName);
        if (!declarations.isEmpty()) {
            into.put(manifestPath, declarations);
        }
    }

    /** The current revision of a manifest: what an earlier recipe in this run left, else what was scanned. */
    private static Json.@Nullable Document documentFor(Accumulator acc, ExecutionContext ctx, Path manifestPath) {
        SourceFile live = PackageJsonHelper.getLiveTree(ctx, manifestPath);
        if (live == null) {
            ProjectState ps = acc.projects.get(manifestPath);
            live = ps == null ? null : ps.capturedPackageJson;
        }
        return live instanceof Json.Document ? (Json.Document) live : null;
    }

    /**
     * A manifest that says {@code catalog:} has delegated its version, so moving the entry is what it
     * asked for, and moving it for every member at once is the point of having one.
     */
    public static void decideCatalogEdits(Accumulator acc, String newVersion) {
        // Called once per source file, not once per cycle, so the verdict is computed only when its
        // inputs have changed.
        if (acc.workspaceFiles.isEmpty() || !acc.catalogEditsStale) {
            return;
        }
        acc.catalogEditsStale = false;
        acc.catalogEdits.clear();
        acc.catalogsResolved.clear();
        for (ProjectState ps : acc.projects.values()) {
            ps.catalogEntriesEdited.clear();
        }
        // An entry is shared, so it is decided once and then applied to each of its consumers, rather
        // than reconsidered from every manifest that happens to reference it.
        for (Map.Entry<Path, Set<NodeCatalogs.CatalogEntry>> workspace : referencedEntries(acc).entrySet()) {
            Yaml.Documents workspaceFile = acc.workspaceFiles.get(workspace.getKey());
            for (NodeCatalogs.CatalogEntry entry : workspace.getValue()) {
                SourceFile edited = NodeCatalogs.updateEntry(
                        workspaceFile, entry.getCatalogName(), entry.getPackageName(), newVersion);
                if (edited == null) {
                    continue; // no such entry, so the reference was not followed and is reported as a skip
                }
                acc.catalogsResolved.computeIfAbsent(workspace.getKey(), k -> new LinkedHashSet<>()).add(entry);
                if (edited == workspaceFile) {
                    continue; // already the requested constraint, so nothing is written and no lock is staled
                }
                acc.catalogEdits.computeIfAbsent(workspace.getKey(), k -> new LinkedHashSet<>()).add(entry);
                for (Path consumer : consumersOf(acc, workspace.getKey(), entry)) {
                    acc.projects.get(consumer).catalogEntriesEdited.add(entry);
                }
            }
        }
    }

    /** Every catalog entry referenced from a scanned manifest, grouped by the workspace file declaring it. */
    private static Map<Path, Set<NodeCatalogs.CatalogEntry>> referencedEntries(Accumulator acc) {
        Map<Path, Set<NodeCatalogs.CatalogEntry>> referenced = new LinkedHashMap<>();
        for (Map.Entry<Path, ProjectState> project : acc.projects.entrySet()) {
            ProjectState ps = project.getValue();
            Path workspacePath = ps.skippedProtocols.isEmpty() ?
                    null :
                    governingWorkspaceFile(acc, project.getKey(), ps.capturedPackageJson);
            if (workspacePath == null) {
                continue;
            }
            for (MatchedDependency skipped : ps.skippedProtocols) {
                String catalogName = NodeCatalogs.catalogReference(skipped.getCurrentVersion());
                if (catalogName != null) {
                    referenced.computeIfAbsent(workspacePath, k -> new LinkedHashSet<>())
                            .add(new NodeCatalogs.CatalogEntry(catalogName, skipped.getPackageName()));
                }
            }
        }
        return referenced;
    }

    public static boolean isFollowedIntoCatalog(Accumulator acc, Path packageJsonPath, MatchedDependency skipped) {
        String catalogName = NodeCatalogs.catalogReference(skipped.getCurrentVersion());
        if (catalogName == null) {
            return false;
        }
        ProjectState ps = acc.projects.get(packageJsonPath);
        Path workspacePath = governingWorkspaceFile(acc, packageJsonPath,
                ps == null ? null : ps.capturedPackageJson);
        Set<NodeCatalogs.CatalogEntry> resolved =
                workspacePath == null ? null : acc.catalogsResolved.get(workspacePath);
        return resolved != null &&
                resolved.contains(new NodeCatalogs.CatalogEntry(catalogName, skipped.getPackageName()));
    }

    /** Every manifest referencing the entry, because the edit stales each of their locks. */
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

    /** Nearest, because catalogs are scoped to their own workspace; by manager, because both files can exist. */
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
