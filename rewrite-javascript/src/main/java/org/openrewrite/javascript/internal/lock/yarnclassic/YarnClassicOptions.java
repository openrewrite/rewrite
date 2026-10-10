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
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of yarn src/config.js, rc.js, registries/ and install.js {@code normalizeFlags} (yarn 1.22.22): the config.
 */
@Getter
public final class YarnClassicOptions {

    static final NodeSemver.Options SEMVER = NodeSemver.Options.V5_5_0;
    static final NodeSemver.Options LOOSE_SEMVER = NodeSemver.Options.V5_5_0_LOOSE;

    static final String YARN_REGISTRY = "https://registry.yarnpkg.com";
    static final String DEFAULT_REGISTRY = "https://registry.npmjs.org/";

    private static final Map<String, Object> DEFAULTS = new LinkedHashMap<>();
    private static final Map<String, String> NPM_MAP = new HashMap<>();
    private static final Pattern RC_ARG = Pattern.compile("^--(?:([^.]+)\\.)?(" + Js.DOT + "*)\\z");
    private static final Pattern ENV_EXPR = Pattern.compile("(\\\\*)\\$\\{([^}]+)}");
    private static final Pattern SCOPED_PKG = Pattern.compile("(?:^|/)(@[^/?]+?)(?=%2f|/)");
    private static final Pattern REGISTRY_PREFIX = Pattern.compile("^(https?:)?//", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNDERSCORE = Pattern.compile("([^_])_");
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

    /**
     * The flags of {@code yarn install} that the .yarnrc's {@code --flag} lines set or the config turns on
     * (install.js {@code normalizeFlags}).
     */
    @Getter
    public static final class Flags {
        boolean ignorePlatform;
        boolean ignoreEngines;
        boolean ignoreScripts;
        boolean ignoreOptional;
        boolean force;
        boolean flat;
        boolean lockfile = true;
        boolean pureLockfile;
        boolean frozenLockfile;
        boolean updateChecksums;
        boolean skipIntegrityCheck;
    }

    private final Map<String, @Nullable Object> yarnConfig;
    private final Map<String, @Nullable Object> npmConfig;
    private final Flags flags;

    private final boolean looseSemver;
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

    private YarnClassicOptions(Map<String, @Nullable Object> yarnConfig, Map<String, @Nullable Object> npmConfig,
                               Map<String, @Nullable Object> commandLine, @Nullable Map<String, String> env) {
        this.yarnConfig = yarnConfig;
        this.npmConfig = npmConfig;
        this.looseSemver = !Js.truthy(commandLine.get("strictSemver"));
        Object production = commandLine.get("production");
        // an environment the host does not know counts as one without these variables
        boolean productionEnv = env != null && "production".equals(env.get("NODE_ENV")) &&
                                !"false".equals(env.get("NPM_CONFIG_PRODUCTION")) &&
                                !"false".equals(env.get("YARN_PRODUCTION"));
        this.production = production != null ? boolify(production) :
                Js.truthy(getOption("production")) || productionEnv;
        this.workspacesEnabled = !Boolean.FALSE.equals(getOption("workspaces-experimental"));
        this.workspacesNohoistEnabled = !Boolean.FALSE.equals(getOption("workspaces-nohoist-experimental"));
        this.enableLockfileVersions = Js.truthy(getOption("yarn-enable-lockfile-versions"));
        this.linkFileDependencies = Js.truthy(getOption("yarn-link-file-dependencies"));
        this.packBuiltPackages = Js.truthy(getOption("experimental-pack-script-packages-in-mirror"));
        this.autoAddIntegrity = !boolifyWithDefault(Js.string(getOption("unsafe-disable-integrity-migration")), true);
        this.ignorePlatform = Js.truthy(commandLine.get("ignorePlatform"));
        this.preferOffline = Js.truthy(commandLine.get("preferOffline"));
        this.offline = preferOffline || Js.truthy(commandLine.get("offline"));
        this.flags = normalizeFlags(commandLine);
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

    public static YarnClassicOptions defaults() {
        return load(Collections.emptyMap(), Collections.emptyMap());
    }

    /**
     * @param configFiles the config files of the project's directory and those above it, by path relative to it.
     * @param env         the environment yarn runs in, {@code null} when the host does not know it.
     */
    public static YarnClassicOptions load(Map<String, String> configFiles, @Nullable Map<String, String> env) {
        return load(rc(configFiles), configFiles, env);
    }

    /**
     * rc.js {@code getRcConfigForCwd}: each directory's .yarnrc then .yarnrc.yml from the topmost down, read before
     * anything else yarn reads.
     */
    static Map<String, @Nullable Object> rc(Map<String, String> configFiles) {
        Map<String, @Nullable Object> rc = new LinkedHashMap<>();
        for (int depth = depth(configFiles.keySet()); depth >= 0; depth--) {
            for (String name : Arrays.asList(".yarnrc", ".yarnrc.yml")) {
                String text = configFiles.get(up(depth) + name);
                if (text != null) {
                    Js.assign(rc, loadRcFile(text, up(depth) + name));
                }
            }
        }
        return rc;
    }

    /**
     * rc.js {@code loadRcFile}, its directory keys resolved against the file's directory as paths relative to the
     * project's root.
     */
    @SuppressWarnings("unchecked")
    static @Nullable Object loadRcFile(String text, String filePath) {
        Object values = LockfileParser.parseLockfile(text.replace("\r\n", "\n"), PosixPath.basename(filePath))
                .getObject();
        Object yarnPath = Js.get(values, "yarnPath");
        if (filePath.endsWith(".yml") && Js.truthy(values) && yarnPath instanceof String) {
            Map<String, @Nullable Object> path = new LinkedHashMap<>();
            path.put("yarn-path", yarnPath);
            values = path;
        }
        if (values instanceof Map) {
            Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) values;
            for (String key : Js.keys(map)) {
                if (PATH_KEYS.contains(PATH_KEY_PREFIX.matcher(key).replaceFirst(""))) {
                    map.put(key, resolve(PosixPath.dirname(filePath), map.get(key)));
                }
            }
        }
        return values;
    }

    private static final List<String> PATH_KEYS = Arrays.asList("yarn-path", "cache-folder", "global-folder",
            "modules-folder", "cwd", "offline-cache-folder");
    private static final Pattern PATH_KEY_PREFIX = Pattern.compile("^(--)?([^.]+\\.)*");

    // path.resolve(dirname(filePath), value) relative to the project's root
    private static String resolve(String dir, @Nullable Object value) {
        if (!(value instanceof String)) {
            throw Js.invalidArgType("The \"paths[1]\" argument must be of type string", value);
        }
        String path = (String) value;
        String resolved = PosixPath.isAbsolute(path) ? PosixPath.normalize(path) : PosixPath.join(dir, path);
        return resolved.length() > 1 && resolved.endsWith("/") ? resolved.substring(0, resolved.length() - 1) :
                resolved;
    }

    /**
     * npm-registry.js {@code getPossibleConfigLocations} without the host's files (home, global prefix): the
     * project's {@code .<filename>}, then the project's again and each directory's above it, nearest first.
     */
    static List<String> getPossibleConfigLocations(String filename, Map<String, String> configFiles) {
        List<String> locations = new ArrayList<>();
        locations.add("." + filename);
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

    /**
     * The registries' {@code init}: npm's config from the environment's {@code yarn_} and {@code npm_config_}
     * keys over every .npmrc, yarn's from its {@code yarn_} keys over every .yarnrc over yarn's defaults.
     */
    static YarnClassicOptions load(Map<String, @Nullable Object> rc, Map<String, String> configFiles,
                                   @Nullable Map<String, String> env) {
        Map<String, @Nullable Object> npmConfig = new LinkedHashMap<>();
        mergeEnv(npmConfig, "yarn_", env);
        mergeEnv(npmConfig, "npm_config_", env);
        for (String loc : getPossibleConfigLocations("npmrc", configFiles)) {
            String file = configFiles.get(loc);
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

        Map<String, @Nullable Object> yarnConfig = new LinkedHashMap<>();
        mergeEnv(yarnConfig, "yarn_", env);
        for (String loc : getPossibleConfigLocations("yarnrc", configFiles)) {
            String file = configFiles.get(loc);
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
        yarnConfig = withDefaults;

        Map<String, @Nullable Object> commandLine = commandLine(rc);
        Object registry = commandLine.get("registry");
        if (registry != null) {
            yarnConfig.put("registry", registry);
            npmConfig.put("registry", registry);
        }
        return new YarnClassicOptions(yarnConfig, npmConfig, commandLine, env);
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

    // config.js _init, before the registries are read
    static boolean looseSemver(Map<String, @Nullable Object> rc) {
        return !Js.truthy(commandLine(rc).get("strictSemver"));
    }

    // the --focus flag config.js init reads
    static boolean focus(Map<String, @Nullable Object> rc) {
        return Js.truthy(commandLine(rc).get("focus"));
    }

    // rc.js buildRcArgs: a `--<option> <value>` line passes the value; a flag passes itself when the value is true
    private static Map<String, @Nullable Object> commandLine(Map<String, @Nullable Object> rc) {
        Map<String, @Nullable Object> args = new LinkedHashMap<>();
        for (String command : Arrays.asList("*", "install")) {
            for (String key : Js.keys(rc)) {
                Matcher keyMatch = RC_ARG.matcher(key);
                if (!keyMatch.find()) {
                    continue;
                }
                String commandName = keyMatch.group(1) == null ? "*" : keyMatch.group(1);
                if (!commandName.equals(command)) {
                    continue;
                }
                String arg = keyMatch.group(2);
                Object value = rc.get(key);
                switch (arg) {
                    case "prod":
                    case "production":
                        args.put("production", boolify(value));
                        break;
                    case "registry":
                    case "modules-folder":
                    case "cache-folder":
                        args.put(camelCase(arg), value);
                        break;
                    case "no-lockfile":
                        if (Boolean.TRUE.equals(value)) {
                            args.put("lockfile", false);
                        }
                        break;
                    case "cwd":
                        throw new CannotKnow("the project yarn installs in the directory the .yarnrc's --cwd " +
                                             Js.string(value) + " names");
                    default:
                        if (Boolean.TRUE.equals(value)) {
                            args.put(camelCase(arg), true);
                        }
                }
            }
        }
        return args;
    }

    private static String camelCase(String flag) {
        StringBuilder out = new StringBuilder();
        boolean upper = false;
        for (char c : flag.toCharArray()) {
            if (c == '-') {
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return out.toString();
    }

    private Flags normalizeFlags(Map<String, @Nullable Object> rawFlags) {
        Flags flags = new Flags();
        flags.ignorePlatform = Js.truthy(rawFlags.get("ignorePlatform"));
        flags.ignoreEngines = Js.truthy(rawFlags.get("ignoreEngines"));
        flags.ignoreScripts = Js.truthy(rawFlags.get("ignoreScripts"));
        flags.ignoreOptional = Js.truthy(rawFlags.get("ignoreOptional"));
        flags.force = Js.truthy(rawFlags.get("force"));
        flags.flat = Js.truthy(rawFlags.get("flat"));
        flags.lockfile = !Boolean.FALSE.equals(rawFlags.get("lockfile"));
        flags.pureLockfile = Js.truthy(rawFlags.get("pureLockfile"));
        flags.updateChecksums = Js.truthy(rawFlags.get("updateChecksums"));
        flags.skipIntegrityCheck = Js.truthy(rawFlags.get("skipIntegrityCheck"));
        flags.frozenLockfile = Js.truthy(rawFlags.get("frozenLockfile"));
        if (Js.truthy(getOption("ignore-scripts"))) {
            flags.ignoreScripts = true;
        }
        if (Js.truthy(getOption("ignore-platform"))) {
            flags.ignorePlatform = true;
        }
        if (Js.truthy(getOption("ignore-engines"))) {
            flags.ignoreEngines = true;
        }
        if (Js.truthy(getOption("ignore-optional"))) {
            flags.ignoreOptional = true;
        }
        if (Js.truthy(getOption("force"))) {
            flags.force = true;
        }
        return flags;
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
     * config.js {@code getOfflineMirrorPath} without a package file: npm's registry config, then yarn's.
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
