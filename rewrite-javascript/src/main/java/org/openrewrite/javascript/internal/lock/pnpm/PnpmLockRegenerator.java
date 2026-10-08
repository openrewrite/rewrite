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

        /**
         * The pnpm version or range pnpm resolves into the env document's packageManagerDependencies first; null
         * when it does not.
         */
        @Nullable
        String packageManagerToResolve;

        /**
         * The config dependencies pnpm resolves into the env document first, name to specifier.
         */
        Map<String, String> configDependenciesToResolve;

        LockfileObject wantedLockfile;

        PnpmOptions options;

        List<Verification.Project> projects;

        boolean lockfileHadConflicts;

        /**
         * The lock text whose env document a write keeps in front.
         */
        @Nullable
        String lockText;

        @Nullable
        ReadPackageHook readPackageHook;
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
        if (e instanceof PeerDependencyIssuesError) {
            return new Failure(Reason.PEER_CONFLICT, ((PeerDependencyIssuesError) e).peerName,
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
     * {@code ERR_PNPM_PEER_DEP_ISSUES}: strictPeerDependencies over peers the resolution left unmet.
     */
    static final class PeerDependencyIssuesError extends PnpmError {
        private final @Nullable String peerName;

        PeerDependencyIssuesError(@Nullable String peerName) {
            super("PEER_DEP_ISSUES", "Unmet peer dependencies");
            this.peerName = peerName;
        }
    }

    /**
     * The rest of {@code _install} once the lock needs resolving: each project's wanted dependencies with the lock's
     * resolutions of changed specifiers forgotten, then {@code _installInContext}'s resolution, its write, and the
     * checks after it.
     */
    String resolve(Install install) {
        if (install.getPackageManagerToResolve() != null) {
            throw new CannotKnow("the packageManagerDependencies pnpm resolves for pnpm " +
                                 install.getPackageManagerToResolve() + " (the env document's resolution of pnpm's " +
                                 "own release is not ported)");
        }
        if (!install.getConfigDependenciesToResolve().isEmpty()) {
            throw new CannotKnow("the config dependencies pnpm resolves into the env document (" +
                                 String.join(", ", install.getConfigDependenciesToResolve().keySet()) + ")");
        }
        PnpmOptions options = install.getOptions();
        LockfileObject wantedLockfile = install.getWantedLockfile();
        Object patched = options.get("patchedDependencies");
        if (Js.truthy(patched)) {
            List<String> patchFiles = new ArrayList<>();
            if (patched instanceof Map) {
                for (Object file : ((Map<?, ?>) patched).values()) {
                    patchFiles.add(Js.string(file));
                }
            }
            throw new CannotKnow("patch file " + String.join(", ", patchFiles) + " (pnpm applies the patch to the " +
                                 "package it resolves; patching while resolving is not ported)");
        }
        if (Js.truthy(options.get("peerDependencyRules")) && options.isTrue("strictPeerDependencies")) {
            throw new CannotKnow("which peer issues peerDependencyRules lets through under strictPeerDependencies");
        }
        boolean autoInstallPeers = options.isTrue("autoInstallPeers");
        boolean workspace = options.getWorkspacePackagePatterns() != null;
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
        if (workspace) {
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
        boolean fullMetadata = ("time-based".equals(options.get("resolutionMode")) ||
                                "no-downgrade".equals(options.get("trustPolicy"))) &&
                               !options.isTrue("registrySupportsTimeField");
        NpmResolver npmResolver = new NpmResolver(inputs.getRegistry(), registries,
                options.get("saveWorkspaceProtocol"), !Boolean.FALSE.equals(
                options.get("minimumReleaseAgeIgnoreMissingTime")), fullMetadata);
        Host host = inputs.getHost();
        if (Js.truthy(options.get("nodeVersion"))) {
            host = host.toBuilder().nodeVersion(Js.string(options.get("nodeVersion"))).build();
        }
        @SuppressWarnings("unchecked")
        Map<String, @Nullable Object> nodeDownloadMirrors = options.get("nodeDownloadMirrors") instanceof Map ?
                (Map<String, @Nullable Object>) options.get("nodeDownloadMirrors") : null;
        PackageRequester requester = new PackageRequester(npmResolver, files, host, options.isTrue("engineStrict"),
                supportedArchitectures, inputs.getRegistry(), nodeDownloadMirrors);

        ResolveDependencies.ResolutionContext ctx = new ResolveDependencies.ResolutionContext(wantedLockfile, "",
                requester);
        ctx.setAutoInstallPeers(autoInstallPeers);
        ctx.setAutoInstallPeersFromHighestMatch(options.isTrue("autoInstallPeersFromHighestMatch"));
        ctx.setCatalogs(catalogs);
        ctx.setForceFullResolution(true);
        ctx.setInjectWorkspacePackages(options.isTrue("injectWorkspacePackages"));
        ctx.setLinkWorkspacePackagesDepth(options.getLinkWorkspacePackagesDepth());
        ctx.setPreferWorkspacePackages(options.isTrue("preferWorkspacePackages"));
        ctx.setReadPackageHook(install.getReadPackageHook());
        ctx.setRegistries(registries);
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
        ctx.setNamedRegistryPrefixes(namedRegistryPrefixes);
        Object resolutionMode = options.get("resolutionMode");
        ctx.setResolutionMode(resolutionMode == null ? null : Js.string(resolutionMode));
        ctx.setWorkspacePackages(workspacePackages(projects));
        ctx.setHoistPeers(autoInstallPeers || options.isTrue("dedupePeerDependents"));
        ctx.setResolvePeersFromWorkspaceRoot(options.isTrue("resolvePeersFromWorkspaceRoot"));
        ctx.setBlockExoticSubdeps(options.isTrue("blockExoticSubdeps"));
        Double minimumReleaseAge = number(options, "minimumReleaseAge");
        List<String> minimumReleaseAgeExclude = strings(options, "minimumReleaseAgeExclude");
        if (minimumReleaseAge != null && minimumReleaseAge != 0) {
            ctx.setMaximumPublishedBy(inputs.getNow().minusMillis((long) (minimumReleaseAge * 60 * 1000)));
        }
        if (minimumReleaseAgeExclude != null) {
            ctx.setPublishedByExclude(PackageVersionPolicy.createPackageVersionPolicyOrThrow(minimumReleaseAgeExclude,
                    "minimumReleaseAgeExclude"));
        }
        Object peersSuffixMaxLength = options.get("peersSuffixMaxLength");
        DepsResolver.Options resolverOptions = new DepsResolver.Options(autoInstallPeers,
                options.isTrue("dedupePeerDependents"), options.isTrue("dedupePeers"),
                options.isTrue("dedupeInjectedDeps"), options.isTrue("excludeLinksFromLockfile"), options.isTrue("lockfileIncludeTarballUrl"),
                options.isTrue("resolvePeersFromWorkspaceRoot"),
                peersSuffixMaxLength instanceof Number ? ((Number) peersSuffixMaxLength).intValue() : 1000,
                registries);
        DepsResolver.Result result = DepsResolver.resolveDependencies(importers, ctx, preferredVersions,
                resolverOptions);
        handleResolutionPolicyViolations(options, result.getResolutionPolicyViolations(), minimumReleaseAge);

        LockfileObject newLockfile = result.getNewLockfile();
        newLockfile.set("lockfileVersion", LockfileObject.LOCKFILE_VERSION);
        String written = LockfileWriter.writeWantedLockfile(newLockfile, install.getLockText());
        reportPeerDependencyIssues(result.getPeerDependencyIssuesByProjects(),
                options.isTrue("strictPeerDependencies"));
        return written;
    }

    // resolveAndInstallConfigDeps from the registry: each config dependency and its exact optional dependencies into
    // the env document, which is then pruned
    @SuppressWarnings("unchecked")
    private void resolveConfigDeps(Map<String, String> depsToResolve, EnvLockfile envLockfile,
                                   Map<String, String> registries) {
        NpmResolver resolveFromNpm = new NpmResolver(inputs.getRegistry(), registries, null, true, false);
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
            Map<String, @Nullable Object> optionalDeps = ResolveDependencies.asMap(manifest.get("optionalDependencies"));
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
        LockfileObject pruned = UpdateLockfile.pruneSharedLockfile(LockfileFormatConverters.convertToLockfileObject(file),
                null);
        Map<String, @Nullable Object> prunedFile = LockfileFormatConverters.convertToLockfileFile(pruned);
        Object packages = prunedFile.get("packages");
        Object snapshots = prunedFile.get("snapshots");
        envLockfile.getDocument().put("packages", packages == null ? new LinkedHashMap<>() : packages);
        envLockfile.getDocument().put("snapshots", snapshots == null ? new LinkedHashMap<>() : snapshots);
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
    private static void forgetResolutionsOfPrevWantedDeps(ProjectSnapshot importer,
                                                          List<ResolveDependencies.WantedDependency> wantedDeps,
                                                          @Nullable Object prevCatalogs,
                                                          Map<String, @Nullable Map<String, @Nullable Object>> catalogs) {
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

    // reportPeerDependencyIssues under strictPeerDependencies
    private static void reportPeerDependencyIssues(Map<String, ResolvePeers.PeerDependencyIssues> issuesByProjects,
                                                   boolean strictPeerDependencies) {
        if (!strictPeerDependencies) {
            return;
        }
        for (ResolvePeers.PeerDependencyIssues issues : issuesByProjects.values()) {
            if (!issues.getBad().isEmpty()) {
                throw new PeerDependencyIssuesError(issues.getBad().keySet().iterator().next());
            }
            for (Map.Entry<String, List<Map<String, @Nullable Object>>> missing : issues.getMissing().entrySet()) {
                boolean allOptional = true;
                for (Map<String, @Nullable Object> issue : missing.getValue()) {
                    allOptional &= Boolean.TRUE.equals(issue.get("optional"));
                }
                if (!allOptional) {
                    throw new PeerDependencyIssuesError(missing.getKey());
                }
            }
        }
    }

    Install install() {
        String rootManifestText = inputs.getManifests().get("");
        Map<String, @Nullable Object> rootManifest = rootManifestText == null ? null : rootManifest(rootManifestText);
        PnpmOptions options = PnpmOptions.load(inputs.getFiles().get("pnpm-workspace.yaml"),
                inputs.getFiles().get(".npmrc"), rootManifest, inputs.getEnv());
        String lockText = inputs.getLock();
        String packageManagerToResolve = checkWantedPackageManager(options,
                () -> EnvLockfile.readEnvLockfile(inputs.getLock()));

        Object configDependencies = options.get("configDependencies");
        Map<String, String> configDependenciesToResolve = Collections.emptyMap();
        if (Js.truthy(configDependencies)) {
            if (!(configDependencies instanceof Map)) {
                throw new CannotKnow("what pnpm makes of configDependencies (" + Js.typeOf(configDependencies) +
                                     ", where pnpm reads an object)");
            }
            EnvLockfile envLockfile = EnvLockfile.readEnvLockfile(lockText);
            if (envLockfile == null) {
                envLockfile = EnvLockfile.createEnvLockfile();
            }
            @SuppressWarnings("unchecked")
            EnvInstaller.ConfigDepsResult configDeps = EnvInstaller.resolveAndInstallConfigDeps(
                    (Map<String, @Nullable Object>) configDependencies, envLockfile, options.getRegistries());
            if (!configDeps.getDepsToResolve().isEmpty()) {
                resolveConfigDeps(configDeps.getDepsToResolve(), envLockfile, options.getRegistries());
                lockText = EnvLockfile.writeEnvLockfile(lockText, envLockfile);
            } else if (configDeps.isLockfileChanged()) {
                lockText = EnvLockfile.writeEnvLockfile(lockText, envLockfile);
            }
        }
        loadHooks(options, configDependencies);

        boolean workspace = options.getWorkspacePackagePatterns() != null;
        List<Verification.Project> projectManifests = projects(options, workspace);
        if (!workspace) {
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
        List<ParseOverrides.VersionOverride> parsedOverrides = ParseOverrides.parseOverrides(options.getOverrides(),
                options.getCatalogs());
        ReadPackageHook readPackageHook = ReadPackageHook.createReadPackageHook(
                options.isTrue("ignoreCompatibilityDb"), "", parsedOverrides,
                options.get("ignoredOptionalDependencies"), options.get("packageExtensions"));
        if (workspace && !projectManifests.isEmpty()) {
            agent(options);
        }
        List<Verification.Project> projects = new ArrayList<>();
        for (Verification.Project project : projectManifests) {
            projects.add(readPackageHook == null ? project : new Verification.Project(project.getRootDir(),
                    project.getId(), readPackageHook.apply(project.getManifest(), project.getRootDir())));
        }

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
                    "lockfileVersion " + Js.string(wantedLockfile.getLockfileVersion()), packageManagerToResolve,
                    configDependenciesToResolve, wantedLockfile, options, projects, read.isHadConflicts(), lockText,
                    readPackageHook);
        }

        boolean pruneLockfileImporters = workspace;
        Map<String, Map<String, Verification.Project>> workspacePackages = workspace ?
                workspacePackages(projects) : null;
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
                options.getLinkWorkspacePackagesDepth() >= 0, wantedLockfile, workspacePackages, inputs.getFiles())) {
            reason = "projects not up to date with the lock";
        }
        if (reason != null) {
            return new Install(null, reason, packageManagerToResolve, configDependenciesToResolve, wantedLockfile,
                    options, projects, read.isHadConflicts(), lockText, readPackageHook);
        }
        boolean envResolves = packageManagerToResolve != null || !configDependenciesToResolve.isEmpty();
        return new Install(envResolves ? null : LockfileWriter.writeWantedLockfile(wantedLockfile, lockText), null,
                packageManagerToResolve, configDependenciesToResolve, wantedLockfile, options, projects, false, lockText,
                readPackageHook);
    }

    // installDeps: each project's manifest as read, under the runtimeOnFail setting
    private List<Verification.Project> projects(PnpmOptions options, boolean workspace) {
        List<String> dirs = new ArrayList<>();
        if (workspace) {
            dirs.addAll(FindPackages.findWorkspaceProjects(inputs.getManifests().keySet(),
                    options.getWorkspacePackagePatterns()));
        } else {
            dirs.add("");
        }
        Object runtimeOnFail = options.get("runtimeOnFail");
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
            if (Js.truthy(runtimeOnFail)) {
                PkgManifestUtils.applyRuntimeOnFailOverride(manifest, runtimeOnFail);
            }
            projects.add(new Verification.Project(dir, FindPackages.getLockfileImporterId(dir), manifest));
        }
        return projects;
    }

    // main.ts' package manager check: what pnpm resolves into packageManagerDependencies, or null
    private @Nullable String checkWantedPackageManager(PnpmOptions options,
                                                       Supplier<@Nullable EnvLockfile> envLockfile) {
        PnpmOptions.WantedPackageManager pm = options.getWantedPackageManager();
        if (pm == null || "ignore".equals(pm.getOnFail())) {
            return null;
        }
        boolean underCorepack = inputs.getEnv() != null && inputs.getEnv().containsKey("COREPACK_ROOT");
        if ("pnpm".equals(pm.getName()) && "download".equals(pm.getOnFail()) && !underCorepack) {
            return switchCliVersion(pm, envLockfile);
        }
        checkPackageManager(pm, underCorepack);
        return syncEnvLockfile(pm, envLockfile);
    }

    private static @Nullable String switchCliVersion(PnpmOptions.WantedPackageManager pm,
                                                     Supplier<@Nullable EnvLockfile> envLockfile) {
        if (!"pnpm".equals(pm.getName()) || pm.getVersion() == null) {
            return null;
        }
        String version = Js.string(pm.getVersion());
        boolean persistLockfile = shouldPersistLockfile(pm);
        if (!persistLockfile && PNPM_VERSION.equals(version)) {
            return null;
        }
        EnvLockfile locked = persistLockfile ? envLockfile.get() : null;
        Object pmVersion = EnvInstaller.lockedVersion(locked == null ? null : locked.packageManagerDependencies(),
                "pnpm");
        if (!Js.truthy(pmVersion) || !NodeSemver.satisfies(pmVersion, version, NodeSemver.Options.INCLUDE_PRERELEASE)) {
            // the registry decides the release, which is this one only within a range the env document records
            if (!persistLockfile ||
                !NodeSemver.satisfies(PNPM_VERSION, version, NodeSemver.Options.INCLUDE_PRERELEASE)) {
                throw switchesTo(version);
            }
            return version;
        }
        if (!PNPM_VERSION.equals(pmVersion)) {
            throw switchesTo(Js.string(pmVersion));
        }
        return EnvInstaller.isPackageManagerResolved(locked, PNPM_VERSION) ? null : PNPM_VERSION;
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
            for (String defaultPnpmfile : new String[]{".pnpmfile.mjs", ".pnpmfile.cjs"}) {
                if (inputs.getFiles().containsKey(defaultPnpmfile) ||
                    inputs.getPaths() != null && inputs.getPaths().contains(defaultPnpmfile)) {
                    throw new CannotKnow("what " + defaultPnpmfile + " does (pnpm runs its hooks)");
                }
            }
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
        Object value = options.get(name);
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
