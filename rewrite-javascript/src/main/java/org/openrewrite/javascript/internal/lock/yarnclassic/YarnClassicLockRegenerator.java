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
 * Port of yarn src/cli/commands/install.js (yarn 1.22.22) for {@code yarn install} over a project without
 * node_modules: the lock and the manifests read, and the lock saved from the resolver's patterns, or left as it is
 * when nothing in it would change.
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

    YarnClassicOptions options() {
        return YarnClassicOptions.load(inputs.getYarnrc(), inputs.getNpmrc(), inputs.getEnv());
    }

    /**
     * The lock as yarn reads it ({@code Lockfile.fromDirectory}).
     *
     * @throws MessageError a lockfile version newer than yarn 1 reads.
     */
    Lockfile lockfile() {
        return Lockfile.fromDirectory(inputs.getLock());
    }

    /**
     * A manifest as yarn reads and normalizes it: {@code fs.readJson}, then {@code normalizeManifest}.
     *
     * @throws JsError the {@code SyntaxError} of JSON that does not parse, its message naming the file.
     */
    @SuppressWarnings("unchecked")
    static Manifest readManifest(String location, String text, YarnClassicOptions config, boolean isRoot,
                                 List<String> warnings) {
        Object json;
        String file = location.isEmpty() ? "package.json" : location + "/package.json";
        try {
            json = Js.jsonParse(LockfileParser.stripBom(text));
        } catch (JsError e) {
            throw new JsError(e.getName(), file + ": " + e.getMessage());
        }
        Manifest manifest = new Manifest(json instanceof Map ? (Map<String, @Nullable Object>) json : listObject(json));
        return NormalizeManifest.normalizeManifest(manifest, location, "", isRoot, config.isLooseSemver(), warnings);
    }

    /**
     * What {@code fetchRequestFromCwd} gathers from the project's manifests: the root requests in yarn's order
     * (resolutions, then dependencies, devDependencies, optionalDependencies, then the workspaces), the top-level
     * patterns, the root manifest and the workspace members.
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
        Manifest projectManifestJson = readManifest("", rootText, config, true, warnings);
        Manifest manifest = projectManifestJson;

        resolutionMap.init(projectManifestJson.getMap("resolutions"));
        for (String packageName : Js.keys(resolutionMap.resolutionsByPackage)) {
            Map<String, @Nullable Object> optionalDependencies = manifest.getMap("optionalDependencies");
            boolean optional = optionalDependencies != null && optionalDependencies.containsKey(packageName) &&
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
            Map<String, @Nullable Object> depMap = depManifest.getMap(depType);
            if (depMap == null) {
                return;
            }
            for (String name : Js.keys(depMap)) {
                String pattern = name;
                if (lockfile.getLocked(pattern) == null) {
                    pattern += "@" + Js.string(depMap.get(name));
                }
                patterns.add(pattern);
                deps.add(DependencyRequestPattern.builder().pattern(pattern).registry(registry).hint(hint)
                        .optional(optional).workspaceName(Js.string(depManifest.getName())).build());
            }
        };

        pushDeps.push("dependencies", projectManifestJson, null, false, true);
        pushDeps.push("devDependencies", projectManifestJson, "dev", false, !config.isProduction());
        pushDeps.push("optionalDependencies", projectManifestJson, "optional", true, true);

        WorkspaceLayout workspaceLayout = null;
        Map<String, WorkspaceLayout.Workspace> workspaces = resolveWorkspaces(projectManifestJson, config, warnings);
        if (workspaces != null) {
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
                Map<String, @Nullable Object> owned = projectManifestJson.getMap(type);
                if (owned != null) {
                    for (String dependencyName : owned.keySet()) {
                        implicitWorkspaceDependencies.remove(dependencyName);
                    }
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
     * config.js {@code resolveWorkspaces}, with the members the host found for the root's {@code workspaces}
     * patterns: {@code null} when the root is no workspace root (no patterns, or not private).
     */
    @SuppressWarnings("unchecked")
    private @Nullable Map<String, WorkspaceLayout.Workspace> resolveWorkspaces(Manifest root, YarnClassicOptions config,
                                                                                List<String> warnings) {
        Object ws = root.get("workspaces");
        Object packages = ws instanceof List ? ws :
                ws instanceof Map ? ((Map<String, Object>) ws).get("packages") : null;
        if (!(packages instanceof List) || ((List<?>) packages).isEmpty() || !Js.truthy(root.get("private"))) {
            return null;
        }
        Map<String, WorkspaceLayout.Workspace> workspaces = new LinkedHashMap<>();
        for (Map.Entry<String, String> member : inputs.getManifests().entrySet()) {
            String loc = member.getKey();
            if (loc.isEmpty()) {
                continue;
            }
            Manifest manifest = readManifest(loc, member.getValue(), config, false, warnings);
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

    // map(JSON.parse(text)) of an array or a primitive: an object holding its indices, or none
    private static Map<String, @Nullable Object> listObject(@Nullable Object json) {
        Map<String, @Nullable Object> object = new LinkedHashMap<>();
        Js.assign(object, json instanceof List ? json : null);
        return object;
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
