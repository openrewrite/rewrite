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
    }

    public static final class ProjectState {
        public @Nullable SourceFile capturedPackageJson;
        public @Nullable String capturedLockContent;
        public @Nullable SourceFile modifiedPackageJson;
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
