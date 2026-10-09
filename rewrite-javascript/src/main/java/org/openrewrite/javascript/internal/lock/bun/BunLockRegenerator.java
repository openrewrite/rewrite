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

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.Kind;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.SharedError;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.singletonMap;
import static org.openrewrite.javascript.internal.lock.bun.Package.INVALID_PACKAGE_ID;

/**
 * Port of bun's src/install/PackageManager/install_with_manager.zig (bun 1.3.10) for {@code bun install
 * --lockfile-only}; the lock written must come back unchanged from a second install, or that install is reported.
 */
public final class BunLockRegenerator {

    /**
     * What the engine is given for a Bun project.
     */
    @Value
    @Builder(toBuilder = true)
    public static class Inputs {
        /**
         * The bun.lock; null when the project has none.
         */
        @Nullable
        String lock;

        /**
         * The project's manifests as edited, by location: {@code ""} the root, {@code packages/a} a member or the
         * directory of a {@code file:} dependency.
         */
        @Singular
        Map<String, String> manifests;

        /**
         * The project's own .npmrc and bunfig.toml; the host's user and global ones are not an input.
         */
        @Nullable
        String npmrc;

        @Nullable
        String bunfig;

        /**
         * The project's package-manager files under the root, by path relative to it, with their contents: the
         * patch files {@code patchedDependencies} names and the .env files are read from here.
         */
        @Singular
        Map<String, String> files;

        /**
         * Every path under the root the project holds, relative to it, whether or not its contents are in
         * {@link #files}; null when the host has not said which paths exist.
         */
        @Nullable
        Set<String> paths;

        /**
         * The process environment, which the project's .env files never override; null when the host does not know
         * it.
         */
        @Nullable
        Map<String, String> env;

        @Builder.Default
        Host host = Host.UNKNOWN;

        /**
         * The clock {@code minimumReleaseAge} counts from.
         */
        @NonNull
        Instant now;

        /**
         * The package.json of each directory above the project that bun walks through from where it runs, nearest
         * first, keyed by the project's location from that directory ({@code packages/a}).
         */
        @Singular
        Map<String, String> parentManifests;
    }

    private static final LockOnlyInstall INSTALL = LockOnlyInstall.BUN;

    /**
     * {@code Lockfile.LoadResult}: a lock that was not found, one that failed to load (bun prints the error and
     * ignores it), or one that loaded.
     */
    @Getter
    public static final class LoadResult {
        private final @Nullable Lockfile lockfile;
        private final TextLockfileReader.@Nullable ParseError error;

        private LoadResult(@Nullable Lockfile lockfile, TextLockfileReader.@Nullable ParseError error) {
            this.lockfile = lockfile;
            this.error = error;
        }

        boolean isOk() {
            return lockfile != null;
        }

        /**
         * {@code chooseConfigVersion}: the lock's, 0 for a lock without one, the current version for a new lock.
         */
        int chooseConfigVersion() {
            if (lockfile == null) {
                return 1;
            }
            return lockfile.savedConfigVersion != null ? lockfile.savedConfigVersion : 0;
        }
    }

    /**
     * The install after the root's diff: the package manager holding the lockfile, and the dependency ids the diff
     * enqueued, in order.
     */
    @Getter
    public static final class PendingInstall {
        private final LoadResult loadResult;
        private final PackageManager manager;
        private final int configVersion;
        private final boolean needsNewLockfile;
        private final PackageDiff.@Nullable Summary summary;
        private final List<Integer> enqueued;
        private final int packagesBefore;

        PendingInstall(LoadResult loadResult, PackageManager manager, int configVersion, boolean needsNewLockfile,
                       PackageDiff.@Nullable Summary summary, List<Integer> enqueued, int packagesBefore) {
            this.loadResult = loadResult;
            this.manager = manager;
            this.configVersion = configVersion;
            this.needsNewLockfile = needsNewLockfile;
            this.summary = summary;
            this.enqueued = enqueued;
            this.packagesBefore = packagesBefore;
        }

        Lockfile getLockfile() {
            return manager.lockfile;
        }

        boolean hasPendingWork() {
            return manager.pendingTaskCount() > 0 || !manager.peerDependencies.isEmpty();
        }

        /**
         * {@code cleanWithLogger} then {@code saveLockfile}: the bun.lock text, or null when bun deletes the empty
         * lockfile. Only meaningful once nothing is left to resolve.
         */
        @Nullable
        String save() {
            if (hasPendingWork()) {
                throw new IllegalStateException("the install has dependencies left to resolve");
            }
            return write(Cleaner.clean(manager.lockfile));
        }

        private @Nullable String write(Lockfile cleaned) {
            if (cleaned.isEmpty()) {
                return null;
            }
            return TextLockfileWriter.saveFromBinary(cleaned, configVersion);
        }
    }

    private final Inputs inputs;
    private final ManifestSource registry;

    public BunLockRegenerator(Inputs inputs, ManifestSource registry) {
        this.inputs = inputs;
        this.registry = registry;
    }

    public Result regenerate() {
        String written;
        try {
            int workspaceRoot = findWorkspaceRoot(inputs);
            if (workspaceRoot > 0) {
                StringBuilder up = new StringBuilder("..");
                for (int i = workspaceRoot; i > 1; i--) {
                    up.append("/..");
                }
                throw new CannotKnow("the lock bun writes: it installs at the workspace root " + up +
                                     ", whose workspaces list this project, and writes that root's bun.lock");
            }
            written = install(inputs.getLock());
        } catch (RuntimeException e) {
            return Result.failure(failure(e));
        }
        return INSTALL.verify(written, this::install, BunLockRegenerator::failure);
    }

    /**
     * How many directories above the project bun installs, writing that root's bun.lock; 0 when it installs here or
     * the walk fails, which {@link #regenerate()} reports, as it walks first.
     */
    public static int installRoot(Inputs inputs) {
        try {
            return findWorkspaceRoot(inputs);
        } catch (RuntimeException e) {
            return 0;
        }
    }

    /**
     * PackageManager.init: how many directories above the project bun installs, at the first package.json above
     * with workspaces when they list the project, writing that root's bun.lock; 0 when it installs here.
     */
    private static int findWorkspaceRoot(Inputs inputs) {
        String manifest = inputs.getManifests().get("");
        if (manifest == null) {
            return 0;
        }
        for (Map.Entry<String, String> parent : inputs.getParentManifests().entrySet()) {
            Json.Expr workspaces = Json.parse(parent.getValue()).get("workspaces");
            if (workspaces == null) {
                continue;
            }
            Json.Expr packages = workspaces.isObject() ? workspaces.get("packages") : workspaces;
            if (packages == null || !packages.isArray()) {
                return 0;
            }
            Workspaces names = new Workspaces();
            try {
                names.processNamesArray(new Log(), packages.items(), singletonMap(parent.getKey(), manifest));
            } catch (IllegalArgumentException ignored) {
                // whether bun errs here depends on the other members' manifests, which are not given
            }
            return names.map.containsKey(parent.getKey()) ? parent.getKey().split("/").length : 0;
        }
        return 0;
    }

    private String install(@Nullable String lock) {
        if (lock == null) {
            throw Rejected.discardsLock("the project has no bun.lock");
        }
        Inputs these = lock.equals(inputs.getLock()) ? inputs : inputs.toBuilder().lock(lock).build();
        requireConfigFiles(these);
        Log log = new Log();
        PendingInstall install;
        try {
            install = installWithManager(these, registry, log);
            if (!install.getLoadResult().isOk()) {
                TextLockfileReader.ParseError error = install.getLoadResult().getError();
                throw Rejected.discardsLock(error == null ?
                        "the lock is empty" : error.error + ": " + error.getMessage().split("\n", 2)[0]);
            }
            requirePatchFiles(these, install.getLockfile());
            install.getManager().resolve(install.getPackagesBefore());
        } catch (IllegalArgumentException e) {
            if (e instanceof Json.SyntaxError || e instanceof TextLockfileReader.ParseError || !log.hasErrors()) {
                throw e;
            }
            throw new Rejected(Kind.PM_REJECTS, Reason.MALFORMED_MANIFEST, null, log.errors.get(0));
        }
        PackageManager manager = install.getManager();

        Lockfile beforeClean = manager.lockfile;
        Lockfile cleaned = Cleaner.clean(beforeClean);
        List<String> failed = PackageManager.verifyResolutions(cleaned, manager.options);
        if (!failed.isEmpty()) {
            String first = manager.log.errors.isEmpty() ? "" : manager.log.errors.get(0) + "; ";
            throw new Rejected(Kind.PM_REJECTS, reasonFor(manager.log.errors), failed.get(0).replaceFirst("@[^@]*$", ""),
                    first + failed.get(0) + " failed to resolve");
        }
        if (manager.options.isFrozenLockfile() && !cleaned.eql(beforeClean, cleaned.packages.size())) {
            throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, "lockfile had changes, but lockfile is frozen");
        }
        String written = install.write(cleaned);
        if (written == null) {
            throw new Rejected(Kind.DELETES_LOCK, Reason.RESOLUTION_REQUIRED, null, "the root has no package left");
        }
        return written;
    }

    // init loads the project's .env files, bunfig.toml and .npmrc: one it holds that the host did not read is unknown
    private static void requireConfigFiles(Inputs inputs) {
        if (inputs.getPaths() == null) {
            return;
        }
        for (String file : DotEnv.FILES) {
            if (inputs.getPaths().contains(file) && !inputs.getFiles().containsKey(file)) {
                throw new CannotKnow(file + " (its contents were not read)");
            }
        }
        if (inputs.getBunfig() == null && inputs.getPaths().contains("bunfig.toml")) {
            throw new CannotKnow("bunfig.toml (its contents were not read)");
        }
        if (inputs.getNpmrc() == null && inputs.getPaths().contains(".npmrc")) {
            throw new CannotKnow(".npmrc (its contents were not read)");
        }
    }

    // calcHash: bun joins each patch path onto the root (joinZBuf), so ./ and .. spellings name the same file
    private static void requirePatchFiles(Inputs inputs, Lockfile lockfile) {
        Map<String, String> files = new HashMap<>();
        for (Map.Entry<String, String> file : inputs.getFiles().entrySet()) {
            files.put(RootRelativePath.normalize(file.getKey().replace('\\', '/')), file.getValue());
        }
        Set<String> paths = inputs.getPaths() == null ? null : new HashSet<>();
        if (paths != null) {
            for (String path : inputs.getPaths()) {
                paths.add(RootRelativePath.normalize(path.replace('\\', '/')));
            }
        }
        for (String patch : lockfile.patchedDependencies.values()) {
            String path = RootRelativePath.normalize(patch.replace('\\', '/'));
            String contents = files.get(path);
            if (contents == null) {
                if (paths == null || paths.contains(path) || path.equals("..") || path.startsWith("../")) {
                    throw new CannotKnow("patch file " + patch + " (its contents were not read)");
                }
                throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, "Couldn't find patch file: '" + patch + "'");
            }
            if (contents.isEmpty()) {
                throw Rejected.rejects(Reason.RESOLUTION_REQUIRED,
                        "patchfile " + path + " is empty, please restore or delete it.");
            }
        }
    }

    private static Reason reasonFor(List<String> errors) {
        String first = errors.isEmpty() ? "" : errors.get(0);
        if (first.startsWith("No version matching") || first.contains("\" with tag \"")) {
            return SharedError.NO_MATCHING_VERSION.getReason();
        }
        if (first.startsWith("GET ")) {
            return SharedError.PACKAGE_NOT_FOUND.getReason();
        }
        return Reason.RESOLUTION_REQUIRED;
    }

    private static Failure failure(RuntimeException e) {
        if (e instanceof Rejected) {
            return INSTALL.failure((Rejected) e);
        }
        if (e instanceof CannotKnow) {
            return INSTALL.failure((CannotKnow) e);
        }
        if (e instanceof Json.SyntaxError) {
            return INSTALL.rejects(Reason.MALFORMED_MANIFEST, null,
                    "ParserError: failed to parse package.json: " + e.getMessage());
        }
        throw e;
    }

    static LoadResult loadFromCwd(@Nullable String lock, BunOptions options) {
        if (lock == null) {
            return new LoadResult(null, null);
        }
        try {
            return new LoadResult(TextLockfileReader.load(lock, options), null);
        } catch (TextLockfileReader.ParseError e) {
            return new LoadResult(null, e);
        }
    }

    static PendingInstall installWithManager(Inputs inputs, @Nullable ManifestSource registry) {
        return installWithManager(inputs, registry, new Log());
    }

    /**
     * Up to the resolution of what the diff enqueued. A lock bun would ignore is not installed over.
     */
    static PendingInstall installWithManager(Inputs inputs, @Nullable ManifestSource registry, Log log) {
        HostEnvironment env = HostEnvironment.of(inputs.getEnv());
        BunOptions options = BunOptions.load(inputs.getBunfig(), inputs.getNpmrc(),
                env.with(DotEnv.load(env.known(), inputs.getFiles())));
        LoadResult loadResult = loadFromCwd(inputs.getLock(), options);
        int configVersion = loadResult.chooseConfigVersion();
        Map<String, String> manifests = inputs.getManifests();
        String rootManifest = manifests.get("");
        if (rootManifest == null) {
            throw new IllegalArgumentException("the root package.json is required");
        }

        Lockfile loaded = loadResult.getLockfile();
        PackageManager manager = new PackageManager(loaded != null ? loaded : new Lockfile(), options, log, manifests,
                registry, inputs.getNow().toEpochMilli());
        boolean needsNewLockfile = !loadResult.isOk();
        PackageDiff.Summary summary = null;
        List<Integer> enqueued = new ArrayList<>();
        int packagesBefore = manager.lockfile.packages.size();
        if (!loadResult.isOk() && inputs.getLock() != null) {
            return new PendingInstall(loadResult, manager, configVersion, true, null, enqueued, packagesBefore);
        }

        if (loadResult.isOk()) {
            Package root = manager.lockfile.rootPackage();
            if (root == null || root.dependenciesLen == 0) {
                needsNewLockfile = true;
            } else {
                Json.Expr rootJson = Json.parse(rootManifest);
                Lockfile lockfile = new Lockfile();
                Package maybeRoot = new Package();
                maybeRoot.parseWithJSON(lockfile, options, log, "", rootJson, Features.MAIN, manifests);
                manager.registerAliases(lockfile, maybeRoot);
                int[] mapping = new int[maybeRoot.dependenciesLen];
                Arrays.fill(mapping, INVALID_PACKAGE_ID);
                summary = PackageDiff.generate(options, log, manager.lockfile, lockfile, root, maybeRoot, mapping,
                        manifests);
                if (summary.hasDiffs()) {
                    replaceRoot(manager, lockfile, maybeRoot, mapping, summary, enqueued);
                }
            }
        }

        if (needsNewLockfile) {
            if (options.isFrozenLockfile() && inputs.getLock() != null) {
                throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, "lockfile had changes, but lockfile is frozen");
            }
            Json.Expr rootJson = Json.parse(rootManifest);
            manager.lockfile = new Lockfile();
            Package root = new Package();
            root.parseWithJSON(manager.lockfile, options, log, "", rootJson, Features.MAIN, manifests);
            manager.registerAliases(manager.lockfile, root);
            root = manager.lockfile.appendPackage(root);
            packagesBefore = manager.lockfile.packages.size();
            for (int depId = root.dependenciesOff; depId < root.dependenciesEnd(); depId++) {
                enqueued.add(depId);
            }
            manager.enqueueDependencyList(root.dependenciesOff, root.dependenciesLen);
        } else {
            manager.drainDependencyList();
        }
        return new PendingInstall(loadResult, manager, configVersion, needsNewLockfile, summary, enqueued,
                packagesBefore);
    }

    private static void replaceRoot(PackageManager manager, Lockfile lockfile, Package maybeRoot, int[] mapping,
                                    PackageDiff.Summary summary, List<Integer> enqueued) {
        Lockfile managerLockfile = manager.lockfile;
        List<Dependency> newDependencies = new ArrayList<>(lockfile.dependenciesOf(maybeRoot));
        int off = managerLockfile.dependencies.size();
        int len = newDependencies.size();
        Package managerRoot = managerLockfile.packages.get(0);
        int oldOff = managerRoot.dependenciesOff;
        int oldLen = managerRoot.dependenciesLen;
        managerRoot.dependenciesOff = off;
        managerRoot.dependenciesLen = len;

        Set<Long> allNameHashes = new LinkedHashSet<>();
        if (summary.overridesChanged) {
            allNameHashes.addAll(managerLockfile.overrides.map.keySet());
            allNameHashes.addAll(lockfile.overrides.map.keySet());
        }

        managerLockfile.overrides = lockfile.overrides.cloneMap();
        managerLockfile.catalogs = lockfile.catalogs.cloneMap();
        managerLockfile.trustedDependencies = lockfile.trustedDependencies == null ? null :
                new LinkedHashSet<>(lockfile.trustedDependencies);

        List<Integer> oldResolutions = new ArrayList<>(managerLockfile.resolutions.subList(oldOff, oldOff + oldLen));
        for (int i = 0; i < len; i++) {
            managerLockfile.dependencies.add(newDependencies.get(i).cloneDependency());
            managerLockfile.resolutions.add(mapping[i] != INVALID_PACKAGE_ID ? oldResolutions.get(mapping[i]) :
                    INVALID_PACKAGE_ID);
        }

        managerLockfile.workspacePaths = new LinkedHashMap<>(lockfile.workspacePaths);
        managerLockfile.workspaceVersions = new LinkedHashMap<>(lockfile.workspaceVersions);

        for (Map.Entry<Long, String> entry : lockfile.patchedDependencies.entrySet()) {
            String existing = managerLockfile.patchedDependencies.get(entry.getKey());
            if (existing == null || !existing.equals(entry.getValue())) {
                managerLockfile.patchedDependencies.put(entry.getKey(), entry.getValue());
            }
        }
        managerLockfile.patchedDependencies.keySet().retainAll(lockfile.patchedDependencies.keySet());

        if (summary.overridesChanged && !allNameHashes.isEmpty()) {
            int n = managerLockfile.dependencies.size();
            for (int i = 0; i < n; i++) {
                Dependency dependency = managerLockfile.dependencies.get(i);
                if (allNameHashes.contains(dependency.nameHash)) {
                    managerLockfile.resolutions.set(i, INVALID_PACKAGE_ID);
                    enqueued.add(i);
                    manager.enqueueDependencyWithMain(i, dependency, INVALID_PACKAGE_ID, false);
                }
            }
        }
        if (summary.catalogsChanged) {
            int n = managerLockfile.dependencies.size();
            for (int i = 0; i < n; i++) {
                Dependency dep = managerLockfile.dependencies.get(i);
                if (dep.version.tag == DependencyVersion.Tag.CATALOG) {
                    managerLockfile.resolutions.set(i, INVALID_PACKAGE_ID);
                    enqueued.add(i);
                    manager.enqueueDependencyWithMain(i, dep, INVALID_PACKAGE_ID, false);
                }
            }
        }
        if (summary.add > 0 || summary.update > 0) {
            for (int i = 0; i < mapping.length; i++) {
                if (mapping[i] == INVALID_PACKAGE_ID) {
                    int dependencyId = off + i;
                    enqueued.add(dependencyId);
                    manager.enqueueDependencyWithMain(dependencyId, managerLockfile.dependencies.get(dependencyId),
                            managerLockfile.resolutions.get(dependencyId), false);
                }
            }
        }
    }
}
