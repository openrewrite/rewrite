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
import org.openrewrite.json.tree.Json;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
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
        public LockFileRegeneration.@Nullable Result regenResult;
        public boolean failureRecorded;
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
}
