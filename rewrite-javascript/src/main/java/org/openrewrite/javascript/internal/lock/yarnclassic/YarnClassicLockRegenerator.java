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

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.npm.Host;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Port of yarn src/cli/commands/install.js (yarn 1.22.22) for {@code yarn install} without node_modules.
 */
public final class YarnClassicLockRegenerator {

    /**
     * What the engine is given for a Yarn classic project.
     */
    @Value
    @Builder
    public static class Inputs {
        /**
         * The yarn.lock as the project holds it; null when the project has none.
         */
        @Nullable
        String lock;

        /**
         * The project's manifests by location: {@code ""} the root, {@code packages/a} a workspace member.
         */
        @Singular
        Map<String, String> manifests;

        @Nullable
        String yarnrc;

        @Nullable
        String npmrc;

        /**
         * The environment yarn runs in; null when the host does not know it.
         */
        @Nullable
        Map<String, String> env;

        @Builder.Default
        Host host = Host.UNKNOWN;

        @NonNull
        Instant now;
    }

    private final Inputs inputs;

    public YarnClassicLockRegenerator(Inputs inputs) {
        this.inputs = inputs;
    }

    /**
     * config.js {@code init} at the project's root, after rc.js read the .yarnrc: the root manifest as
     * {@code findWorkspaceRoot} reads it, the registries' config, the manifest again, the workspaces check.
     */
    YarnClassicOptions init(List<String> warnings) {
        Map<String, @Nullable Object> rc = YarnClassicOptions.rc(inputs.getYarnrc());
        boolean looseSemver = YarnClassicOptions.looseSemver(rc);
        String rootText = inputs.getManifests().get("");
        Manifest root = rootText == null ? null : tryManifest("", rootText, looseSemver, true, null, warnings);
        YarnClassicOptions config = YarnClassicOptions.load(rc, inputs.getNpmrc(), inputs.getEnv());
        if (rootText != null) {
            tryManifest("", rootText, looseSemver, false, "", warnings);
        }
        if (root != null && isWorkspaceRoot(root) && !config.isWorkspacesEnabled()) {
            throw new MessageError("Your project root defines workspaces but the feature is disabled in your Yarn " +
                                   "config. Please check \"workspaces-experimental\" in your .yarnrc file.");
        }
        return config;
    }

    /**
     * install.js {@code run}: the lock as yarn reads it, none under {@code --no-lockfile}.
     */
    Lockfile lockfile(YarnClassicOptions config) {
        return config.getFlags().isLockfile() ? Lockfile.fromDirectory(inputs.getLock()) : new Lockfile();
    }

    /**
     * config.js {@code tryManifest}: the manifest read, its registry and location set, then normalized.
     */
    static Manifest tryManifest(String location, String text, boolean looseSemver, boolean isRoot,
                                @Nullable String lockfileFolder, List<String> warnings) {
        String file = location.isEmpty() ? "package.json" : location + "/package.json";
        Object json = readJson(file, text);
        if (json == null) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot set properties of null (setting '_registry')");
        }
        Manifest manifest = manifest(json);
        manifest.put("_registry", NpmResolver.NPM_REGISTRY_ID);
        manifest.put("_loc", file);
        return NormalizeManifest.normalizeManifest(manifest, location, lockfileFolder, isRoot, looseSemver, warnings);
    }

    /**
     * util/fs.js {@code readJson}.
     *
     * @throws JsError the {@code SyntaxError} of JSON that does not parse, its message naming the file.
     */
    private static @Nullable Object readJson(String file, String text) {
        try {
            return Js.jsonParse(LockfileParser.stripBom(text));
        } catch (JsError e) {
            throw new JsError(e.getName(), file + ": " + e.getMessage());
        }
    }

    // an object, or an array with its indices; properties set on any other value are a TypeError
    @SuppressWarnings("unchecked")
    private static Manifest manifest(@Nullable Object json) {
        if (json instanceof Map) {
            return new Manifest((Map<String, @Nullable Object>) json);
        }
        if (json instanceof List) {
            Map<String, @Nullable Object> object = new LinkedHashMap<>();
            Js.assign(object, json);
            return new Manifest(object);
        }
        throw new JsError(JsError.TYPE_ERROR, "Cannot create property '_registry' on " + Js.typeOf(json) + " '" +
                                              Js.string(json) + "'");
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
     * install.js {@code fetchRequestFromCwd} for an install run at the project's root. The workspace members are
     * the manifests the host gives besides the root's.
     */
    @SuppressWarnings("unchecked")
    InstallCwdRequest fetchRequestFromCwd(Lockfile lockfile, YarnClassicOptions config, ResolutionMap resolutionMap,
                                          List<String> warnings) {
        List<String> patterns = new ArrayList<>();
        List<DependencyRequestPattern> deps = new ArrayList<>();
        List<DependencyRequestPattern> resolutionDeps = new ArrayList<>();
        String rootText = inputs.getManifests().get("");
        if (rootText == null) {
            return new InstallCwdRequest(resolutionDeps, patterns, new Manifest(), null, config.getFlags().isFlat());
        }
        String registry = NpmResolver.NPM_REGISTRY_ID;
        Manifest projectManifestJson = NormalizeManifest.normalizeManifest(manifest(readJson("package.json",
                rootText)), "", "", true, config.isLooseSemver(), warnings);
        Manifest manifest = projectManifestJson;
        boolean workspaceRootFolder = isWorkspaceRoot(projectManifestJson);

        Map<String, @Nullable Object> resolutions = new LinkedHashMap<>();
        Js.assign(resolutions, projectManifestJson.get("resolutions"));
        resolutionMap.init(resolutions);
        for (String packageName : Js.keys(resolutionMap.resolutionsByPackage)) {
            boolean optional = ObjectPath.has(manifest.get("optionalDependencies"), packageName) &&
                               config.getFlags().isIgnoreOptional();
            for (ResolutionMap.Resolution resolution : resolutionMap.resolutionsByPackage.get(packageName)) {
                resolutionDeps.add(DependencyRequestPattern.builder().registry(registry)
                        .pattern(resolution.getPattern()).optional(optional).hint("resolution").build());
            }
        }

        boolean flat = config.getFlags().isFlat();
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
        pushDeps.push("devDependencies", projectManifestJson, "dev", false, !config.isProduction());
        pushDeps.push("optionalDependencies", projectManifestJson, "optional", true, true);

        WorkspaceLayout workspaceLayout = null;
        if (workspaceRootFolder) {
            Map<String, WorkspaceLayout.Workspace> workspaces = resolveWorkspaces(projectManifestJson, config,
                    warnings);
            workspaceLayout = new WorkspaceLayout(workspaces, config);
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

    private static final String WORKSPACE_AGGREGATOR_UUID = "00000000-0000-4000-8000-000000000000";
    private static final List<String> OWNED_DEPENDENCY_TYPES = Arrays.asList("devDependencies",
            "dependencies", "optionalDependencies");

    private interface PushDeps {
        void push(String depType, Manifest manifest, @Nullable String hint, boolean optional, boolean isUsed);
    }

    /**
     * config.js {@code findWorkspaceRoot} for the project's own directory: a {@code workspaces} array, even an
     * empty one, or an object with one, makes the project a workspace root.
     */
    static boolean isWorkspaceRoot(Manifest manifest) {
        Map<String, @Nullable Object> ws = extractWorkspaces(manifest);
        return ws != null && Js.truthy(ws.get("packages"));
    }

    @SuppressWarnings("unchecked")
    static @Nullable Map<String, @Nullable Object> extractWorkspaces(Manifest manifest) {
        Object workspaces = manifest.get("workspaces");
        if (!Js.truthy(workspaces)) {
            return null;
        }
        if (workspaces instanceof List) {
            Map<String, @Nullable Object> ws = new LinkedHashMap<>();
            ws.put("packages", workspaces);
            return ws;
        }
        if (workspaces instanceof Map) {
            Map<String, @Nullable Object> ws = (Map<String, @Nullable Object>) workspaces;
            if ((Js.truthy(ws.get("packages")) && ws.get("packages") instanceof List) ||
                (Js.truthy(ws.get("nohoist")) && ws.get("nohoist") instanceof List)) {
                return ws;
            }
        }
        return null;
    }

    /**
     * config.js {@code getWorkspaces}: the root's workspaces config once it is eligible.
     *
     * @throws MessageError members or a nohoist list in a root that is not private, when {@code shouldThrow}.
     */
    static @Nullable Map<String, @Nullable Object> getWorkspaces(Manifest manifest, boolean shouldThrow,
                                                                   YarnClassicOptions config,
                                                                   List<String> warnings) {
        if (!config.isWorkspacesEnabled()) {
            return null;
        }
        Map<String, @Nullable Object> ws = extractWorkspaces(manifest);
        if (ws == null) {
            return null;
        }
        Map<String, @Nullable Object> wsCopy = new LinkedHashMap<>(ws);
        List<String> warned = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        boolean isPrivate = Js.truthy(manifest.get("private"));
        Object packages = wsCopy.get("packages");
        if (Js.truthy(packages) && Js.length(packages) > 0 && !isPrivate) {
            errors.add("Workspaces can only be enabled in private projects.");
            wsCopy = null;
        }
        if (wsCopy != null && Js.truthy(wsCopy.get("nohoist")) && Js.length(wsCopy.get("nohoist")) > 0) {
            if (!config.isWorkspacesNohoistEnabled()) {
                warned.add(Lang.format("$0 defines nohoist but the feature is disabled in your Yarn config " +
                                       "(\"workspaces-nohoist-experimental\" in .yarnrc file)", manifest.getName()));
                wsCopy.put("nohoist", null);
            } else if (!isPrivate) {
                errors.add(Lang.format("nohoist config is ignored in $0 because it is not a private package. If " +
                                       "you think nohoist should be allowed in public packages, please submit an " +
                                       "issue for your use case.", manifest.getName()));
                wsCopy.put("nohoist", null);
            }
        }
        if (!errors.isEmpty() && shouldThrow) {
            throw new MessageError(String.join("\n", errors));
        }
        List<String> msg = new ArrayList<>(errors);
        msg.addAll(warned);
        if (!msg.isEmpty()) {
            warnings.add(String.join("\n", msg));
        }
        return wsCopy;
    }

    /**
     * config.js {@code resolveWorkspaces} over the member manifests the host gives.
     */
    private Map<String, WorkspaceLayout.Workspace> resolveWorkspaces(Manifest root, YarnClassicOptions config,
                                                                      List<String> warnings) {
        Map<String, WorkspaceLayout.Workspace> workspaces = new LinkedHashMap<>();
        if (!config.isWorkspacesEnabled()) {
            return workspaces;
        }
        Map<String, @Nullable Object> ws = getWorkspaces(root, true, config, warnings);
        Object patterns = ws != null && Js.truthy(ws.get("packages")) ? ws.get("packages") : new ArrayList<>();
        if (!(patterns instanceof List)) {
            throw new MessageError("The workspaces field in package.json must be an array.");
        }
        if (((List<?>) patterns).isEmpty()) {
            return workspaces;
        }
        for (Map.Entry<String, String> member : inputs.getManifests().entrySet()) {
            String loc = member.getKey();
            if (loc.isEmpty()) {
                continue;
            }
            Manifest manifest = tryManifest(loc, member.getValue(), config.isLooseSemver(), false, "", warnings);
            if (!Js.truthy(manifest.getName())) {
                warnings.add(Lang.format("Missing name in workspace at $0, ignoring.", loc));
                continue;
            }
            if (!Js.truthy(manifest.getVersion())) {
                warnings.add(Lang.format("Missing version in workspace at $0, ignoring.", loc));
                continue;
            }
            String name = Js.string(manifest.getName());
            if (workspaces.containsKey(name)) {
                throw new MessageError(Lang.format("There are more than one workspace with name $0", name));
            }
            workspaces.put(name, new WorkspaceLayout.Workspace(loc, manifest));
        }
        return workspaces;
    }

    /**
     * {@code saveLockfileAndIntegrity}: the lock text yarn writes for the resolver's patterns, or {@code null} when
     * yarn leaves the file as it is.
     *
     * @param patterns the top-level patterns of the install
     */
    static @Nullable String saveLockfileAndIntegrity(PackageResolver resolver, List<String> patterns,
                                                     @Nullable WorkspaceLayout workspaceLayout,
                                                     YarnClassicOptions config, @Nullable String rawLock,
                                                     @Nullable String nodeVersion) {
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

        Lockfile lockfile = resolver.lockfile;
        Map<String, @Nullable Object> lockfileBasedOnResolver = lockfile.getLockfile(resolvedPatterns);

        YarnClassicOptions.Flags flags = config.getFlags();
        if (!flags.isLockfile() || flags.isPureLockfile() || flags.isFrozenLockfile()) {
            return null;
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
                config.isEnableLockfileVersions(), processVersion(nodeVersion, config));
        return writeFilePreservingEol(rawLock, lockSource);
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

    /**
     * util/fs.js {@code writeFilePreservingEol}: the line ending of the file it replaces, the first CR or LF in it.
     */
    static String writeFilePreservingEol(@Nullable String existing, String data) {
        String eol = null;
        if (existing != null) {
            for (int i = 0; i < existing.length(); i++) {
                char c = existing.charAt(i);
                if (c == '\r') {
                    eol = "\r\n";
                    break;
                }
                if (c == '\n') {
                    eol = "\n";
                    break;
                }
            }
        }
        if (eol == null) {
            eol = "\n";
        }
        return "\n".equals(eol) ? data : data.replace("\n", eol);
    }

    /**
     * What the engine cannot know: tarball contents, git checkouts, network timing, the host's environment.
     */
    static final class CannotKnow extends RuntimeException {
        CannotKnow(String subject) {
            super(subject);
        }
    }
}
