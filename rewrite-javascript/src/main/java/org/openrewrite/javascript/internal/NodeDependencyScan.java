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

import lombok.Getter;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.SourceFile;
import org.openrewrite.Tree;
import org.openrewrite.javascript.internal.lock.Spec;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.PackageManager;
import org.openrewrite.javascript.marker.NodeRuntime;
import org.openrewrite.json.JsonIsoVisitor;
import org.openrewrite.json.tree.Json;
import org.openrewrite.marker.Marker;
import org.openrewrite.marker.Markers;
import org.openrewrite.quark.Quark;
import org.openrewrite.tree.ParseError;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/** Shared per-project scan-state for the Node dependency {@code ScanningRecipe}s. */
public final class NodeDependencyScan {

    private NodeDependencyScan() {
    }

    public static final class Accumulator {
        public final Map<Path, ProjectState> projects = new TreeMap<>();
        public final Map<Path, Path> lockToPackage = new HashMap<>();
        public final Map<Path, String> locks = new HashMap<>();
        public final Map<Path, String> packageManagerFiles = new HashMap<>();
        /** Every file the scan visited: what the host holds of the repository. */
        public final Set<Path> paths = new HashSet<>();
        /** Each package manager's answer to where it installs when run beside a package.json, by {@link #lockFor}. */
        final Map<Path, LockFileRegeneration.InstallRoot> installRoots = new HashMap<>();
        /** A file read only beside a lock, kept until the scan ends and the locks its edits write are known. */
        final Map<Path, String> besideLock = new HashMap<>();
        /** Each lock's importers by {@link #lockFor}, once asked for; null until then and while the scan goes on. */
        @Nullable Map<Path, List<Path>> importers;
        /** The package manager that installs each manifest, by {@link #packageManager}, once asked for. */
        final Map<Path, Optional<PackageManager>> packageManagers = new HashMap<>();
    }

    /**
     * What a lock regeneration reads of the project besides the edited manifest and the lock, by path, and which
     * paths the project holds at all (null when the host has not said).
     */
    @Value
    public static class ProjectFiles {
        public static final ProjectFiles NONE = new ProjectFiles(emptyMap(), emptyMap(), null, null);

        Map<Path, String> manifests;
        Map<Path, String> packageManagerFiles;
        @Nullable Set<Path> paths;

        /**
         * The package manager that installs the edited manifest, by {@link #packageManager}; null to take its
         * marker's.
         */
        @Nullable PackageManager packageManager;
    }

    /**
     * Note a file the scan visits and keep its contents when a package manager reads it: true when {@code sf} is
     * such a file. One read only beside a lock is kept when the scan ends; a {@link Quark} has no contents, nor has
     * a {@link ParseError} without text.
     */
    public static boolean capturePackageManagerFile(Accumulator acc, SourceFile sf) {
        Path path = sf.getSourcePath();
        acc.paths.add(path);
        Path name = path.getFileName();
        if (name == null || !readByAny(name.toString())) {
            return false;
        }
        // a parse error holds no text where the host could not read the file either
        if (!(sf instanceof Quark) && !(sf instanceof ParseError && ((ParseError) sf).getText().isEmpty())) {
            if (readAwayFromLock(name.toString())) {
                acc.packageManagerFiles.put(path, sf.printAll());
            } else {
                acc.besideLock.put(path, sf.printAll());
            }
        }
        return true;
    }

    private static boolean readByAny(String fileName) {
        for (LockFileRegeneration regeneration : LockFileRegeneration.ALL) {
            if (regeneration.reads(fileName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean readAwayFromLock(String fileName) {
        for (LockFileRegeneration regeneration : LockFileRegeneration.ALL) {
            if (regeneration.reads(fileName) && !regeneration.readsOnlyBesideLock(fileName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean readBesideOneOf(Path file, Set<Path> locks) {
        String name = String.valueOf(file.getFileName());
        for (LockFileRegeneration regeneration : LockFileRegeneration.ALL) {
            if (regeneration.readsOnlyBesideLock(name) &&
                locks.contains(file.resolveSibling(regeneration.getLockFile()))) {
                return true;
            }
        }
        return false;
    }

    private static boolean readBy(@Nullable LockFileRegeneration regeneration, Path file) {
        String name = String.valueOf(file.getFileName());
        return regeneration == null ? readAwayFromLock(name) : regeneration.reads(name);
    }

    // a member's manifest in another format than package.json, kept under the project as a patch is
    private static boolean readAsManifest(@Nullable LockFileRegeneration regeneration, Path file) {
        String name = String.valueOf(file.getFileName());
        if (regeneration != null) {
            return regeneration.readsAsManifest(name);
        }
        for (LockFileRegeneration any : LockFileRegeneration.ALL) {
            if (any.readsAsManifest(name)) {
                return true;
            }
        }
        return false;
    }

    public static final class ProjectState {
        public @Nullable Manifest capturedPackageJson;
        public @Nullable String capturedLockContent;
    }

    /**
     * A package.json kept in place of its tree: its text, the dependencies it declares, and what the recipes read of
     * its markers, which parsing the text again does not give back.
     */
    public static final class Manifest {
        @Getter
        private final Path path;

        @Getter
        private final String text;

        private final boolean json;

        /** Its dependency maps, by scope then name. */
        @Getter
        private final Map<String, Map<String, String>> declared;

        /** Whether it holds a NodeResolutionResult. */
        @Getter
        private final boolean marked;

        /** The package manager its NodeResolutionResult names. */
        @Getter
        private final @Nullable PackageManager packageManager;

        private final @Nullable List<NodeResolutionResult.Npmrc> npmrcConfigs;

        @Getter
        private final @Nullable NodeRuntime runtime;

        private final boolean resolves;

        // the values its parser read string literals as, by how they are written, where that is not the text between
        // their quotes: the TypeScript parser reads escapes, the Java one, which parses the text again, does not
        private final Map<String, Object> escaped = new HashMap<>();

        private @Nullable List<Path> linkTargets;

        private Manifest(SourceFile packageJson, @Nullable String resolved) {
            NodeResolutionResult marker = packageJson.getMarkers().findFirst(NodeResolutionResult.class).orElse(null);
            this.path = packageJson.getSourcePath();
            this.text = PackageJsonHelper.printed(packageJson);
            this.json = packageJson instanceof Json.Document;
            this.declared = PackageJsonHelper.declaredDependencies(packageJson);
            this.marked = marker != null;
            this.packageManager = marker == null ? null : marker.getPackageManager();
            this.npmrcConfigs = marker == null ? null : marker.getNpmrcConfigs();
            this.runtime = packageJson.getMarkers().findFirst(NodeRuntime.class).orElse(null);
            this.resolves = resolved == null || resolves(marker, resolved);
            if (json) {
                new JsonIsoVisitor<Map<String, Object>>() {
                    @Override
                    public Json.Literal visitLiteral(Json.Literal literal, Map<String, Object> values) {
                        String source = literal.getSource();
                        if (literal.getValue() instanceof String && source.length() > 1 &&
                            !source.substring(1, source.length() - 1).equals(literal.getValue())) {
                            values.put(source, literal.getValue());
                        }
                        return literal;
                    }
                }.visit(packageJson, escaped);
            }
        }

        public static Manifest of(SourceFile packageJson) {
            return new Manifest(packageJson, null);
        }

        /**
         * {@code packageJson} kept with whether its marker resolves {@code resolved}, as {@link #resolves()} answers.
         */
        public static Manifest of(SourceFile packageJson, @Nullable String resolved) {
            return new Manifest(packageJson, resolved);
        }

        // whether the lock the marker records resolves name; true when it records no resolutions at all
        private static boolean resolves(@Nullable NodeResolutionResult marker, String name) {
            if (marker == null || marker.getResolvedDependencies().isEmpty()) {
                return true;
            }
            for (NodeResolutionResult.ResolvedDependency resolved : marker.getResolvedDependencies()) {
                if (name.equals(resolved.getName())) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Whether the lock its marker records resolves the package it was kept with; true when it records no
         * resolutions at all, or none was asked about.
         */
        public boolean resolves() {
            return resolves;
        }

        /**
         * Its NodeResolutionResult made again with no more than the lock engine reads of it, the package manager and
         * the npmrc files; null without one.
         */
        public @Nullable NodeResolutionResult marker() {
            return marked ? new NodeResolutionResult(Tree.randomId(), null, null, null, path.toString(), null,
                    emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), packageManager, null,
                    npmrcConfigs) : null;
        }

        /** Its text parsed again, with its NodeRuntime and {@link #marker()}; null when it is no JSON. */
        public Json.@Nullable Document document() {
            return document(text);
        }

        /** {@code text}, another revision of it, parsed as {@link #document()} is. */
        public Json.@Nullable Document document(String text) {
            Json.Document document = parse(text);
            if (document == null) {
                return null;
            }
            List<Marker> markers = new ArrayList<>(2);
            NodeResolutionResult marker = marker();
            if (marker != null) {
                markers.add(marker);
            }
            if (runtime != null) {
                markers.add(runtime);
            }
            return document.withMarkers(Markers.build(markers));
        }

        /**
         * {@code text}, another revision of it, parsed with its string literals read as its own parser read them, and
         * no markers; null when either is no JSON.
         */
        public Json.@Nullable Document parse(String text) {
            Json.Document document = json ? PackageJsonHelper.parseJson(path, text) : null;
            if (document == null || escaped.isEmpty()) {
                return document;
            }
            return (Json.Document) new JsonIsoVisitor<Integer>() {
                @Override
                public Json.Literal visitLiteral(Json.Literal literal, Integer p) {
                    Object value = escaped.get(literal.getSource());
                    return value == null ? literal : literal.withValue(value);
                }
            }.visitNonNull(document, 0);
        }

        // the package.json of each directory in the repository that a dependency of the manifest names, once
        List<Path> linkTargets() {
            if (linkTargets == null) {
                linkTargets = NodeDependencyScan.linkTargets(path, declared);
            }
            return linkTargets;
        }
    }

    /** Note a lock the scan visits; it belongs to the package.json beside it. */
    public static void captureLock(Accumulator acc, SourceFile lock) {
        Path packagePath = PackageJsonHelper.correspondingPackageJsonPath(lock.getSourcePath());
        acc.projects.computeIfAbsent(packagePath, k -> new ProjectState());
        acc.locks.put(lock.getSourcePath(), lock.printAll());
        acc.lockToPackage.put(lock.getSourcePath(), packagePath);
    }

    /**
     * Give each package.json the content of the lock {@link #lockFor} says its edits regenerate, and keep the files
     * read only beside a lock that lie beside one of those.
     */
    public static void linkWorkspaceMembers(Accumulator acc) {
        acc.installRoots.clear();
        acc.packageManagers.clear();
        acc.importers = null;
        Set<Path> written = new HashSet<>();
        for (Map.Entry<Path, ProjectState> project : acc.projects.entrySet()) {
            Path lock = lockFor(acc, project.getKey());
            project.getValue().capturedLockContent = lock == null ? null : acc.locks.get(lock);
            if (lock != null) {
                written.add(lock);
            }
        }
        for (Map.Entry<Path, String> file : acc.besideLock.entrySet()) {
            if (readBesideOneOf(file.getKey(), written)) {
                acc.packageManagerFiles.put(file.getKey(), file.getValue());
            }
        }
        acc.besideLock.clear();
    }

    /**
     * The lock the package manager writes for an edit of {@code packageJsonPath}, by its own walk from there, if held;
     * a walk that stops goes to the lock it names, else the nearest, whose regeneration reports why.
     */
    public static @Nullable Path lockFor(Accumulator acc, Path packageJsonPath) {
        ProjectState ps = acc.projects.get(packageJsonPath);
        LockFileRegeneration regeneration = LockFileRegeneration.forPackageManager(packageManager(acc,
                packageJsonPath));
        Path own = lockBeside(acc, packageJsonPath, regeneration);
        if (regeneration == null || ps == null || ps.capturedPackageJson == null) {
            return own;
        }
        LockFileRegeneration.InstallRoot walked = acc.installRoots.computeIfAbsent(packageJsonPath,
                k -> installRoot(acc, packageJsonPath, ps, own, regeneration));
        Path lock = lockBeside(acc, up(packageJsonPath, walked.getDepth()), regeneration);
        if (lock == null && walked.isStopped()) {
            lock = own != null ? own : nearestLockAbove(acc, packageJsonPath, regeneration);
        }
        return lock;
    }

    /**
     * The package.json of the directory {@code pm} installs in when run from {@code packageJsonPath}'s, by its own
     * walk; where the walk stops, that of the lock the edit lands in, else {@code packageJsonPath}.
     */
    public static Path installRootOf(Accumulator acc, Path packageJsonPath, @Nullable PackageManager pm) {
        ProjectState ps = acc.projects.get(packageJsonPath);
        LockFileRegeneration regeneration = LockFileRegeneration.forPackageManager(pm);
        if (ps == null || ps.capturedPackageJson == null || regeneration == null) {
            return packageJsonPath;
        }
        LockFileRegeneration.InstallRoot walked = pm == packageManager(acc, packageJsonPath) ?
                acc.installRoots.computeIfAbsent(packageJsonPath, k -> installRoot(acc, packageJsonPath, ps,
                        lockBeside(acc, packageJsonPath, regeneration), regeneration)) :
                installRoot(acc, packageJsonPath, ps, lockBeside(acc, packageJsonPath, regeneration), regeneration);
        if (!walked.isStopped()) {
            return up(packageJsonPath, walked.getDepth());
        }
        Path lock = lockFor(acc, packageJsonPath);
        Path root = lock == null ? null : acc.lockToPackage.get(lock);
        return root == null ? packageJsonPath : root;
    }

    /**
     * The package manager that installs {@code packageJsonPath}: its marker's where a lock of that package manager
     * lies beside it, and for a manifest parsed without its marker, the one whose lock lies beside it. A manifest
     * without such a lock is installed by the nearest package manager above that holds its own lock and, run in the
     * manifest's directory, installs there or stops on the way; a host that parsed a workspace member beside its
     * installed node_modules marked it as npm's, whatever its workspace is. Else its marker's.
     */
    public static @Nullable PackageManager packageManager(Accumulator acc, Path packageJsonPath) {
        ProjectState ps = acc.projects.get(packageJsonPath);
        if (ps == null || ps.capturedPackageJson == null) {
            return null;
        }
        return acc.packageManagers.computeIfAbsent(packageJsonPath,
                k -> Optional.ofNullable(packageManager(acc, packageJsonPath, ps))).orElse(null);
    }

    private static @Nullable PackageManager packageManager(Accumulator acc, Path packageJsonPath, ProjectState ps) {
        Manifest marked = marked(ps);
        PackageManager named = marked == null ? lockWriterBeside(acc, packageJsonPath) : marked.getPackageManager();
        LockFileRegeneration own = LockFileRegeneration.forPackageManager(named);
        if (own != null && lockBeside(acc, packageJsonPath, own) != null) {
            return named;
        }
        Path dir = directory(packageJsonPath);
        int segments = dir.toString().isEmpty() ? 0 : dir.getNameCount();
        for (int depth = 1; depth <= segments; depth++) {
            Path above = up(packageJsonPath, depth);
            Manifest aboveMarked = marked(acc.projects.get(above));
            PackageManager pm = aboveMarked == null ? lockWriterBeside(acc, above) : aboveMarked.getPackageManager();
            LockFileRegeneration regeneration = LockFileRegeneration.forPackageManager(pm);
            if (regeneration == null || lockBeside(acc, above, regeneration) == null) {
                continue;
            }
            if (pm == named) {
                return named;
            }
            LockFileRegeneration.InstallRoot walked = installRoot(acc, packageJsonPath, ps, null, regeneration);
            if (walked.isStopped() || walked.getDepth() == depth) {
                return pm;
            }
        }
        return named;
    }

    // the package manager a host's parser names by the lock beside a manifest, in its order of precedence
    private static @Nullable PackageManager lockWriterBeside(Accumulator acc, Path packageJsonPath) {
        for (LockFileRegeneration regeneration : Arrays.asList(LockFileRegeneration.NPM, LockFileRegeneration.BUN,
                LockFileRegeneration.PNPM, LockFileRegeneration.YARN_BERRY, LockFileRegeneration.YARN_CLASSIC)) {
            Path lock = lockBeside(acc, packageJsonPath, regeneration);
            String text = lock == null ? null : acc.locks.get(lock);
            if (text != null && regeneration.writes(text)) {
                return regeneration.getPackageManager();
            }
        }
        return null;
    }

    // the scanned manifest when it holds a NodeResolutionResult
    private static @Nullable Manifest marked(@Nullable ProjectState ps) {
        return ps == null || ps.capturedPackageJson == null || !ps.capturedPackageJson.isMarked() ? null :
                ps.capturedPackageJson;
    }

    private static LockFileRegeneration.InstallRoot installRoot(Accumulator acc, Path packageJsonPath,
                                                                ProjectState ps, @Nullable Path lock,
                                                                LockFileRegeneration regeneration) {
        Manifest captured = requireNonNull(ps.capturedPackageJson);
        String manifest = print(p -> null, packageJsonPath, ps);
        ProjectFiles files = projectFiles(acc, p -> null, packageJsonPath, singletonMap(packageJsonPath, manifest),
                regeneration.getPackageManager());
        return regeneration.installRoot(LockFileRegeneration.Request.builder()
                .packageJson(manifest)
                .lock(lock == null ? null : acc.locks.get(lock))
                .marker(captured.marker())
                .runtime(captured.getRuntime())
                .packageJsonPath(packageJsonPath)
                .manifests(files.getManifests())
                .packageManagerFiles(files.getPackageManagerFiles())
                .projectPaths(files.getPaths())
                .build());
    }

    // the package.json depth directories above packageJsonPath's; its own beyond the repository's root
    private static Path up(Path packageJsonPath, int depth) {
        Path dir = directory(packageJsonPath);
        int segments = dir.toString().isEmpty() ? 0 : dir.getNameCount();
        if (depth <= 0 || depth > segments) {
            return packageJsonPath;
        }
        return (depth == segments ? Paths.get("") : dir.subpath(0, segments - depth)).resolve("package.json");
    }

    private static @Nullable Path nearestLockAbove(Accumulator acc, Path packageJsonPath,
                                                   LockFileRegeneration regeneration) {
        Path dir = directory(packageJsonPath);
        int segments = dir.toString().isEmpty() ? 0 : dir.getNameCount();
        for (int depth = 1; depth <= segments; depth++) {
            Path lock = lockBeside(acc, up(packageJsonPath, depth), regeneration);
            if (lock != null) {
                return lock;
            }
        }
        return null;
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

    // the package.json beside the lock lockFor names and every package.json the scan read under its directory: the
    // package manager maps the root's workspaces over them itself
    private static Map<Path, String> projectManifests(Accumulator acc, Function<Path, @Nullable String> before,
                                                      Path packageJsonPath) {
        Path lock = lockFor(acc, packageJsonPath);
        Path root = lock == null ? null : acc.lockToPackage.get(lock);
        ProjectState rootPs = root == null ? null : acc.projects.get(root);
        if (rootPs == null || rootPs.capturedPackageJson == null) {
            return emptyMap();
        }
        Map<Path, String> manifests = new LinkedHashMap<>();
        manifests.put(root, print(before, root, rootPs));
        Set<Path> rootDir = Collections.singleton(directory(root));
        for (Map.Entry<Path, ProjectState> project : acc.projects.entrySet()) {
            if (project.getValue().capturedPackageJson != null && under(directory(project.getKey()), rootDir)) {
                manifests.putIfAbsent(project.getKey(), print(before, project.getKey(), project.getValue()));
            }
        }
        return manifests;
    }

    /**
     * What the regeneration for an edit of {@code packageJsonPath} reads besides it, each text as {@code before} gives
     * it (null: as scanned): the lock's manifests, files beside or above them, the other locks, the paths walked.
     */
    public static ProjectFiles projectFiles(Accumulator acc, Function<Path, @Nullable String> before,
                                            Path packageJsonPath) {
        return projectFiles(acc, before, packageJsonPath, projectManifests(acc, before, packageJsonPath),
                packageManager(acc, packageJsonPath));
    }

    private static ProjectFiles projectFiles(Accumulator acc, Function<Path, @Nullable String> before,
                                             Path packageJsonPath, Map<Path, String> manifests,
                                             @Nullable PackageManager pm) {
        LockFileRegeneration regeneration = LockFileRegeneration.forPackageManager(pm);
        Set<Path> directories = new LinkedHashSet<>();
        for (Path manifest : manifests.keySet()) {
            directories.add(directory(manifest));
        }
        directories.add(directory(packageJsonPath));
        Map<Path, String> files = new LinkedHashMap<>();
        for (Map.Entry<Path, String> file : acc.packageManagerFiles.entrySet()) {
            Path dir = directory(file.getKey());
            if (!readBy(regeneration, file.getKey())) {
                continue;
            }
            // a patch file is wherever the manifest's patchedDependencies says, above the lock too
            if (directories.contains(dir) || above(dir, directories) || file.getKey().toString().endsWith(".patch") ||
                readAsManifest(regeneration, file.getKey()) && under(dir, directories)) {
                String text = before.apply(file.getKey());
                files.put(file.getKey(), text == null ? file.getValue() : text);
            }
        }
        Path root = directory(manifests.isEmpty() ? packageJsonPath : manifests.keySet().iterator().next());
        Path edited = directory(packageJsonPath);
        for (Map.Entry<Path, String> lock : acc.locks.entrySet()) {
            if (regeneration != null && directory(lock.getKey()).equals(root) &&
                regeneration.writesBesideLock(String.valueOf(lock.getKey().getFileName()))) {
                String text = before.apply(lock.getKey());
                files.put(lock.getKey(), text == null ? lock.getValue() : text);
            }
        }
        if (regeneration != null && regeneration.readsOtherLocks()) {
            Path own = root.resolve(regeneration.getLockFile());
            for (Map.Entry<Path, String> lock : acc.locks.entrySet()) {
                if (!lock.getKey().equals(own) &&
                    regeneration.getLockFile().equals(String.valueOf(lock.getKey().getFileName()))) {
                    files.put(lock.getKey(), lock.getValue());
                }
            }
        }
        // the walk of a package manager maps an ancestor's workspaces over every package.json under it
        Set<Path> ancestors = new LinkedHashSet<>();
        for (Path path : acc.paths) {
            Path dir = directory(path);
            if (isPackageJson(path) && !dir.equals(root) && above(dir, Collections.singleton(root))) {
                ancestors.add(dir);
            }
        }
        Set<Path> roots = new LinkedHashSet<>(Arrays.asList(root, edited));
        Set<Path> paths = new LinkedHashSet<>();
        for (Path path : acc.paths) {
            Path dir = directory(path);
            if (under(dir, roots) || above(dir, roots) || isPackageJson(path) && under(dir, ancestors)) {
                paths.add(path);
            }
        }
        // each package.json among those paths that the scan read, with its text
        for (Map.Entry<Path, ProjectState> project : acc.projects.entrySet()) {
            Path dir = directory(project.getKey());
            if (project.getValue().capturedPackageJson != null && !manifests.containsKey(project.getKey()) &&
                (under(dir, roots) || above(dir, roots) || under(dir, ancestors))) {
                files.put(project.getKey(), print(before, project.getKey(), project.getValue()));
            }
        }
        linkTargets(acc, before, manifests.isEmpty() ? Collections.singleton(packageJsonPath) : manifests.keySet(),
                files, paths);
        return new ProjectFiles(manifests, files, paths, pm);
    }

    // a dependency on a directory makes the package manager read the package.json there, wherever in the repository
    // it is, and that one's dependencies on directories in turn
    private static void linkTargets(Accumulator acc, Function<Path, @Nullable String> before, Set<Path> manifests,
                                    Map<Path, String> files, Set<Path> paths) {
        Deque<Path> unfollowed = new ArrayDeque<>(manifests);
        Set<Path> followed = new HashSet<>(manifests);
        while (!unfollowed.isEmpty()) {
            ProjectState ps = acc.projects.get(unfollowed.poll());
            if (ps == null || ps.capturedPackageJson == null) {
                continue;
            }
            for (Path target : ps.capturedPackageJson.linkTargets()) {
                if (!followed.add(target)) {
                    continue;
                }
                if (acc.paths.contains(target)) {
                    paths.add(target);
                }
                ProjectState targetPs = acc.projects.get(target);
                if (targetPs != null && targetPs.capturedPackageJson != null) {
                    files.putIfAbsent(target, print(before, target, targetPs));
                    unfollowed.add(target);
                }
            }
        }
    }

    // the package.json of each directory in the repository that a dependency of the manifest names
    private static List<Path> linkTargets(Path manifest, Map<String, Map<String, String>> declared) {
        List<Path> targets = new ArrayList<>();
        Path dir = directory(manifest);
        for (Map<String, String> scope : declared.values()) {
            for (Map.Entry<String, String> dependency : scope.entrySet()) {
                String value = dependency.getValue();
                if (value.indexOf('/') < 0 && value.indexOf(':') < 0 && !value.startsWith(".")) {
                    continue;
                }
                Spec spec = Spec.ofDependencyOrNull(dependency.getKey(), value);
                String named = spec == null ? null : spec.getDirectory();
                if (named == null) {
                    continue;
                }
                Path target = dir.resolve(named).normalize();
                if (!target.startsWith("..")) {
                    targets.add(target.resolve("package.json"));
                }
            }
        }
        return targets;
    }

    private static boolean isPackageJson(Path path) {
        return "package.json".equals(String.valueOf(path.getFileName()));
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

    // the manifest as the caller's recipe reads it, else as scanned
    private static String print(Function<Path, @Nullable String> before, Path path, ProjectState ps) {
        String text = before.apply(path);
        return text != null ? text : requireNonNull(ps.capturedPackageJson).getText();
    }

    /**
     * The manifests whose edits regenerate {@code lock}, by {@link #lockFor}: the sibling manifest first, then every
     * other one the package manager's own walk leads there from.
     */
    public static List<Path> lockImporters(Accumulator acc, Path lock) {
        if (acc.importers == null) {
            Map<Path, List<Path>> importers = new HashMap<>();
            for (Path project : acc.projects.keySet()) {
                Path written = lockFor(acc, project);
                if (written != null) {
                    List<Path> of = importers.computeIfAbsent(written, k -> new ArrayList<>());
                    if (project.equals(acc.lockToPackage.get(written))) {
                        of.add(0, project);
                    } else {
                        of.add(project);
                    }
                }
            }
            acc.importers = importers;
        }
        List<Path> importers = acc.importers.get(lock);
        return importers == null ? emptyList() : unmodifiableList(importers);
    }
}
