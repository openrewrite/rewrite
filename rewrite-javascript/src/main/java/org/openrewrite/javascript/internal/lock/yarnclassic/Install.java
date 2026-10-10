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
package org.openrewrite.javascript.internal.lock.yarnclassic;

import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.SharedError;
import org.openrewrite.javascript.internal.lock.StripBom;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator.Inputs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Port of yarn src/cli/commands/install.js (yarn 1.22.22) for {@code yarn install} without node_modules: {@code run},
 * then {@code Install.init}'s steps up to {@code saveLockfileAndIntegrity}.
 */
final class Install {

    private static final String WORKSPACE_AGGREGATOR_UUID = "00000000-0000-4000-8000-000000000000";
    private static final List<String> OWNED_DEPENDENCY_TYPES = Arrays.asList("devDependencies",
            "dependencies", "optionalDependencies");

    private final Inputs inputs;
    private final Config config;
    private final YarnClassicOptions options;
    private final RegistrySource registry;
    private final List<String> warnings;

    Install(Inputs inputs, Config config, YarnClassicOptions options, RegistrySource registry,
            List<String> warnings) {
        this.inputs = inputs;
        this.config = config;
        this.options = options;
        this.registry = registry;
        this.warnings = warnings;
    }

    /**
     * {@code run}: the lock yarn writes, or null when it leaves the file as it is.
     */
    @Nullable
    String run() {
        return init(lockfile());
    }

    /**
     * {@code run}'s lock of {@code lockfileFolder}, none under {@code --no-lockfile}; at a workspace root above the
     * project as far as the inputs reach.
     */
    Lockfile lockfile() {
        if (config.getWorkspaceRoot() > 0) {
            installAtWorkspaceRoot(config.getWorkspaceRoot(),
                    Objects.requireNonNull(config.getLockfileFolderManifest()));
        }
        return options.getFlags().isLockfile() ? Lockfile.fromDirectory(inputs.getLock()) : new Lockfile();
    }

    /**
     * {@code init}: the steps up to {@code saveLockfileAndIntegrity}, which writes whatever the flags say.
     */
    @Nullable
    String init(Lockfile lockfile) {
        if (lockfile.getLockfilev2() != null) {
            throw Rejected.discardsLock("the lock is a Yarn Berry lock (__metadata)");
        }
        if (options.getFlags().isLockfile() && LockfileParser.CONFLICT.equals(lockfile.getParseResultType())) {
            throw Rejected.discardsLock("the lock's merge conflict does not merge");
        }
        YarnClassicOptions.Flags flags = options.getFlags();
        ResolutionMap resolutionMap = new ResolutionMap(options);
        InstallCwdRequest request = fetchRequestFromCwd(lockfile, resolutionMap);
        String rootText = inputs.getManifests().get("");
        if (rootText != null && Js.jsonParse(StripBom.stripBom(rootText)) instanceof List) {
            throw Rejected.rejects(Reason.MALFORMED_MANIFEST, "package.json is an array, not an object: yarn writes " +
                                                              "a lock with no entries");
        }

        List<String> errors = new ArrayList<>();
        PackageCompatibility compatibility = new PackageCompatibility(options, inputs.getHost(), errors, warnings);
        try {
            if (PackageCompatibility.shouldCheck(request.getManifest(), flags.isIgnoreEngines(),
                    flags.isIgnorePlatform())) {
                compatibility.checkOne(request.getManifest(), flags.isIgnoreEngines());
            }

            PackageResolver resolver = new PackageResolver(options, lockfile, resolutionMap, registry,
                    new ProjectFiles(inputs.getManifests(), inputs.getPaths(), inputs.getCwd()), inputs.getHost(),
                    warnings);
            resolver.focus = config.isFocus();
            resolver.focusedWorkspaceName = config.getFocusedWorkspaceName();
            resolver.init(request.getRequests(), request.isFlat(), flags.isFrozenLockfile(),
                    request.getWorkspaceLayout());
            List<String> topLevelPatterns = request.getPatterns();
            flatten(resolver, topLevelPatterns, request.getManifest(), request.isFlat());
            bailout(topLevelPatterns, request.getWorkspaceLayout(), lockfile, flags);

            if (options.isOffline() && !options.isPreferOffline()) {
                throw options.offlineCache();
            }
            fetchStep(resolver, compatibility);
            return saveLockfileAndIntegrity(resolver, topLevelPatterns, request.getWorkspaceLayout(), options,
                    lockfile, inputs.getLock(), inputs.getHost());
        } catch (MessageError e) {
            if (!errors.isEmpty()) {
                throw Rejected.rejects(errors.get(0).startsWith("The engine ") ? SharedError.UNSUPPORTED_ENGINE :
                        SharedError.UNSUPPORTED_PLATFORM, null, errors.get(0) + " " + e.getMessage());
            }
            throw e;
        }
    }

    // the fetch step, then the link step's read of each package's .yarn-metadata.json in yarn's cache
    private void fetchStep(PackageResolver resolver, PackageCompatibility compatibility) {
        Set<String> unfetched = new HashSet<>();
        resolver.updateManifests(PackageFetcher.fetch(resolver.getManifests(), PackageFetcher.remote(resolver,
                registry, options), warnings, unfetched));
        compatibility.check(resolver.getManifests(), options.getFlags().isIgnoreEngines());
        for (Manifest manifest : resolver.getManifests()) {
            PackageReference ref = manifest.reference;
            if (ref != null && !ref.ignore && unfetched.contains(PackageFetcher.generateModuleCachePath(ref))) {
                throw new JsError(JsError.ERROR, "ENOENT: no such file or directory, open '" +
                                                 PackageFetcher.generateModuleCachePath(ref) + "/.yarn-metadata.json'");
            }
        }
    }

    /**
     * {@code flatten}: under {@code flat}, every package with more than one version collapses to the one the root's
     * {@code resolutions} names; without one yarn asks, which it cannot without a terminal.
     */
    private static void flatten(PackageResolver resolver, List<String> patterns, Manifest root, boolean flat) {
        if (!flat) {
            return;
        }
        Map<String, @Nullable Object> resolutions = new LinkedHashMap<>();
        Js.assign(resolutions, root.get("resolutions"));
        for (String name : resolver.getAllDependencyNamesByLevelOrder(patterns)) {
            List<Manifest> infos = new ArrayList<>();
            for (Manifest manifest : resolver.getAllInfoForPackageName(name)) {
                if (manifest.reference == null) {
                    throw new JsError(Js.INVARIANT_VIOLATION, "expected package reference");
                }
                if (!manifest.reference.ignore) {
                    infos.add(manifest);
                }
            }
            if (infos.size() <= 1) {
                continue;
            }
            List<@Nullable Object> versions = new ArrayList<>();
            for (Manifest info : infos) {
                versions.add(info.getVersion());
            }
            Object resolutionVersion = resolutions.get(name);
            if (!Js.truthy(resolutionVersion) || !versions.contains(resolutionVersion)) {
                throw new JsError(JsError.ERROR, "Can't answer a question unless a user TTY");
            }
            resolver.collapseAllVersionsOfPackage(name, Js.string(resolutionVersion));
        }
    }

    /**
     * {@code bailout} without node_modules: only {@code --frozen-lockfile} stops here, when the lock misses a
     * top-level pattern or did not parse cleanly.
     */
    private static void bailout(List<String> patterns, @Nullable WorkspaceLayout workspaceLayout, Lockfile lockfile,
                                YarnClassicOptions.Flags flags) {
        if (flags.isSkipIntegrityCheck() || flags.isForce() || !Js.truthy(lockfile.getCache())) {
            return;
        }
        boolean lockfileClean = LockfileParser.SUCCESS.equals(lockfile.getParseResultType());
        boolean missing = false;
        for (String p : patterns) {
            if (!Js.truthy(lockfile.cached(p)) &&
                (workspaceLayout == null || workspaceLayout.getManifestByPattern(p) == null)) {
                missing = true;
                break;
            }
        }
        if (flags.isFrozenLockfile() && (!lockfileClean || missing)) {
            throw new MessageError("Your lockfile needs to be updated, but yarn was run with `--frozen-lockfile`.");
        }
    }

    /**
     * {@code run} and {@code fetchRequestFromCwd} at a workspace root above the project, as far as they go without
     * what the inputs lack: the root's lock when it has one, its resolutions, then its workspaces up to their glob.
     */
    private void installAtWorkspaceRoot(int workspaceRoot, Manifest root) {
        String up = YarnClassicOptions.up(workspaceRoot);
        Set<String> paths = inputs.getPaths();
        if (!options.getFlags().isLockfile() || paths != null && !paths.contains(up + "yarn.lock")) {
            String cwd = up.substring(0, up.length() - 1);
            String text = Objects.requireNonNull(inputs.getConfigFiles().get(up + "package.json"));
            Manifest projectManifestJson = NormalizeManifest.normalizeManifest(Config.manifest(Fs.readJson(up +
                    "package.json", text), "_registry"), cwd, cwd, true, options.isLooseSemver(), warnings);
            Map<String, @Nullable Object> resolutions = new LinkedHashMap<>();
            Js.assign(resolutions, projectManifestJson.get("resolutions"));
            new ResolutionMap(options).init(resolutions);
            Config.workspacePatterns(root, options, warnings);
        }
        throw new CannotKnow("the lock yarn writes for this edit: yarn installs at the workspace root " + up +
                             ", whose workspaces list " + inputs.relativePath(workspaceRoot) + ", and writes " + up +
                             "yarn.lock, not this project's");
    }

    /**
     * What {@code fetchRequestFromCwd} gathers: the root requests in yarn's order, the top-level patterns, the root
     * manifest and the workspace members.
     */
    @Value
    static class InstallCwdRequest {
        List<DependencyRequestPattern> requests;
        List<String> patterns;
        Manifest manifest;
        @Nullable
        WorkspaceLayout workspaceLayout;
        boolean flat;
    }

    /**
     * {@code fetchRequestFromCwd} for an install run at the project's root.
     */
    InstallCwdRequest fetchRequestFromCwd(Lockfile lockfile, ResolutionMap resolutionMap) {
        List<String> patterns = new ArrayList<>();
        List<DependencyRequestPattern> deps = new ArrayList<>();
        List<DependencyRequestPattern> resolutionDeps = new ArrayList<>();
        String rootText = inputs.getManifests().get("");
        if (rootText == null) {
            return new InstallCwdRequest(resolutionDeps, patterns, new Manifest(), null, options.getFlags().isFlat());
        }
        String registry = NpmResolver.NPM_REGISTRY_ID;
        Manifest projectManifestJson = NormalizeManifest.normalizeManifest(Config.manifest(Fs.readJson(
                "package.json", rootText), "_registry"), "", "", true, options.isLooseSemver(), warnings);
        Manifest manifest = projectManifestJson;
        boolean workspaceRootFolder = Config.isWorkspaceRoot(projectManifestJson);

        Map<String, @Nullable Object> resolutions = new LinkedHashMap<>();
        Js.assign(resolutions, projectManifestJson.get("resolutions"));
        resolutionMap.init(resolutions);
        for (String packageName : Js.keys(resolutionMap.resolutionsByPackage)) {
            boolean optional = ObjectPath.has(manifest.get("optionalDependencies"), packageName) &&
                               options.getFlags().isIgnoreOptional();
            for (ResolutionMap.Resolution resolution : resolutionMap.resolutionsByPackage.get(packageName)) {
                resolutionDeps.add(DependencyRequestPattern.builder().registry(registry)
                        .pattern(resolution.getPattern()).optional(optional).hint("resolution").build());
            }
        }

        boolean flat = options.getFlags().isFlat();
        PushDeps pushDeps = (depType, depManifest, hint, optional, isUsed) -> {
            if (flat && !isUsed) {
                return;
            }
            Object depMap = depManifest.get(depType);
            Object workspaceName = depManifest.getName();
            for (String name : Js.keys(depMap)) {
                String pattern = name;
                if (lockfile.getLocked(pattern) == null) {
                    pattern += "@" + Js.string(Js.get(depMap, name));
                }
                patterns.add(pattern);
                deps.add(DependencyRequestPattern.builder().pattern(pattern).registry(registry).hint(hint)
                        .optional(optional).workspaceName(workspaceName == null ? null : Js.string(workspaceName))
                        .build());
            }
        };

        pushDeps.push("dependencies", projectManifestJson, null, false, true);
        pushDeps.push("devDependencies", projectManifestJson, "dev", false, !options.isProduction());
        pushDeps.push("optionalDependencies", projectManifestJson, "optional", true, true);

        WorkspaceLayout workspaceLayout = null;
        if (workspaceRootFolder) {
            Map<String, WorkspaceLayout.Workspace> workspaces = config.resolveWorkspaces(projectManifestJson, options,
                    warnings);
            workspaceLayout = new WorkspaceLayout(workspaces, options);
            Map<String, @Nullable Object> workspaceDependencies = new LinkedHashMap<>();
            Js.assign(workspaceDependencies, projectManifestJson.get("dependencies"));
            for (String workspaceName : Js.keys(workspaces)) {
                workspaceDependencies.put(workspaceName, workspaces.get(workspaceName).getManifest().getVersion());
            }
            Manifest virtualDependencyManifest = new Manifest();
            virtualDependencyManifest.put("_uid", "");
            virtualDependencyManifest.put("name", "workspace-aggregator-" + WORKSPACE_AGGREGATOR_UUID);
            virtualDependencyManifest.put("version", "1.0.0");
            virtualDependencyManifest.put("_registry", "npm");
            virtualDependencyManifest.put("dependencies", workspaceDependencies);
            Map<String, @Nullable Object> devDependencies = new LinkedHashMap<>();
            Js.assign(devDependencies, projectManifestJson.get("devDependencies"));
            virtualDependencyManifest.put("devDependencies", devDependencies);
            Map<String, @Nullable Object> optionalDependencies = new LinkedHashMap<>();
            Js.assign(optionalDependencies, projectManifestJson.get("optionalDependencies"));
            virtualDependencyManifest.put("optionalDependencies", optionalDependencies);
            virtualDependencyManifest.put("private", projectManifestJson.get("private"));
            virtualDependencyManifest.put("workspaces", projectManifestJson.get("workspaces"));
            String virtualName = Js.string(virtualDependencyManifest.getName());
            workspaceLayout.virtualManifestName = virtualName;
            workspaces.put(virtualName, new WorkspaceLayout.Workspace("", virtualDependencyManifest));

            Manifest virtualDep = new Manifest();
            Map<String, @Nullable Object> virtualDeps = new LinkedHashMap<>();
            virtualDeps.put(virtualName, "1.0.0");
            virtualDep.put("workspaces", virtualDeps);
            pushDeps.push("workspaces", virtualDep, "workspaces", false, true);

            Map<String, @Nullable Object> implicitWorkspaceDependencies = new LinkedHashMap<>(workspaceDependencies);
            for (String type : OWNED_DEPENDENCY_TYPES) {
                for (String dependencyName : Js.keys(projectManifestJson.get(type))) {
                    implicitWorkspaceDependencies.remove(dependencyName);
                }
            }
            Manifest implicit = new Manifest();
            implicit.put("dependencies", implicitWorkspaceDependencies);
            pushDeps.push("dependencies", implicit, "workspaces", false, true);
        }

        List<DependencyRequestPattern> requests = new ArrayList<>(resolutionDeps);
        requests.addAll(deps);
        return new InstallCwdRequest(requests, patterns, manifest, workspaceLayout,
                flat || Js.truthy(manifest.get("flat")));
    }

    private interface PushDeps {
        void push(String depType, Manifest manifest, @Nullable String hint, boolean optional, boolean isUsed);
    }

    /**
     * {@code saveLockfileAndIntegrity}: the lock text yarn writes for the install's top-level patterns, or null when
     * yarn leaves the file as it is.
     */
    static @Nullable String saveLockfileAndIntegrity(PackageResolver resolver, List<String> patterns,
                                                     @Nullable WorkspaceLayout workspaceLayout,
                                                     YarnClassicOptions config, Lockfile lockfile,
                                                     @Nullable String rawLock, Host host) {
        Map<String, Manifest> resolvedPatterns = new LinkedHashMap<>();
        for (String pattern : Js.keys(resolver.patterns)) {
            if (workspaceLayout == null || workspaceLayout.getManifestByPattern(pattern) == null) {
                resolvedPatterns.put(pattern, resolver.patterns.get(pattern));
            }
        }
        List<String> topLevel = new ArrayList<>();
        for (String p : patterns) {
            if (workspaceLayout == null || workspaceLayout.getManifestByPattern(p) == null) {
                topLevel.add(p);
            }
        }

        Map<String, @Nullable Object> lockfileBasedOnResolver = lockfile.getLockfile(resolvedPatterns);

        YarnClassicOptions.Flags flags = config.getFlags();
        if (!flags.isLockfile() || flags.isPureLockfile() || flags.isFrozenLockfile()) {
            return null;
        }
        resolver.requireNoRaces();
        if (flags.isUpdateChecksums()) {
            for (Manifest manifest : resolver.getManifests()) {
                if (manifest.reference != null && "tarball".equals(manifest.reference.remote.type)) {
                    throw new CannotKnow("whether yarn's cache holds " + manifest + ": --update-checksums rewrites " +
                                         "the integrity of each package it downloads, not of one it finds there");
                }
            }
        }

        boolean lockFileHasAllPatterns = true;
        for (String p : topLevel) {
            if (lockfile.getLocked(p) == null) {
                lockFileHasAllPatterns = false;
                break;
            }
        }
        boolean lockfilePatternsMatch = true;
        Map<String, @Nullable Object> cacheKeys = new LinkedHashMap<>();
        Js.assign(cacheKeys, lockfile.getCache());
        for (String p : cacheKeys.keySet()) {
            if (lockfileBasedOnResolver.get(p) == null) {
                lockfilePatternsMatch = false;
                break;
            }
        }
        boolean resolverPatternsAreSameAsInLockfile = true;
        boolean integrityPatternsAreSameAsInLockfile = true;
        for (String pattern : Js.keys(lockfileBasedOnResolver)) {
            Map<?, ?> entry = (Map<?, ?>) lockfileBasedOnResolver.get(pattern);
            Map<String, @Nullable Object> manifest = lockfile.getLocked(pattern);
            if (resolverPatternsAreSameAsInLockfile && (manifest == null ||
                    !Objects.equals(manifest.get("resolved"), entry.get("resolved")) ||
                    !DeepEqual.deepEqual(manifest.get("prebuiltVariants"), entry.get("prebuiltVariants")))) {
                resolverPatternsAreSameAsInLockfile = false;
            }
            Object existingIntegrityInfo = entry.get("integrity");
            if (integrityPatternsAreSameAsInLockfile && Js.truthy(existingIntegrityInfo)) {
                Object manifestIntegrity = manifest == null ? null : manifest.get("integrity");
                if (!Js.truthy(manifestIntegrity) ||
                    !Ssri.stringify(manifestIntegrity).equals(existingIntegrityInfo)) {
                    integrityPatternsAreSameAsInLockfile = false;
                }
            }
        }

        if (!flags.isForce() &&
            LockfileParser.SUCCESS.equals(lockfile.getParseResultType()) &&
            lockFileHasAllPatterns &&
            lockfilePatternsMatch &&
            resolverPatternsAreSameAsInLockfile &&
            integrityPatternsAreSameAsInLockfile &&
            !topLevel.isEmpty()) {
            return null;
        }

        String lockSource = LockfileStringifier.stringify(lockfileBasedOnResolver, false,
                config.isEnableLockfileVersions(), processVersion(host.getNodeVersion(), config));
        return Fs.writeFilePreservingEol(rawLock, lockSource, host.getOs());
    }

    private static @Nullable String processVersion(@Nullable String nodeVersion, YarnClassicOptions config) {
        if (!config.isEnableLockfileVersions()) {
            return null;
        }
        if (nodeVersion == null) {
            throw new CannotKnow("the Node version yarn-enable-lockfile-versions writes");
        }
        return nodeVersion.startsWith("v") ? nodeVersion : "v" + nodeVersion;
    }
}
