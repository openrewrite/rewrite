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
import org.openrewrite.semver.NodeSemver;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of pnpm 11.2.2's {@code pnpm install --lockfile-only} up to the resolution: pnpm/src main.ts and getConfig.ts
 * before the command, then @pnpm/installing.commands installDeps and @pnpm/installing.deps-installer install/index.ts.
 */
public final class PnpmLockRegenerator {

    /**
     * What the engine is given for a pnpm project.
     */
    @Value
    @Builder
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
    }


    private static final String PNPM_VERSION = "11.2.2";

    private final Inputs inputs;

    // a project pnpm lists without a package.json, which it reports once the lock is verified
    private @Nullable String missingManifest;

    public PnpmLockRegenerator(Inputs inputs) {
        this.inputs = inputs;
    }

    Install install() {
        String rootManifestText = inputs.getManifests().get("");
        Map<String, @Nullable Object> rootManifest = rootManifestText == null ? null : rootManifest(rootManifestText);
        PnpmOptions options = PnpmOptions.load(inputs.getFiles().get("pnpm-workspace.yaml"),
                inputs.getFiles().get(".npmrc"), rootManifest, inputs.getEnv());
        String lockText = inputs.getLock();
        String packageManagerToResolve = checkWantedPackageManager(options, EnvLockfile.readEnvLockfile(lockText));

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
            configDependenciesToResolve = configDeps.getDepsToResolve();
            if (configDependenciesToResolve.isEmpty() && configDeps.isLockfileChanged()) {
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
                    configDependenciesToResolve, wantedLockfile, options, projects, read.isHadConflicts());
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
                    options, projects, read.isHadConflicts());
        }
        boolean envResolves = packageManagerToResolve != null || !configDependenciesToResolve.isEmpty();
        return new Install(envResolves ? null : LockfileWriter.writeWantedLockfile(wantedLockfile, lockText), null,
                packageManagerToResolve, configDependenciesToResolve, wantedLockfile, options, projects, false);
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

    // main.ts: the wanted package manager is checked before anything else, and a pnpm release other than this one
    // takes the install over; what pnpm resolves into the env document's packageManagerDependencies, or null
    private @Nullable String checkWantedPackageManager(PnpmOptions options, @Nullable EnvLockfile envLockfile) {
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
                                                     @Nullable EnvLockfile envLockfile) {
        if (!"pnpm".equals(pm.getName()) || pm.getVersion() == null) {
            return null;
        }
        String version = Js.string(pm.getVersion());
        boolean persistLockfile = shouldPersistLockfile(pm);
        if (!persistLockfile && PNPM_VERSION.equals(version)) {
            return null;
        }
        EnvLockfile locked = persistLockfile ? envLockfile : null;
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

    static @Nullable String syncEnvLockfile(PnpmOptions.WantedPackageManager pm, @Nullable EnvLockfile envLockfile) {
        if (!"pnpm".equals(pm.getName()) || pm.getVersion() == null || !shouldPersistLockfile(pm)) {
            return null;
        }
        String version = Js.string(pm.getVersion());
        if (!NodeSemver.satisfies(PNPM_VERSION, version, NodeSemver.Options.INCLUDE_PRERELEASE)) {
            return null;
        }
        Object lockedVersion = EnvInstaller.lockedVersion(envLockfile == null ? null :
                envLockfile.packageManagerDependencies(), "pnpm");
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
                if (inputs.getFiles().containsKey(defaultPnpmfile)) {
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
