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
import org.openrewrite.javascript.internal.LockFileRegeneration.Failure;
import org.openrewrite.javascript.internal.LockFileRegeneration.InstallRoot;
import org.openrewrite.javascript.internal.LockFileRegeneration.Reason;
import org.openrewrite.javascript.internal.LockFileRegeneration.Result;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.LockOnlyInstall;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.Rejected;
import org.openrewrite.javascript.internal.lock.SharedError;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;

/**
 * Port of yarn src/cli/commands/install.js (yarn 1.22.22) for {@code yarn install} without node_modules.
 */
public final class YarnClassicLockRegenerator {

    private static final LockOnlyInstall INSTALL = LockOnlyInstall.YARN;

    /**
     * What the engine is given for a Yarn classic project.
     */
    @Value
    @Builder(toBuilder = true)
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

        /**
         * The .yarnrc, .yarnrc.yml and .npmrc files of the project's directory, the directories above it and those from
         * {@link #cwd} up, and the package.json of the directories above, by path from the root.
         */
        @Singular
        Map<String, String> configFiles;

        /**
         * The paths the project holds relative to its root, and the files of the directories above it
         * ({@code ../.npmrc}), when the host says; a config file among them the host did not read is unknown.
         */
        @Nullable
        Set<String> paths;

        /**
         * The project's directory from the repository's root ({@code mid/app}, {@code ""} at the root), when the
         * host says; yarn looks above it for a workspace root that lists the project.
         */
        @Nullable
        String directory;

        /**
         * The directory yarn runs in relative to the root ({@code packages/a}), {@code ""} the root's: a workspace
         * member whose own lock yarn passes over for the root's.
         */
        @Builder.Default
        String cwd = "";

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
    private final PackumentSource registry;

    public YarnClassicLockRegenerator(Inputs inputs, PackumentSource registry) {
        this.inputs = inputs;
        this.registry = registry;
    }

    YarnClassicLockRegenerator(Inputs inputs) {
        this(inputs, (r, name) -> null);
    }

    /**
     * The lock yarn writes for the edited manifests over the project's lock, or the failure, after a second install
     * over the written lock changed nothing.
     */
    public Result regenerate() {
        String lock = inputs.getLock();
        if (lock == null) {
            return Result.failure(INSTALL.failure(Rejected.discardsLock("the project has no yarn.lock")));
        }
        String written;
        try {
            written = install(lock);
        } catch (RuntimeException e) {
            return Result.failure(failure(e));
        }
        return INSTALL.verify(written, this::install, this::failure);
    }

    /**
     * One install over a lock: the lock the project holds afterwards.
     */
    String install(String lock) {
        Inputs these = lock.equals(inputs.getLock()) ? inputs : inputs.toBuilder().lock(lock).build();
        YarnClassicLockRegenerator regenerator = these == inputs ? this : new YarnClassicLockRegenerator(these,
                registry);
        return regenerator.install();
    }

    private String install() {
        requireConfigFiles();
        List<String> warnings = new ArrayList<>();
        YarnClassicOptions config = init(warnings);
        YarnClassicOptions.Flags flags = config.getFlags();
        String lock = Objects.requireNonNull(inputs.getLock());
        String saved = install(config, flags.isLockfile() ? lockfile(config) : Lockfile.fromDirectory(lock), warnings);
        if (!flags.isLockfile() || flags.isPureLockfile() || flags.isFrozenLockfile()) {
            if (saved != null && !saved.equals(lock)) {
                String flag = !flags.isLockfile() ? "--no-lockfile" : flags.isPureLockfile() ? "--pure-lockfile" :
                        "--frozen-lockfile";
                throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, flag + " in .yarnrc (yarn exits 0 and writes no " +
                                                                   "lock)");
            }
            return lock;
        }
        return saved == null ? lock : saved;
    }

    // install.js init: the steps up to saveLockfileAndIntegrity, which here writes whatever the flags say
    private @Nullable String install(YarnClassicOptions config, Lockfile lockfile, List<String> warnings) {
        if (lockfile.getLockfilev2() != null) {
            throw Rejected.discardsLock("the lock is a Yarn Berry lock (__metadata)");
        }
        if (config.getFlags().isLockfile() && LockfileParser.CONFLICT.equals(lockfile.getParseResultType())) {
            throw Rejected.discardsLock("the lock's merge conflict does not merge");
        }
        YarnClassicOptions.Flags flags = config.getFlags();
        ResolutionMap resolutionMap = new ResolutionMap(config);
        InstallCwdRequest request = fetchRequestFromCwd(lockfile, config, resolutionMap, warnings);
        String rootText = inputs.getManifests().get("");
        if (rootText != null && Js.jsonParse(LockfileParser.stripBom(rootText)) instanceof List) {
            throw Rejected.rejects(Reason.MALFORMED_MANIFEST, "package.json is an array, not an object: yarn writes " +
                                                              "a lock with no entries");
        }

        List<String> errors = new ArrayList<>();
        PackageCompatibility compatibility = new PackageCompatibility(config, inputs.getHost(), errors, warnings);
        try {
            if (PackageCompatibility.shouldCheck(request.getManifest(), flags.isIgnoreEngines(),
                    flags.isIgnorePlatform())) {
                compatibility.checkOne(request.getManifest(), flags.isIgnoreEngines());
            }

            PackageResolver resolver = new PackageResolver(config, lockfile, resolutionMap, registry,
                    new ProjectFiles(inputs.getManifests(), inputs.getPaths(), inputs.getCwd()), inputs.getHost(),
                    warnings);
            resolver.init(request.getRequests(), request.isFlat(), flags.isFrozenLockfile(),
                    request.getWorkspaceLayout());
            if (!resolver.races.isEmpty()) {
                throw new CannotKnow("which of " + String.join("; ", resolver.races) + " yarn resolves first: " +
                                     "the entry they share follows the order the registry answers in");
            }
            List<String> topLevelPatterns = request.getPatterns();
            flatten(resolver, topLevelPatterns, request.getManifest(), request.isFlat());
            bailout(topLevelPatterns, request.getWorkspaceLayout(), lockfile, flags);

            if (config.isOffline() && !config.isPreferOffline()) {
                throw config.offlineCache();
            }
            resolver.updateManifests(PackageFetcher.fetch(resolver.getManifests(), ref -> fetched(resolver, ref,
                    config), warnings));
            compatibility.check(resolver.getManifests(), flags.isIgnoreEngines());

            return saveLockfileAndIntegrity(resolver, topLevelPatterns, request.getWorkspaceLayout(), config,
                    lockfile, inputs.getLock(), inputs.getHost());
        } catch (MessageError e) {
            if (!errors.isEmpty()) {
                throw Rejected.rejects(errors.get(0).startsWith("The engine ") ? SharedError.UNSUPPORTED_ENGINE :
                        SharedError.UNSUPPORTED_PLATFORM, null, errors.get(0) + " " + e.getMessage());
            }
            throw e;
        }
    }

    // every install reads the config files of the directory it runs in and of each directory above it
    private void requireConfigFiles() {
        Set<String> paths = inputs.getPaths();
        if (paths == null) {
            return;
        }
        Map<String, String> configFiles = configFilesFromCwd();
        for (String path : paths) {
            String fromCwd = fromCwd(path);
            if (fromCwd != null && CONFIG_FILE.matcher(fromCwd).matches() && !configFiles.containsKey(fromCwd)) {
                throw new CannotKnow(path + " (its contents were not read)");
            }
        }
    }

    private static final Pattern CONFIG_FILE = Pattern.compile("(\\.\\./)*\\.(yarnrc|yarnrc\\.yml|npmrc)");

    // the config files by their path from the directory yarn runs in
    private Map<String, String> configFilesFromCwd() {
        if (inputs.getCwd().isEmpty()) {
            return inputs.getConfigFiles();
        }
        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : inputs.getConfigFiles().entrySet()) {
            String path = fromCwd(file.getKey());
            if (path != null) {
                files.put(path, file.getValue());
            }
        }
        return files;
    }

    // a path from the root as a path from cwd, when it sits in cwd or a directory above it
    private @Nullable String fromCwd(String path) {
        String cwd = inputs.getCwd();
        int slash = path.lastIndexOf('/');
        String dir = slash < 0 || path.startsWith("../") ? "" : path.substring(0, slash);
        if (!dir.isEmpty() && !cwd.equals(dir) && !cwd.startsWith(dir + "/")) {
            return null;
        }
        return YarnClassicOptions.up(segments(cwd) - segments(dir)) + (dir.isEmpty() ? path :
                path.substring(slash + 1));
    }

    private static int segments(String location) {
        return location.isEmpty() ? 0 : location.split("/").length;
    }

    /**
     * package-fetcher.js {@code fetchOneRemote}'s package.json: a pick from the registry stands for its tarball's,
     * a reused entry for what it holds, a directory reads its own.
     */
    private Manifest fetched(PackageResolver resolver, PackageReference ref, YarnClassicOptions config) {
        Manifest pkg = resolver.patterns.get(ref.patterns.get(0));
        if ("workspace".equals(ref.remote.type)) {
            return pkg;
        }
        if ("copy".equals(ref.remote.type)) {
            Manifest copy = pkg.copy();
            copy.data.remove("_uid");
            return copy;
        }
        if (pkg.packageJson != null) {
            Manifest packageJson = new Manifest(PlainObject.copyOf(pkg.packageJson));
            packageJson.data.remove("_uid");
            return NormalizeManifest.normalizeManifest(packageJson, "", null, false, config.isLooseSemver(),
                    resolver.warnings);
        }
        Manifest locked = new Manifest();
        Map<String, @Nullable Object> entry = pkg.locked;
        if (entry != null) {
            locked.put("name", entry.get("name"));
            locked.put("version", entry.get("version"));
            locked.put("dependencies", entry.get("dependencies"));
            locked.put("optionalDependencies", entry.get("optionalDependencies"));
        } else {
            locked.put("name", pkg.getName());
            locked.put("version", pkg.getVersion());
            locked.put("dependencies", pkg.get("dependencies"));
            locked.put("optionalDependencies", pkg.get("optionalDependencies"));
        }
        return locked;
    }

    /**
     * install.js {@code flatten}: under {@code flat}, every package with more than one version collapses to the one
     * the root's {@code resolutions} names; without one yarn asks, which it cannot without a terminal.
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
                    throw new JsError(JsError.INVARIANT_VIOLATION, "expected package reference");
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
     * install.js {@code bailout} without node_modules: only {@code --frozen-lockfile} stops here, when the lock
     * misses a top-level pattern or did not parse cleanly.
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

    private Failure failure(RuntimeException e) {
        return INSTALL.failure(outcome(e));
    }

    private static RuntimeException outcome(RuntimeException e) {
        if (e instanceof LocaleCompare.UnknownOrder) {
            return new CannotKnow("the order yarn finds the workspaces in: " + e.getMessage());
        }
        if (e instanceof MessageError) {
            String message = String.valueOf(e.getMessage());
            return Rejected.rejects(reasonFor(message), message);
        }
        // the registry's 404 is its verdict, as in every port; yarn's own plain Errors (a TTY question) have no code
        if (e instanceof NpmRegistry.NotFound) {
            return Rejected.rejects(SharedError.PACKAGE_NOT_FOUND, ((NpmRegistry.NotFound) e).getPackageName(),
                    e.toString());
        }
        if (e instanceof JsError) {
            JsError error = (JsError) e;
            Reason reason = error.isSyntaxError() ? Reason.MALFORMED_MANIFEST : Reason.RESOLUTION_REQUIRED;
            return Rejected.crashes(reason, null, error.toString());
        }
        return e;
    }

    private static Reason reasonFor(String message) {
        if (message.startsWith("Couldn't find any versions for") || message.startsWith("No valid versions found")) {
            return SharedError.NO_MATCHING_VERSION.getReason();
        }
        if (message.startsWith("Couldn't find package")) {
            return SharedError.PACKAGE_NOT_FOUND.getReason();
        }
        if (message.startsWith("Can't install from a lockfile of version")) {
            return Reason.UNSUPPORTED_LOCKFILE_VERSION;
        }
        if (message.startsWith("Can't add ")) {
            return Reason.MALFORMED_LOCK;
        }
        if (message.startsWith("package.json: ") || message.contains("workspaces") ||
            message.contains("Workspaces") || message.contains("nohoist") ||
            message.startsWith("There are more than one workspace with name")) {
            return Reason.MALFORMED_MANIFEST;
        }
        return Reason.RESOLUTION_REQUIRED;
    }

    /**
     * cli/index.js {@code start} and {@code main}, then config.js {@code init}: the workspace root from the project's
     * directory up, the registries' config, the manifest of the lock's directory, the workspaces check.
     */
    YarnClassicOptions init(List<String> warnings) {
        Map<String, @Nullable Object> rc = YarnClassicOptions.rc(configFilesFromCwd());
        requireRelease(rc);
        boolean looseSemver = YarnClassicOptions.looseSemver(rc);
        requirePackageManager();
        requireWorkspaceRootAbove(looseSemver);
        int workspaceRoot = findWorkspaceRoot(looseSemver, warnings);
        if (YarnClassicOptions.focus(rc) && workspaceRoot <= 0) {
            throw new MessageError("This command can only be run inside an individual workspace.");
        }
        YarnClassicOptions config = YarnClassicOptions.load(rc, configFilesFromCwd(), inputs.getEnv());
        String up = YarnClassicOptions.up(Math.max(workspaceRoot, 0));
        String lockfileFolder = up.isEmpty() ? "" : up.substring(0, up.length() - 1);
        String manifest = up.isEmpty() ? inputs.getManifests().get("") :
                inputs.getConfigFiles().get(up + "package.json");
        Manifest root = manifest == null ? null :
                tryManifest(lockfileFolder, manifest, looseSemver, false, lockfileFolder, warnings);
        if (workspaceRoot >= 0 && !config.isWorkspacesEnabled()) {
            throw new MessageError("Your project root defines workspaces but the feature is disabled in your Yarn " +
                                   "config. Please check \"workspaces-experimental\" in your .yarnrc file.");
        }
        if (workspaceRoot > 0) {
            installAtWorkspaceRoot(workspaceRoot, Objects.requireNonNull(root), config, warnings);
        }
        return config;
    }

    /**
     * Where yarn run in the project's directory installs and writes its lock: here under {@code --focus}; a walk that
     * fails, which {@link #regenerate()} reports, stops here.
     */
    public static InstallRoot installRoot(Inputs inputs) {
        try {
            return new InstallRoot(new YarnClassicLockRegenerator(inputs).installRoot(), false);
        } catch (RuntimeException e) {
            return InstallRoot.STOPS_HERE;
        }
    }

    // config.js findWorkspaceRoot starts where yarn runs: a manifest with workspaces below this lock's stops it there
    private void requireWorkspaceRootAbove(boolean looseSemver) {
        Set<String> paths = inputs.getPaths();
        for (String dir = inputs.getCwd(); !dir.isEmpty(); dir = dir.contains("/") ?
                dir.substring(0, dir.lastIndexOf('/')) : "") {
            String file = dir + "/package.json";
            String text = inputs.getManifests().containsKey(dir) ? inputs.getManifests().get(dir) :
                    inputs.getConfigFiles().get(file);
            if (text == null) {
                if (paths != null && paths.contains(file)) {
                    throw new CannotKnow(file + " (its contents were not read)");
                }
                continue;
            }
            Map<String, @Nullable Object> ws = extractWorkspaces(tryManifest(dir, text, looseSemver, true, null,
                    new ArrayList<>()));
            if (ws != null && Js.truthy(ws.get("packages"))) {
                throw new CannotKnow("the lock yarn writes: run in " + inputs.getCwd() + " it stops at the " +
                                     "workspaces of " + file + ", not at this lock");
            }
        }
    }

    private int installRoot() {
        Map<String, @Nullable Object> rc = YarnClassicOptions.rc(configFilesFromCwd());
        requireRelease(rc);
        requirePackageManager();
        int workspaceRoot = findWorkspaceRoot(YarnClassicOptions.looseSemver(rc), new ArrayList<>());
        if (workspaceRoot <= 0 || YarnClassicOptions.focus(rc)) {
            return 0;
        }
        return workspaceRoot;
    }

    /**
     * install.js {@code run} and {@code fetchRequestFromCwd} at a workspace root above the project, as far as they go
     * without what the inputs lack: the root's lock when it has one, then its workspaces up to their glob.
     */
    private void installAtWorkspaceRoot(int workspaceRoot, Manifest root, YarnClassicOptions config,
                                        List<String> warnings) {
        String up = YarnClassicOptions.up(workspaceRoot);
        Set<String> paths = inputs.getPaths();
        if (!config.getFlags().isLockfile() || paths != null && !paths.contains(up + "yarn.lock")) {
            workspacePatterns(root, config, warnings);
        }
        throw new CannotKnow("the lock yarn writes for this edit: yarn installs at the workspace root " + up +
                             ", whose workspaces list " + relativePath(workspaceRoot) + ", and writes " + up +
                             "yarn.lock, not this project's");
    }

    /**
     * config.js {@code findWorkspaceRoot}: how many directories above the project's the nearest manifest with a
     * workspaces list is when that list matches the project's path from it, 0 for the project's own, else -1.
     */
    int findWorkspaceRoot(boolean looseSemver, List<String> warnings) {
        for (int depth = 0; depth <= reach(); depth++) {
            Manifest manifest = findManifest(depth, looseSemver, warnings);
            Map<String, @Nullable Object> ws = manifest == null ? null : extractWorkspaces(manifest);
            if (ws != null && Js.truthy(ws.get("packages"))) {
                if (depth == 0) {
                    return 0;
                }
                return Micromatch.micromatch(singletonList(relativePath(depth)), ws.get("packages")).isEmpty() ?
                        -1 : depth;
            }
        }
        return -1;
    }

    // config.js findManifest: the directory's package.json, else its yarn.json
    private @Nullable Manifest findManifest(int depth, boolean looseSemver, List<String> warnings) {
        String up = YarnClassicOptions.up(depth);
        String text = depth == 0 ? inputs.getManifests().get("") : inputs.getConfigFiles().get(up + "package.json");
        if (text != null) {
            return tryManifest(depth == 0 ? "" : up.substring(0, up.length() - 1), text, looseSemver, true, null,
                    warnings);
        }
        Set<String> paths = inputs.getPaths();
        for (String name : depth == 0 ? singletonList("yarn.json") : Arrays.asList("package.json", "yarn.json")) {
            if (paths != null && paths.contains(up + name)) {
                throw new CannotKnow(up + name + " (its contents were not read)");
            }
        }
        return null;
    }

    // path.relative from the directory depth levels above the project's to the project's
    private String relativePath(int depth) {
        String directory = inputs.getDirectory();
        if (directory == null) {
            throw new CannotKnow("whether the workspaces of " + YarnClassicOptions.up(depth) + "package.json list " +
                                 "this project, whose directory in the repository the host did not give");
        }
        String[] segments = directory.split("/");
        return String.join("/", Arrays.asList(segments).subList(segments.length - depth, segments.length));
    }

    // how many directories above the project's the inputs reach: the repository's root when the host says where the
    // project sits in it, else the deepest file given
    private int reach() {
        String directory = inputs.getDirectory();
        if (directory != null) {
            return directory.isEmpty() ? 0 : directory.split("/").length;
        }
        Set<String> files = new LinkedHashSet<>(inputs.getConfigFiles().keySet());
        if (inputs.getPaths() != null) {
            files.addAll(inputs.getPaths());
        }
        return YarnClassicOptions.depth(files);
    }

    private static final Pattern RELEASE = Pattern.compile("yarn-1\\.22\\.22\\.[cm]?js");
    private static final Pattern IN_A_DIRECTORY_ABOVE = Pattern.compile("(\\.\\./)+[^/]+");
    private static final Pattern YARN_0_OR_1 = Pattern.compile("^yarn@[01]\\.");
    private static final Pattern BUILD_METADATA = Pattern.compile("\\+" + Js.DOT + "*");

    /**
     * cli/index.js {@code start}: a yarn-path in the rc chain hands the install to the release it names, which is
     * this port only when it is a yarn 1.22.22 release the project holds.
     */
    void requireRelease(Map<String, @Nullable Object> rc) {
        Object yarnPath = Js.truthy(rc.get("yarn-path")) ? rc.get("yarn-path") : rc.get("yarnPath");
        Map<String, String> env = inputs.getEnv();
        if (!Js.truthy(yarnPath) ||
            YarnClassicOptions.boolifyWithDefault(env == null ? null : env.get("YARN_IGNORE_PATH"), false)) {
            return;
        }
        String release = PosixPath.normalize(Js.string(yarnPath));
        if (!PosixPath.isAbsolute(release)) {
            release = PosixPath.normalize(PosixPath.join(inputs.getCwd(), release));
        }
        Set<String> paths = inputs.getPaths();
        if (yarnPath instanceof String && RELEASE.matcher(PosixPath.basename(release)).matches()) {
            if (paths == null || paths.contains(release)) {
                return;
            }
            // the host's paths cover the project and the files directly in each directory above it they reach
            if (!PosixPath.isAbsolute(release) && (!release.startsWith("../") ||
                IN_A_DIRECTORY_ABOVE.matcher(release).matches() &&
                YarnClassicOptions.depth(singletonList(release)) <= reach())) {
                throw new JsError(JsError.ERROR, "Cannot find module '" + release + "'");
            }
        }
        throw new CannotKnow("what the release yarn-path names (" + release + ") writes: yarn 1.22.22 hands the " +
                             "install to it");
    }

    /**
     * cli/index.js {@code main} without corepack: the nearest package.json from the project's directory up with a
     * packageManager field stops yarn when that field names another package manager than yarn 0 or 1.
     */
    private void requirePackageManager() {
        Map<String, String> env = inputs.getEnv();
        if (env != null && (Js.truthy(env.get("COREPACK_ROOT")) || Js.truthy(env.get("SKIP_YARN_COREPACK_CHECK")))) {
            return;
        }
        String packageManager = findPackageManager();
        if (packageManager != null && !YARN_0_OR_1.matcher(packageManager).find()) {
            String version = BUILD_METADATA.matcher(packageManager.replaceFirst("^yarn@", "")).replaceFirst("");
            throw Rejected.rejects(Reason.RESOLUTION_REQUIRED, "This project's package.json defines " +
                                                           "\"packageManager\": \"yarn@" + version + "\". However " +
                                                           "the current global version of Yarn is 1.22.22.");
        }
    }

    // cli/index.js findPackageManager from where yarn runs, which passes over a package.json JSON.parse rejects
    private @Nullable String findPackageManager() {
        Set<String> paths = inputs.getPaths();
        for (String dir = inputs.getCwd(); !dir.isEmpty(); dir = dir.substring(0, Math.max(dir.lastIndexOf('/'), 0))) {
            String text = inputs.getManifests().containsKey(dir) ? inputs.getManifests().get(dir) :
                    inputs.getConfigFiles().get(dir + "/package.json");
            if (text == null && paths != null && paths.contains(dir + "/package.json")) {
                throw new CannotKnow(dir + "/package.json (its contents were not read)");
            }
            String packageManager = text == null ? null : packageManager(text);
            if (packageManager != null) {
                return packageManager;
            }
        }
        String up = "";
        for (int depth = 0; depth <= reach(); depth++, up += "../") {
            String file = up + "package.json";
            String text = depth == 0 ? inputs.getManifests().get("") : inputs.getConfigFiles().get(file);
            if (text == null) {
                if (depth > 0 && paths != null && paths.contains(file)) {
                    throw new CannotKnow(file + " (its contents were not read)");
                }
                continue;
            }
            String packageManager = packageManager(text);
            if (packageManager != null) {
                return packageManager;
            }
        }
        return null;
    }

    private static @Nullable String packageManager(String text) {
        Object data;
        try {
            data = Js.jsonParse(text);
        } catch (JsError e) {
            return null;
        }
        Object packageManager = Js.truthy(data) ? Js.get(data, "packageManager") : null;
        return packageManager instanceof String ? (String) packageManager : null;
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
     * install.js {@code fetchRequestFromCwd} for an install run at the project's root.
     */
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
     * Whether a manifest makes its directory a workspace root, as config.js {@code findWorkspaceRoot} reads it: a
     * {@code workspaces} array, even an empty one, or an object with one.
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
     * config.js {@code resolveWorkspaces}: the members whose package.json a {@code packages} glob finds, in the
     * order glob lists them, outside any node_modules.
     */
    private Map<String, WorkspaceLayout.Workspace> resolveWorkspaces(Manifest root, YarnClassicOptions config,
                                                                      List<String> warnings) {
        Map<String, WorkspaceLayout.Workspace> workspaces = new LinkedHashMap<>();
        if (!config.isWorkspacesEnabled()) {
            return workspaces;
        }
        Set<String> files = new LinkedHashSet<>();
        for (Object pattern : workspacePatterns(root, config, warnings)) {
            files.addAll(glob((String) pattern));
        }
        for (String file : files) {
            String loc = Objects.requireNonNull(ProjectFiles.location(PosixPath.dirname(file)));
            Manifest manifest = tryManifest(loc, Objects.requireNonNull(inputs.getManifests().get(loc)),
                    config.isLooseSemver(), false, "", warnings);
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

    // resolveWorkspaces up to its glob, which calls replace on every pattern before the first glob starts
    private static List<?> workspacePatterns(Manifest root, YarnClassicOptions config, List<String> warnings) {
        Map<String, @Nullable Object> ws = getWorkspaces(root, true, config, warnings);
        Object patterns = ws != null && Js.truthy(ws.get("packages")) ? ws.get("packages") : new ArrayList<>();
        if (!(patterns instanceof List)) {
            throw new MessageError("The workspaces field in package.json must be an array.");
        }
        for (Object pattern : (List<?>) patterns) {
            if (!(pattern instanceof String)) {
                throw new JsError(JsError.TYPE_ERROR, pattern == null ?
                        "Cannot read properties of null (reading 'replace')" : "pattern.replace is not a function");
            }
        }
        return (List<?>) patterns;
    }

    /**
     * glob 7.1.2 of {@code <pattern>/+(package.json|yarn.json)} over the project's files, without those under the
     * pattern's node_modules, sorted as glob sorts and spelled with the pattern's leading {@code ./}.
     */
    private List<String> glob(String pattern) {
        String base = pattern.replaceFirst("/?\\z", "");
        String prefix = "";
        while (base.startsWith("./")) {
            base = base.substring(2);
            prefix += "./";
        }
        String include = base + "/+(package.json|yarn.json)";
        String ignore = base + "/node_modules/**/+(package.json|yarn.json)";
        Set<String> files = new LinkedHashSet<>();
        for (String loc : inputs.getManifests().keySet()) {
            if (!loc.isEmpty()) {
                files.add(loc + "/package.json");
            }
        }
        if (inputs.getPaths() != null) {
            for (String path : inputs.getPaths()) {
                if (path.endsWith("/package.json") || path.endsWith("/yarn.json")) {
                    files.add(path);
                }
            }
        }
        List<String> found = new ArrayList<>();
        for (String file : files) {
            if (Minimatch.minimatch(file, include) && !Minimatch.minimatch(file, ignore)) {
                String loc = file.substring(0, file.lastIndexOf('/'));
                if (file.endsWith("/yarn.json") || !inputs.getManifests().containsKey(loc)) {
                    throw new CannotKnow("the workspace member " + file + " (the host did not give its contents)");
                }
                found.add(file);
            }
        }
        found.sort(LocaleCompare::en);
        List<String> spelled = new ArrayList<>();
        for (String file : found) {
            spelled.add(prefix + file);
        }
        return spelled;
    }

    /**
     * {@code saveLockfileAndIntegrity}: the lock text yarn writes for the install's top-level patterns, or {@code null}
     * when yarn leaves the file as it is; the lockfile flags are the caller's.
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

        if (!config.getFlags().isForce() &&
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
        return writeFilePreservingEol(rawLock, lockSource, host.getOs());
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
     * util/fs.js {@code writeFilePreservingEol}: the line ending of the file it replaces, the first CR or LF in it,
     * else the host's {@code os.EOL} ({@code os} is {@code process.platform}, null when unknown).
     */
    static String writeFilePreservingEol(@Nullable String existing, String data, @Nullable String os) {
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
            if (os == null) {
                throw new CannotKnow("the line ending yarn writes (os.EOL of the host), as the lock holds none");
            }
            eol = "win32".equals(os) ? "\r\n" : "\n";
        }
        return "\n".equals(eol) ? data : data.replace("\n", eol);
    }
}
