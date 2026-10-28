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
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.EcmaScript;
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
 * --lockfile-only}, as the release that built the LST installs ({@link BunRelease}); the lock written must come back
 * unchanged from a second install, or that install is reported.
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
         * The project's own .npmrc and bunfig.toml.
         */
        @Nullable
        String npmrc;

        @Nullable
        String bunfig;

        /**
         * The user's .npmrc, the host's registries, which bun reads after bunfig.toml and before the project's .npmrc;
         * null for none.
         */
        @Nullable
        String userNpmrc;

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

        @Builder.Default
        Host host = Host.UNKNOWN;

        /**
         * The clock {@code minimumReleaseAge} counts from.
         */
        @NonNull
        Instant now;

        /**
         * The directory bun runs in, the edited manifest's, relative to the lock's.
         */
        @Builder.Default
        String runsIn = "";

        /**
         * The package.json of each directory above the one bun runs in, which it walks through, nearest first,
         * keyed by that directory's location from there ({@code packages/a}).
         */
        @Singular
        Map<String, String> parentManifests;

        /**
         * The Bun release that installs: the one that built the LST ({@link BunReleases#installedAs}).
         */
        @Builder.Default
        BunRelease release = BunRelease.PORTED;
    }

    private static final LockOnlyInstall INSTALL = LockOnlyInstall.BUN;

    /**
     * {@code Lockfile.LoadResult}: a lock that was not found, one that failed to load (bun prints the error and
     * ignores it), or one that loaded.
     */
    @Getter
    private static final class LoadResult {
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
     * The install after the root's diff: the package manager holding the lockfile and the package count before it
     * enqueued anything.
     */
    @Getter
    private static final class PendingInstall {
        private final LoadResult loadResult;
        private final PackageManager manager;
        private final int configVersion;
        private final int packagesBefore;

        /**
         * The root's diff against the lock; null when there was none to diff.
         */
        private final PackageDiff.@Nullable Summary summary;

        /**
         * From 1.4.0, the root's and the workspaces' rows before the diff, whose moves the dependents follow.
         */
        private final DirectDependencies directBefore;

        PendingInstall(LoadResult loadResult, PackageManager manager, int configVersion, int packagesBefore,
                       PackageDiff.@Nullable Summary summary, DirectDependencies directBefore) {
            this.loadResult = loadResult;
            this.manager = manager;
            this.configVersion = configVersion;
            this.packagesBefore = packagesBefore;
            this.summary = summary;
            this.directBefore = directBefore;
        }

        Lockfile getLockfile() {
            return manager.lockfile;
        }

        /**
         * The root's diff changes resolutions ({@code manager.summary.changes_resolutions()}); a new lock's does not.
         */
        boolean isChangesResolutions() {
            return summary != null && summary.changesResolutions();
        }

        /**
         * {@code saveLockfile} over the cleaned lockfile: the bun.lock text, or null when bun deletes the empty
         * lockfile.
         */
        @Nullable
        String write(Lockfile cleaned) {
            if (cleaned.isEmpty()) {
                return null;
            }
            return TextLockfileWriter.saveFromBinary(cleaned, configVersion, manager.options.getRelease());
        }
    }

    private final Inputs inputs;
    private final ManifestSource registry;

    public BunLockRegenerator(Inputs inputs, ManifestSource registry) {
        this.inputs = inputs;
        this.registry = registry;
    }

    /**
     * The lock bun writes as the release that installs ({@link Inputs#release}), after a second install over it
     * changed nothing.
     */
    public Result regenerate() {
        String written;
        try {
            INSTALL.requireInstallsAt(findWorkspaceRoot(inputs.getRelease(),
                    inputs.getManifests().get(inputs.getRunsIn()), inputs.getParentManifests()), inputs.getRunsIn(),
                    "bun.lock");
            written = install(inputs.getLock());
        } catch (RuntimeException e) {
            return Result.failure(failure(e));
        }
        return INSTALL.verify(written, this::install, BunLockRegenerator::failure);
    }

    /**
     * Where bun {@code release} run in the project's directory installs and writes its bun.lock; a walk that fails,
     * which {@link #regenerate()} reports as it walks first, stops here.
     */
    public static InstallRoot installRoot(BunRelease release, String runsIn, Map<String, String> manifests,
                                          Map<String, String> parentManifests) {
        try {
            return InstallRoot.at(findWorkspaceRoot(release, manifests.get(runsIn), parentManifests));
        } catch (RuntimeException e) {
            return InstallRoot.STOPS_HERE;
        }
    }

    /**
     * PackageManager.init: how many directories above the one it runs in bun installs, at the first package.json
     * above with workspaces when they list it, writing that root's bun.lock; 0 when it installs there.
     */
    private static int findWorkspaceRoot(BunRelease release, @Nullable String manifest,
                                         Map<String, String> parentManifests) {
        if (manifest == null) {
            return 0;
        }
        for (Map.Entry<String, String> parent : parentManifests.entrySet()) {
            Json.Expr workspaces = Json.parse(parent.getValue(), release).get("workspaces");
            if (workspaces == null) {
                continue;
            }
            Json.Expr packages = workspaces.isObject() ? workspaces.get("packages") : workspaces;
            if (packages == null || !packages.isArray()) {
                return 0;
            }
            Workspaces names = new Workspaces(release);
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
        requireWorkspaceManifests(these);
        Log log = new Log();
        PendingInstall install;
        try {
            install = installWithManager(these.getRelease(), these, registry, log);
            if (!install.getManager().options.isLoadLockfile()) {
                throw Rejected.discardsLock("BUN_CONFIG_SKIP_LOAD_LOCKFILE is set, so bun does not load bun.lock");
            }
            if (!install.getLoadResult().isOk()) {
                TextLockfileReader.ParseError error = install.getLoadResult().getError();
                throw Rejected.discardsLock(error == null ?
                        "the lock is empty" : error.error + ": " + error.getMessage().split("\n", 2)[0]);
            }
            requirePatchFiles(these, install.getLockfile(), install.getManager().options.getRelease());
            install.getManager().resolve(install.getPackagesBefore());
            install.getDirectBefore().redirectDependents(install.getLockfile());
        } catch (IllegalArgumentException e) {
            if (e instanceof Json.SyntaxError || e instanceof TextLockfileReader.ParseError || !log.hasErrors()) {
                throw e;
            }
            throw Rejected.rejects(Reason.MALFORMED_MANIFEST, log.errors.get(0));
        }
        PackageManager manager = install.getManager();

        Lockfile beforeClean = manager.lockfile;
        Lockfile cleaned = Cleaner.clean(beforeClean,
                manager.options.isFrozenLockfile() || !install.isChangesResolutions());
        List<String[]> failed = PackageManager.verifyResolutions(cleaned, manager.options);
        if (!failed.isEmpty()) {
            String first = manager.log.errors.isEmpty() ? "" : manager.log.errors.get(0) + "; ";
            throw Rejected.rejects(reasonFor(manager.log.errors),
                    failed.get(0)[0].replaceFirst("@[^@]*$", ""),
                    first + failed.get(0)[1]);
        }
        if (manager.options.isFrozenLockfile()) {
            BunRelease release = manager.options.getRelease();
            PackageDiff.Summary summary = install.getSummary();
            if (release.toleratesAPrunedCheckoutWhenFrozen() && summary != null && summary.catalogsChanged) {
                throw new CannotKnow("what bun " + release + " makes of a frozen install whose catalogs changed, " +
                                     "which it may take for a pruned checkout (pruned_workspaces.rs)");
            }
            String changedSection = frozenChangedSection(release, summary, these.getManifests().get(""));
            if (changedSection != null || !(release.countsAPlacementBoundAfterLoadingAsAChange() ?
                    cleaned.eqlLoaded(beforeClean, install.getPackagesBefore()) :
                    cleaned.eql(beforeClean, cleaned.packages.size()))) {
                throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, "lockfile had changes, but lockfile is frozen" +
                        (changedSection == null ? "" :
                                "; note: " + changedSection + " in package.json changed since bun.lock was saved"));
            }
            if (release.writesNothingForAFrozenLockfileOnlyInstall()) {
                return lock;
            }
        }
        String written = install.write(cleaned);
        if (written == null) {
            throw Rejected.deletesLock("the root has no package left");
        }
        return written;
    }

    // install_with_manager.rs frozen_changed_section and overrides_field_name, from 1.4.0
    private static @Nullable String frozenChangedSection(BunRelease release, PackageDiff.@Nullable Summary summary,
                                                         @Nullable String rootManifest) {
        if (!release.failsAFrozenInstallWhoseOverridesOrCatalogsChanged() || summary == null) {
            return null;
        }
        if (summary.overridesChanged) {
            return rootManifest != null && "resolutions".equals(OverrideMap.readsFrom(rootManifest)) ?
                    "resolutions" : "overrides";
        }
        return summary.catalogsChanged ? "the catalog" : null;
    }

    // init loads the project's .env files, bunfig.toml and .npmrc: one it holds that the host did not read is unknown
    private static void requireConfigFiles(Inputs inputs) {
        if (inputs.getPaths() == null) {
            return;
        }
        for (String file : DotEnv.FILES) {
            CannotKnow.requireRead(inputs.getFiles().get(file), inputs.getPaths(), file);
        }
        CannotKnow.requireRead(inputs.getBunfig(), inputs.getPaths(), "bunfig.toml");
        CannotKnow.requireRead(inputs.getNpmrc(), inputs.getPaths(), ".npmrc");
    }

    // the root's workspaces name directories whose package.json bun reads: one the host holds without its contents is
    // unknown, where the install over the manifests it was given would report the workspace missing or pass it over
    private static void requireWorkspaceManifests(Inputs inputs) {
        String root = inputs.getManifests().get("");
        if (inputs.getPaths() == null || root == null) {
            return;
        }
        Map<String, String> held = new LinkedHashMap<>(inputs.getManifests());
        Set<String> unread = new LinkedHashSet<>();
        for (String path : inputs.getPaths()) {
            String location = path.endsWith("/package.json") ?
                    path.substring(0, path.length() - "/package.json".length()) : null;
            if (location != null && !held.containsKey(location)) {
                unread.add(location);
                // named after its directory, which no other workspace is
                held.put(location, "{\"name\":" + EcmaScript.quote(location) + "}");
            }
        }
        if (unread.isEmpty()) {
            return;
        }
        Json.Expr packages;
        try {
            Json.Expr workspaces = Json.parse(root).get("workspaces");
            packages = workspaces != null && workspaces.isObject() ? workspaces.get("packages") : workspaces;
        } catch (RuntimeException e) {
            // a root bun cannot parse is the install's to reject
            return;
        }
        if (packages == null || !packages.isArray()) {
            return;
        }
        Workspaces names = new Workspaces(inputs.getRelease());
        try {
            names.processNamesArray(new Log(), packages.items(), held);
        } catch (IllegalArgumentException ignored) {
            // an error of bun's own, which the install reports
        }
        for (String location : names.map.keySet()) {
            if (unread.contains(location)) {
                throw CannotKnow.unread(location + "/package.json");
            }
        }
    }

    // patch_install.zig calcHash: bun joins each patch path onto the root (joinZBuf), so ./ and .. spellings name the
    // same file
    private static void requirePatchFiles(Inputs inputs, Lockfile lockfile, BunRelease release) {
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
            String path = RootRelativePath.normalize(separated(patch, inputs.getHost()));
            String contents = files.get(path);
            if (contents == null) {
                if (paths == null || paths.contains(path) || RootRelativePath.climbsAboveRoot(path)) {
                    throw CannotKnow.unread("patch file " + patch);
                }
                if (holdsBelow(path, paths, files.keySet())) {
                    throw Rejected.rejects(Reason.RESOLUTION_REQUIRED,
                            "failed to read from patch file: EISDIR: Is a directory (pread()) (" + path + ")");
                }
                throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, release.quotesAMissingPatchFile() ?
                        "Couldn't find patch file: '" + patch + "'" : "could not find patch file " + patch);
            }
            if (contents.isEmpty()) {
                throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, "patchfile " + path + " is empty, " +
                        (release.spellsPleaseForAnEmptyPatchFile() ? "please" : "plese") + " restore or delete it.");
            }
        }
    }

    // joinZBuf(.auto) keeps a backslash on POSIX, as recorded; on Windows it separates, which no recording shows
    private static String separated(String patch, Host host) {
        if (patch.indexOf('\\') >= 0 && (host.getOs() == null || "win32".equals(host.getOs()))) {
            throw new CannotKnow("patch file " + patch + " (bun on Windows reads its backslash as a separator, and " +
                                 "the host is not known to be another platform)");
        }
        return patch;
    }

    // a held path below it makes the patch path a directory, which stat accepts and pread refuses
    private static boolean holdsBelow(String dir, Set<String> paths, Set<String> files) {
        for (Set<String> held : Arrays.asList(paths, files)) {
            for (String path : held) {
                if (dir.isEmpty() || path.startsWith(dir + "/")) {
                    return true;
                }
            }
        }
        return false;
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

    // the parser's error, which bun prints after its log: install_with_manager's "failed to parse" for a package.json,
    // Arguments.zig loadConfig's "failed to load bunfig" for bunfig.toml
    private static Failure failure(RuntimeException e) {
        if (e instanceof Toml.SyntaxError) {
            return INSTALL.failure(Rejected.rejects(Reason.MALFORMED_MANIFEST,
                    ((Json.SyntaxError) e).error + ": failed to load bunfig: " + e.getMessage()));
        }
        if (e instanceof Json.SyntaxError) {
            return INSTALL.failure(Rejected.rejects(Reason.MALFORMED_MANIFEST,
                    ((Json.SyntaxError) e).error + ": failed to parse package.json: " + e.getMessage()));
        }
        return INSTALL.failure(e);
    }

    private static LoadResult loadFromCwd(@Nullable String lock, BunOptions options) {
        if (lock == null) {
            return new LoadResult(null, null);
        }
        try {
            return new LoadResult(TextLockfileReader.load(lock, options), null);
        } catch (TextLockfileReader.ParseError e) {
            return new LoadResult(null, e);
        }
    }

    /**
     * Up to the resolution of what the diff enqueued. A lock bun would ignore is not installed over.
     */
    private static PendingInstall installWithManager(BunRelease release, Inputs inputs, ManifestSource registry,
                                                     Log log) {
        BunOptions options = BunOptions.load(release, inputs.getBunfig(), inputs.getUserNpmrc(), inputs.getNpmrc(),
                DotEnv.load(release, HostEnvironment.UNKNOWN, inputs.getFiles()));
        if (options.isReadsTheManifestCache()) {
            throw new CannotKnow("what bun " + release + " resolves from its manifest cache, which bunfig.toml's " +
                                 "install.offline or install.prefer = \"offline\" has it read");
        }
        if (options.getSecurityScanner() != null) {
            throw new CannotKnow("whether bun " + release + " saves the lock past the security scanner " +
                                 options.getSecurityScanner() + " bunfig.toml's install.security.scanner names, " +
                                 "a package's code it runs over the resolution");
        }
        LoadResult loadResult = loadFromCwd(options.isLoadLockfile() ? inputs.getLock() : null, options);
        int configVersion = loadResult.chooseConfigVersion();
        Map<String, String> manifests = inputs.getManifests();
        String rootManifest = manifests.get("");
        if (rootManifest == null) {
            throw new IllegalArgumentException("the root package.json is required");
        }

        Lockfile loaded = loadResult.getLockfile();
        PackageManager manager = new PackageManager(loaded != null ? loaded : new Lockfile(release), options, log,
                manifests, registry, inputs.getNow().toEpochMilli());
        manager.lockfile.markLoadedPackages();
        boolean needsNewLockfile = !loadResult.isOk();
        int packagesBefore = manager.lockfile.packages.size();
        if (!loadResult.isOk() && inputs.getLock() != null) {
            return new PendingInstall(loadResult, manager, configVersion, packagesBefore, null,
                    DirectDependencies.NONE);
        }

        if (options.isFrozenLockfile() && release.toleratesAPrunedCheckoutWhenFrozen() && loaded != null) {
            for (Package pkg : loaded.packages) {
                if (pkg.resolution.tag == Resolution.Tag.WORKSPACE && !manifests.containsKey(pkg.resolution.value)) {
                    throw new CannotKnow("what bun " + release + " makes of a frozen install over a checkout " +
                                         "without the workspace " + pkg.resolution.value + " bun.lock lists " +
                                         "(pruned_workspaces.rs)");
                }
            }
        }

        PackageDiff.Summary summary = null;
        DirectDependencies directBefore = DirectDependencies.NONE;
        if (loadResult.isOk()) {
            Package root = manager.lockfile.rootPackage();
            if (root == null || root.dependenciesLen == 0) {
                needsNewLockfile = true;
            } else {
                Json.Expr rootJson = Json.parse(rootManifest, release);
                Lockfile lockfile = new Lockfile(release);
                Package maybeRoot = new Package();
                maybeRoot.parseWithJSON(lockfile, options, log, "", rootJson, Features.MAIN, manifests);
                manager.registerAliases(lockfile, maybeRoot);
                int[] mapping = new int[maybeRoot.dependenciesLen];
                Arrays.fill(mapping, INVALID_PACKAGE_ID);
                summary = PackageDiff.generate(options, log, manager.lockfile, lockfile, root,
                        maybeRoot, mapping, manifests);
                if (release.redirectsDependentsOfAMovedDirectDependency() && summary.changesResolutions()) {
                    directBefore = DirectDependencies.snapshot(manager.lockfile);
                }
                if (summary.hasDiffs()) {
                    replaceRoot(manager, lockfile, maybeRoot, mapping, summary);
                }
            }
        }

        if (needsNewLockfile) {
            if (options.isFrozenLockfile() && inputs.getLock() != null) {
                throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, "lockfile had changes, but lockfile is frozen");
            }
            Json.Expr rootJson = Json.parse(rootManifest, release);
            manager.lockfile = new Lockfile(release);
            if (release.keepsTheLockfileVersionItRead() && loaded != null) {
                manager.lockfile.textLockfileVersion = loaded.textLockfileVersion;
            }
            Package root = new Package();
            root.parseWithJSON(manager.lockfile, options, log, "", rootJson, Features.MAIN, manifests);
            manager.registerAliases(manager.lockfile, root);
            root = manager.lockfile.appendPackage(root);
            packagesBefore = manager.lockfile.packages.size();
            manager.enqueueDependencyList(root.dependenciesOff, root.dependenciesLen);
        } else {
            manager.drainDependencyList();
        }
        return new PendingInstall(loadResult, manager, configVersion, packagesBefore, summary, directBefore);
    }

    private static void replaceRoot(PackageManager manager, Lockfile lockfile, Package maybeRoot, int[] mapping,
                                    PackageDiff.Summary summary) {
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
            managerLockfile.overrides.appendOverriddenNameHashes(allNameHashes);
            lockfile.overrides.appendOverriddenNameHashes(allNameHashes);
        }

        managerLockfile.overrides = lockfile.overrides.cloneMap(lockfile.release);
        managerLockfile.catalogs = lockfile.catalogs.cloneMap(lockfile.release);
        managerLockfile.trustedDependencies = lockfile.trustedDependencies == null ? null :
                new LinkedHashSet<>(lockfile.trustedDependencies);

        List<Integer> oldResolutions = new ArrayList<>(managerLockfile.resolutions.subList(oldOff, oldOff + oldLen));
        for (int i = 0; i < len; i++) {
            managerLockfile.dependencies.add(newDependencies.get(i).cloneDependency(lockfile.release));
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
                    manager.enqueueDependencyWithMain(i, dependency, INVALID_PACKAGE_ID, false);
                }
            }
        }
        if (summary.catalogsChanged) {
            Set<Long> catalogOverridden = new HashSet<>();
            if (manager.options.getRelease().reresolvesCatalogValuedOverrides()) {
                managerLockfile.overrides.appendCatalogValuedNameHashes(catalogOverridden);
            }
            int n = managerLockfile.dependencies.size();
            for (int i = 0; i < n; i++) {
                Dependency dep = managerLockfile.dependencies.get(i);
                if (dep.version.tag == DependencyVersion.Tag.CATALOG || catalogOverridden.contains(dep.nameHash)) {
                    managerLockfile.resolutions.set(i, INVALID_PACKAGE_ID);
                    manager.enqueueDependencyWithMain(i, dep, INVALID_PACKAGE_ID, false);
                }
            }
        }
        if (summary.add > 0 || summary.update > 0) {
            for (int i = 0; i < mapping.length; i++) {
                if (mapping[i] == INVALID_PACKAGE_ID) {
                    int dependencyId = off + i;
                    manager.enqueueDependencyWithMain(dependencyId, managerLockfile.dependencies.get(dependencyId),
                            managerLockfile.resolutions.get(dependencyId), false);
                }
            }
        }
    }
}
