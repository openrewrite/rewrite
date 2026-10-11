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

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.pnpm.ExtendInstallOptions.InstallOptions;
import org.openrewrite.javascript.internal.lock.pnpm.ExtendInstallOptions.StrictInstallOptions;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.openrewrite.javascript.semver.NodeSemver;

/**
 * Port of @pnpm/installing.deps-installer src/install/index.ts (pnpm 11.2.2) for one lock: install, mutateModules,
 * _install up to its resolution, _installInContext's resolution and write, and installSome's catalogMode check.
 */
final class Install {

    private static final String CATALOG_PROTOCOL = "catalog:";
    private static final String DEFAULT_CATALOG = "default";

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
     * What {@code _install} decides before it resolves: the lock it writes as it stands, or why it resolves.
     */
    @Value
    static class Result {
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

        /**
         * Every project of the workspace as getContext read it, which {@code workspace:} specs resolve to; the
         * installed ones outside a workspace.
         */
        List<Verification.Project> allProjects;

        /**
         * Each {@code patchedDependencies} key with its patch file's hash.
         */
        Map<String, String> patchedDependencies;
    }

    private final PnpmLockRegenerator.Inputs inputs;
    private final String lockDir;
    private final EnvLockfile.OnDisk lockFile;
    private final RegistryMetadata registry;
    private final CreateNewStoreController stores;
    private final Instant now;

    Install(PnpmLockRegenerator.Inputs inputs, String lockDir, EnvLockfile.OnDisk lockFile, RegistryMetadata registry,
            CreateNewStoreController stores, Instant now) {
        this.inputs = inputs;
        this.lockDir = lockDir;
        this.lockFile = lockFile;
        this.registry = registry;
        this.stores = stores;
        this.now = now;
    }

    String getLockDir() {
        return lockDir;
    }

    /**
     * {@code install(manifest, opts)}: with an agent configured, the agent resolves; else mutateModules over the
     * project in the lock's directory, null when installDeps found no package.json there.
     */
    Result install(Verification.@Nullable Project project, InstallOptions opts) {
        agent(opts.getConfig());
        return project == null ? mutateModules(Collections.emptyList(), lockDir, opts) :
                mutateModules(Collections.singletonList(project), null, opts);
    }

    /**
     * The lock as it stands, which pnpm writes when no project of the run installs into it.
     */
    Result unchanged(InstallOptions opts) {
        return new Result(lockFile.text(), null, new PnpmContext(new LockfileObject(), lockDir,
                Collections.emptyList(), false), new StrictInstallOptions(opts, Collections.emptyList(), null),
                Collections.emptyList(), Collections.emptyMap());
    }

    /**
     * {@code mutateModules} over the projects installed into the lock, up to {@code _install}'s resolution;
     * {@code missingManifest} names a project pnpm lists without a package.json.
     */
    Result mutateModules(List<Verification.Project> projectManifests, @Nullable String missingManifest,
                         InstallOptions installOptions) {
        StrictInstallOptions opts = ExtendInstallOptions.extendOptions(installOptions, lockDir);
        PnpmOptions options = opts.getConfig();
        boolean workspace = installOptions.getAllProjects() != null;
        agent(options);
        if (options.isTrue("useGitBranchLockfile")) {
            throw new CannotKnow("the git branch (gitBranchLockfile names the lock after it)");
        }
        Object branchPattern = options.get("mergeGitBranchLockfilesBranchPattern");
        if (options.isTrue("mergeGitBranchLockfiles") ||
            branchPattern instanceof List && !((List<?>) branchPattern).isEmpty()) {
            throw new CannotKnow("the git branch lockfiles mergeGitBranchLockfiles merges");
        }
        ReadPackageHook readPackageHook = opts.getReadPackageHook();
        List<Verification.Project> projects = readPackage(readPackageHook, projectManifests);
        List<Verification.Project> allProjects = installOptions.getAllProjects();
        List<Verification.Project> workspaceProjects = allProjects == null || options.getLockfileDir(lockDir) != null ?
                projects : readPackage(readPackageHook, allProjects);

        String lockText = lockFile.text();
        LockfileReader.ReadResult read = lockText == null ?
                new LockfileReader.ReadResult(createLockfileObject(projects, options), false) :
                LockfileReader.read(lockText);
        if (read.getLockfile() == null) {
            throw new LockfileReader.IgnoredLockfile("no " + LockfileReader.WANTED_LOCKFILE);
        }
        LockfileObject wantedLockfile = read.getLockfile();
        boolean existsWantedLockfile = lockText != null;
        for (Verification.Project project : projects) {
            if (!wantedLockfile.getImporters().containsKey(project.getId())) {
                wantedLockfile.getImporters().put(project.getId(), new ProjectSnapshot());
            }
        }
        boolean existsNonEmptyWantedLockfile = existsWantedLockfile && !LockfileWriter.isEmptyLockfile(wantedLockfile);
        VerifyLockfileResolutions.verifyLockfileResolutions(wantedLockfile, stores.resolutionVerifiers(options));
        if (missingManifest != null) {
            throw new JsError(JsError.ERROR,
                    "No package.json found in \"" + (missingManifest.isEmpty() ? "." : missingManifest) + "\"");
        }
        PnpmContext ctx = new PnpmContext(wantedLockfile, lockDir, projects, read.isHadConflicts());

        Object packageExtensions = options.get("packageExtensions");
        String packageExtensionsChecksum = ObjectHasher.hashObjectNullableWithPrefix(packageExtensions);
        Map<String, @Nullable Object> patchedDependencies = patchedDependencies(options);
        Map<String, @Nullable Object> overridesMap = ParseOverrides.createOverridesMapFromParsed(
                opts.getParsedOverrides());
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
                    packageExtensionsChecksum == null ? Js.UNDEFINED : packageExtensionsChecksum);
            wantedLockfile.set("ignoredOptionalDependencies",
                    options.get("ignoredOptionalDependencies") == null ? Js.UNDEFINED :
                            options.get("ignoredOptionalDependencies"));
            wantedLockfile.set("pnpmfileChecksum", Js.UNDEFINED);
            wantedLockfile.set("patchedDependencies", patchedDependencies);
            return new Result(null, outdatedLockfileSettings ? outdatedLockfileSettingName :
                    "lockfileVersion " + Js.string(wantedLockfile.getLockfileVersion()), ctx, opts,
                    workspaceProjects, patchHashes(patchedDependencies));
        }

        boolean pruneLockfileImporters = installOptions.isPruneLockfileImporters();
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
        Map<String, String> patches = patchHashes(patchedDependencies);
        if (reason != null) {
            return new Result(null, reason, ctx, opts, workspaceProjects, patches);
        }
        return new Result(lockFile.writeWantedLockfile(wantedLockfile), null, ctx, opts, workspaceProjects, patches);
    }

    /**
     * The rest of {@code _install} once the lock needs resolving: {@code _installInContext}'s resolution and write.
     */
    String installInContext(Result install) {
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
        if (opts.getOpts().isPruneLockfileImporters()) {
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
        PackageRequester requester = new PackageRequester(opts.getOpts().getStoreController(), files,
                inputs.getHost(), options.isTrue("engineStrict"), options.isTrue("force"), options.getNodeVersion(),
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
        ctx.setWorkspacePackages(workspacePackages(install.getAllProjects()));
        ctx.setHoistPeers(autoInstallPeers || options.isTrue("dedupePeerDependents"));
        ctx.setResolvePeersFromWorkspaceRoot(options.isTrue("resolvePeersFromWorkspaceRoot"));
        ctx.setBlockExoticSubdeps(options.isTrue("blockExoticSubdeps"));
        ctx.setPatchedDependencies(PatchingConfig.groupPatchedDependencies(install.getPatchedDependencies()));
        Double minimumReleaseAge = options.getNumber("minimumReleaseAge");
        List<String> minimumReleaseAgeExclude = options.getStrings("minimumReleaseAgeExclude");
        if (minimumReleaseAge != null && minimumReleaseAge != 0) {
            ctx.setMaximumPublishedBy(now.minusMillis((long) (minimumReleaseAge * 60 * 1000)));
        }
        if (minimumReleaseAgeExclude != null) {
            ctx.setPublishedByExclude(PackageVersionPolicy.createPackageVersionPolicyOrThrow(minimumReleaseAgeExclude,
                    "minimumReleaseAgeExclude"));
        }
        Object trustPolicy = options.get("trustPolicy");
        ctx.setTrustPolicy(trustPolicy == null || trustPolicy == Js.UNDEFINED ? null : Js.string(trustPolicy));
        List<String> trustPolicyExclude = options.getStrings("trustPolicyExclude");
        if (trustPolicyExclude != null) {
            ctx.setTrustPolicyExclude(PackageVersionPolicy.createPackageVersionPolicyOrThrow(trustPolicyExclude,
                    "trustPolicyExclude"));
        }
        ctx.setTrustPolicyIgnoreAfter(options.getNumber("trustPolicyIgnoreAfter"));
        Object peersSuffixMaxLength = options.get("peersSuffixMaxLength");
        Consumer<List<ResolveDependencies.PolicyViolation>> handler =
                opts.getOpts().getHandleResolutionPolicyViolations();
        DepsResolver.Options resolverOptions = new DepsResolver.Options(autoInstallPeers,
                options.isTrue("dedupePeerDependents"), options.isTrue("dedupePeers"),
                options.isTrue("dedupeInjectedDeps"), options.isTrue("excludeLinksFromLockfile"),
                options.isTrue("lockfileIncludeTarballUrl"),
                options.isTrue("resolvePeersFromWorkspaceRoot"),
                peersSuffixMaxLength instanceof Number ? ((Number) peersSuffixMaxLength).intValue() : 1000,
                registries, options.isTrue("allowUnusedPatches"), violations -> {
                    if (handler != null) {
                        handler.accept(violations);
                    }
                });
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

    // readLockfiles without a lock: lockfile.fs' createLockfileObject over the projects installed
    private static LockfileObject createLockfileObject(List<Verification.Project> projects, PnpmOptions options) {
        LockfileObject lockfile = new LockfileObject();
        for (Verification.Project project : projects) {
            ProjectSnapshot importer = new ProjectSnapshot();
            importer.setDependencies(new LinkedHashMap<>());
            importer.setSpecifiers(new LinkedHashMap<>());
            lockfile.getImporters().put(project.getId(), importer);
        }
        lockfile.set("lockfileVersion", LockfileObject.LOCKFILE_VERSION);
        Map<String, @Nullable Object> settings = new LinkedHashMap<>();
        settings.put("autoInstallPeers", options.get("autoInstallPeers"));
        settings.put("excludeLinksFromLockfile", options.get("excludeLinksFromLockfile"));
        settings.put("peersSuffixMaxLength", options.get("peersSuffixMaxLength"));
        lockfile.set("settings", settings);
        return lockfile;
    }

    // getContext runs the readPackage hooks over each project's manifest
    private static List<Verification.Project> readPackage(@Nullable ReadPackageHook readPackageHook,
                                                          List<Verification.Project> projects) {
        List<Verification.Project> read = new ArrayList<>();
        for (Verification.Project project : projects) {
            read.add(readPackageHook == null ? project : new Verification.Project(project.getRootDir(),
                    project.getId(), readPackageHook.apply(project.getManifest(), project.getRootDir())));
        }
        return read;
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

    /**
     * installing.context's arrayOfWorkspacePackagesToMap: each project by name, then version.
     */
    static Map<String, Map<String, Verification.Project>> workspacePackages(List<Verification.Project> projects) {
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

    /**
     * The specifier pnpm writes into the manifest, and the catalog it adds {@code alias: specifier} to (null for
     * none).
     */
    @Value
    static class Saved {
        String bareSpecifier;

        @Nullable
        String catalogName;
    }

    /**
     * installSome's catalogMode check for {@code pnpm add <alias>@<specifier>}, between ParseWantedDependencies and
     * ResolveDependencyTree: what it saves, under the workspace manifest as its reader reads it (null without one).
     */
    static Saved installSome(String alias, String bareSpecifier, @Nullable Map<String, @Nullable Object> workspace) {
        Map<String, @Nullable Map<String, @Nullable Object>> catalogs;
        try {
            catalogs = Catalogs.getCatalogsFromWorkspaceManifest(workspace);
        } catch (PnpmError e) {
            throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, alias, "[" + e.getCode() + "] " + e.getMessage());
        }
        Map<String, @Nullable Object> defaultCatalog = catalogs.get(DEFAULT_CATALOG);
        String wanted = ParseWantedDependencies.bareSpecifier(alias, bareSpecifier, defaultCatalog);
        String saveCatalogName = saveCatalogName(workspace);
        if (!"manual".equals(catalogMode(workspace))) {
            String perDepCatalogName = saveCatalogName == null ? DEFAULT_CATALOG : saveCatalogName;
            String catalogBareSpecifier = CATALOG_PROTOCOL + (DEFAULT_CATALOG.equals(perDepCatalogName) ? "" :
                    perDepCatalogName);
            Catalogs.Resolution catalog = Catalogs.resolveFromCatalog(catalogs, alias, catalogBareSpecifier);
            String catalogDepSpecifier = "found".equals(catalog.getType()) ? catalog.getSpecifier() : null;
            if (catalogDepSpecifier == null || wanted.equals(catalogBareSpecifier) || eq(alias, wanted,
                    catalogDepSpecifier)) {
                saveCatalogName = perDepCatalogName;
            } else if ("strict".equals(catalogMode(workspace))) {
                throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, alias, "[ERR_PNPM_CATALOG_VERSION_MISMATCH] " +
                                                                          "Wanted dependency outside the version " +
                                                                          "range defined in catalog");
            }
        }
        String lookup = CATALOG_PROTOCOL.equals(wanted) ? null :
                ResolveDependencyTree.catalogLookup(alias, saveCatalogName, defaultCatalog);
        return lookup == null ? new Saved(wanted, null) : new Saved(lookup, saveCatalogName);
    }

    // semver.validRange of both, then semver.eq, which takes versions only
    private static boolean eq(String alias, String wanted, String catalogDepSpecifier) {
        if (NodeSemver.validRange(wanted, NodeSemver.Options.DEFAULT) == null ||
            NodeSemver.validRange(catalogDepSpecifier, NodeSemver.Options.DEFAULT) == null) {
            return false;
        }
        try {
            return NodeSemver.compare(wanted, catalogDepSpecifier, NodeSemver.Options.DEFAULT) == 0;
        } catch (IllegalArgumentException e) {
            throw Rejected.crashes(Reason.RESOLUTION_REQUIRED, alias, "TypeError: " + e.getMessage());
        }
    }

    // getConfig's catalogMode: manual when unset
    static String catalogMode(@Nullable Map<String, @Nullable Object> workspace) {
        Object mode = workspace == null || !workspace.containsKey("catalogMode") ? "manual" :
                workspace.get("catalogMode");
        return mode == null ? "null" : Js.string(mode);
    }

    static @Nullable String saveCatalogName(@Nullable Map<String, @Nullable Object> workspace) {
        Object value = workspace == null ? null : workspace.get("saveCatalogName");
        return value == null || value instanceof Map || value instanceof List ? null : Js.string(value);
    }
}
