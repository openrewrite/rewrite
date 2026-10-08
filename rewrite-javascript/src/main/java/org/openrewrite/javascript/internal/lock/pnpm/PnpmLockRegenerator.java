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
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.npm.Host;
import org.openrewrite.semver.NodeSemver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Port of pnpm 11.2.2's {@code pnpm install --lockfile-only}: pnpm/src main.ts and getConfig.ts before the command,
 * then @pnpm/installing.commands installDeps and @pnpm/installing.deps-installer install/index.ts.
 */
public final class PnpmLockRegenerator {

    static final String PNPM_REJECTS = "pnpm rejects this edit";
    static final String DISCARDS_LOCK = "pnpm would discard the lock and resolve everything from scratch";
    static final String SECOND_INSTALL = "a second pnpm install over the lock written for this edit";

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

        @NonNull
        RegistryMetadata registry;

        @Builder.Default
        Host host = Host.UNKNOWN;

        /**
         * The files the project holds relative to its root, when the host says.
         */
        @Nullable
        Set<String> paths;
    }

    /**
     * What a lock-only install does before it resolves: the lock it writes as it stands, or why it resolves.
     */
    @Value
    static class Install {
        @Nullable
        String written;

        /**
         * Why pnpm resolves the graph: the setting the lock records differently, or the projects not being up to
         * date with it; null when it writes the lock as it stands.
         */
        @Nullable
        String resolution;

        LockfileObject wantedLockfile;

        PnpmOptions options;

        /**
         * The lock's directory, which importer ids and {@code link:} and {@code file:} paths in it are relative to.
         */
        String lockfileDir;

        /**
         * The projects installed into the lock.
         */
        List<Verification.Project> projects;

        /**
         * Every project of the workspace, which {@code workspace:} specs resolve to; the installed ones without one.
         */
        List<Verification.Project> workspaceProjects;

        boolean pruneLockfileImporters;

        boolean lockfileHadConflicts;

        /**
         * The lock text whose env document a write keeps in front.
         */
        @Nullable
        String lockText;

        @Nullable
        ReadPackageHook readPackageHook;

        /**
         * Each {@code patchedDependencies} key with its patch file's hash.
         */
        Map<String, String> patchedDependencies;
    }


    private static final String PNPM_VERSION = "11.2.2";

    private final Inputs inputs;

    // a project pnpm lists without a package.json, which it reports once the lock is verified
    private @Nullable String missingManifest;

    public PnpmLockRegenerator(Inputs inputs) {
        this.inputs = inputs;
    }

    /**
     * The lock pnpm writes for the edited manifests over the project's lock, or the failure, after a second install
     * over the written lock changed nothing.
     */
    public Result regenerate() {
        if (inputs.getLock() == null) {
            return Result.failure(new Failure(Reason.MALFORMED_LOCK, null, DISCARDS_LOCK + ": the project has no " +
                                                                           LockfileReader.WANTED_LOCKFILE));
        }
        String written;
        try {
            written = writeLock();
        } catch (RuntimeException e) {
            return Result.failure(failure(e, ""));
        }
        try {
            String rewritten = new PnpmLockRegenerator(inputs.toBuilder().lock(written).build()).writeLock();
            if (!rewritten.equals(written)) {
                return Result.failure(new Failure(Reason.RESOLUTION_REQUIRED, null,
                        SECOND_INSTALL + " would rewrite it: " + firstDifference(written, rewritten)));
            }
        } catch (RuntimeException e) {
            return Result.failure(failure(e, SECOND_INSTALL + " would reject it: "));
        }
        return Result.success(written);
    }

    /**
     * How many directories above the lock's the workspace root is whose shared lock pnpm installs the lock's project
     * into, when the workspace lists it; else 0.
     */
    public static int installRoot(Inputs inputs) {
        String workspaceManifest = inputs.getFiles().get("pnpm-workspace.yaml");
        if (inputs.getLockDir().isEmpty() || workspaceManifest == null) {
            return 0;
        }
        String rootManifestText = inputs.getManifests().get("");
        PnpmOptions options = PnpmOptions.load(workspaceManifest, inputs.getFiles().get(".npmrc"),
                rootManifestText == null ? null : rootManifest(rootManifestText), inputs.getEnv());
        if (!options.isTrue("sharedWorkspaceLockfile") || !FindPackages.findWorkspaceProjects(
                inputs.getManifests().keySet(), options.getWorkspacePackagePatterns()).contains(inputs.getLockDir())) {
            return 0;
        }
        return inputs.getLockDir().split("/").length;
    }

    /**
     * One {@code pnpm install --lockfile-only} over the lock: the lock it writes.
     */
    String writeLock() {
        Install install = install();
        if (install.getWritten() != null) {
            return install.getWritten();
        }
        return resolve(install);
    }

    private static Failure failure(RuntimeException e, String prefix) {
        if (e instanceof LockfileReader.IgnoredLockfile) {
            return new Failure(Reason.MALFORMED_LOCK, null, prefix + DISCARDS_LOCK + ": " + e.getMessage());
        }
        if (e instanceof CannotKnow) {
            return new Failure(Reason.RESOLUTION_REQUIRED, null, prefix + e.getMessage());
        }
        if (e instanceof ReportPeerDependencyIssues.PeerDependencyIssuesError) {
            return new Failure(Reason.PEER_CONFLICT,
                    ((ReportPeerDependencyIssues.PeerDependencyIssuesError) e).firstPeerName(),
                    prefix + PNPM_REJECTS + ": [ERR_PNPM_PEER_DEP_ISSUES] " + e.getMessage());
        }
        if (e instanceof PnpmError) {
            String code = ((PnpmError) e).getCode();
            return new Failure(reasonFor(code), null, prefix + PNPM_REJECTS + ": [" + code + "] " + e.getMessage());
        }
        if (e instanceof JsError) {
            String code = ((JsError) e).getCode();
            Reason reason = "ERR_INVALID_PACKAGE_CONFIG".equals(code) ? Reason.MALFORMED_MANIFEST :
                    Reason.RESOLUTION_REQUIRED;
            return new Failure(reason, null, prefix + PNPM_REJECTS + ": " + (code == null ? "" : "[" + code + "] ") +
                                             e.getMessage());
        }
        // the lock and the env document catch their own; what reaches here is pnpm-workspace.yaml's
        if (e instanceof JsYaml.YamlException) {
            return new Failure(Reason.MALFORMED_MANIFEST, null, prefix + PNPM_REJECTS + ": " + e.getMessage());
        }
        if (e instanceof LockfileFormatConverters.TypeError) {
            return new Failure(Reason.MALFORMED_LOCK, null, prefix + PNPM_REJECTS + ": TypeError: " + e.getMessage());
        }
        throw e;
    }

    private static Reason reasonFor(String code) {
        switch (code) {
            case "ERR_PNPM_NO_MATCHING_VERSION":
            case "ERR_PNPM_NO_MATURE_MATCHING_VERSION":
            case "ERR_PNPM_NO_MATCHING_VERSION_INSIDE_WORKSPACE":
            case "ERR_PNPM_NODEJS_VERSION_NOT_FOUND":
                return Reason.VERSION_NOT_FOUND;
            case "ERR_PNPM_FETCH_404":
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

    private static String firstDifference(String a, String b) {
        String[] as = a.split("\n", -1);
        String[] bs = b.split("\n", -1);
        for (int i = 0; i < Math.max(as.length, bs.length); i++) {
            String x = i < as.length ? as[i] : "";
            String y = i < bs.length ? bs[i] : "";
            if (!x.equals(y)) {
                return "line " + (i + 1) + " \"" + x.trim() + "\" becomes \"" + y.trim() + "\"";
            }
        }
        return "the line endings change";
    }

    /**
     * The rest of {@code _install} once the lock needs resolving: {@code _installInContext}'s resolution and write.
     */
    String resolve(Install install) {
        PnpmOptions options = install.getOptions();
        LockfileObject wantedLockfile = install.getWantedLockfile();
        boolean autoInstallPeers = options.isTrue("autoInstallPeers");
        List<Verification.Project> projects = install.getProjects();

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
        if (install.isPruneLockfileImporters()) {
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
        @SuppressWarnings("unchecked")
        Map<String, @Nullable Object> supportedArchitectures = options.get("supportedArchitectures") instanceof Map ?
                (Map<String, @Nullable Object>) options.get("supportedArchitectures") : null;
        NpmResolver npmResolver = storeResolver(options, registries);
        PackageRequester requester = new PackageRequester(npmResolver, files, inputs.getHost(),
                options.isTrue("engineStrict"), options.isTrue("force"), nodeVersion(options), supportedArchitectures,
                inputs.getRegistry(), nodeDownloadMirrors(options));

        ResolveDependencies.ResolutionContext ctx = new ResolveDependencies.ResolutionContext(wantedLockfile,
                install.getLockfileDir(), requester);
        ctx.setAutoInstallPeers(autoInstallPeers);
        ctx.setAutoInstallPeersFromHighestMatch(options.isTrue("autoInstallPeersFromHighestMatch"));
        ctx.setCatalogs(catalogs);
        ctx.setForceFullResolution(true);
        ctx.setInjectWorkspacePackages(options.isTrue("injectWorkspacePackages"));
        ctx.setLinkWorkspacePackagesDepth(options.getLinkWorkspacePackagesDepth());
        ctx.setPreferWorkspacePackages(options.isTrue("preferWorkspacePackages"));
        ctx.setReadPackageHook(install.getReadPackageHook());
        ctx.setRegistries(registries);
        ctx.setNamedRegistryPrefixes(namedRegistryPrefixes(options));
        Object resolutionMode = options.get("resolutionMode");
        ctx.setResolutionMode(resolutionMode == null ? null : Js.string(resolutionMode));
        ctx.setWorkspacePackages(workspacePackages(install.getWorkspaceProjects()));
        ctx.setHoistPeers(autoInstallPeers || options.isTrue("dedupePeerDependents"));
        ctx.setResolvePeersFromWorkspaceRoot(options.isTrue("resolvePeersFromWorkspaceRoot"));
        ctx.setBlockExoticSubdeps(options.isTrue("blockExoticSubdeps"));
        ctx.setPatchedDependencies(PatchingConfig.groupPatchedDependencies(install.getPatchedDependencies()));
        Double minimumReleaseAge = number(options, "minimumReleaseAge");
        List<String> minimumReleaseAgeExclude = strings(options, "minimumReleaseAgeExclude");
        if (minimumReleaseAge != null && minimumReleaseAge != 0) {
            ctx.setMaximumPublishedBy(inputs.getNow().minusMillis((long) (minimumReleaseAge * 60 * 1000)));
        }
        if (minimumReleaseAgeExclude != null) {
            ctx.setPublishedByExclude(PackageVersionPolicy.createPackageVersionPolicyOrThrow(minimumReleaseAgeExclude,
                    "minimumReleaseAgeExclude"));
        }
        Object trustPolicy = options.get("trustPolicy");
        ctx.setTrustPolicy(trustPolicy == null || trustPolicy == JsYaml.UNDEFINED ? null : Js.string(trustPolicy));
        List<String> trustPolicyExclude = strings(options, "trustPolicyExclude");
        if (trustPolicyExclude != null) {
            ctx.setTrustPolicyExclude(PackageVersionPolicy.createPackageVersionPolicyOrThrow(trustPolicyExclude,
                    "trustPolicyExclude"));
        }
        ctx.setTrustPolicyIgnoreAfter(number(options, "trustPolicyIgnoreAfter"));
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
        String written = LockfileWriter.writeWantedLockfile(newLockfile, install.getLockText());
        ReportPeerDependencyIssues.reportPeerDependencyIssues(result.getPeerDependencyIssuesByProjects(),
                peerDependencyRules(options), options.isTrue("strictPeerDependencies"));
        return written;
    }

    // getConfig's resolveAndInstallConfigDeps: the lock text with the env document it leaves
    @SuppressWarnings("unchecked")
    private @Nullable String installConfigDeps(Object configDependencies, PnpmOptions options,
                                               Map<String, String> namedRegistries, @Nullable String lockText) {
        EnvLockfile envLockfile = EnvLockfile.readEnvLockfile(lockText);
        if (envLockfile == null) {
            envLockfile = EnvLockfile.createEnvLockfile();
        }
        EnvInstaller.ConfigDepsResult configDeps = EnvInstaller.resolveAndInstallConfigDeps(
                (Map<String, @Nullable Object>) configDependencies, envLockfile, options.getRegistries());
        if (!configDeps.getDepsToResolve().isEmpty()) {
            resolveConfigDeps(configDeps.getDepsToResolve(), envLockfile, options.getRegistries(), namedRegistries);
            return EnvLockfile.writeEnvLockfile(lockText, envLockfile);
        }
        return configDeps.isLockfileChanged() ? EnvLockfile.writeEnvLockfile(lockText, envLockfile) : lockText;
    }

    // resolveAndInstallConfigDeps from the registry: each config dependency and its exact optional dependencies into
    // the env document, which is then pruned
    @SuppressWarnings("unchecked")
    private void resolveConfigDeps(Map<String, String> depsToResolve, EnvLockfile envLockfile,
                                   Map<String, String> registries, Map<String, String> namedRegistries) {
        NpmResolver resolveFromNpm = new NpmResolver(inputs.getRegistry(), registries, namedRegistries, null, true,
                false, inputs.getNow());
        for (Map.Entry<String, String> dep : depsToResolve.entrySet()) {
            String name = dep.getKey();
            String specifier = dep.getValue();
            NpmResolver.ResolveResult resolution = resolveFromNpm(resolveFromNpm, name, specifier, false);
            if (resolution == null || !(resolution.resolution.get("integrity") instanceof String) ||
                ((String) resolution.resolution.get("integrity")).isEmpty()) {
                throw new PnpmError("BAD_CONFIG_DEP", "Cannot resolve " + name + "@" + specifier +
                                                      " as a configuration dependency because it has no integrity");
            }
            Map<String, @Nullable Object> manifest = resolution.manifest == null ? new LinkedHashMap<>() :
                    resolution.manifest;
            String version = Js.string(manifest.get("version"));
            String registry = PickRegistryForPackage.pickRegistryForPackage(registries, name, null);
            String pkgKey = name + "@" + version;
            Map<String, @Nullable Object> locked = new LinkedHashMap<>();
            locked.put("specifier", specifier);
            locked.put("version", version);
            envLockfile.configDependencies().put(name, locked);
            Map<String, @Nullable Object> entry = new LinkedHashMap<>();
            entry.put("resolution", LockfileUtils.toLockfileResolution(name, version, resolution.resolution, registry,
                    false));
            envLockfile.packages().put(pkgKey, entry);
            Map<String, @Nullable Object> optionalSubdeps = new LinkedHashMap<>();
            Map<String, @Nullable Object> optionalDeps =
                    ResolveDependencies.asMap(manifest.get("optionalDependencies"));
            for (String subdepName : Js.keys(optionalDeps)) {
                String subdepSpec = Js.string(optionalDeps.get(subdepName));
                if (NodeSemver.valid(subdepSpec, NodeSemver.Options.DEFAULT) == null) {
                    throw new PnpmError("CONFIG_DEP_OPTIONAL_NOT_EXACT", "Cannot install \"" + subdepName + "@" +
                            subdepSpec + "\" as an optionalDependency of config dependency \"" + name +
                            "\": only exact versions are supported (got \"" + subdepSpec + "\")");
                }
                NpmResolver.ResolveResult sub;
                try {
                    sub = resolveFromNpm(resolveFromNpm, subdepName, subdepSpec, true);
                } catch (PnpmError e) {
                    if ("ERR_PNPM_TRUST_DOWNGRADE".equals(e.getCode())) {
                        throw e;
                    }
                    continue;
                } catch (JsError e) {
                    continue;
                }
                if (sub == null || !(sub.resolution.get("integrity") instanceof String) ||
                    ((String) sub.resolution.get("integrity")).isEmpty() || sub.manifest == null) {
                    throw new PnpmError("BAD_CONFIG_DEP", "Cannot resolve optionalDependency \"" + subdepName +
                            "\" of config dependency \"" + name + "\" because it has no integrity");
                }
                String subVersion = Js.string(sub.manifest.get("version"));
                String subKey = subdepName + "@" + subVersion;
                Map<String, @Nullable Object> subEntry = new LinkedHashMap<>();
                subEntry.put("resolution", LockfileUtils.toLockfileResolution(subdepName, subVersion, sub.resolution,
                        PickRegistryForPackage.pickRegistryForPackage(registries, subdepName, null), false));
                for (String field : new String[]{"os", "cpu", "libc"}) {
                    Object value = sub.manifest.get(field);
                    if (value instanceof List && !((List<?>) value).isEmpty() ||
                        value instanceof String && !((String) value).isEmpty()) {
                        subEntry.put(field, value);
                    }
                }
                envLockfile.packages().put(subKey, subEntry);
                if (envLockfile.snapshots().get(subKey) == null) {
                    Map<String, @Nullable Object> optional = new LinkedHashMap<>();
                    optional.put("optional", true);
                    envLockfile.snapshots().put(subKey, optional);
                }
                optionalSubdeps.put(subdepName, subVersion);
            }
            Map<String, @Nullable Object> snapshot = new LinkedHashMap<>();
            if (!optionalSubdeps.isEmpty()) {
                snapshot.put("optionalDependencies", optionalSubdeps);
            }
            envLockfile.snapshots().put(pkgKey, snapshot);
        }
        pruneEnvLockfile(envLockfile, convertToLockfileEnvObject(envLockfile));
    }

    // pruneEnvLockfile.ts convertToLockfileEnvObject: config and package manager dependencies as one importer
    private static LockfileObject convertToLockfileEnvObject(EnvLockfile envLockfile) {
        Map<String, @Nullable Object> dependencies = new LinkedHashMap<>(envLockfile.configDependencies());
        if (envLockfile.packageManagerDependencies() != null) {
            dependencies.putAll(envLockfile.packageManagerDependencies());
        }
        Map<String, @Nullable Object> importer = new LinkedHashMap<>();
        importer.put("dependencies", dependencies);
        Map<String, @Nullable Object> importers = new LinkedHashMap<>();
        importers.put(".", importer);
        Map<String, @Nullable Object> file = new LinkedHashMap<>();
        file.put("lockfileVersion", envLockfile.getDocument().get("lockfileVersion"));
        file.put("importers", importers);
        file.put("packages", envLockfile.packages());
        file.put("snapshots", envLockfile.snapshots());
        return LockfileFormatConverters.convertToLockfileObject(file);
    }

    private static void pruneEnvLockfile(EnvLockfile envLockfile, LockfileObject lockfileObject) {
        LockfileObject pruned = UpdateLockfile.pruneSharedLockfile(lockfileObject, null);
        Map<String, @Nullable Object> prunedFile = LockfileFormatConverters.convertToLockfileFile(pruned);
        Object packages = prunedFile.get("packages");
        Object snapshots = prunedFile.get("snapshots");
        envLockfile.getDocument().put("packages", packages == null ? new LinkedHashMap<>() : packages);
        envLockfile.getDocument().put("snapshots", snapshots == null ? new LinkedHashMap<>() : snapshots);
    }

    /**
     * The env document after resolvePackageManagerIntegrities, and the lock text it was saved into.
     */
    @Value
    private static class PackageManagerIntegrities {
        EnvLockfile envLockfile;

        @Nullable
        String lockText;
    }

    // resolvePackageManagerIntegrities.ts: pnpm, @pnpm/exe and their dependencies into packageManagerDependencies
    private PackageManagerIntegrities resolvePackageManagerIntegrities(String pnpmVersion,
                                                                       @Nullable EnvLockfile envLockfile,
                                                                       boolean save, PnpmOptions options,
                                                                       @Nullable String lockText) {
        EnvLockfile lockfileToUpdate = envLockfile != null ? envLockfile :
                save ? EnvLockfile.readEnvLockfile(lockText) : null;
        if (lockfileToUpdate == null) {
            lockfileToUpdate = EnvLockfile.createEnvLockfile();
        }
        if (EnvInstaller.isPackageManagerResolved(lockfileToUpdate, pnpmVersion)) {
            return new PackageManagerIntegrities(lockfileToUpdate, lockText);
        }
        Map<String, @Nullable Object> dependencies = new LinkedHashMap<>();
        dependencies.put("pnpm", pnpmVersion);
        dependencies.put("@pnpm/exe", pnpmVersion);
        Map<String, @Nullable Object> manifest = new LinkedHashMap<>();
        manifest.put("dependencies", dependencies);
        LockfileObject lockfile = resolveManifestDependencies(manifest, options);
        if (lockfile.getPackages() == null) {
            return new PackageManagerIntegrities(lockfileToUpdate, lockText);
        }
        ProjectSnapshot importer = lockfile.getImporters().get(".");
        Map<String, @Nullable Object> packageManagerDependencies = new LinkedHashMap<>();
        if (importer.getDependencies() != null) {
            for (Map.Entry<String, @Nullable String> dependency : importer.getDependencies().entrySet()) {
                Map<String, @Nullable Object> locked = new LinkedHashMap<>();
                locked.put("specifier", importer.getSpecifiers().get(dependency.getKey()));
                locked.put("version", dependency.getValue());
                packageManagerDependencies.put(dependency.getKey(), locked);
            }
        }
        lockfileToUpdate.importer().put("packageManagerDependencies", packageManagerDependencies);
        LockfileObject merged = convertToLockfileEnvObject(lockfileToUpdate);
        if (merged.getPackages() == null) {
            merged.setPackages(new LinkedHashMap<>());
        }
        merged.getPackages().putAll(lockfile.getPackages());
        pruneEnvLockfile(lockfileToUpdate, merged);
        return new PackageManagerIntegrities(lockfileToUpdate,
                save ? EnvLockfile.writeEnvLockfile(lockText, lockfileToUpdate) : lockText);
    }

    // resolveManifestDependencies.ts: the lock deps-resolver writes for one manifest over an empty lock, with the
    // store controller's resolver
    private LockfileObject resolveManifestDependencies(Map<String, @Nullable Object> manifest, PnpmOptions options) {
        LockfileObject emptyLockfile = new LockfileObject();
        emptyLockfile.set("lockfileVersion", LockfileObject.LOCKFILE_VERSION);
        emptyLockfile.getImporters().put(".", new ProjectSnapshot());
        List<ResolveDependencies.WantedDependency> wantedDependencies =
                ResolveDependencyTree.getWantedDependencies(manifest, false);
        for (ResolveDependencies.WantedDependency dep : wantedDependencies) {
            dep.setUpdateSpec(true);
        }
        Map<String, String> registries = options.getRegistries();
        PackageRequester requester = new PackageRequester(storeResolver(options, registries), new LinkedHashMap<>(),
                inputs.getHost(), options.isTrue("engineStrict"), options.isTrue("force"), nodeVersion(options), null,
                inputs.getRegistry(), nodeDownloadMirrors(options));
        ResolveDependencies.ResolutionContext ctx = new ResolveDependencies.ResolutionContext(emptyLockfile, "",
                requester);
        ctx.setForceFullResolution(true);
        ctx.setRegistries(registries);
        ctx.setNamedRegistryPrefixes(namedRegistryPrefixes(options));
        ctx.setWorkspacePackages(new LinkedHashMap<>());
        DepsResolver.Options resolverOptions = new DepsResolver.Options(false, false, false, false, false, false,
                false, 1000, registries, true, violations -> {
            throw new PnpmError("RESOLUTION_POLICY_VIOLATIONS_UNHANDLED", violations.size() +
                    " resolution-policy " + (violations.size() == 1 ? "violation was" : "violations were") +
                    " produced but no handleResolutionPolicyViolations callback was wired to react to them.");
        });
        List<ResolveDependencyTree.Importer> importers = Collections.singletonList(
                new ResolveDependencyTree.Importer(".", "", manifest, wantedDependencies));
        return DepsResolver.resolveDependencies(importers, ctx, PreferredVersions.ByName.of(new LinkedHashMap<>()),
                resolverOptions).getNewLockfile();
    }

    // createNewStoreController's npm resolver
    private NpmResolver storeResolver(PnpmOptions options, Map<String, String> registries) {
        boolean fullMetadata = ("time-based".equals(options.get("resolutionMode")) ||
                                "no-downgrade".equals(options.get("trustPolicy"))) &&
                               !options.isTrue("registrySupportsTimeField");
        return new NpmResolver(inputs.getRegistry(), registries,
                NpmResolver.mergeNamedRegistries(namedRegistries(options)), options.get("saveWorkspaceProtocol"),
                !Boolean.FALSE.equals(options.get("minimumReleaseAgeIgnoreMissingTime")), fullMetadata,
                inputs.getNow());
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, @Nullable Object> nodeDownloadMirrors(PnpmOptions options) {
        return options.get("nodeDownloadMirrors") instanceof Map ?
                (Map<String, @Nullable Object>) options.get("nodeDownloadMirrors") : null;
    }

    private static List<String> namedRegistryPrefixes(PnpmOptions options) {
        List<String> namedRegistryPrefixes = new ArrayList<>();
        namedRegistryPrefixes.add("gh:");
        Object named = options.get("namedRegistries");
        if (named instanceof Map) {
            for (Object key : ((Map<?, ?>) named).keySet()) {
                if (!"gh".equals(key)) {
                    namedRegistryPrefixes.add(key + ":");
                }
            }
        }
        return namedRegistryPrefixes;
    }

    private static NpmResolver.@Nullable ResolveResult resolveFromNpm(NpmResolver resolver, String name,
                                                                      String specifier, boolean optional) {
        ResolveDependencies.WantedDependency wanted = new ResolveDependencies.WantedDependency(name, specifier, false,
                optional);
        PackageRequester.RequestPackageOptions opts = new PackageRequester.RequestPackageOptions();
        opts.lockfileDir = "";
        opts.projectDir = "";
        return resolver.resolveNpm(wanted, opts, PreferredVersions.ByName.of(new LinkedHashMap<>()));
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

    @SuppressWarnings("unchecked")
    private static ReportPeerDependencyIssues.@Nullable PeerDependencyRules peerDependencyRules(PnpmOptions options) {
        Object rules = options.get("peerDependencyRules");
        if (rules == null || rules == JsYaml.UNDEFINED) {
            return null;
        }
        if (!(rules instanceof Map)) {
            throw new CannotKnow("what pnpm makes of peerDependencyRules (" + Js.typeOf(rules) +
                                 ", where pnpm reads an object)");
        }
        Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) rules;
        Map<String, String> allowedVersions = null;
        Object allowed = map.get("allowedVersions");
        if (allowed != null && allowed != JsYaml.UNDEFINED) {
            if (!(allowed instanceof Map)) {
                throw new CannotKnow("what pnpm makes of peerDependencyRules.allowedVersions (" + Js.typeOf(allowed) +
                                     ", where pnpm reads an object)");
            }
            allowedVersions = new LinkedHashMap<>();
            for (String key : Js.keys((Map<String, @Nullable Object>) allowed)) {
                Object range = ((Map<String, @Nullable Object>) allowed).get(key);
                if (!(range instanceof String)) {
                    throw new CannotKnow("what pnpm makes of peerDependencyRules.allowedVersions." + key + " (" +
                                         Js.typeOf(range) + ", where pnpm reads a string)");
                }
                allowedVersions.put(key, (String) range);
            }
        }
        return new ReportPeerDependencyIssues.PeerDependencyRules(
                strings(map.get("ignoreMissing"), "peerDependencyRules.ignoreMissing"),
                strings(map.get("allowAny"), "peerDependencyRules.allowAny"), allowedVersions);
    }

    Install install() {
        String rootManifestText = inputs.getManifests().get("");
        Map<String, @Nullable Object> rootManifest = rootManifestText == null ? null : rootManifest(rootManifestText);
        PnpmOptions options = PnpmOptions.load(inputs.getFiles().get("pnpm-workspace.yaml"),
                inputs.getFiles().get(".npmrc"), rootManifest, inputs.getEnv());
        boolean workspace = options.getWorkspacePackagePatterns() != null;
        boolean shared = workspace && options.isTrue("sharedWorkspaceLockfile");
        String lockfileDir = shared ? "" : inputs.getLockDir();
        // the env document belongs to the lock of rootProjectManifestDir, the workspace's
        boolean envDocument = !workspace || inputs.getLockDir().isEmpty();
        String lockText = checkWantedPackageManager(options, inputs.getLock(), envDocument);

        Object configDependencies = options.get("configDependencies");
        if (Js.truthy(configDependencies)) {
            Map<String, String> namedRegistries = NpmResolver.mergeNamedRegistries(namedRegistries(options));
            if (!(configDependencies instanceof Map)) {
                throw new CannotKnow("what pnpm makes of configDependencies (" + Js.typeOf(configDependencies) +
                                     ", where pnpm reads an object)");
            }
            // pnpm installs them into the workspace's lock
            if (envDocument) {
                lockText = installConfigDeps(configDependencies, options, namedRegistries, lockText);
            }
        }
        loadHooks(options, configDependencies);
        List<Verification.Project> allProjects = workspace ? projects(options, true) : null;
        ProjectsGraph.Result projectsGraph = allProjects == null ? null :
                ProjectsGraph.createProjectsGraph(allProjects, false, Js.truthy(options.get(
                        "linkWorkspacePackages")));
        // installDeps creates its store controller, whose npm resolver checks the named registries
        NpmResolver.mergeNamedRegistries(namedRegistries(options));

        // in a workspace pnpm install is recursive over the projects it lists, which without a shared lock each
        // install into their own
        List<Verification.Project> projectManifests;
        if (allProjects == null) {
            projectManifests = projects(options, false);
        } else if (shared) {
            projectManifests = inputs.getLockDir().isEmpty() ? allProjects : Collections.emptyList();
        } else {
            projectManifests = new ArrayList<>();
            for (Verification.Project project : allProjects) {
                if (project.getRootDir().equals(inputs.getLockDir())) {
                    projectManifests.add(new Verification.Project(project.getRootDir(), ".", project.getManifest()));
                    if (!inputs.getLockDir().isEmpty() && !options.isTrue("ignorePnpmfile")) {
                        loadDefaultPnpmfile(inputs.getLockDir() + "/");
                    }
                }
            }
        }
        Object runtimeOnFail = options.get("runtimeOnFail");
        if (Js.truthy(runtimeOnFail)) {
            for (Verification.Project project : allProjects != null ? allProjects : projectManifests) {
                PkgManifestUtils.applyRuntimeOnFailOverride(project.getManifest(), runtimeOnFail);
            }
        }
        if (projectsGraph != null) {
            checkWorkspaceCycles(options, projectsGraph);
        } else {
            agent(options);
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
            return new Install(inputs.getLock(), null, new LockfileObject(), options, lockfileDir,
                    Collections.emptyList(), Collections.emptyList(), false, false, inputs.getLock(), null,
                    Collections.emptyMap());
        }
        List<ParseOverrides.VersionOverride> parsedOverrides = ParseOverrides.parseOverrides(options.getOverrides(),
                options.getCatalogs());
        ReadPackageHook readPackageHook = ReadPackageHook.createReadPackageHook(
                options.isTrue("ignoreCompatibilityDb"), lockfileDir, parsedOverrides,
                options.get("ignoredOptionalDependencies"), options.get("packageExtensions"));
        if (workspace) {
            agent(options);
        }
        List<Verification.Project> projects = readPackage(readPackageHook, projectManifests);
        List<Verification.Project> workspaceProjects = allProjects == null ? projects :
                shared ? projects : readPackage(readPackageHook, allProjects);

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

        Object packageExtensions = options.get("packageExtensions");
        String packageExtensionsChecksum = ObjectHasher.hashObjectNullableWithPrefix(packageExtensions);
        Map<String, @Nullable Object> patchedDependencies = patchedDependencies(options);
        Map<String, @Nullable Object> overridesMap = ParseOverrides.createOverridesMapFromParsed(parsedOverrides);
        Object ignoredOptionalDependencies = options.get("ignoredOptionalDependencies");
        String outdatedLockfileSettingName = SettingsChecker.getOutdatedLockfileSetting(wantedLockfile,
                SettingsChecker.Settings.builder()
                        .catalogs(options.getCatalogs())
                        .overrides(overridesMap)
                        .packageExtensionsChecksum(packageExtensionsChecksum)
                        .patchedDependencies(patchedDependencies)
                        .ignoredOptionalDependencies(sorted(ignoredOptionalDependencies))
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
        wantedLockfile.set("settings", lockfileSettings(options));
        if (needsFullResolution) {
            wantedLockfile.set("overrides", overridesMap);
            wantedLockfile.set("packageExtensionsChecksum",
                    packageExtensionsChecksum == null ? JsYaml.UNDEFINED : packageExtensionsChecksum);
            wantedLockfile.set("ignoredOptionalDependencies",
                    ignoredOptionalDependencies == null ? JsYaml.UNDEFINED : ignoredOptionalDependencies);
            wantedLockfile.set("pnpmfileChecksum", JsYaml.UNDEFINED);
            wantedLockfile.set("patchedDependencies", patchedDependencies);
            return new Install(null, outdatedLockfileSettings ? outdatedLockfileSettingName :
                    "lockfileVersion " + Js.string(wantedLockfile.getLockfileVersion()), wantedLockfile, options,
                    lockfileDir, projects, workspaceProjects, workspace, read.isHadConflicts(), lockText,
                    readPackageHook, patchHashes(patchedDependencies));
        }

        boolean pruneLockfileImporters = workspace;
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
                lockfileDir)) {
            reason = "projects not up to date with the lock";
        }
        if (reason != null) {
            return new Install(null, reason, wantedLockfile, options, lockfileDir, projects, workspaceProjects,
                    pruneLockfileImporters, read.isHadConflicts(), lockText, readPackageHook,
                    patchHashes(patchedDependencies));
        }
        return new Install(LockfileWriter.writeWantedLockfile(wantedLockfile, lockText), null, wantedLockfile,
                options, lockfileDir, projects, workspaceProjects, pruneLockfileImporters, false, lockText,
                readPackageHook, patchHashes(patchedDependencies));
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

    // a project's own .pnpmfile.mjs or .pnpmfile.cjs, which recursive.ts loads for its install
    private void loadDefaultPnpmfile(String dir) {
        for (String defaultPnpmfile : new String[]{".pnpmfile.mjs", ".pnpmfile.cjs"}) {
            String path = dir + defaultPnpmfile;
            if (inputs.getFiles().containsKey(path) || inputs.getPaths() != null && inputs.getPaths().contains(path)) {
                throw new CannotKnow("what " + path + " does (pnpm runs its hooks)");
            }
        }
    }

    // main.ts' findWorkspaceProjects or installDeps' tryReadProjectManifest: each project's manifest as read, which
    // cli.utils' packageIsInstallable checks
    private List<Verification.Project> projects(PnpmOptions options, boolean workspace) {
        List<String> dirs = new ArrayList<>();
        if (workspace) {
            dirs.addAll(FindPackages.findWorkspaceProjects(inputs.getManifests().keySet(),
                    options.getWorkspacePackagePatterns()));
        } else {
            dirs.add(inputs.getLockDir());
        }
        List<Verification.Project> projects = new ArrayList<>();
        for (String dir : dirs) {
            String text = inputs.getManifests().get(dir);
            if (text == null) {
                if (missingManifest == null) {
                    missingManifest = dir;
                }
                continue;
            }
            Map<String, @Nullable Object> manifest = Manifests.readProjectManifest(text,
                    dir.isEmpty() ? "package.json" : dir + "/package.json");
            projects.add(new Verification.Project(dir, workspace ? FindPackages.getLockfileImporterId(dir) : ".",
                    manifest));
        }
        for (Verification.Project project : projects) {
            PackageIsInstallable.projectIsInstallable(project.getId(), project.getManifest(),
                    options.isTrue("engineStrict"), nodeVersion(options),
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

    // the nodeVersion setting as pnpm-workspace.yaml may give it, in any type; null for the host's Node.js
    private static @Nullable Object nodeVersion(PnpmOptions options) {
        Object nodeVersion = options.get("nodeVersion");
        return nodeVersion == JsYaml.UNDEFINED ? null : nodeVersion;
    }

    // main.ts' package manager check: the lock text after switchCliVersion or syncEnvLockfile wrote pnpm's release
    // into the env document, when this lock holds it
    private @Nullable String checkWantedPackageManager(PnpmOptions options, @Nullable String lockText,
                                                       boolean envDocument) {
        PnpmOptions.WantedPackageManager pm = options.getWantedPackageManager();
        if (pm == null || "ignore".equals(pm.getOnFail())) {
            return lockText;
        }
        boolean underCorepack = inputs.getEnv() != null && inputs.getEnv().containsKey("COREPACK_ROOT");
        if ("pnpm".equals(pm.getName()) && "download".equals(pm.getOnFail()) && !underCorepack) {
            return switchCliVersion(pm, options, lockText, envDocument);
        }
        checkPackageManager(pm, underCorepack);
        if (!envDocument) {
            return lockText;
        }
        String version = syncEnvLockfile(pm, () -> EnvLockfile.readEnvLockfile(lockText));
        return version == null ? lockText :
                resolvePackageManagerIntegrities(version, EnvLockfile.readEnvLockfile(lockText), true, options,
                        lockText).getLockText();
    }

    private @Nullable String switchCliVersion(PnpmOptions.WantedPackageManager pm, PnpmOptions options,
                                              @Nullable String lockText, boolean envDocument) {
        if (!"pnpm".equals(pm.getName()) || pm.getVersion() == null) {
            return lockText;
        }
        String version = Js.string(pm.getVersion());
        boolean persistLockfile = shouldPersistLockfile(pm);
        if (!persistLockfile && PNPM_VERSION.equals(version)) {
            return lockText;
        }
        if (!envDocument && persistLockfile) {
            if (PNPM_VERSION.equals(version)) {
                return lockText;
            }
            if (NodeSemver.valid(version, NodeSemver.Options.DEFAULT) == null) {
                throw new CannotKnow("which pnpm release the workspace's " + LockfileReader.WANTED_LOCKFILE +
                                     " pins for " + version + " (pnpm runs that release)");
            }
        }
        boolean save = persistLockfile && envDocument;
        EnvLockfile envLockfile = save ? EnvLockfile.readEnvLockfile(lockText) : null;
        Object pmVersion = EnvInstaller.lockedVersion(
                envLockfile == null ? null : envLockfile.packageManagerDependencies(), "pnpm");
        if (!Js.truthy(pmVersion) || !NodeSemver.satisfies(pmVersion, version, NodeSemver.Options.INCLUDE_PRERELEASE)) {
            PackageManagerIntegrities resolved = resolvePackageManagerIntegrities(version, envLockfile, save,
                    options, lockText);
            lockText = resolved.getLockText();
            pmVersion = EnvInstaller.lockedVersion(resolved.getEnvLockfile().packageManagerDependencies(), "pnpm");
            if (!Js.truthy(pmVersion)) {
                // pnpm warns that it cannot resolve the release and goes on
                return lockText;
            }
        } else if (!EnvInstaller.isPackageManagerResolved(envLockfile, Js.string(pmVersion))) {
            lockText = resolvePackageManagerIntegrities(Js.string(pmVersion), envLockfile, save, options, lockText)
                    .getLockText();
        }
        if (PNPM_VERSION.equals(Js.string(pmVersion))) {
            return lockText;
        }
        throw switchesTo(Js.string(pmVersion));
    }

    private static CannotKnow switchesTo(String release) {
        return new CannotKnow("the lock pnpm " + release + " writes (pnpm " + PNPM_VERSION + " hands the install " +
                              "to the release the project names)");
    }

    private static void checkPackageManager(PnpmOptions.WantedPackageManager pm, boolean underCorepack) {
        if (!Js.truthy(pm.getName())) {
            return;
        }
        boolean shouldError = "error".equals(pm.getOnFail()) || "download".equals(pm.getOnFail());
        if (!"pnpm".equals(pm.getName())) {
            if (shouldError) {
                throw new PnpmError("OTHER_PM_EXPECTED", "This project is configured to use " +
                                                         Js.string(pm.getName()));
            }
            return;
        }
        if (Js.truthy(pm.getVersion()) &&
            !NodeSemver.satisfies(PNPM_VERSION, Js.string(pm.getVersion()), NodeSemver.Options.INCLUDE_PRERELEASE)) {
            String msg = "This project is configured to use " + Js.string(pm.getVersion()) +
                         " of pnpm. Your current pnpm is v" + PNPM_VERSION;
            if (underCorepack) {
                msg += "\nCorepack invoked pnpm with this version, and pnpm does not switch versions when running " +
                       "under corepack.";
            }
            if (shouldError) {
                String baseHint = "If you want to bypass this version check, you can set the \"pmOnFail\" " +
                                  "configuration to \"warn\" or \"ignore\" (e.g. via --pm-on-fail=ignore). If using " +
                                  "\"devEngines.packageManager\", you can set its \"onFail\" to \"warn\" or \"ignore\"";
                throw new PnpmError("BAD_PM_VERSION", msg, underCorepack ?
                        "Align the \"packageManager\" field in package.json with \"devEngines.packageManager\", " +
                        "or invoke pnpm directly (without corepack) so it can switch versions automatically.\n" +
                        baseHint : baseHint);
            }
        }
    }

    static @Nullable String syncEnvLockfile(PnpmOptions.WantedPackageManager pm,
                                            Supplier<@Nullable EnvLockfile> envLockfile) {
        if (!"pnpm".equals(pm.getName()) || pm.getVersion() == null || !shouldPersistLockfile(pm)) {
            return null;
        }
        String version = Js.string(pm.getVersion());
        if (!NodeSemver.satisfies(PNPM_VERSION, version, NodeSemver.Options.INCLUDE_PRERELEASE)) {
            return null;
        }
        EnvLockfile locked = envLockfile.get();
        Object lockedVersion = EnvInstaller.lockedVersion(locked == null ? null : locked.packageManagerDependencies(),
                "pnpm");
        if (lockedVersion != null && lockedVersion != JsYaml.UNDEFINED &&
            NodeSemver.satisfies(lockedVersion, version, NodeSemver.Options.INCLUDE_PRERELEASE)) {
            return null;
        }
        return PNPM_VERSION;
    }

    static boolean shouldPersistLockfile(PnpmOptions.WantedPackageManager pm) {
        if (pm.isFromDevEngines()) {
            return true;
        }
        if (pm.getVersion() == null) {
            return false;
        }
        String valid = NodeSemver.valid(Js.string(pm.getVersion()), NodeSemver.Options.DEFAULT);
        return valid != null && Long.parseLong(valid.substring(0, valid.indexOf('.'))) >= 12;
    }

    // installConfigDepsAndLoadHooks: every pnpmfile pnpm loads is JavaScript, its plugins' among them
    private void loadHooks(PnpmOptions options, @Nullable Object configDependencies) {
        if (options.isTrue("ignorePnpmfile")) {
            return;
        }
        if (configDependencies instanceof Map) {
            @SuppressWarnings("unchecked")
            List<String> names = new ArrayList<>(Js.keys((Map<String, @Nullable Object>) configDependencies));
            Collections.sort(names);
            for (String name : names) {
                if (isPluginName(name)) {
                    throw new CannotKnow("what the pnpmfile of the config dependency " + name + " does (pnpm runs " +
                                         "its hooks)");
                }
            }
        }
        Object globalPnpmfile = options.get("globalPnpmfile");
        if (Js.truthy(globalPnpmfile)) {
            throw new CannotKnow("what " + Js.string(globalPnpmfile) + " does (pnpm runs its hooks)");
        }
        Object pnpmfile = options.get("pnpmfile");
        if (pnpmfile == null || pnpmfile == JsYaml.UNDEFINED) {
            loadDefaultPnpmfile("");
            return;
        }
        Object first = pnpmfile instanceof List ? ((List<?>) pnpmfile).isEmpty() ? null : ((List<?>) pnpmfile).get(0) :
                pnpmfile;
        if (first != null) {
            throw new CannotKnow("what " + Js.string(first) + " does (pnpm runs its hooks, or fails when there is " +
                                 "no such file)");
        }
    }

    private static boolean isPluginName(String configDepName) {
        if (configDepName.startsWith("pnpm-plugin-")) {
            return true;
        }
        if (configDepName.isEmpty() || configDepName.charAt(0) != '@') {
            return false;
        }
        return configDepName.startsWith("@pnpm/plugin-") || configDepName.contains("/pnpm-plugin-");
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

    // Node reads the package.json pnpm runs beside before pnpm starts, and stops at one that is not JSON; config
    // reads it as every project manifest is read
    private static Map<String, @Nullable Object> rootManifest(String text) {
        try {
            return Manifests.readProjectManifest(text, "package.json");
        } catch (PnpmError e) {
            if ("ERR_PNPM_JSON_PARSE".equals(e.getCode())) {
                throw new JsError("ERR_INVALID_PACKAGE_CONFIG", "Invalid package config package.json.");
            }
            throw e;
        }
    }

    // createResolutionVerifiers: the npm verifier when minimumReleaseAge or trustPolicy is active
    private List<ResolutionVerifier> resolutionVerifiers(PnpmOptions options) {
        NpmResolutionVerifier npmVerifier = NpmResolutionVerifier.createNpmResolutionVerifier(
                NpmResolutionVerifier.Options.builder()
                        .minimumReleaseAge(number(options, "minimumReleaseAge"))
                        .minimumReleaseAgeExclude(strings(options, "minimumReleaseAgeExclude"))
                        .ignoreMissingTimeField(
                                Boolean.TRUE.equals(options.get("minimumReleaseAgeIgnoreMissingTime")))
                        .trustPolicy(options.get("trustPolicy") == null ? null :
                                Js.string(options.get("trustPolicy")))
                        .trustPolicyExclude(strings(options, "trustPolicyExclude"))
                        .trustPolicyIgnoreAfter(number(options, "trustPolicyIgnoreAfter"))
                        .registries(options.getRegistries())
                        .namedRegistries(namedRegistries(options))
                        .fetch(inputs.getRegistry())
                        .now(inputs.getNow())
                        .build());
        return npmVerifier == null ? Collections.emptyList() : Collections.singletonList(npmVerifier);
    }

    private static @Nullable Double number(PnpmOptions options, String name) {
        Object value = options.get(name);
        if (value == null || value == JsYaml.UNDEFINED) {
            return null;
        }
        if (!(value instanceof Number)) {
            throw new CannotKnow("what pnpm makes of " + name + " (" + Js.typeOf(value) +
                                 ", where pnpm reads a number)");
        }
        return ((Number) value).doubleValue();
    }

    private static @Nullable List<String> strings(PnpmOptions options, String name) {
        return strings(options.get(name), name);
    }

    private static @Nullable List<String> strings(@Nullable Object value, String name) {
        if (value == null || value == JsYaml.UNDEFINED) {
            return null;
        }
        List<String> out = new ArrayList<>();
        if (value instanceof String) {
            // for...of over a string: its code points
            ((String) value).codePoints().forEach(c -> out.add(new String(Character.toChars(c))));
            return out;
        }
        if (!(value instanceof List)) {
            throw new CannotKnow("what pnpm makes of " + name + " (" + Js.typeOf(value) +
                                 ", where pnpm reads an array)");
        }
        for (Object item : (List<?>) value) {
            if (!(item instanceof String)) {
                throw new CannotKnow("what pnpm makes of " + name + " (an item that is not a string)");
            }
            out.add((String) item);
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable Map<String, @Nullable Object> namedRegistries(PnpmOptions options) {
        Object value = options.get("namedRegistries");
        if (value == null || value == JsYaml.UNDEFINED) {
            return null;
        }
        if (!(value instanceof Map)) {
            throw new CannotKnow("what pnpm makes of namedRegistries (" + Js.typeOf(value) +
                                 ", where pnpm reads an object)");
        }
        return (Map<String, @Nullable Object>) value;
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

    // the settings object pnpm sets on the lock, in its key order
    private static Map<String, @Nullable Object> lockfileSettings(PnpmOptions options) {
        Map<String, @Nullable Object> settings = new LinkedHashMap<>();
        settings.put("autoInstallPeers", undefinedIfAbsent(options.get("autoInstallPeers")));
        Object dedupePeers = options.get("dedupePeers");
        settings.put("dedupePeers", Js.truthy(dedupePeers) ? dedupePeers : JsYaml.UNDEFINED);
        settings.put("excludeLinksFromLockfile", undefinedIfAbsent(options.get("excludeLinksFromLockfile")));
        settings.put("peersSuffixMaxLength", undefinedIfAbsent(options.get("peersSuffixMaxLength")));
        settings.put("injectWorkspacePackages", undefinedIfAbsent(options.get("injectWorkspacePackages")));
        return settings;
    }

    private static Object undefinedIfAbsent(@Nullable Object value) {
        return value == null ? JsYaml.UNDEFINED : value;
    }

    @SuppressWarnings("unchecked")
    private static @Nullable List<@Nullable Object> sorted(@Nullable Object value) {
        if (value == null || value == JsYaml.UNDEFINED) {
            return null;
        }
        if (!(value instanceof List)) {
            throw new CannotKnow("what pnpm makes of ignoredOptionalDependencies (" + Js.typeOf(value) +
                                 ", where pnpm reads an array)");
        }
        List<@Nullable Object> list = (List<@Nullable Object>) value;
        list.sort((a, b) -> Js.string(a).compareTo(Js.string(b)));
        return list;
    }
}
