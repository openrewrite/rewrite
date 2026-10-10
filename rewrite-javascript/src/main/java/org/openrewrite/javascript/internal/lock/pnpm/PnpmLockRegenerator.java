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
package org.openrewrite.javascript.internal.lock.pnpm;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.SharedError;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Port of pnpm 11.2.2's {@code pnpm install --lockfile-only} after {@link Main}: @pnpm/installing.commands
 * installDeps and recursive.ts, then @pnpm/installing.deps-installer install/index.ts.
 */
public final class PnpmLockRegenerator {

    private static final LockOnlyInstall INSTALL = LockOnlyInstall.PNPM;

    /**
     * What the engine is given for a pnpm project.
     */
    @Value
    @Builder(toBuilder = true)
    public static class Inputs {
        /**
         * The pnpm-lock.yaml; null when the project has none.
         */
        @Nullable
        String lock;

        /**
         * The lock's directory relative to the root: {@code ""}, or a workspace member's own lock's.
         */
        @Builder.Default
        String lockDir = "";

        /**
         * The directory pnpm runs in relative to the root, the edited manifest's; null for the lock's.
         */
        @Nullable
        String dir;

        /**
         * The project's manifests by location: {@code ""} the root, {@code packages/a} a workspace member.
         */
        @Singular
        Map<String, String> manifests;

        /**
         * The project's package-manager files by path relative to the root: pnpm-workspace.yaml, .npmrc, patch
         * files, a pnpmfile, and the package.json of a {@code file:} directory.
         */
        @Singular
        Map<String, String> files;

        /**
         * The process environment pnpm runs in; null when the host does not know it.
         */
        @Nullable
        Map<String, String> env;

        /**
         * The clock {@code minimumReleaseAge} counts from.
         */
        @NonNull
        Instant now;

        @Builder.Default
        Host host = Host.UNKNOWN;

        /**
         * The files the project holds relative to its root, when the host says.
         */
        @Nullable
        Set<String> paths;

        boolean holds(String path) {
            return files.containsKey(path) || paths != null && paths.contains(path);
        }
    }

    /**
     * @pnpm/installing.context's PnpmContext as a lock-only install reads it.
     */
    @Value
    static class PnpmContext {
        LockfileObject wantedLockfile;

        /**
         * The lock's directory, which importer ids and {@code link:} and {@code file:} paths in it are relative to.
         */
        String lockfileDir;

        /**
         * The projects installed into the lock.
         */
        List<Verification.Project> projects;

        boolean lockfileHadConflicts;
    }

    /**
     * The StrictInstallOptions installDeps and extendInstallOptions give {@code install}: the config, and what they
     * set over it.
     */
    @Value
    static class StrictInstallOptions {
        PnpmOptions config;

        /**
         * Every project of the workspace, which {@code workspace:} specs resolve to; the installed ones without one.
         */
        List<Verification.Project> allProjects;

        boolean pruneLockfileImporters;

        @Nullable
        ReadPackageHook readPackageHook;

        /**
         * Each {@code patchedDependencies} key with its patch file's hash.
         */
        Map<String, String> patchedDependencies;
    }

    /**
     * What {@code _install} decides before it resolves: the lock it writes as it stands, or why it resolves.
     */
    @Value
    static class Install {
        @Nullable
        String written;

        /**
         * Why pnpm resolves the graph: the setting the lock records differently, or the projects not being up to
         * date with it; null when it writes {@link #written}.
         */
        @Nullable
        String resolution;

        PnpmContext ctx;

        StrictInstallOptions opts;
    }

    /**
     * workspace.project-manifest-reader's other manifest names, which it reads where there is no package.json.
     */
    public static final List<String> OTHER_MANIFEST_NAMES = Collections.unmodifiableList(Arrays.asList("package.json5",
            "package.yaml"));

    private final Inputs inputs;
    private final RegistryMetadata registry;
    private final EnvLockfile.OnDisk lockFile;
    private final EnvInstaller envInstaller;

    // a project pnpm lists without a package.json, which it reports once the lock is verified
    private @Nullable String missingManifest;

    public PnpmLockRegenerator(Inputs inputs, RegistryMetadata registry) {
        this.inputs = inputs;
        this.registry = registry;
        this.lockFile = new EnvLockfile.OnDisk(inputs.getLock());
        this.envInstaller = new EnvInstaller(registry, inputs.getNow(), inputs.getHost());
    }

    /**
     * The lock pnpm writes for the edited manifests over the project's lock, or the failure, after a second install
     * over the written lock changed nothing.
     */
    public Result regenerate() {
        if (inputs.getLock() == null) {
            return Result.failure(INSTALL.failure(Rejected.discardsLock("the project has no " +
                                                                        LockfileReader.WANTED_LOCKFILE)));
        }
        String written;
        try {
            String dir = inputs.getDir();
            if (dir != null && !dir.equals(inputs.getLockDir())) {
                String into = Main.installsInto(inputs, dir);
                if (!into.equals(inputs.getLockDir())) {
                    throw new CannotKnow("which lock pnpm writes for this edit (run in " + dir + " it installs " +
                                         "into " + lockfilePath(into) + ", not " + lockfilePath(inputs.getLockDir()) +
                                         ")");
                }
            }
            written = writeLock();
        } catch (RuntimeException e) {
            return Result.failure(failure(e));
        }
        return INSTALL.verify(written, again ->
                new PnpmLockRegenerator(inputs.toBuilder().lock(again).build(), registry)
                .writeLock(), PnpmLockRegenerator::failure);
    }

    /**
     * Where pnpm installs the lock's project: {@link Main#installRoot}.
     */
    public static InstallRoot installRoot(Inputs inputs) {
        return Main.installRoot(inputs);
    }

    private static String lockfilePath(String dir) {
        return dir.isEmpty() ? LockfileReader.WANTED_LOCKFILE : dir + "/" + LockfileReader.WANTED_LOCKFILE;
    }

    /**
     * One {@code pnpm install --lockfile-only} over the lock: the lock it writes.
     */
    String writeLock() {
        Install install = install();
        requireOtherInstalls(install);
        if (install.getResolution() == null) {
            return install.getWritten();
        }
        return resolve(install);
    }

    // recursive.ts installs every project of an unshared workspace in one run: one that resolves shares pnpm's
    // resolution cache with this lock's, and its failure stops the run whether or not this lock is written yet
    private void requireOtherInstalls(Install install) {
        PnpmOptions options = install.getOpts().getConfig();
        String lockDir = inputs.getLockDir();
        if (options.getWorkspacePackagePatterns() == null || options.getLockfileDir(lockDir) != null) {
            return;
        }
        Map<String, Map<String, @Nullable Object>> projectConfigs =
                ProjectConfigs.createProjectConfigRecord(options.get("packageConfigs"));
        Map<String, String> ownOverrides = null;
        for (Verification.Project project : install.getOpts().getAllProjects()) {
            String dir = project.getRootDir();
            String lock = inputs.getFiles().get(RootRelativePath.join(dir, LockfileReader.WANTED_LOCKFILE));
            if (dir.equals(lockDir) || lock == null) {
                continue;
            }
            if (ownOverrides == null) {
                ownOverrides = projectOverrides(projectConfigs, lockDir, install.getOpts().getAllProjects(), options);
            }
            boolean ownHooks = !options.isTrue("ignorePnpmfile") &&
                               (inputs.holds(RootRelativePath.join(dir, ".pnpmfile.mjs")) ||
                                inputs.holds(RootRelativePath.join(dir, ".pnpmfile.cjs"))) ||
                               !ownOverrides.equals(projectOverrides(projectConfigs, dir,
                                       install.getOpts().getAllProjects(), options));
            PnpmLockRegenerator other = new PnpmLockRegenerator(inputs.toBuilder().lock(lock).lockDir(dir).dir(null)
                    .build(), registry);
            Install otherInstall;
            try {
                otherInstall = other.install();
            } catch (RuntimeException e) {
                throw ownHooks ? sharesCacheWith(dir) : failsBeside(dir, e);
            }
            if (otherInstall.getResolution() == null) {
                continue;
            }
            if (ownHooks) {
                throw sharesCacheWith(dir);
            }
            try {
                other.resolve(otherInstall);
            } catch (RuntimeException e) {
                throw failsBeside(dir, e);
            }
        }
    }

    private static Map<String, String> projectOverrides(Map<String, Map<String, @Nullable Object>> projectConfigs,
                                                        String dir, List<Verification.Project> allProjects,
                                                        PnpmOptions options) {
        for (Verification.Project project : allProjects) {
            if (project.getRootDir().equals(dir)) {
                return projectOverrides(ProjectConfigs.getProjectConfig(projectConfigs,
                        project.getManifest().get("name")), options.getOverrides());
            }
        }
        return options.getOverrides();
    }

    private CannotKnow sharesCacheWith(String dir) {
        return new CannotKnow("what the install of " + project(dir) + ", in the same run with readPackage hooks of " +
                              "its own, leaves in the resolution cache " + lockfilePath(inputs.getLockDir()) +
                              " reads");
    }

    private CannotKnow failsBeside(String dir, RuntimeException e) {
        return new CannotKnow("whether pnpm writes " + lockfilePath(inputs.getLockDir()) + " before the install of " +
                              project(dir) + ", in the same run, fails (" + e.getMessage() + ")");
    }

    private static String project(String dir) {
        return dir.isEmpty() ? "the root project" : dir;
    }

    private static Failure failure(RuntimeException e) {
        return INSTALL.failure(outcome(e));
    }

    // pnpm's own errors carry an ERR_PNPM_ code, though main.ts's printError logs none; an error without one crashes
    private static RuntimeException outcome(RuntimeException e) {
        if (e instanceof LockfileReader.IgnoredLockfile) {
            return Rejected.discardsLock(String.valueOf(e.getMessage()));
        }
        if (e instanceof ReportPeerDependencyIssues.PeerDependencyIssuesError) {
            return Rejected.rejects(Reason.PEER_CONFLICT,
                    ((ReportPeerDependencyIssues.PeerDependencyIssuesError) e).firstPeerName(),
                    "[ERR_PNPM_PEER_DEP_ISSUES] " + e.getMessage());
        }
        if (e instanceof PnpmError) {
            String code = ((PnpmError) e).getCode();
            return Rejected.rejects(reasonFor(code), "[" + code + "] " + e.getMessage());
        }
        if (e instanceof JsError) {
            String code = ((JsError) e).getCode();
            Reason reason = "ERR_INVALID_PACKAGE_CONFIG".equals(code) ? Reason.MALFORMED_MANIFEST :
                    Reason.RESOLUTION_REQUIRED;
            return Rejected.crashes(reason, null, (code == null ? "" : "[" + code + "] ") + e.getMessage());
        }
        // the lock and the env document catch their own; what reaches here is pnpm-workspace.yaml's
        if (e instanceof JsYaml.YamlException) {
            return Rejected.crashes(Reason.MALFORMED_MANIFEST, null, String.valueOf(e.getMessage()));
        }
        if (e instanceof LockfileFormatConverters.TypeError) {
            return Rejected.crashes(Reason.MALFORMED_LOCK, null, "TypeError: " + e.getMessage());
        }
        return e;
    }

    private static Reason reasonFor(String code) {
        switch (code) {
            case "ERR_PNPM_NO_MATCHING_VERSION":
                return SharedError.NO_MATCHING_VERSION.getReason();
            case "ERR_PNPM_FETCH_404":
                return SharedError.PACKAGE_NOT_FOUND.getReason();
            case "ERR_PNPM_UNSUPPORTED_ENGINE":
                return SharedError.UNSUPPORTED_ENGINE.getReason();
            case "ERR_PNPM_UNSUPPORTED_PLATFORM":
                return SharedError.UNSUPPORTED_PLATFORM.getReason();
            case "ERR_PNPM_NO_MATURE_MATCHING_VERSION":
            case "ERR_PNPM_NO_MATCHING_VERSION_INSIDE_WORKSPACE":
            case "ERR_PNPM_NODEJS_VERSION_NOT_FOUND":
                return Reason.VERSION_NOT_FOUND;
            case "ERR_PNPM_WORKSPACE_PKG_NOT_FOUND":
                return Reason.PACKAGE_NOT_FOUND;
            case "ERR_PNPM_JSON_PARSE":
            case "ERR_PNPM_INVALID_PEER_DEPENDENCY_SPECIFICATION":
            case "ERR_PNPM_CATALOG_ENTRY_NOT_FOUND_FOR_SPEC":
                return Reason.MALFORMED_MANIFEST;
            case "ERR_PNPM_BROKEN_LOCKFILE":
            case "ERR_PNPM_ENV_LOCKFILE_CORRUPTED":
                return Reason.MALFORMED_LOCK;
            default:
                return Reason.RESOLUTION_REQUIRED;
        }
    }

    /**
     * The rest of {@code _install} once the lock needs resolving: {@code _installInContext}'s resolution and write.
     */
    String resolve(Install install) {
        StrictInstallOptions opts = install.getOpts();
        PnpmOptions options = opts.getConfig();
        LockfileObject wantedLockfile = install.getCtx().getWantedLockfile();
        boolean autoInstallPeers = options.isTrue("autoInstallPeers");
        List<Verification.Project> projects = install.getCtx().getProjects();

        Map<String, @Nullable Map<String, @Nullable Object>> catalogs = options.getCatalogs();
        List<ResolveDependencyTree.Importer> importers = new ArrayList<>();
        for (Verification.Project project : projects) {
            List<ResolveDependencies.WantedDependency> wantedDependencies =
                    ResolveDependencyTree.getWantedDependencies(project.getManifest(), autoInstallPeers);
            for (ResolveDependencies.WantedDependency dep : wantedDependencies) {
                dep.setUpdateSpec(true);
            }
            ProjectSnapshot importer = wantedLockfile.getImporters().get(project.getId());
            if (importer != null) {
                forgetResolutionsOfPrevWantedDeps(importer, wantedDependencies, wantedLockfile.getCatalogs(),
                        catalogs);
            }
            importers.add(new ResolveDependencyTree.Importer(project.getId(), project.getRootDir(),
                    project.getManifest(), wantedDependencies));
        }
        for (ResolveDependencyTree.Importer importer : importers) {
            wantedLockfile.getImporters().computeIfAbsent(importer.getId(), id -> new ProjectSnapshot());
        }
        if (opts.isPruneLockfileImporters()) {
            Set<String> projectIds = new LinkedHashSet<>();
            for (ResolveDependencyTree.Importer importer : importers) {
                projectIds.add(importer.getId());
            }
            wantedLockfile.getImporters().keySet().retainAll(projectIds);
        }
        List<Map<String, @Nullable Object>> manifests = new ArrayList<>();
        for (Verification.Project project : projects) {
            manifests.add(project.getManifest());
        }
        PreferredVersions.ByName preferredVersions = PreferredVersions.ByName.of(
                PreferredVersions.getPreferredVersionsFromLockfileAndManifests(wantedLockfile.getPackages(),
                        manifests));

        Map<String, String> registries = options.getRegistries();
        Map<String, String> files = new LinkedHashMap<>(inputs.getFiles());
        for (Map.Entry<String, String> manifest : inputs.getManifests().entrySet()) {
            files.put(manifest.getKey().isEmpty() ? "package.json" : manifest.getKey() + "/package.json",
                    manifest.getValue());
        }
        NpmResolver npmResolver = CreateNewStoreController.createNewStoreController(options,
                options.getFetchFullMetadata(), registry, inputs.getNow());
        PackageRequester requester = new PackageRequester(npmResolver, files, inputs.getHost(),
                options.isTrue("engineStrict"), options.isTrue("force"), options.getNodeVersion(),
                options.getSupportedArchitectures(), registry, options.getNodeDownloadMirrors());

        ResolveDependencies.ResolutionContext ctx = new ResolveDependencies.ResolutionContext(wantedLockfile,
                install.getCtx().getLockfileDir(), requester);
        ctx.setAutoInstallPeers(autoInstallPeers);
        ctx.setAutoInstallPeersFromHighestMatch(options.isTrue("autoInstallPeersFromHighestMatch"));
        ctx.setCatalogs(catalogs);
        ctx.setDefaultTag(options.getTag());
        ctx.setForceFullResolution(true);
        ctx.setInjectWorkspacePackages(options.isTrue("injectWorkspacePackages"));
        ctx.setLinkWorkspacePackagesDepth(options.getLinkWorkspacePackagesDepth());
        ctx.setPreferWorkspacePackages(options.isTrue("preferWorkspacePackages"));
        ctx.setReadPackageHook(opts.getReadPackageHook());
        ctx.setRegistries(registries);
        ctx.setNamedRegistryPrefixes(options.getNamedRegistryPrefixes());
        Object resolutionMode = options.get("resolutionMode");
        ctx.setResolutionMode(resolutionMode == null ? null : Js.string(resolutionMode));
        ctx.setWorkspacePackages(workspacePackages(opts.getAllProjects()));
        ctx.setHoistPeers(autoInstallPeers || options.isTrue("dedupePeerDependents"));
        ctx.setResolvePeersFromWorkspaceRoot(options.isTrue("resolvePeersFromWorkspaceRoot"));
        ctx.setBlockExoticSubdeps(options.isTrue("blockExoticSubdeps"));
        ctx.setPatchedDependencies(PatchingConfig.groupPatchedDependencies(opts.getPatchedDependencies()));
        Double minimumReleaseAge = options.getNumber("minimumReleaseAge");
        List<String> minimumReleaseAgeExclude = options.getStrings("minimumReleaseAgeExclude");
        if (minimumReleaseAge != null && minimumReleaseAge != 0) {
            ctx.setMaximumPublishedBy(inputs.getNow().minusMillis((long) (minimumReleaseAge * 60 * 1000)));
        }
        if (minimumReleaseAgeExclude != null) {
            ctx.setPublishedByExclude(PackageVersionPolicy.createPackageVersionPolicyOrThrow(minimumReleaseAgeExclude,
                    "minimumReleaseAgeExclude"));
        }
        Object trustPolicy = options.get("trustPolicy");
        ctx.setTrustPolicy(trustPolicy == null || trustPolicy == JsYaml.UNDEFINED ? null : Js.string(trustPolicy));
        List<String> trustPolicyExclude = options.getStrings("trustPolicyExclude");
        if (trustPolicyExclude != null) {
            ctx.setTrustPolicyExclude(PackageVersionPolicy.createPackageVersionPolicyOrThrow(trustPolicyExclude,
                    "trustPolicyExclude"));
        }
        ctx.setTrustPolicyIgnoreAfter(options.getNumber("trustPolicyIgnoreAfter"));
        Object peersSuffixMaxLength = options.get("peersSuffixMaxLength");
        DepsResolver.Options resolverOptions = new DepsResolver.Options(autoInstallPeers,
                options.isTrue("dedupePeerDependents"), options.isTrue("dedupePeers"),
                options.isTrue("dedupeInjectedDeps"), options.isTrue("excludeLinksFromLockfile"),
                options.isTrue("lockfileIncludeTarballUrl"),
                options.isTrue("resolvePeersFromWorkspaceRoot"),
                peersSuffixMaxLength instanceof Number ? ((Number) peersSuffixMaxLength).intValue() : 1000,
                registries, options.isTrue("allowUnusedPatches"),
                violations -> handleResolutionPolicyViolations(options, violations, minimumReleaseAge));
        DepsResolver.Result result = DepsResolver.resolveDependencies(importers, ctx, preferredVersions,
                resolverOptions);

        LockfileObject newLockfile = result.getNewLockfile();
        newLockfile.set("lockfileVersion", LockfileObject.LOCKFILE_VERSION);
        String written = lockFile.writeWantedLockfile(newLockfile);
        ReportPeerDependencyIssues.reportPeerDependencyIssues(result.getPeerDependencyIssuesByProjects(),
                options.getPeerDependencyRules(), options.isTrue("strictPeerDependencies"));
        return written;
    }

    // forgetResolutionsOfPrevWantedDeps: a specifier that changed re-resolves
    private static void forgetResolutionsOfPrevWantedDeps(
            ProjectSnapshot importer, List<ResolveDependencies.WantedDependency> wantedDeps,
            @Nullable Object prevCatalogs, Map<String, @Nullable Map<String, @Nullable Object>> catalogs) {
        if (importer.getDependencies() == null) {
            importer.setDependencies(new LinkedHashMap<>());
        }
        if (importer.getDevDependencies() == null) {
            importer.setDevDependencies(new LinkedHashMap<>());
        }
        if (importer.getOptionalDependencies() == null) {
            importer.setOptionalDependencies(new LinkedHashMap<>());
        }
        for (ResolveDependencies.WantedDependency dep : wantedDeps) {
            String alias = dep.getAlias();
            if (!Js.truthy(alias) || isWantedDepBareSpecifierSame(prevCatalogs, catalogs, alias,
                    importer.getSpecifiers().get(alias), dep.getBareSpecifier())) {
                continue;
            }
            String ref = importer.getDependencies().get(alias);
            if (ref == null || !ref.startsWith("link:")) {
                importer.getDependencies().remove(alias);
            }
            importer.getDevDependencies().remove(alias);
            importer.getOptionalDependencies().remove(alias);
        }
    }

    private static boolean isWantedDepBareSpecifierSame(@Nullable Object prevCatalogs,
                                                        Map<String, @Nullable Map<String, @Nullable Object>> catalogs,
                                                        String alias, @Nullable String prevBareSpecifier,
                                                        String nextBareSpecifier) {
        if (!nextBareSpecifier.equals(prevBareSpecifier)) {
            return false;
        }
        String catalogName = Catalogs.parseCatalogProtocol(prevBareSpecifier);
        if (catalogName == null) {
            return true;
        }
        Object prevCatalog = prevCatalogs instanceof Map ? ((Map<?, ?>) prevCatalogs).get(catalogName) : null;
        Object prevEntry = prevCatalog instanceof Map ? ((Map<?, ?>) prevCatalog).get(alias) : null;
        Object prevSpec = prevEntry instanceof Map ? ((Map<?, ?>) prevEntry).get("specifier") : null;
        Map<String, @Nullable Object> catalog = catalogs.get(catalogName);
        Object nextSpec = catalog == null ? null : catalog.get(alias);
        return Js.strictEquals(prevSpec, nextSpec);
    }

    // the minimumReleaseAge handler of installing.commands' setupPolicyHandlers, outside a terminal
    private static void handleResolutionPolicyViolations(PnpmOptions options,
                                                         List<ResolveDependencies.PolicyViolation> violations,
                                                         @Nullable Double minimumReleaseAge) {
        if (violations.isEmpty() || minimumReleaseAge == null || minimumReleaseAge == 0 ||
            !options.isTrue("minimumReleaseAgeStrict")) {
            return;
        }
        List<ResolveDependencies.PolicyViolation> immature = new ArrayList<>();
        for (ResolveDependencies.PolicyViolation violation : violations) {
            if (NpmResolver.MINIMUM_RELEASE_AGE_VIOLATION_CODE.equals(violation.getCode())) {
                immature.add(violation);
            }
        }
        if (immature.isEmpty()) {
            return;
        }
        immature.sort((a, b) -> (a.getName() + "@" + a.getVersion()).compareTo(b.getName() + "@" + b.getVersion()));
        StringBuilder list = new StringBuilder();
        for (ResolveDependencies.PolicyViolation v : immature) {
            if (list.length() > 0) {
                list.append('\n');
            }
            list.append("  ").append(v.getName()).append('@').append(v.getVersion()).append(' ').append(v.getReason());
        }
        throw new PnpmError("NO_MATURE_MATCHING_VERSION", immature.size() + " " + (immature.size() == 1 ?
                "version does" : "versions do") + " not meet the minimumReleaseAge constraint:\n" + list);
    }

    Install install() {
        Main main = new Main(inputs, envInstaller, lockFile);
        PnpmOptions options = main.config();
        requireSettings(options);
        boolean workspace = options.getWorkspacePackagePatterns() != null;
        String lockDir = inputs.getLockDir();
        String lockfileDir = workspace ? options.getLockfileDir(lockDir) : null;
        main.checkWantedPackageManager(options);
        main.installConfigDepsAndLoadHooks(options, lockfileDir);
        List<Verification.Project> allProjects = workspace ? projects(options, true) : null;
        if (options.isTrue("global")) {
            throw new PnpmError("GLOBAL_INSTALL_NOT_SUPPORTED", "\"pnpm install -g\" is not supported. Use \"pnpm " +
                                                                "add -g <pkg>\" to install global packages.");
        }
        ProjectsGraph.Result projectsGraph = allProjects == null ? null :
                ProjectsGraph.createProjectsGraph(allProjects, false, Js.truthy(options.get(
                        "linkWorkspacePackages")));
        // installDeps creates its store controller, whose npm resolver checks the named registries
        NpmResolver.mergeNamedRegistries(options.getNamedRegistries());

        // in a workspace pnpm install is recursive over the projects it lists, which all install into the lock of
        // lockfileDir, or without one each into its own
        List<Verification.Project> projectManifests;
        if (allProjects == null) {
            projectManifests = projects(options, false);
        } else if (lockfileDir != null) {
            projectManifests = new ArrayList<>();
            if (lockfileDir.equals(lockDir)) {
                for (Verification.Project project : allProjects) {
                    projectManifests.add(new Verification.Project(project.getRootDir(),
                            FindPackages.getLockfileImporterId(RootRelativePath.relative(lockDir,
                                    project.getRootDir())),
                            project.getManifest()));
                }
            }
        } else {
            projectManifests = new ArrayList<>();
            for (Verification.Project project : allProjects) {
                if (project.getRootDir().equals(lockDir)) {
                    projectManifests.add(new Verification.Project(project.getRootDir(), ".", project.getManifest()));
                }
            }
        }
        Object runtimeOnFail = options.get("runtimeOnFail");
        if (Js.truthy(runtimeOnFail)) {
            for (Verification.Project project : allProjects != null ? allProjects : projectManifests) {
                PkgManifestUtils.applyRuntimeOnFailOverride(project.getManifest(), runtimeOnFail);
            }
        }
        Map<String, String> overrides = options.getOverrides();
        if (projectsGraph != null) {
            checkWorkspaceCycles(options, projectsGraph);
            Map<String, Map<String, @Nullable Object>> projectConfigs =
                    ProjectConfigs.createProjectConfigRecord(options.get("packageConfigs"));
            // recursive.ts installs each project on its own: its hooks, its packageConfigs, then install()
            if (lockfileDir == null && !projectManifests.isEmpty()) {
                requireHooks(options, lockDir);
                overrides = projectOverrides(ProjectConfigs.getProjectConfig(projectConfigs,
                        projectManifests.get(0).getManifest().get("name")), overrides);
                agent(options);
            }
        } else {
            agent(options);
        }
        if (Js.truthy(options.get("virtualStoreOnly")) && !Js.truthy(options.get("enableModulesDir")) &&
            !Js.truthy(options.get("enableGlobalVirtualStore"))) {
            throw new PnpmError("CONFIG_CONFLICT_VIRTUAL_STORE_ONLY_WITH_NO_MODULES_DIR", "Cannot use " +
                    "virtualStoreOnly when enableModulesDir is false (the standard virtual store requires " +
                    "node_modules/.pnpm)");
        }
        if (!options.isTrue("useLockfile")) {
            throw new PnpmError("CONFIG_CONFLICT_LOCKFILE_ONLY_WITH_NO_LOCKFILE", "Cannot generate a " +
                    LockfileReader.WANTED_LOCKFILE + " because lockfile is set to false");
        }
        if (options.isTrue("useGitBranchLockfile")) {
            throw new CannotKnow("the git branch (gitBranchLockfile names the lock after it)");
        }
        Object branchPattern = options.get("mergeGitBranchLockfilesBranchPattern");
        if (options.isTrue("mergeGitBranchLockfiles") ||
            branchPattern instanceof List && !((List<?>) branchPattern).isEmpty()) {
            throw new CannotKnow("the git branch lockfiles mergeGitBranchLockfiles merges");
        }
        if (projectManifests.isEmpty() && workspace) {
            // no project of the workspace installs into this lock: pnpm leaves it as it is
            return new Install(lockFile.text(), null, new PnpmContext(new LockfileObject(), lockDir,
                    Collections.emptyList(), false), new StrictInstallOptions(options, Collections.emptyList(),
                    false, null, Collections.emptyMap()));
        }
        List<ParseOverrides.VersionOverride> parsedOverrides = ParseOverrides.parseOverrides(overrides,
                options.getCatalogs());
        ReadPackageHook readPackageHook = ReadPackageHook.createReadPackageHook(
                options.isTrue("ignoreCompatibilityDb"), lockDir, parsedOverrides,
                options.get("ignoredOptionalDependencies"), options.get("packageExtensions"));
        if (workspace && lockfileDir != null) {
            agent(options);
        }
        List<Verification.Project> projects = readPackage(readPackageHook, projectManifests);
        List<Verification.Project> workspaceProjects = allProjects == null ? projects :
                lockfileDir != null ? projects : readPackage(readPackageHook, allProjects);

        String lockText = lockFile.text();
        LockfileReader.ReadResult read = lockText == null ? new LockfileReader.ReadResult(null, false) :
                LockfileReader.read(lockText);
        if (read.getLockfile() == null) {
            throw new LockfileReader.IgnoredLockfile("no " + LockfileReader.WANTED_LOCKFILE);
        }
        LockfileObject wantedLockfile = read.getLockfile();
        boolean existsWantedLockfile = true;
        for (Verification.Project project : projects) {
            if (!wantedLockfile.getImporters().containsKey(project.getId())) {
                wantedLockfile.getImporters().put(project.getId(), new ProjectSnapshot());
            }
        }
        boolean existsNonEmptyWantedLockfile = existsWantedLockfile && !LockfileWriter.isEmptyLockfile(wantedLockfile);
        VerifyLockfileResolutions.verifyLockfileResolutions(wantedLockfile, resolutionVerifiers(options));
        if (missingManifest != null) {
            throw new JsError("No package.json found in \"" + (missingManifest.isEmpty() ? "." : missingManifest) +
                              "\"");
        }
        PnpmContext ctx = new PnpmContext(wantedLockfile, lockDir, projects, read.isHadConflicts());

        Object packageExtensions = options.get("packageExtensions");
        String packageExtensionsChecksum = ObjectHasher.hashObjectNullableWithPrefix(packageExtensions);
        Map<String, @Nullable Object> patchedDependencies = patchedDependencies(options);
        Map<String, @Nullable Object> overridesMap = ParseOverrides.createOverridesMapFromParsed(parsedOverrides);
        List<@Nullable Object> ignoredOptionalDependencies = options.getIgnoredOptionalDependencies();
        if (ignoredOptionalDependencies != null) {
            ignoredOptionalDependencies.sort((a, b) -> Js.string(a).compareTo(Js.string(b)));
        }
        String outdatedLockfileSettingName = SettingsChecker.getOutdatedLockfileSetting(wantedLockfile,
                SettingsChecker.Settings.builder()
                        .catalogs(options.getCatalogs())
                        .overrides(overridesMap)
                        .packageExtensionsChecksum(packageExtensionsChecksum)
                        .patchedDependencies(patchedDependencies)
                        .ignoredOptionalDependencies(ignoredOptionalDependencies)
                        .autoInstallPeers(options.get("autoInstallPeers"))
                        .dedupePeers(Js.truthy(options.get("dedupePeers")) ? options.get("dedupePeers") : null)
                        .excludeLinksFromLockfile(options.get("excludeLinksFromLockfile"))
                        .peersSuffixMaxLength(options.get("peersSuffixMaxLength"))
                        .pnpmfileChecksum(null)
                        .injectWorkspacePackages(options.get("injectWorkspacePackages"))
                        .build());
        boolean outdatedLockfileSettings = outdatedLockfileSettingName != null;
        boolean upToDateLockfileMajorVersion = Js.string(wantedLockfile.getLockfileVersion())
                .startsWith(LockfileObject.LOCKFILE_MAJOR_VERSION + ".");
        boolean needsFullResolution = outdatedLockfileSettings || !upToDateLockfileMajorVersion;
        wantedLockfile.set("settings", options.getLockfileSettings());
        if (needsFullResolution) {
            wantedLockfile.set("overrides", overridesMap);
            wantedLockfile.set("packageExtensionsChecksum",
                    packageExtensionsChecksum == null ? JsYaml.UNDEFINED : packageExtensionsChecksum);
            wantedLockfile.set("ignoredOptionalDependencies",
                    options.get("ignoredOptionalDependencies") == null ? JsYaml.UNDEFINED :
                            options.get("ignoredOptionalDependencies"));
            wantedLockfile.set("pnpmfileChecksum", JsYaml.UNDEFINED);
            wantedLockfile.set("patchedDependencies", patchedDependencies);
            return new Install(null, outdatedLockfileSettings ? outdatedLockfileSettingName :
                    "lockfileVersion " + Js.string(wantedLockfile.getLockfileVersion()), ctx,
                    new StrictInstallOptions(options, workspaceProjects, workspace, readPackageHook,
                            patchHashes(patchedDependencies)));
        }

        boolean pruneLockfileImporters = workspace;
        StrictInstallOptions opts = new StrictInstallOptions(options, workspaceProjects, pruneLockfileImporters,
                readPackageHook, patchHashes(patchedDependencies));
        Map<String, Map<String, Verification.Project>> workspacePackages = workspace ?
                workspacePackages(workspaceProjects) : null;
        String reason = null;
        if (read.isHadConflicts()) {
            reason = "merge conflicts";
        } else if (!options.isTrue("preferFrozenLockfile")) {
            reason = "preferFrozenLockfile off";
        } else if (pruneLockfileImporters && wantedLockfile.getImporters().size() != projects.size()) {
            reason = "importers of projects the workspace no longer has";
        } else if (!existsNonEmptyWantedLockfile) {
            reason = "an empty lock";
        } else if (!LockfileObject.LOCKFILE_VERSION.equals(wantedLockfile.getLockfileVersion())) {
            reason = "lockfileVersion " + Js.string(wantedLockfile.getLockfileVersion());
        } else if (!Verification.allProjectsAreUpToDate(projects, options.getCatalogs(),
                options.isTrue("autoInstallPeers"), options.isTrue("excludeLinksFromLockfile"),
                options.getLinkWorkspacePackagesDepth() >= 0, wantedLockfile, workspacePackages, inputs.getFiles(),
                lockDir)) {
            reason = "projects not up to date with the lock";
        }
        if (reason != null) {
            return new Install(null, reason, ctx, opts);
        }
        return new Install(lockFile.writeWantedLockfile(wantedLockfile), null, ctx, opts);
    }

    // getConfig copies every camelCase key of pnpm-workspace.yaml into the config the install reads as its options;
    // these change the lock or the install's outcome and are not settings the port reads
    private static void requireSettings(PnpmOptions options) {
        Map<String, Boolean> asIfUnset = new LinkedHashMap<>(AS_IF_UNSET);
        if (options.getWorkspacePackagePatterns() != null) {
            // recursive.ts: true when every project of the workspace is installed
            asIfUnset.put("pruneLockfileImporters", true);
        }
        for (String option : UNPORTED_OPTIONS) {
            if (options.isExplicitlySet(option) && !(asIfUnset.containsKey(option) &&
                                                     asIfUnset.get(option).equals(options.get(option)))) {
                throw new CannotKnow("what pnpm does with " + option + " (" +
                                     Js.jsonStringify(options.get(option)) + "), an option of the install");
            }
        }
        Object only = options.get("only");
        boolean production = "prod".equals(only) || "production".equals(only) ||
                             !Js.truthy(only) && options.isTrue("production");
        boolean dev = !production && ("dev".equals(only) || "development".equals(only) || options.isTrue("dev"));
        if (production || dev || Boolean.FALSE.equals(options.get("optional"))) {
            throw new CannotKnow("the lock pnpm writes without its " + (production ? "devDependencies" :
                    dev ? "dependencies" : "optionalDependencies") + " (only, production, dev or optional)");
        }
        if (options.isExplicitlySet("recursiveInstall") && !options.isTrue("recursiveInstall") ||
            options.isTrue("workspaceRoot") || options.isExplicitlySet("filter") ||
            options.isExplicitlySet("filterProd")) {
            throw new CannotKnow("which projects pnpm installs (recursiveInstall, workspaceRoot, filter or " +
                                 "filterProd select them from the directory it runs in)");
        }
        for (String networkOnly : new String[]{"offline", "preferOffline"}) {
            if (options.isTrue(networkOnly)) {
                throw new CannotKnow("the metadata pnpm's cache holds (" + networkOnly + ")");
            }
        }
    }

    private static final List<String> UNPORTED_OPTIONS = Arrays.asList("dedupe", "dir", "explicitlySetKeys",
            "finders", "fixLockfile", "forceFullResolution", "frozenLockfile", "frozenLockfileIfExists", "hooks",
            "ignoredPackages", "ignorePackageManifest", "latest", "lockfileCheck", "packageManager",
            "packageVulnerabilityAudit", "pruneDirectDependencies", "pruneLockfileImporters", "resolutionOnly",
            "rootProjectManifest", "rootProjectManifestDir", "saveLockfile", "update", "updateMatching",
            "updatePackageManifest", "updateToLatest", "wantedPackageManager", "workspace", "workspaceDir",
            "workspacePackagePatterns");

    // the value of an option above that a lock-only install treats as the option unset (extendInstallOptions'
    // defaults, install.ts' frozenLockfileIfExists under lockfileOnly, the commands' truthiness checks)
    private static final Map<String, Boolean> AS_IF_UNSET = new LinkedHashMap<>();

    static {
        for (String option : Arrays.asList("dedupe", "fixLockfile", "forceFullResolution", "frozenLockfile",
                "frozenLockfileIfExists", "ignorePackageManifest", "latest", "pruneDirectDependencies",
                "resolutionOnly", "update", "updatePackageManifest", "updateToLatest", "workspace")) {
            AS_IF_UNSET.put(option, false);
        }
        AS_IF_UNSET.put("saveLockfile", true);
    }

    // recursive.ts: a project's packageConfigs overrides replace the workspace's for its own lock
    private static Map<String, String> projectOverrides(@Nullable Map<String, @Nullable Object> localConfig,
                                                        Map<String, String> overrides) {
        if (localConfig == null || !localConfig.containsKey("overrides")) {
            return overrides;
        }
        Map<String, @Nullable Object> local = ProjectConfigs.properties(localConfig.get("overrides"));
        Map<String, String> out = new LinkedHashMap<>();
        for (String selector : Js.keys(local)) {
            Object spec = local.get(selector);
            if (!(spec instanceof String)) {
                throw new CannotKnow("what pnpm makes of packageConfigs overrides." + selector + " (" +
                                     Js.typeOf(spec) + ", where pnpm reads a string)");
            }
            out.put(selector, (String) spec);
        }
        return out;
    }

    private static List<Verification.Project> readPackage(@Nullable ReadPackageHook readPackageHook,
                                                          List<Verification.Project> projects) {
        List<Verification.Project> read = new ArrayList<>();
        for (Verification.Project project : projects) {
            read.add(readPackageHook == null ? project : new Verification.Project(project.getRootDir(),
                    project.getId(), readPackageHook.apply(project.getManifest(), project.getRootDir())));
        }
        return read;
    }

    // recursive.ts' requireHooks for a project installed into its own lock, from its directory: its default
    // pnpmfile and the pnpmfiles setting (which getConfig's requireHooks does not read)
    private void requireHooks(PnpmOptions options, String dir) {
        if (options.isTrue("ignorePnpmfile")) {
            return;
        }
        Object pnpmfiles = options.get("pnpmfiles");
        if (Js.truthy(pnpmfiles) && !(pnpmfiles instanceof List)) {
            throw new CannotKnow("what pnpm makes of pnpmfiles (" + Js.typeOf(pnpmfiles) + ", where pnpm reads a " +
                                 "list)");
        }
        new RequireHooks(inputs).requireHooks(dir, options.get("globalPnpmfile"), Js.truthy(pnpmfiles) ?
                (List<?>) pnpmfiles : Collections.emptyList(), Main.tryLoadDefaultPnpmfile(options));
    }

    // main.ts' findWorkspaceProjects or installDeps' tryReadProjectManifest: each project's manifest as read, which
    // cli.utils' packageIsInstallable checks
    private List<Verification.Project> projects(PnpmOptions options, boolean workspace) {
        List<String> dirs = new ArrayList<>();
        if (workspace) {
            dirs.addAll(FindPackages.findWorkspaceProjects(Main.manifestDirs(inputs),
                    options.getWorkspacePackagePatterns()));
        } else {
            dirs.add(inputs.getLockDir());
        }
        List<Verification.Project> projects = new ArrayList<>();
        for (String dir : dirs) {
            String text = inputs.getManifests().get(dir);
            List<String> others = new ArrayList<>();
            for (String other : workspace ? OTHER_MANIFEST_NAMES : Collections.<String>emptyList()) {
                String path = RootRelativePath.join(dir, other);
                if (inputs.holds(path)) {
                    others.add(path);
                }
            }
            if (!others.isEmpty() && (text != null || others.size() > 1)) {
                // findPackages lists every manifest file of a directory, in the order the filesystem lists them
                throw new CannotKnow("the manifest pnpm reads from " + others.get(others.size() - 1) +
                                     " (findPackages lists each manifest of " + (dir.isEmpty() ? "the root" : dir) +
                                     ", in the order the filesystem gives)");
            }
            if (text == null && others.isEmpty()) {
                if (missingManifest == null) {
                    missingManifest = dir;
                }
                continue;
            }
            Map<String, @Nullable Object> manifest;
            if (text != null) {
                manifest = Manifests.readProjectManifest(text, RootRelativePath.join(dir, "package.json"));
            } else {
                String path = others.get(0);
                String other = inputs.getFiles().get(path);
                if (other == null) {
                    throw new CannotKnow(path + " (its contents were not read)");
                }
                if (!path.endsWith("/package.yaml") && !"package.yaml".equals(path)) {
                    throw new CannotKnow("what pnpm reads from " + path + " (json5's parse is not ported)");
                }
                manifest = Manifests.readPackageYaml(other, path);
            }
            projects.add(new Verification.Project(dir, workspace ? FindPackages.getLockfileImporterId(dir) : ".",
                    manifest));
        }
        for (Verification.Project project : projects) {
            PackageIsInstallable.projectIsInstallable(project.getId(), project.getManifest(),
                    options.isTrue("engineStrict"), options.getNodeVersion(),
                    workspace ? null : options.get("supportedArchitectures"), inputs.getHost());
        }
        return projects;
    }

    // installDeps: projects that depend on each other in a cycle, an error under disallowWorkspaceCycles
    private static void checkWorkspaceCycles(PnpmOptions options, ProjectsGraph.Result projectsGraph) {
        if (options.isTrue("ignoreWorkspaceCycles") || !options.isTrue("disallowWorkspaceCycles")) {
            return;
        }
        GraphSequencer.Result<String> sequencedGraph = GraphSequencer.sequenceGraph(projectsGraph.getGraph());
        if (!sequencedGraph.isSafe()) {
            List<String> cycles = new ArrayList<>();
            for (List<String> cycle : sequencedGraph.getCycles()) {
                List<String> ids = new ArrayList<>();
                for (String dir : cycle) {
                    ids.add(FindPackages.getLockfileImporterId(dir));
                }
                cycles.add(String.join(", ", ids));
            }
            String cyclicDependenciesInfo = cycles.isEmpty() ? "" : ": " + String.join("; ", cycles);
            throw new PnpmError("DISALLOW_WORKSPACE_CYCLES", "There are cyclic workspace dependencies" +
                                                             cyclicDependenciesInfo);
        }
        if (!projectsGraph.getUnknown().isEmpty()) {
            throw new CannotKnow("whether " + projectsGraph.getUnknown().get(0) + " names a project of the " +
                                 "workspace, which disallowWorkspaceCycles checks for cycles");
        }
    }

    // install / mutateModules: with an agent configured, the agent server resolves the install
    private static void agent(PnpmOptions options) {
        Object agent = options.get("agent");
        if (!Js.truthy(agent)) {
            return;
        }
        if ("no-downgrade".equals(options.get("trustPolicy"))) {
            throw new PnpmError("TRUST_POLICY_INCOMPATIBLE_WITH_AGENT", "The pnpm agent does not yet enforce " +
                    "`trustPolicy: no-downgrade`, so running an install through the agent under this policy would " +
                    "produce a lockfile that the local verifier rejects.", "Unset `trustPolicy` for this install, or " +
                    "disable the agent (unset `--agent` / `agent` in pnpm-workspace.yaml) so resolution runs locally " +
                    "and the trust check applies.");
        }
        throw new CannotKnow("what the pnpm agent at " + Js.string(agent) + " resolves");
    }

    // createResolutionVerifiers: the npm verifier when minimumReleaseAge or trustPolicy is active
    private List<ResolutionVerifier> resolutionVerifiers(PnpmOptions options) {
        NpmResolutionVerifier npmVerifier = NpmResolutionVerifier.createNpmResolutionVerifier(
                NpmResolutionVerifier.Options.builder()
                        .minimumReleaseAge(options.getNumber("minimumReleaseAge"))
                        .minimumReleaseAgeExclude(options.getStrings("minimumReleaseAgeExclude"))
                        .ignoreMissingTimeField(
                                Boolean.TRUE.equals(options.get("minimumReleaseAgeIgnoreMissingTime")))
                        .trustPolicy(options.get("trustPolicy") == null ? null :
                                Js.string(options.get("trustPolicy")))
                        .trustPolicyExclude(options.getStrings("trustPolicyExclude"))
                        .trustPolicyIgnoreAfter(options.getNumber("trustPolicyIgnoreAfter"))
                        .registries(options.getRegistries())
                        .namedRegistries(options.getNamedRegistries())
                        .fetch(registry)
                        .now(inputs.getNow())
                        .build());
        return npmVerifier == null ? Collections.emptyList() : Collections.singletonList(npmVerifier);
    }

    // arrayOfWorkspacePackagesToMap: each project by name, then version
    private static Map<String, Map<String, Verification.Project>> workspacePackages(
            List<Verification.Project> projects) {
        Map<String, Map<String, Verification.Project>> packages = new LinkedHashMap<>();
        for (Verification.Project project : projects) {
            Object name = project.getManifest().get("name");
            if (!Js.truthy(name)) {
                continue;
            }
            Object version = project.getManifest().get("version");
            packages.computeIfAbsent(Js.string(name), n -> new LinkedHashMap<>())
                    .put(Js.truthy(version) ? Js.string(version) : "0.0.0", project);
        }
        return packages;
    }

    @SuppressWarnings("unchecked")
    private Map<String, @Nullable Object> patchedDependencies(PnpmOptions options) {
        Object patched = options.get("patchedDependencies");
        if (!Js.truthy(patched)) {
            return new LinkedHashMap<>();
        }
        if (!(patched instanceof Map)) {
            throw new CannotKnow("what pnpm makes of patchedDependencies (" + Js.typeOf(patched) +
                                 ", where pnpm reads an object)");
        }
        Map<String, String> patches = new LinkedHashMap<>();
        for (String key : Js.keys((Map<String, @Nullable Object>) patched)) {
            patches.put(key, Js.string(((Map<String, @Nullable Object>) patched).get(key)));
        }
        return SettingsChecker.calcPatchHashes(patches, inputs.getFiles());
    }

    private static Map<String, String> patchHashes(Map<String, @Nullable Object> patchedDependencies) {
        Map<String, String> hashes = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable Object> entry : patchedDependencies.entrySet()) {
            hashes.put(entry.getKey(), Js.string(entry.getValue()));
        }
        return hashes;
    }
}
