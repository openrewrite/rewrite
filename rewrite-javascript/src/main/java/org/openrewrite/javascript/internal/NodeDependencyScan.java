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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.PrintOutputCapture;
import org.openrewrite.SourceFile;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.json.tree.Json;
import org.openrewrite.quark.Quark;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;

/** Shared per-project scan-state for the Node dependency {@code ScanningRecipe}s. */
public final class NodeDependencyScan {

    private NodeDependencyScan() {
    }

    // the four .env files are the ones bun install loads (env_loader.zig loadDefaultFiles for .production)
    private static final List<String> PACKAGE_MANAGER_FILES = Arrays.asList(
            ".npmrc", "bunfig.toml", ".yarnrc", ".yarnrc.yml", "pnpm-workspace.yaml",
            ".env", ".env.local", ".env.production", ".env.production.local");

    public static final class Accumulator {
        public final Map<Path, ProjectState> projects = new HashMap<>();
        public final Map<Path, Path> lockToPackage = new HashMap<>();
        public final Map<Path, String> locks = new HashMap<>();
        public final Map<Path, String> packageManagerFiles = new HashMap<>();
        /** Every file the scan visited: what the host holds of the repository. */
        public final Set<Path> paths = new HashSet<>();
    }

    /**
     * What a lock regeneration reads of the project besides the edited manifest and the lock, by path, and
     * which paths the project holds at all (null when the host has not said).
     */
    @Value
    public static class ProjectFiles {
        public static final ProjectFiles NONE = new ProjectFiles(emptyMap(), emptyMap(), null);

        Map<Path, String> manifests;
        Map<Path, String> packageManagerFiles;
        @Nullable Set<Path> paths;
    }

    /**
     * Note a file the scan visits and keep its contents when it is a package manager's configuration file or a
     * patch file; true when {@code sf} is one. A {@link Quark} has a path and no contents.
     */
    public static boolean capturePackageManagerFile(Accumulator acc, SourceFile sf) {
        acc.paths.add(sf.getSourcePath());
        Path name = sf.getSourcePath().getFileName();
        if (name == null || !PACKAGE_MANAGER_FILES.contains(name.toString()) && !isPatch(sf.getSourcePath())) {
            return false;
        }
        if (!(sf instanceof Quark)) {
            acc.packageManagerFiles.put(sf.getSourcePath(), sf.printAll());
        }
        return true;
    }

    private static boolean isPatch(Path path) {
        return path.toString().endsWith(".patch");
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

    /** Note a lock the scan visits; it belongs to the package.json beside it. */
    public static void captureLock(Accumulator acc, SourceFile lock) {
        Path packagePath = PackageJsonHelper.correspondingPackageJsonPath(lock.getSourcePath());
        acc.projects.computeIfAbsent(packagePath, k -> new ProjectState());
        acc.locks.put(lock.getSourcePath(), lock.printAll());
        acc.lockToPackage.put(lock.getSourcePath(), packagePath);
    }

    /** Give each package.json the content of the lock {@link #lockFor} says its edits regenerate. */
    public static void linkWorkspaceMembers(Accumulator acc) {
        for (Map.Entry<Path, ProjectState> project : acc.projects.entrySet()) {
            Path lock = lockFor(acc, project.getKey());
            project.getValue().capturedLockContent = lock == null ? null : acc.locks.get(lock);
        }
    }

    /**
     * The lock the package manager writes when it installs for an edit of {@code packageJsonPath}. Yarn Berry's
     * project is the nearest directory with a yarn.lock. npm, Yarn classic, pnpm and Bun install at the nearest
     * workspace root that lists the package; that root's lock of the same package manager is the one written, and
     * without one the package's own lock stands, for the engine to find the root itself.
     */
    public static @Nullable Path lockFor(Accumulator acc, Path packageJsonPath) {
        ProjectState ps = acc.projects.get(packageJsonPath);
        PackageManager pm = null;
        if (ps != null && ps.capturedPackageJson != null) {
            NodeResolutionResult marker = ps.capturedPackageJson.getMarkers()
                    .findFirst(NodeResolutionResult.class).orElse(null);
            pm = marker == null ? null : marker.getPackageManager();
        }
        LockFileRegeneration regeneration = LockFileRegeneration.forPackageManager(pm);
        Path own = lockBeside(acc, packageJsonPath, regeneration);
        if (own != null && pm == PackageManager.YarnBerry) {
            return own;
        }
        Path root = nearestListingRoot(acc, packageJsonPath);
        Path rootLock = root == null ? null : lockBeside(acc, root, regeneration);
        return rootLock == null ? own : rootLock;
    }

    private static @Nullable Path lockBeside(Accumulator acc, Path packageJsonPath,
                                             @Nullable LockFileRegeneration regeneration) {
        Path found = null;
        for (Map.Entry<Path, Path> lock : acc.lockToPackage.entrySet()) {
            Path name = lock.getKey().getFileName();
            if (lock.getValue().equals(packageJsonPath) && name != null &&
                (regeneration == null ? found == null || lock.getKey().compareTo(found) < 0 :
                        regeneration.getLockFile().equals(name.toString()))) {
                found = lock.getKey();
            }
        }
        return found;
    }

    private static @Nullable Path nearestListingRoot(Accumulator acc, Path packageJsonPath) {
        Path nearest = null;
        for (Map.Entry<Path, ProjectState> project : acc.projects.entrySet()) {
            Path candidate = project.getKey();
            SourceFile manifest = project.getValue().capturedPackageJson;
            if (!candidate.equals(packageJsonPath) && manifest != null &&
                PackageJsonHelper.workspaceMemberPaths(manifest).contains(packageJsonPath) &&
                (nearest == null || candidate.getNameCount() > nearest.getNameCount() ||
                 candidate.getNameCount() == nearest.getNameCount() && candidate.compareTo(nearest) < 0)) {
                nearest = candidate;
            }
        }
        return nearest;
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

    /**
     * The manifests of the project whose lock an edit of {@code packageJsonPath} regenerates, by path, as
     * edited so far in this run: the package.json beside the lock {@link #lockFor} names and every workspace member
     * in the source set.
     */
    public static Map<Path, String> projectManifests(Accumulator acc, ExecutionContext ctx, Path packageJsonPath) {
        Path lock = lockFor(acc, packageJsonPath);
        Path root = lock == null ? null : acc.lockToPackage.get(lock);
        ProjectState rootPs = root == null ? null : acc.projects.get(root);
        if (rootPs == null || rootPs.capturedPackageJson == null) {
            return emptyMap();
        }
        Map<Path, String> manifests = new LinkedHashMap<>();
        manifests.put(root, print(ctx, root, rootPs));
        for (Path member : PackageJsonHelper.workspaceMemberPaths(rootPs.capturedPackageJson)) {
            ProjectState memberPs = acc.projects.get(member);
            if (memberPs != null && memberPs.capturedPackageJson != null) {
                manifests.put(member, print(ctx, member, memberPs));
            }
        }
        return manifests;
    }

    /**
     * {@link #projectManifests}, the package-manager files beside any of them or in a directory above them, the
     * package.json of each directory above the project, and the paths under them or directly in a directory above.
     */
    public static ProjectFiles projectFiles(Accumulator acc, ExecutionContext ctx, Path packageJsonPath) {
        Map<Path, String> manifests = projectManifests(acc, ctx, packageJsonPath);
        Set<Path> directories = new LinkedHashSet<>();
        for (Path manifest : manifests.keySet()) {
            directories.add(directory(manifest));
        }
        directories.add(directory(packageJsonPath));
        Map<Path, String> files = new LinkedHashMap<>();
        for (Map.Entry<Path, String> file : acc.packageManagerFiles.entrySet()) {
            Path dir = directory(file.getKey());
            if (directories.contains(dir) || above(dir, directories) ||
                isPatch(file.getKey()) && under(dir, directories)) {
                files.put(file.getKey(), file.getValue());
            }
        }
        Path root = directory(manifests.isEmpty() ? packageJsonPath : manifests.keySet().iterator().next());
        for (Map.Entry<Path, ProjectState> project : acc.projects.entrySet()) {
            Path dir = directory(project.getKey());
            if (project.getValue().capturedPackageJson != null && !dir.equals(root) &&
                above(dir, Collections.singleton(root))) {
                files.put(project.getKey(), print(ctx, project.getKey(), project.getValue()));
            }
        }
        Set<Path> paths = new LinkedHashSet<>();
        for (Path path : acc.paths) {
            Path dir = directory(path);
            if (under(dir, directories) || above(dir, directories)) {
                paths.add(path);
            }
        }
        return new ProjectFiles(manifests, files, paths);
    }

    private static Path directory(Path file) {
        Path parent = file.normalize().getParent();
        return parent == null ? Paths.get("") : parent;
    }

    private static boolean under(Path dir, Set<Path> directories) {
        for (Path candidate : directories) {
            if (candidate.toString().isEmpty() || dir.startsWith(candidate)) {
                return true;
            }
        }
        return false;
    }

    private static boolean above(Path dir, Set<Path> directories) {
        for (Path candidate : directories) {
            if (dir.toString().isEmpty() || candidate.startsWith(dir)) {
                return true;
            }
        }
        return false;
    }

    private static String print(ExecutionContext ctx, Path path, ProjectState ps) {
        SourceFile current = PackageJsonHelper.getLiveTree(ctx, path);
        if (current == null) {
            current = ps.modifiedPackageJson != null ? ps.modifiedPackageJson : requireNonNull(ps.capturedPackageJson);
        }
        return current.printAll(new PrintOutputCapture<>(0, PrintOutputCapture.MarkerPrinter.SANITIZED));
    }

    /**
     * The manifests whose edits regenerate {@code lock}, by {@link #lockFor}: the sibling manifest first, then the
     * workspace members it lists.
     */
    public static List<Path> lockImporters(Accumulator acc, Path lock) {
        List<Path> importers = new ArrayList<>();
        Path packagePath = acc.lockToPackage.get(lock);
        ProjectState rootPs = packagePath == null ? null : acc.projects.get(packagePath);
        if (rootPs == null) {
            return importers;
        }
        if (lock.equals(lockFor(acc, packagePath))) {
            importers.add(packagePath);
        }
        if (rootPs.capturedPackageJson != null) {
            for (Path member : PackageJsonHelper.workspaceMemberPaths(rootPs.capturedPackageJson)) {
                if (!member.equals(packagePath) && acc.projects.containsKey(member) &&
                    lock.equals(lockFor(acc, member))) {
                    importers.add(member);
                }
            }
        }
        return importers;
    }
}
