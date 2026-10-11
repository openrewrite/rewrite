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

import lombok.AccessLevel;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.Ini;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.LocaleCompare;
import org.openrewrite.javascript.internal.lock.Minimatch;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.UriComponent;
import org.openrewrite.javascript.internal.lock.yarnclassic.YarnClassicLockRegenerator.Inputs;
import org.openrewrite.javascript.semver.NodeSemver;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;

/**
 * Port of yarn src/config.js (yarn 1.22.22) with the config its registries load (registries/npm-registry.js,
 * yarn-registry.js, base-registry.js): {@code init} sets it, and it is only read afterwards.
 */
@Getter
final class Config {

    static final NodeSemver.Options SEMVER = NodeSemver.Options.V5_5_0;
    static final NodeSemver.Options LOOSE_SEMVER = NodeSemver.Options.V5_5_0_LOOSE;

    static final String YARN_REGISTRY = "https://registry.yarnpkg.com";
    static final String DEFAULT_REGISTRY = "https://registry.npmjs.org/";

    private static final Map<String, Object> DEFAULTS = new LinkedHashMap<>();
    private static final Map<String, String> NPM_MAP = new HashMap<>();
    private static final Pattern ENV_EXPR = Pattern.compile("(\\\\*)\\$\\{([^}]+)}");
    private static final Pattern SCOPED_PKG = Pattern.compile("(?:^|/)(@[^/?]+?)(?=%2f|/)");
    private static final Pattern REGISTRY_PREFIX = Pattern.compile("^(https?:)?//", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNDERSCORE = Pattern.compile("([^_])_");
    private static final Pattern HTTP_PROTOCOL = Pattern.compile("^https?:", Pattern.CASE_INSENSITIVE);
    private static final Pattern REGISTRY_SUFFIX = Pattern.compile("registry/?\\z");
    private static final Pattern PREPENDED = Pattern.compile("^\\.*/|^(?!localhost)\\w+:");
    private static final Pattern PARSED_URL = Pattern.compile(
            "^([A-Za-z][A-Za-z0-9+.-]*:)//(?:[^@/?#]*@)?([^:/?#]*)(?::(\\d*))?(/[^?#]*)?([?#].*)?\\z");
    private static final List<String> FALSY_STRINGS = Arrays.asList("0", "false");

    static {
        DEFAULTS.put("version-tag-prefix", "v");
        DEFAULTS.put("version-git-tag", true);
        DEFAULTS.put("version-commit-hooks", true);
        DEFAULTS.put("version-git-sign", false);
        DEFAULTS.put("version-git-message", "v%s");
        DEFAULTS.put("init-version", "1.0.0");
        DEFAULTS.put("init-license", "MIT");
        DEFAULTS.put("save-prefix", "^");
        DEFAULTS.put("bin-links", true);
        DEFAULTS.put("ignore-scripts", false);
        DEFAULTS.put("ignore-optional", false);
        DEFAULTS.put("registry", YARN_REGISTRY);
        DEFAULTS.put("strict-ssl", true);
        NPM_MAP.put("version-git-sign", "sign-git-tag");
        NPM_MAP.put("version-tag-prefix", "tag-version-prefix");
        NPM_MAP.put("version-git-tag", "git-tag-version");
        NPM_MAP.put("version-commit-hooks", "commit-hooks");
        NPM_MAP.put("version-git-message", "message");
    }

    @Getter(AccessLevel.NONE)
    private final Inputs inputs;

    private final boolean looseSemver;
    private final boolean focus;

    /**
     * {@code workspaceRootFolder} as directories above the project's: 0 the project's own, -1 none.
     */
    private final int workspaceRoot;

    /**
     * The name of the workspace a {@code --focus} install runs for: the manifest's where yarn runs.
     */
    private final @Nullable Object focusedWorkspaceName;

    private final Map<String, @Nullable Object> npmConfig;
    private final Map<String, @Nullable Object> yarnConfig;

    /**
     * The manifest of {@code lockfileFolder}, read once the registries are.
     */
    private final @Nullable Manifest lockfileFolderManifest;

    private final boolean production;
    private final boolean workspacesEnabled;
    private final boolean workspacesNohoistEnabled;
    private final boolean enableLockfileVersions;
    private final boolean linkFileDependencies;
    private final boolean packBuiltPackages;
    private final boolean autoAddIntegrity;
    private final boolean ignorePlatform;

    /**
     * cli/index.js: {@code --prefer-offline} turns {@code offline} on too.
     */
    private final boolean offline;
    private final boolean preferOffline;

    @Getter(AccessLevel.NONE)
    private final PackageConstraintResolver constraintResolver = new PackageConstraintResolver(this);

    /**
     * {@code init} with the command line's {@code flags}: the workspace root from where yarn runs up, the focus check,
     * the registries' config, the manifest of the lock's directory, the workspaces check.
     */
    Config(Inputs inputs, Map<String, @Nullable Object> flags) {
        this.inputs = inputs;
        looseSemver = !Js.truthy(flags.get("strictSemver"));
        focus = Js.truthy(flags.get("focus"));
        ignorePlatform = Js.truthy(flags.get("ignorePlatform"));
        preferOffline = Js.truthy(flags.get("preferOffline"));
        offline = preferOffline || Js.truthy(flags.get("offline"));

        requireWorkspaceRootAbove(inputs, looseSemver);
        workspaceRoot = findWorkspaceRoot(inputs, looseSemver);
        if (focus && (workspaceRoot < 0 || workspaceRoot == 0 && inputs.getCwd().isEmpty())) {
            throw new MessageError("This command can only be run inside an individual workspace.");
        }
        if (workspaceRoot < 0 && !inputs.getCwd().isEmpty()) {
            throw new CannotKnow("the lock yarn writes: run in " + inputs.getCwd() + ", which no workspaces list, it " +
                                 "makes that directory its lockfileFolder and writes its yarn.lock, not this lock");
        }
        focusedWorkspaceName = focus ? readRootManifest().getName() : null;

        Map<String, String> configFiles = inputs.configFilesFromCwd();
        Map<String, String> env = inputs.getEnv();
        npmConfig = loadNpmConfig(configFiles, inputs.getHomeFiles(), env);
        yarnConfig = loadYarnConfig(configFiles, inputs.getHomeFiles(), env);
        Object registry = flags.get("registry");
        if (registry != null) {
            yarnConfig.put("registry", registry);
            npmConfig.put("registry", registry);
        }

        String up = up(Math.max(workspaceRoot, 0));
        String lockfileFolder = up.isEmpty() ? "" : up.substring(0, up.length() - 1);
        String manifest = up.isEmpty() ? inputs.getManifests().get("") :
                inputs.getConfigFiles().get(up + "package.json");
        lockfileFolderManifest = manifest == null ? null :
                tryManifest(lockfileFolder, manifest, looseSemver, false, lockfileFolder);

        workspacesEnabled = !Boolean.FALSE.equals(getOption("workspaces-experimental"));
        workspacesNohoistEnabled = !Boolean.FALSE.equals(getOption("workspaces-nohoist-experimental"));
        enableLockfileVersions = Js.truthy(getOption("yarn-enable-lockfile-versions"));
        linkFileDependencies = Js.truthy(getOption("yarn-link-file-dependencies"));
        packBuiltPackages = Js.truthy(getOption("experimental-pack-script-packages-in-mirror"));
        autoAddIntegrity = !boolifyWithDefault(Js.string(getOption("unsafe-disable-integrity-migration")), true);
        Object productionFlag = flags.get("production");
        // an environment the host does not know counts as one without these variables
        boolean productionEnv = env != null && "production".equals(env.get("NODE_ENV")) &&
                                !"false".equals(env.get("NPM_CONFIG_PRODUCTION")) &&
                                !"false".equals(env.get("YARN_PRODUCTION"));
        production = productionFlag != null ? boolify(productionFlag) :
                Js.truthy(getOption("production")) || productionEnv;
        if (workspaceRoot >= 0 && !workspacesEnabled) {
            throw new MessageError("Your project root defines workspaces but the feature is disabled in your Yarn " +
                                   "config. Please check \"workspaces-experimental\" in your .yarnrc file.");
        }
    }

    /**
     * What an install reads from yarn's cache instead of the registry, which the engine does not see.
     */
    CannotKnow offlineCache() {
        return new CannotKnow((preferOffline ? "--prefer-offline" : "--offline") + " in .yarnrc: what yarn " +
                              "resolves and fetches depends on its cache");
    }

    @Nullable
    String resolveConstraints(List<String> versions, String range) {
        return constraintResolver.reduce(versions, range);
    }

    // readRootManifest: the manifest where yarn runs, which yarn requires
    private Manifest readRootManifest() {
        String cwd = inputs.getCwd();
        String text = inputs.getManifests().get(cwd);
        if (text == null) {
            if (inputs.getPaths() != null && !inputs.getPaths().contains(cwd + "/package.json")) {
                throw new MessageError("Couldn't find a package.json file in \"" + cwd + "\"", "ENOENT");
            }
            throw CannotKnow.unread(cwd + "/package.json");
        }
        return tryManifest(cwd, text, looseSemver, true, "");
    }

    // findWorkspaceRoot starts where yarn runs: a manifest with workspaces below this lock's stops it there
    private static void requireWorkspaceRootAbove(Inputs inputs, boolean looseSemver) {
        Set<String> paths = inputs.getPaths();
        for (String dir = inputs.getCwd(); !dir.isEmpty(); dir = dir.contains("/") ?
                dir.substring(0, dir.lastIndexOf('/')) : "") {
            String file = dir + "/package.json";
            String text = inputs.getManifests().containsKey(dir) ? inputs.getManifests().get(dir) :
                    inputs.getConfigFiles().get(file);
            if (text == null) {
                if (paths != null && paths.contains(file)) {
                    throw CannotKnow.unread(file);
                }
                continue;
            }
            Map<String, @Nullable Object> ws = extractWorkspaces(tryManifest(dir, text, looseSemver, true, null));
            if (ws != null && Js.truthy(ws.get("packages"))) {
                throw new CannotKnow("the lock yarn writes: run in " + inputs.getCwd() + " it stops at the " +
                                     "workspaces of " + file + ", not at this lock");
            }
        }
    }

    /**
     * {@code findWorkspaceRoot}: how many directories above the project's the nearest manifest with a workspaces
     * list is when it is where yarn runs or that list matches the path from it to there, 0 the project's, else -1.
     */
    static int findWorkspaceRoot(Inputs inputs, boolean looseSemver) {
        String cwd = inputs.getCwd();
        for (int depth = 0; depth <= inputs.reach(); depth++) {
            Manifest manifest = findManifest(inputs, depth, looseSemver);
            Map<String, @Nullable Object> ws = manifest == null ? null : extractWorkspaces(manifest);
            if (ws != null && Js.truthy(ws.get("packages"))) {
                String relativePath = depth == 0 ? cwd : cwd.isEmpty() ? inputs.relativePath(depth) :
                        inputs.relativePath(depth) + "/" + cwd;
                return relativePath.isEmpty() ||
                       !Micromatch.micromatch(singletonList(relativePath), ws.get("packages")).isEmpty() ? depth : -1;
            }
        }
        return -1;
    }

    // findManifest: the directory's package.json, else its yarn.json
    private static @Nullable Manifest findManifest(Inputs inputs, int depth, boolean looseSemver) {
        String up = up(depth);
        String text = depth == 0 ? inputs.getManifests().get("") : inputs.getConfigFiles().get(up + "package.json");
        if (text != null) {
            return tryManifest(depth == 0 ? "" : up.substring(0, up.length() - 1), text, looseSemver, true, null);
        }
        Set<String> paths = inputs.getPaths();
        for (String name : depth == 0 ? singletonList("yarn.json") : Arrays.asList("package.json", "yarn.json")) {
            if (paths != null && paths.contains(up + name)) {
                throw CannotKnow.unread(up + name);
            }
        }
        return null;
    }

    /**
     * {@code tryManifest}: the manifest read, its registry and location set, then normalized.
     */
    static Manifest tryManifest(String location, String text, boolean looseSemver, boolean isRoot,
                                @Nullable String lockfileFolder) {
        String file = location.isEmpty() ? "package.json" : location + "/package.json";
        Manifest manifest = manifest(Fs.readJson(file, text), "_registry");
        manifest.put("_registry", NpmResolver.NPM_REGISTRY_ID);
        manifest.put("_loc", file);
        return NormalizeManifest.normalizeManifest(manifest, location, lockfileFolder, isRoot, looseSemver);
    }

    /**
     * What {@link #readManifest} finds in a directory that holds no manifest.
     */
    static final Object NO_MANIFEST = new Object();

    /**
     * {@code readManifest} of a directory a fetch extracted into yarn's cache: its .yarn-metadata.json's manifest as
     * it is, else its package.json or yarn.json normalized, else {@link #NO_MANIFEST}.
     */
    static @Nullable Object readManifest(TarFs dest, String dir, Object registry, boolean looseSemver) {
        TarFs.Node metadata = dest.get(".yarn-metadata.json");
        if (metadata != null) {
            Object json = Fs.readJson(PosixPath.join(dir, ".yarn-metadata.json"), text(metadata));
            if (json == null) {
                throw new JsError(JsError.TYPE_ERROR, "Cannot read properties of null (reading 'manifest')");
            }
            if (json instanceof Map && ((Map<?, ?>) json).containsKey("manifest")) {
                Object manifest = ((Map<?, ?>) json).get("manifest");
                return Js.truthy(manifest) ? manifest : NO_MANIFEST;
            }
        }
        List<String> names = "yarn".equals(registry) ? Arrays.asList("yarn.json", "package.json") :
                Arrays.asList("package.json", "yarn.json");
        for (String filename : names) {
            TarFs.Node file = dest.get(filename);
            if (file == null) {
                continue;
            }
            String loc = PosixPath.join(dir, filename);
            Manifest manifest = manifest(Fs.readJson(loc, text(file)), "_registry");
            manifest.put("_registry", "yarn.json".equals(filename) ? "yarn" : NpmResolver.NPM_REGISTRY_ID);
            manifest.put("_loc", loc);
            return NormalizeManifest.normalizeManifest(manifest, null, "", false, looseSemver);
        }
        return NO_MANIFEST;
    }

    // readFile: utf8 with CRLF as LF
    private static String text(TarFs.Node node) {
        if (node.directory) {
            throw new JsError(JsError.ERROR, "EISDIR: illegal operation on a directory, read");
        }
        return new String(node.content(), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    // an object, or an array with its indices; a property set on any other value is a TypeError
    @SuppressWarnings("unchecked")
    static Manifest manifest(@Nullable Object json, String property) {
        if (json instanceof Map) {
            return new Manifest((Map<String, @Nullable Object>) json);
        }
        if (json instanceof List) {
            Map<String, @Nullable Object> object = new LinkedHashMap<>();
            Js.assign(object, json);
            return new Manifest(object);
        }
        if (json == null) {
            throw new JsError(JsError.TYPE_ERROR, "Cannot set properties of null (setting '" + property + "')");
        }
        throw new JsError(JsError.TYPE_ERROR, "Cannot create property '" + property + "' on " + Js.typeOf(json) +
                                              " '" + Js.string(json) + "'");
    }

    /**
     * Whether a manifest makes its directory a workspace root, as {@code findWorkspaceRoot} reads it: a
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
     * {@code getWorkspaces}: the root's workspaces config once it is eligible.
     * @throws MessageError members or a nohoist list in a root that is not private, when {@code shouldThrow}.
     */
    @Nullable
    Map<String, @Nullable Object> getWorkspaces(Manifest manifest, boolean shouldThrow) {
        if (!workspacesEnabled) {
            return null;
        }
        Map<String, @Nullable Object> ws = extractWorkspaces(manifest);
        if (ws == null) {
            return null;
        }
        Map<String, @Nullable Object> wsCopy = new LinkedHashMap<>(ws);
        List<String> errors = new ArrayList<>();
        boolean isPrivate = Js.truthy(manifest.get("private"));
        Object packages = wsCopy.get("packages");
        if (Js.truthy(packages) && Js.length(packages) > 0 && !isPrivate) {
            errors.add("Workspaces can only be enabled in private projects.");
            wsCopy = null;
        }
        if (wsCopy != null && Js.truthy(wsCopy.get("nohoist")) && Js.length(wsCopy.get("nohoist")) > 0) {
            if (!workspacesNohoistEnabled) {
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
        return wsCopy;
    }

    /**
     * {@code resolveWorkspaces}: the members whose package.json a {@code packages} glob finds, in the order glob lists
     * them, outside any node_modules.
     */
    Map<String, WorkspaceLayout.Workspace> resolveWorkspaces(Manifest root) {
        Map<String, WorkspaceLayout.Workspace> workspaces = new LinkedHashMap<>();
        if (!workspacesEnabled) {
            return workspaces;
        }
        Set<String> files = new LinkedHashSet<>();
        for (Object pattern : workspacePatterns(root)) {
            files.addAll(glob((String) pattern));
        }
        for (String file : files) {
            String loc = Objects.requireNonNull(ProjectFiles.location(PosixPath.dirname(file)));
            Manifest manifest = tryManifest(loc, Objects.requireNonNull(inputs.getManifests().get(loc)),
                    looseSemver, false, "");
            if (!Js.truthy(manifest.getName()) || !Js.truthy(manifest.getVersion())) {
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
     * {@code resolveWorkspaces} up to its glob, which calls replace on every pattern before the first glob starts.
     */
    List<?> workspacePatterns(Manifest root) {
        Map<String, @Nullable Object> ws = getWorkspaces(root, true);
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
            if (Minimatch.minimatch(file, include, Minimatch.Release.V3_0_4) &&
                !Minimatch.minimatch(file, ignore, Minimatch.Release.V3_0_4)) {
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
     * npm-registry.js {@code getPossibleConfigLocations}: the project's {@code .<filename>}, the home directory's
     * ({@code ~/}), then the project's again and each directory's above it, nearest first.
     */
    static List<String> getPossibleConfigLocations(String filename, Map<String, String> configFiles) {
        List<String> locations = new ArrayList<>();
        locations.add("." + filename);
        locations.add(Rc.HOME + "." + filename);
        for (int depth = 0; depth <= depth(configFiles.keySet()); depth++) {
            locations.add(up(depth) + "." + filename);
        }
        return locations;
    }

    /**
     * How many directories above the project's the deepest of these paths ({@code ../../.npmrc}) is.
     */
    static int depth(Collection<String> paths) {
        int depth = 0;
        for (String path : paths) {
            int d = 0;
            while (path.startsWith("../", 3 * d)) {
                d++;
            }
            depth = Math.max(depth, d);
        }
        return depth;
    }

    static String up(int depth) {
        StringBuilder up = new StringBuilder();
        for (int i = 0; i < depth; i++) {
            up.append("../");
        }
        return up.toString();
    }

    // base-registry.js init then npm-registry.js loadConfig: the yarn_ and npm_config_ keys over every .npmrc
    private static Map<String, @Nullable Object> loadNpmConfig(Map<String, String> configFiles,
                                                               Map<String, String> home,
                                                               @Nullable Map<String, String> env) {
        Map<String, @Nullable Object> npmConfig = new LinkedHashMap<>();
        mergeEnv(npmConfig, "yarn_", env);
        mergeEnv(npmConfig, "npm_config_", env);
        for (String loc : getPossibleConfigLocations("npmrc", configFiles)) {
            String file = loc.startsWith(Rc.HOME) ? home.get(loc.substring(Rc.HOME.length())) : configFiles.get(loc);
            if (file == null) {
                continue;
            }
            Map<String, @Nullable Object> config = new LinkedHashMap<>();
            String text = file.replace("\r\n", "\n");
            for (Map.Entry<String, @Nullable Object> entry : Ini.decode(text, Ini.Release.V1_3_5).entrySet()) {
                config.put(entry.getKey(), envReplace(normalizeConfigOption(entry.getValue()), env));
            }
            config.putAll(npmConfig);
            npmConfig = config;
        }
        return npmConfig;
    }

    // base-registry.js init then yarn-registry.js loadConfig: the yarn_ keys over every .yarnrc over yarn's defaults
    private static Map<String, @Nullable Object> loadYarnConfig(Map<String, String> configFiles,
                                                                Map<String, String> home,
                                                                @Nullable Map<String, String> env) {
        Map<String, @Nullable Object> yarnConfig = new LinkedHashMap<>();
        mergeEnv(yarnConfig, "yarn_", env);
        for (String loc : getPossibleConfigLocations("yarnrc", configFiles)) {
            String file = loc.startsWith(Rc.HOME) ? home.get(loc.substring(Rc.HOME.length())) : configFiles.get(loc);
            if (file == null) {
                continue;
            }
            Map<String, @Nullable Object> config = new LinkedHashMap<>();
            Js.assign(config, LockfileParser.parseLockfile(file.replace("\r\n", "\n"), ".yarnrc").getObject());
            config.putAll(yarnConfig);
            yarnConfig = config;
        }
        Map<String, @Nullable Object> withDefaults = new LinkedHashMap<>(DEFAULTS);
        withDefaults.putAll(yarnConfig);
        return withDefaults;
    }

    // base-registry.js mergeEnv
    private static void mergeEnv(Map<String, @Nullable Object> config, String prefix,
                                 @Nullable Map<String, String> env) {
        if (env == null) {
            return;
        }
        for (Map.Entry<String, String> envEntry : env.entrySet()) {
            String key = envEntry.getKey().toLowerCase(Locale.ROOT);
            if (!key.startsWith(prefix)) {
                continue;
            }
            Object val = normalizeConfigOption(envEntry.getValue());
            key = key.substring(prefix.length()).replace("__", ".");
            key = UNDERSCORE.matcher(key).replaceAll("$1-");
            ObjectPath.set(config, key, val);
        }
    }

    /**
     * yarn-registry.js {@code getOption}: the yarn config's value, else the npm config's under npm's name for it,
     * else under the same name.
     */
    @Nullable
    Object getOption(String key) {
        if (yarnConfig.containsKey(key)) {
            return yarnConfig.get(key);
        }
        // npmMap[key] of a key npm has no name for is undefined, which reads the npm config's key "undefined"
        String npmKey = NPM_MAP.containsKey(key) ? NPM_MAP.get(key) : "undefined";
        if (npmConfig.containsKey(npmKey)) {
            return npmConfig.get(npmKey);
        }
        if (npmConfig.containsKey(key)) {
            return npmConfig.get(key);
        }
        return DEFAULTS.get(key);
    }

    /**
     * {@code getOfflineMirrorPath} without a package file: npm's registry config, then yarn's.
     */
    @Nullable
    Object getOfflineMirrorPath() {
        Object mirrorPath = null;
        for (Map<String, @Nullable Object> registry : Arrays.asList(npmConfig, yarnConfig)) {
            Object registryMirrorPath = registry.get("yarn-offline-mirror");
            if (Boolean.FALSE.equals(registryMirrorPath)) {
                return null;
            }
            if (registryMirrorPath != null) {
                mirrorPath = registryMirrorPath;
            }
        }
        return mirrorPath;
    }

    /**
     * npm-registry.js {@code getRegistry}: the registry a package's metadata is requested from.
     */
    String getRegistry(String packageIdent) {
        if (REGISTRY_PREFIX.matcher(packageIdent).find()) {
            for (Object registry : getAvailableRegistries()) {
                if (packageIdent.startsWith(Js.string(registry))) {
                    // find's first match, which an empty registry is as well, and only a truthy one is taken
                    if (Js.truthy(registry)) {
                        return Js.string(registry);
                    }
                    break;
                }
            }
        }
        String scope = getScope(packageIdent);
        for (String s : scope.isEmpty() ? Collections.singletonList("") : Arrays.asList(scope, "")) {
            String key = s + (s.isEmpty() ? "" : ":") + "registry";
            Object registry = npmConfig.get(key);
            if (!Js.truthy(registry)) {
                registry = getOption(key);
            }
            if (Js.truthy(registry)) {
                return Js.string(registry);
            }
        }
        return DEFAULT_REGISTRY;
    }

    /**
     * npm-registry.js {@code request}'s Authorization header for {@code packageIdent} at {@code requestUrl}: the
     * credentials of the package's registry, sent to it under always-auth or for a scoped package; null for none.
     */
    @Nullable
    String getAuthorization(String packageIdent, String requestUrl) {
        String registry = getRegistry(packageIdent);
        Object alwaysAuth = getRegistryOrGlobalOption(registry, "always-auth");
        // urlParts is pure in yarn, so the url's parts are asked only where they decide
        if (!Js.truthy(alwaysAuth) && getScope(packageIdent).isEmpty() ||
            !isRequestToRegistry(requestUrl, registry) && !requestNeedsAuth(requestUrl)) {
            return null;
        }
        String auth = getAuth(packageIdent);
        return auth.isEmpty() ? null : auth;
    }

    private boolean isRequestToRegistry(String requestUrl, String registryUrl) {
        String[] request = urlParts(requestUrl);
        String[] registry = urlParts(registryUrl);
        Object customHostSuffix = getRegistryOrGlobalOption(registryUrl, "custom-host-suffix");
        boolean requestToRegistryHost = request[0].equals(registry[0]);
        boolean requestToYarn = YARN_REGISTRY.contains(request[0]) && DEFAULT_REGISTRY.contains(registry[0]);
        boolean requestToRegistryPath = request[1].startsWith(registry[1]);
        boolean customHostSuffixInUse = customHostSuffix instanceof String &&
                                        request[0].endsWith((String) customHostSuffix);
        return (requestToRegistryHost || requestToYarn) && (requestToRegistryPath || customHostSuffixInUse);
    }

    private boolean requestNeedsAuth(String requestUrl) {
        String[] request = urlParts(requestUrl);
        for (String option : Js.keys(npmConfig)) {
            String[] parts = option.split(":", -1);
            if (parts.length > 1 && (parts.length == 2 && "_authToken".equals(parts[1]) ||
                                     "_password".equals(parts[1]))) {
                String[] registry = urlParts(parts[0]);
                if (request[0].equals(registry[0]) && request[1].startsWith(registry[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getAuth(String packageIdent) {
        String baseRegistry = getRegistry(packageIdent);
        List<String> registries = YARN_REGISTRY.equals(baseRegistry) ? Arrays.asList(baseRegistry, DEFAULT_REGISTRY) :
                singletonList(baseRegistry);
        for (String registry : registries) {
            String auth = getAuthByRegistry(registry);
            if (!auth.isEmpty()) {
                return auth;
            }
        }
        return "";
    }

    private String getAuthByRegistry(String registry) {
        Object authToken = getRegistryOrGlobalOption(registry, "_authToken");
        if (Js.truthy(authToken)) {
            return "Bearer " + Js.string(authToken);
        }
        Object auth = getRegistryOrGlobalOption(registry, "_auth");
        if (Js.truthy(auth)) {
            return "Basic " + Js.string(auth);
        }
        Object username = getRegistryOrGlobalOption(registry, "username");
        Object password = getRegistryOrGlobalOption(registry, "_password");
        if (Js.truthy(username) && Js.truthy(password)) {
            String pw = new String(Base64.getMimeDecoder().decode(Js.string(password)), StandardCharsets.UTF_8);
            return "Basic " + Base64.getEncoder().encodeToString((Js.string(username) + ":" + pw)
                    .getBytes(StandardCharsets.UTF_8));
        }
        return "";
    }

    private @Nullable Object getRegistryOption(String registry, String option) {
        String reg = registry.endsWith("/") ? registry : registry + "/";
        Object value = npmConfig.get(reg + ":" + option);
        if (!Js.truthy(value) && HTTP_PROTOCOL.matcher(reg).find()) {
            value = getRegistryOption(HTTP_PROTOCOL.matcher(reg).replaceFirst(""), option);
        }
        if (!Js.truthy(value) && REGISTRY_SUFFIX.matcher(reg).find()) {
            value = getRegistryOption(REGISTRY_SUFFIX.matcher(reg).replaceFirst(""), option);
        }
        return value;
    }

    private @Nullable Object getRegistryOrGlobalOption(String registry, String option) {
        Object value = getRegistryOption(registry, option);
        return Js.truthy(value) ? value : npmConfig.get(option);
    }

    /**
     * npm-registry.js {@code urlParts}: the host and path normalize-url 2.0.1 and url.parse give a url, for the urls a
     * registry and its packages have; any other is one the engine cannot know the parts of.
     */
    private static String[] urlParts(String requestUrl) {
        String str = requestUrl.trim();
        if (!PREPENDED.matcher(str).find()) {
            str = str.replaceFirst("^(?!(?:\\w+:)?//)", "http://");
        }
        str = str.replaceFirst("^//", "http://");
        Matcher url = PARSED_URL.matcher(str);
        String path = url.matches() && url.group(5) == null ? decodeUri(url.group(4) == null ? "" : url.group(4)) :
                null;
        if (path == null || path.contains("/./") || path.contains("/../") || path.matches(".*/\\.\\.?\\z") ||
            !url.group(2).matches("[A-Za-z0-9.-]*")) {
            throw new CannotKnow("the parts normalize-url gives " + requestUrl + " (yarn's credentials check)");
        }
        String protocol = url.group(1).toLowerCase(Locale.ROOT);
        String hostname = url.group(2).toLowerCase(Locale.ROOT).replaceFirst("\\.\\z", "").replaceFirst("^www\\.", "");
        String port = url.group(3);
        if (port != null && ("http:".equals(protocol) && "80".equals(port) || "https:".equals(protocol) &&
                                                                             "443".equals(port))) {
            port = null;
        }
        path = path.replaceAll("/{2,}", "/");
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return new String[]{port == null ? hostname : hostname + ":" + port, path.isEmpty() ? "/" : path};
    }

    // decodeURI; null where it throws a URIError
    private static @Nullable String decodeUri(String path) {
        try {
            return UriComponent.decodeURI(path);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // base-registry.js getAvailableRegistries over npm's config
    private List<@Nullable Object> getAvailableRegistries() {
        List<@Nullable Object> registries = new ArrayList<>();
        for (String option : Js.keys(npmConfig)) {
            String[] parts = option.split(":", -1);
            if ("registry".equals(option) || (parts.length > 1 && "registry".equals(parts[1]))) {
                registries.add(npmConfig.get(option));
            }
        }
        return registries;
    }

    static String getScope(String packageIdent) {
        Matcher match = SCOPED_PKG.matcher(packageIdent);
        return match.find() ? match.group(1) : "";
    }

    private static @Nullable Object normalizeConfigOption(@Nullable Object val) {
        if ("true".equals(val)) {
            return true;
        }
        if ("false".equals(val)) {
            return false;
        }
        return val;
    }

    // util/env-replace.js
    static @Nullable Object envReplace(@Nullable Object value, @Nullable Map<String, String> env) {
        if (!(value instanceof String) || ((String) value).isEmpty()) {
            return value;
        }
        Matcher m = ENV_EXPR.matcher((String) value);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String esc = m.group(1);
            String replacement;
            if (!esc.isEmpty() && esc.length() % 2 != 0) {
                replacement = m.group();
            } else {
                replacement = HostEnvironment.of(env).reference(m.group(2));
                if (replacement == null) {
                    throw new JsError(JsError.ERROR, "Failed to replace env in config: " + m.group());
                }
            }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    // util/conversion.js
    static boolean boolify(@Nullable Object val) {
        return !FALSY_STRINGS.contains(Js.string(val).toLowerCase(Locale.ROOT));
    }

    static boolean boolifyWithDefault(@Nullable Object val, boolean defaultResult) {
        return "".equals(val) || val == null ? defaultResult : boolify(val);
    }
}
