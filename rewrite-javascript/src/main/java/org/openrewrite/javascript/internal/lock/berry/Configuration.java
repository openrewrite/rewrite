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
package org.openrewrite.javascript.internal.lock.berry;

import lombok.AccessLevel;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.Host;
import org.openrewrite.javascript.internal.lock.HostEnvironment;
import org.openrewrite.javascript.internal.lock.JsError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openrewrite.javascript.internal.lock.YarnpkgSemverUtils;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * Port of @yarnpkg/core sources/Configuration.ts (Yarn 4.10.3): {@code find} for the settings a lock reads, and what
 * follows loading, with its plugins' hooks, once {@link #restrictTo} names the release.
 */
@Getter
public final class Configuration {

    public static final String DEFAULT_RC_FILENAME = ".yarnrc.yml";
    public static final String HOME = "~/";
    private static final String ENVIRONMENT_PREFIX = "yarn_";
    private static final Pattern YARN_RELEASE = Pattern.compile("(?:^|/)yarn-(\\d+\\.\\d+\\.\\d+[^/]*?)\\.c?js$");
    private static final Pattern LEADING_SEPARATORS = Pattern.compile("^[_.\\- ]+");
    private static final Pattern SEPARATED_WORD = Pattern.compile("[_.\\- ]+(\\w|\\z)");
    private static final Pattern DIGITS_WORD = Pattern.compile("\\d+(\\w|\\z)");

    private static final Set<String> IGNORED_ENV_VARIABLES = new LinkedHashSet<>(Arrays.asList("isTestEnv",
            "injectNpmUser", "injectNpmPassword", "injectNpm2FaToken", "zipDataEpilogue", "cacheCheckpointOverride",
            "cacheVersionOverride", "lockfileVersionOverride", "osOverride", "cpuOverride", "libcOverride",
            "binFolder", "version", "flags", "profile", "gpg", "ignoreNode", "wrapOutput", "home", "confDir",
            "registry", "ignoreCwd"));

    private static final List<String> PRIMARY_FIELDS = Arrays.asList("yarnPath", "ignorePath",
            "injectEnvironmentFiles");

    // the keys of Yarn 4.10.3's coreDefinitions; every other setting is a plugin's or unknown
    private static final Set<String> CORE_FIELDS = new LinkedHashSet<>(Arrays.asList("lastUpdateCheck", "yarnPath",
            "ignorePath", "globalFolder", "cacheFolder", "compressionLevel", "virtualFolder", "installStatePath",
            "immutablePatterns", "rcFilename", "enableGlobalCache", "cacheMigrationMode", "enableColors",
            "enableHyperlinks", "enableInlineBuilds", "enableMessageNames", "enableProgressBars", "enableTimers",
            "enableTips", "preferInteractive", "preferTruncatedLines", "progressBarStyle", "defaultLanguageName",
            "defaultProtocol", "enableTransparentWorkspaces", "supportedArchitectures", "enableMirror",
            "enableNetwork", "enableOfflineMode", "httpProxy", "httpsProxy", "unsafeHttpWhitelist", "httpTimeout",
            "httpRetry", "networkConcurrency", "taskPoolConcurrency", "taskPoolMode", "networkSettings",
            "httpsCaFilePath", "httpsKeyFilePath", "httpsCertFilePath", "enableStrictSsl", "logFilters",
            "enableTelemetry", "telemetryInterval", "telemetryUserId", "enableHardenedMode", "enableScripts",
            "enableStrictSettings", "enableImmutableCache", "enableCacheClean", "checksumBehavior",
            "injectEnvironmentFiles", "packageExtensions"));

    private static final Set<String> LEGACY_PLUGINS = new LinkedHashSet<>(Arrays.asList(
            "@yarnpkg/plugin-constraints", "@yarnpkg/plugin-exec", "@yarnpkg/plugin-interactive-tools",
            "@yarnpkg/plugin-stage", "@yarnpkg/plugin-typescript", "@yarnpkg/plugin-version",
            "@yarnpkg/plugin-workspace-tools"));

    private enum Type {
        ANY, BOOLEAN, ABSOLUTE_PATH, NUMBER, STRING, DURATION, SECRET, SHAPE, MAP
    }

    // the minutes an npmMinimalAgeGate duration is read in (DurationUnit.MINUTES)
    private static final String MINUTES = "m";

    /**
     * A setting's definition, as {@code SettingsDefinition} declares it.
     */
    private static final class Definition {
        final Type type;

        /**
         * {@link Syml#UNDEFINED} where the definition has none, which unlike a null default rejects a null value.
         */
        final @Nullable Object defaultValue;

        final @Nullable List<Object> values;
        final boolean isArray;
        final boolean isNullable;
        final Map<String, Definition> properties;
        final @Nullable Definition valueDefinition;
        final @Nullable Function<String, String> normalizeKeys;

        /**
         * The unit of a duration ({@code m}), whose default is a duration too.
         */
        final @Nullable String unit;

        Definition(Type type, @Nullable Object defaultValue, @Nullable List<Object> values, boolean isArray,
                   boolean isNullable, Map<String, Definition> properties, @Nullable Definition valueDefinition,
                   @Nullable Function<String, String> normalizeKeys, @Nullable String unit) {
            this.type = type;
            this.defaultValue = defaultValue;
            this.values = values;
            this.isArray = isArray;
            this.isNullable = isNullable;
            this.properties = properties;
            this.valueDefinition = valueDefinition;
            this.normalizeKeys = normalizeKeys;
            this.unit = unit;
        }

        Definition(Type type, @Nullable Object defaultValue, @Nullable List<Object> values, boolean isArray,
                   boolean isNullable, Map<String, Definition> properties, @Nullable Definition valueDefinition,
                   @Nullable Function<String, String> normalizeKeys) {
            this(type, defaultValue, values, isArray, isNullable, properties, valueDefinition, normalizeKeys, null);
        }

        static Definition duration(String defaultValue, String unit) {
            return new Definition(Type.DURATION, defaultValue, null, false, false, Collections.emptyMap(), null, null,
                    unit);
        }

        Definition(Type type, @Nullable Object defaultValue, @Nullable List<Object> values, boolean isArray,
                   boolean isNullable, Map<String, Definition> properties, @Nullable Definition valueDefinition) {
            this(type, defaultValue, values, isArray, isNullable, properties, valueDefinition, null);
        }

        static Definition of(Type type, @Nullable Object defaultValue) {
            return new Definition(type, defaultValue, null, false, false, Collections.emptyMap(), null);
        }

        static Definition withoutDefault(Type type) {
            return of(type, Syml.UNDEFINED);
        }

        static Definition map(Definition valueDefinition) {
            return map(valueDefinition, null);
        }

        static Definition map(Definition valueDefinition, @Nullable Function<String, String> normalizeKeys) {
            return new Definition(Type.MAP, null, null, false, false, Collections.emptyMap(), valueDefinition,
                    normalizeKeys);
        }

        static Definition shape(Map<String, Definition> properties) {
            return new Definition(Type.SHAPE, null, null, false, false, properties, null);
        }
    }

    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();

    static {
        DEFINITIONS.put("yarnPath", Definition.of(Type.ABSOLUTE_PATH, null));
        DEFINITIONS.put("ignorePath", Definition.of(Type.BOOLEAN, false));
        DEFINITIONS.put("enableStrictSettings", Definition.of(Type.BOOLEAN, true));
        DEFINITIONS.put("compressionLevel", new Definition(Type.NUMBER, 0,
                Arrays.asList("mixed", 0, 1, 2, 3, 4, 5, 6, 7, 8, 9), false, false, Collections.emptyMap(), null));
        DEFINITIONS.put("defaultLanguageName", Definition.of(Type.STRING, "node"));
        DEFINITIONS.put("defaultProtocol", Definition.of(Type.STRING, "npm:"));
        DEFINITIONS.put("enableTransparentWorkspaces", Definition.of(Type.BOOLEAN, true));
        DEFINITIONS.put("enableHardenedMode", Definition.of(Type.BOOLEAN, false));
        DEFINITIONS.put("enableOfflineMode", Definition.of(Type.BOOLEAN, false));
        DEFINITIONS.put("checksumBehavior", Definition.of(Type.STRING, "throw"));

        Map<String, Definition> peerMeta = new LinkedHashMap<>();
        peerMeta.put("optional", Definition.of(Type.BOOLEAN, false));
        Map<String, Definition> extension = new LinkedHashMap<>();
        extension.put("dependencies", Definition.map(Definition.withoutDefault(Type.STRING)));
        extension.put("peerDependencies", Definition.map(Definition.withoutDefault(Type.STRING)));
        extension.put("peerDependenciesMeta", Definition.map(Definition.shape(peerMeta)));
        DEFINITIONS.put("packageExtensions", Definition.map(Definition.shape(extension)));

        Map<String, Definition> registry = new LinkedHashMap<>();
        registry.put("npmAlwaysAuth", Definition.of(Type.BOOLEAN, false));
        registry.put("npmAuthIdent", Definition.of(Type.SECRET, null));
        registry.put("npmAuthToken", Definition.of(Type.SECRET, null));
        Map<String, Definition> scope = new LinkedHashMap<>(registry);
        scope.put("npmAuditRegistry", Definition.of(Type.STRING, null));
        scope.put("npmPublishRegistry", Definition.of(Type.STRING, null));
        scope.put("npmRegistryServer", Definition.of(Type.STRING, "https://registry.yarnpkg.com"));
        DEFINITIONS.putAll(scope);
        DEFINITIONS.put("npmMinimalAgeGate", Definition.of(Type.NUMBER, 0));
        DEFINITIONS.put("npmPreapprovedPackages", new Definition(Type.STRING, new ArrayList<>(), null, true, false,
                Collections.emptyMap(), null));
        DEFINITIONS.put("npmScopes", Definition.map(Definition.shape(scope)));
        DEFINITIONS.put("npmRegistries", Definition.map(Definition.shape(registry), NpmHttpUtils::normalizeRegistry));

        Map<String, Definition> architectures = new LinkedHashMap<>();
        for (String property : new String[]{"os", "cpu", "libc"}) {
            architectures.put(property, new Definition(Type.STRING, Collections.singletonList("current"), null, true,
                    true, Collections.emptyMap(), null));
        }
        DEFINITIONS.put("supportedArchitectures", Definition.shape(architectures));

        DEFINITIONS.put("catalog", Definition.map(Definition.withoutDefault(Type.STRING)));
        DEFINITIONS.put("catalogs", Definition.map(Definition.map(Definition.withoutDefault(Type.STRING))));

        // plugin-git's, from Yarn 4.14.0
        DEFINITIONS.put("approvedGitRepositories", new Definition(Type.STRING, new ArrayList<>(), null, true, false,
                Collections.emptyMap(), null));
    }

    /**
     * The settings as a release defines them: Yarn 4.10.3's of those the release has (a setting it lacks is unknown to
     * it), the age gate a duration from 4.11.0 with a default of {@code 1d} from 4.15.0, and a key of each
     * {@code npmScopes} entry from 4.17.0.
     */
    private static Map<String, Definition> definitions(@Nullable YarnRelease release) {
        if (release == null) {
            return DEFINITIONS;
        }
        Map<String, Definition> definitions = new LinkedHashMap<>();
        for (Map.Entry<String, Definition> definition : DEFINITIONS.entrySet()) {
            if (release.getSettings().contains(definition.getKey())) {
                definitions.put(definition.getKey(), definition.getValue());
            }
        }
        if (!release.readsAgeGateAsDuration()) {
            return definitions;
        }
        Definition gate = Definition.duration(release.gatesAgeByDefault() ? "1d" : "0m", MINUTES);
        definitions.put("npmMinimalAgeGate", gate);
        if (release.gatesAgePerScope()) {
            Map<String, Definition> scope = new LinkedHashMap<>(
                    requireNonNull(DEFINITIONS.get("npmScopes").valueDefinition).properties);
            scope.put("npmMinimalAgeGate", gate);
            definitions.put("npmScopes", Definition.map(Definition.shape(scope)));
        }
        return definitions;
    }

    /**
     * Yarn's configuration error: a value its definition rejects, or a key no definition has.
     */
    public static final class UsageError extends RuntimeException {
        UsageError(String message) {
            super(message);
        }
    }

    // parseShape's usage error, which names the command of the release that reads the sources
    private static final class UnrecognizedShapeKey extends RuntimeException {
        final String path;

        UnrecognizedShapeKey(String path) {
            super(path);
            this.path = path;
        }
    }

    static final Pattern TAG_REGEXP = Pattern.compile("^(?!v)[a-z0-9._-]+\\z", Pattern.CASE_INSENSITIVE);

    private final Map<String, @Nullable Object> values = new LinkedHashMap<>();
    private final Map<String, String> sources = new LinkedHashMap<>();

    /**
     * Every key the environment and the rc files set, with its first source; the release decides whether Yarn
     * knows it.
     */
    private final Map<String, String> keySources = new LinkedHashMap<>();

    private final HostEnvironment env;

    /**
     * The release whose definitions the sources were read by, Yarn 4.10.3's where none is named.
     */
    @Getter(AccessLevel.NONE)
    private final @Nullable YarnRelease readBy;

    @Getter(AccessLevel.NONE)
    private final Map<String, Definition> definitions;

    @Getter(AccessLevel.NONE)
    private @Nullable YarnRelease release;

    @Getter(AccessLevel.NONE)
    private @Nullable Map<String, List<PackageExtensions.RangeExtensions>> packageExtensions;

    /**
     * The first plugin the sources name that is not one the CLI bundles, with its source; Yarn runs its code.
     */
    @Getter
    private @Nullable String thirdPartyPlugin;

    /**
     * A key a shape setting lacks, with its source: Yarn's reading of the sources ended there.
     */
    private @Nullable String unrecognizedShapeKey;
    private @Nullable String unrecognizedShapeKeySource;

    private Configuration(HostEnvironment env, @Nullable YarnRelease readBy) {
        this.env = env;
        this.readBy = readBy;
        this.definitions = definitions(readBy);
        for (Map.Entry<String, Definition> definition : definitions.entrySet()) {
            values.put(definition.getKey(), getDefaultValue(definition.getValue(), false));
        }
    }

    private Configuration(HostEnvironment env) {
        this(env, null);
    }

    /**
     * The configuration as the sources set it, for one install to restrict and to apply migration rules to.
     */
    Configuration copy() {
        Configuration copy = new Configuration(env, readBy);
        copy.values.putAll(values);
        copy.sources.putAll(sources);
        copy.keySources.putAll(keySources);
        copy.thirdPartyPlugin = thirdPartyPlugin;
        copy.unrecognizedShapeKey = unrecognizedShapeKey;
        copy.unrecognizedShapeKeySource = unrecognizedShapeKeySource;
        return copy;
    }

    /**
     * The defaults, as a project with no rc file and no environment has them.
     */
    public static Configuration defaults() {
        return new Configuration(HostEnvironment.of(Collections.emptyMap()));
    }

    /**
     * Configuration.findRcFiles's walk: of {@code files}, by path from the project's directory, those in the directory
     * Yarn runs in ({@code startingCwd}, from there too) or in one above it.
     */
    public static Map<String, String> findRcFiles(String startingCwd, Map<String, String> files) {
        Map<String, String> found = new LinkedHashMap<>();
        for (Map.Entry<String, String> file : files.entrySet()) {
            String path = file.getKey();
            String dir = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
            if (dir.isEmpty() || dir.matches("\\.\\.(/\\.\\.)*") || startingCwd.equals(dir) ||
                startingCwd.startsWith(dir + "/")) {
                found.put(path, file.getValue());
            }
        }
        return found;
    }

    /**
     * Reads {@code configFiles}, the rc files by path from the project's directory from where Yarn runs up and the
     * home directory's under {@code ~/}, read first, under {@code env}; throws UsageError where Yarn rejects one.
     */
    public static Configuration load(Map<String, String> configFiles, HostEnvironment env) {
        return load(configFiles, env, null);
    }

    /**
     * {@link #load(Map, HostEnvironment)} with the settings as {@code release} defines them; Yarn 4.10.3's where it is
     * null.
     */
    static Configuration load(Map<String, String> configFiles, HostEnvironment env, @Nullable YarnRelease release) {
        Configuration options = new Configuration(env, release);
        String rcFilename = getRcFilename(env.known());
        TreeMap<Integer, Object[]> byDepth = new TreeMap<>(Collections.reverseOrder());
        for (Map.Entry<String, String> file : configFiles.entrySet()) {
            String path = file.getKey();
            int up = 0;
            String rest = path;
            while (rest.startsWith("../")) {
                rest = rest.substring(3);
                up++;
            }
            int slash = rest.lastIndexOf('/');
            boolean home = rest.startsWith(HOME);
            if (home) {
                up = Integer.MAX_VALUE;
                rest = rest.substring(HOME.length());
            } else if (slash >= 0 && up == 0) {
                up = -rest.substring(0, slash).split("/").length;
                rest = rest.substring(slash + 1);
            }
            // findFolderRcFile reads the home directory's under Filename.rc, whatever YARN_RC_FILENAME names
            if (!rest.equals(home ? DEFAULT_RC_FILENAME : rcFilename)) {
                continue;
            }
            Object data;
            try {
                data = Syml.parseSyml(file.getValue(), release);
            } catch (RuntimeException e) {
                String tip = Pattern.compile("^\\s+(?!-)[^:]+\\s+\\S+", Pattern.MULTILINE).matcher(file.getValue())
                        .find() ? " (in particular, make sure you list the colons after each key name)" : "";
                throw new UsageError("Parse error when loading " + path + "; please check it's proper Yaml" + tip);
            }
            byDepth.put(up, new Object[]{path, data});
        }
        List<Object[]> rcFiles = new ArrayList<>(byDepth.values());
        ConfigUtils.Resolved resolvedRcFile = ConfigUtils.resolveRcFiles(rcFiles);

        Map<String, @Nullable Object> environmentSettings = getEnvironmentSettings(env.known());
        environmentSettings.remove("rcFilename");
        options.use("<environment>", pickPrimaryCoreFields(environmentSettings));
        if (resolvedRcFile != null) {
            options.use(resolvedRcFile.getSource(), pickPrimaryCoreFields(asMap(resolvedRcFile.getValue())));
        }
        options.use("<environment>", pickSecondaryCoreFields(environmentSettings));
        if (resolvedRcFile != null) {
            options.use(resolvedRcFile.getSource(), pickSecondaryCoreFields(asMap(resolvedRcFile.getValue())));
        }
        // the plugins' settings are read after the third-party plugins run
        options.thirdPartyPlugin = thirdPartyPlugin(environmentSettings, rcFiles);
        if (options.thirdPartyPlugin != null) {
            return options;
        }
        options.use("<environment>", pickPluginFields(environmentSettings));
        if (resolvedRcFile != null) {
            options.use(resolvedRcFile.getSource(), pickPluginFields(asMap(resolvedRcFile.getValue())));
        }
        return options;
    }

    // Configuration.find's pickPrimaryCoreFields, read first: the release yarnPath picks reads the others
    private static Map<String, @Nullable Object> pickPrimaryCoreFields(Map<String, @Nullable Object> data) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        for (String key : PRIMARY_FIELDS) {
            if (data.containsKey(key)) {
                out.put(key, data.get(key));
            }
        }
        return out;
    }

    private static Map<String, @Nullable Object> pickSecondaryCoreFields(Map<String, @Nullable Object> data) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable Object> entry : data.entrySet()) {
            if (CORE_FIELDS.contains(entry.getKey()) && !PRIMARY_FIELDS.contains(entry.getKey())) {
                out.put(entry.getKey(), entry.getValue());
            }
        }
        return out;
    }

    private static Map<String, @Nullable Object> pickPluginFields(Map<String, @Nullable Object> data) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>(data);
        out.keySet().removeAll(CORE_FIELDS);
        return out;
    }

    // Configuration.find's third-party plugin pass, up to the first plugin it would run
    private static @Nullable String thirdPartyPlugin(Map<String, @Nullable Object> environmentSettings,
                                                     List<Object[]> rcFiles) {
        Object fromEnvironment = environmentSettings.get("plugins");
        if (Js.truthy(fromEnvironment)) {
            return Js.string(fromEnvironment).split(";", -1)[0] + " (<environment>)";
        }
        for (Object[] rcFile : rcFiles) {
            Object plugins = Js.get(rcFile[1], "plugins");
            if (!(plugins instanceof List)) {
                continue;
            }
            for (Object entry : (List<?>) plugins) {
                Object path = entry instanceof String ? entry : Js.read(entry, "path");
                Object spec = entry instanceof Map ? Js.get(entry, "spec") : Syml.UNDEFINED;
                if (spec != null && spec != Syml.UNDEFINED && LEGACY_PLUGINS.contains(Js.string(spec))) {
                    continue;
                }
                return Js.string(path) + " (" + rcFile[0] + ")";
            }
        }
        return null;
    }

    /**
     * The version in the file name of the release {@code yarnPath} names ({@code .yarn/releases/yarn-4.10.3.cjs}).
     */
    /**
     * The {@code catalog} and {@code catalogs} settings of one rc file as Yarn parses them, {@code ""} the default
     * catalog; throws where Yarn rejects the file or them.
     */
    static Map<String, Map<String, String>> catalogs(String rcFile, HostEnvironment env) {
        Object data;
        try {
            data = Syml.parseSyml(rcFile);
        } catch (RuntimeException e) {
            throw new UsageError("Parse error when loading " + DEFAULT_RC_FILENAME + "; please check it's proper Yaml");
        }
        Configuration options = new Configuration(env);
        Map<String, Map<String, String>> catalogs = new LinkedHashMap<>();
        Map<String, @Nullable Object> settings = asMap(data);
        if (settings.get("catalog") != null) {
            catalogs.put("", strings(options.parseValue("catalog", settings.get("catalog"),
                    DEFINITIONS.get("catalog"))));
        }
        if (settings.get("catalogs") != null) {
            for (Map.Entry<String, @Nullable Object> named : asMap(options.parseValue("catalogs",
                    settings.get("catalogs"), DEFINITIONS.get("catalogs"))).entrySet()) {
                if (!named.getKey().isEmpty()) {
                    catalogs.put(named.getKey(), strings(named.getValue()));
                }
            }
        }
        return catalogs;
    }

    private static Map<String, String> strings(@Nullable Object catalog) {
        Map<String, String> entries = new LinkedHashMap<>();
        for (Map.Entry<String, @Nullable Object> entry : asMap(catalog).entrySet()) {
            if (entry.getValue() instanceof String) {
                entries.put(entry.getKey(), (String) entry.getValue());
            }
        }
        return entries;
    }

    static @Nullable String yarnPathVersion(String yarnPath) {
        Matcher m = YARN_RELEASE.matcher(yarnPath);
        return m.find() ? m.group(1) : null;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> asMap(@Nullable Object value) {
        return value instanceof Map ? (Map<String, @Nullable Object>) value : new LinkedHashMap<>();
    }

    private static String getRcFilename(Map<String, String> env) {
        for (Map.Entry<String, String> entry : env.entrySet()) {
            if ((ENVIRONMENT_PREFIX + "rc_filename").equals(entry.getKey().toLowerCase(Locale.ROOT))) {
                return entry.getValue();
            }
        }
        return DEFAULT_RC_FILENAME;
    }

    private static Map<String, @Nullable Object> getEnvironmentSettings(Map<String, String> env) {
        Map<String, @Nullable Object> environmentSettings = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            if (!key.startsWith(ENVIRONMENT_PREFIX)) {
                continue;
            }
            environmentSettings.put(camelcase(key.substring(ENVIRONMENT_PREFIX.length())), entry.getValue());
        }
        return environmentSettings;
    }

    // camelcase 5.3.1 of a key already lower-cased, so preserveCamelCase never runs
    static String camelcase(String key) {
        String input = Js.trim(key);
        if (input.length() <= 1) {
            return input.toLowerCase(Locale.ROOT);
        }
        input = LEADING_SEPARATORS.matcher(input).replaceFirst("").toLowerCase(Locale.ROOT);
        input = replaceAll(input, SEPARATED_WORD, m -> m.group(1).toUpperCase(Locale.ROOT));
        return replaceAll(input, DIGITS_WORD, m -> m.group().toUpperCase(Locale.ROOT));
    }

    private static String replaceAll(String s, Pattern pattern, Function<Matcher, String> replacer) {
        Matcher m = pattern.matcher(s);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (m.find()) {
            out.append(s, last, m.start()).append(replacer.apply(m));
            last = m.end();
        }
        return out.append(s.substring(last)).toString();
    }

    void use(String source, Map<String, @Nullable Object> data) {
        if (unrecognizedShapeKey != null) {
            return;
        }
        List<String> keys = new ArrayList<>();
        keys.add("enableStrictSettings");
        keys.addAll(Js.keys(data));
        for (String key : keys) {
            Object value = data.containsKey(key) ? data.get(key) : Syml.UNDEFINED;
            String fieldSource = ConfigUtils.getSource(value);
            if (fieldSource != null) {
                source = fieldSource;
            }
            if (value == Syml.UNDEFINED || "plugins".equals(key)) {
                continue;
            }
            if ("<environment>".equals(source) && ignoresEnvVariable(key)) {
                continue;
            }
            if ("rcFilename".equals(key)) {
                throw new UsageError("The rcFilename settings can only be set via YARN_RC_FILENAME, not via a rc file");
            }
            if (!"enableStrictSettings".equals(key)) {
                keySources.putIfAbsent(key, source);
            }
            Definition definition = definitions.get(key);
            if (definition == null) {
                continue;
            }
            // only logFilters concatenates its sources' arrays, and it never reaches a lock
            if (sources.containsKey(key) && definition.type != Type.MAP) {
                continue;
            }
            Object parsed;
            try {
                parsed = parseValue(key, value, definition);
            } catch (UnrecognizedShapeKey e) {
                unrecognizedShapeKey = e.path;
                unrecognizedShapeKeySource = source;
                return;
            } catch (UsageError e) {
                throw new UsageError(e.getMessage() + " in " + source);
            } catch (JsError e) {
                throw new JsError(e.getName(), e.getMessage() + " in " + source);
            }
            if ("enableStrictSettings".equals(key) && !"<environment>".equals(source)) {
                continue;
            }
            if (definition.type == Type.MAP) {
                Map<String, @Nullable Object> merged = new LinkedHashMap<>(asMap(parsed));
                merged.putAll(asMap(values.get(key)));
                values.put(key, merged);
                sources.put(key, (sources.containsKey(key) ? sources.get(key) : "undefined") + ", " + source);
            } else {
                values.put(key, parsed);
                sources.put(key, source);
            }
        }
    }

    // IGNORED_ENV_VARIABLES of the release the sources are read by: zipDataEpilogue from 4.1.0, the architecture
    // overrides from 4.10.0, ignoreScripts from 4.15.0
    private boolean ignoresEnvVariable(String key) {
        if (readBy != null) {
            if ("zipDataEpilogue".equals(key)) {
                return readBy.ignoresZipDataEpilogue();
            }
            if ("osOverride".equals(key) || "cpuOverride".equals(key) || "libcOverride".equals(key)) {
                return readBy.ignoresArchitectureOverrides();
            }
            if ("ignoreScripts".equals(key)) {
                return readBy.ignoresYarnClassicIgnoreScripts();
            }
        }
        return IGNORED_ENV_VARIABLES.contains(key);
    }

    /**
     * {@code use}'s check of a key against the settings the release defines: under {@code enableStrictSettings}
     * an unknown key is Yarn's usage error but in the home rc file; otherwise it is set aside and its default stays.
     */
    void restrictTo(YarnRelease release) {
        this.release = release;
        checkShapeKeys(release);
        // an unknown key is read in the plugin pass, under the stored value, which only the environment sets
        boolean strict = getBoolean("enableStrictSettings");
        for (Map.Entry<String, String> key : keySources.entrySet()) {
            if (release.getSettings().contains(key.getKey())) {
                continue;
            }
            if (strict && !key.getValue().startsWith(HOME)) {
                throw new UsageError("Unrecognized or legacy configuration settings found: " + key.getKey() +
                                     " - run \"" + release.unknownSettingHint() + "\" to see the list of " +
                                     "settings supported in Yarn (in " + key.getValue() + ")");
            }
            Definition definition = definitions.get(key.getKey());
            if (definition != null) {
                values.put(key.getKey(), getDefaultValue(definition, false));
                sources.remove(key.getKey());
            }
        }
    }

    /**
     * parseShape's usage error for a key a shape setting lacks, as the release that reads the sources words it.
     */
    void checkShapeKeys(YarnRelease release) {
        if (unrecognizedShapeKey != null) {
            throw new UsageError("Unrecognized configuration settings found: " + unrecognizedShapeKey + " - run \"" +
                                 release.unknownSettingHint() + "\" to see the list of settings supported in " +
                                 "Yarn in " + unrecognizedShapeKeySource);
        }
    }

    private @Nullable Object parseValue(String path, @Nullable Object valueBase, Definition definition) {
        Object value = ConfigUtils.getValue(valueBase);
        if (definition.isArray || (definition.type == Type.ANY && value instanceof List)) {
            List<@Nullable Object> out = new ArrayList<>();
            if (!(value instanceof List)) {
                for (String segment : Js.string(value).split(",", -1)) {
                    out.add(parseSingleValue(path, segment, definition));
                }
            } else {
                List<?> list = (List<?>) value;
                for (int i = 0; i < list.size(); i++) {
                    out.add(parseSingleValue(path + "[" + i + "]", list.get(i), definition));
                }
            }
            return out;
        }
        if (value instanceof List) {
            throw new JsError(JsError.ERROR, "Non-array configuration settings \"" + path +
                                             "\" cannot be an array");
        }
        return parseSingleValue(path, valueBase, definition);
    }

    private @Nullable Object parseSingleValue(String path, @Nullable Object valueBase, Definition definition) {
        Object value = ConfigUtils.getValue(valueBase);
        switch (definition.type) {
            case ANY:
                return ConfigUtils.getValueByTree(value);
            case SHAPE:
                return parseShape(path, valueBase, definition);
            case MAP:
                return parseMap(path, valueBase, definition);
            default:
                break;
        }
        if (value == null && !definition.isNullable && definition.defaultValue != null) {
            throw new JsError(JsError.ERROR, "Non-nullable configuration settings \"" + path +
                                             "\" cannot be set to null");
        }
        if (definition.values != null && definition.values.contains(value)) {
            return value;
        }
        Object interpreted;
        if (definition.type == Type.BOOLEAN && !(value instanceof String)) {
            interpreted = MiscUtils.parseBoolean(value);
        } else {
            if (!(value instanceof String)) {
                throw new JsError(JsError.ERROR, "Expected configuration setting \"" + path +
                                                 "\" to be a string, got " + Js.typeOf(value));
            }
            String replaced = readBy == null ? MiscUtils.replaceEnvVariables((String) value, env) :
                    MiscUtils.replaceEnvVariables((String) value, env, readBy);
            switch (definition.type) {
                case NUMBER:
                    interpreted = number(Js.parseInt(replaced, 0));
                    break;
                case DURATION:
                    interpreted = MiscUtils.parseDuration(replaced, requireNonNull(definition.unit));
                    break;
                case BOOLEAN:
                    interpreted = MiscUtils.parseBoolean(replaced);
                    break;
                default:
                    interpreted = replaced;
            }
        }
        if (definition.values != null && !definition.values.contains(interpreted)) {
            List<String> expected = new ArrayList<>();
            for (Object v : definition.values) {
                expected.add(String.valueOf(v));
            }
            throw new JsError(JsError.ERROR, "Invalid value, expected one of " + String.join(", ", expected));
        }
        return interpreted;
    }

    private Map<String, @Nullable Object> parseShape(String path, @Nullable Object valueBase, Definition definition) {
        Object value = ConfigUtils.getValue(valueBase);
        if (!(value instanceof Map) && value != null) {
            throw new UsageError("Object configuration settings \"" + path + "\" must be an object");
        }
        Map<String, @Nullable Object> result = asMap(getDefaultValue(definition, true));
        if (value == null) {
            return result;
        }
        for (Map.Entry<String, @Nullable Object> entry : Js.entries(value)) {
            String subPath = path + "." + entry.getKey();
            Definition subDefinition = definition.properties.get(entry.getKey());
            if (subDefinition == null) {
                throw new UnrecognizedShapeKey(subPath);
            }
            result.put(entry.getKey(), parseValue(subPath, entry.getValue(), subDefinition));
        }
        return result;
    }

    private Map<String, @Nullable Object> parseMap(String path, @Nullable Object valueBase, Definition definition) {
        Object value = ConfigUtils.getValue(valueBase);
        Map<String, @Nullable Object> result = new LinkedHashMap<>();
        if (!(value instanceof Map) && value != null) {
            throw new UsageError("Map configuration settings \"" + path + "\" must be an object");
        }
        if (value == null) {
            return result;
        }
        for (Map.Entry<String, @Nullable Object> entry : Js.entries(value)) {
            String normalizedKey = definition.normalizeKeys != null ? definition.normalizeKeys.apply(entry.getKey()) :
                    entry.getKey();
            result.put(normalizedKey, parseValue(path + "['" + normalizedKey + "']", entry.getValue(),
                    definition.valueDefinition));
        }
        return result;
    }

    private static @Nullable Object getDefaultValue(Definition definition, boolean ignoreArrays) {
        switch (definition.type) {
            case SHAPE: {
                if (definition.isArray && !ignoreArrays) {
                    return new ArrayList<>();
                }
                Map<String, @Nullable Object> result = new LinkedHashMap<>();
                for (Map.Entry<String, Definition> property : definition.properties.entrySet()) {
                    result.put(property.getKey(), getDefaultValue(property.getValue(), false));
                }
                return result;
            }
            case MAP:
                return definition.isArray && !ignoreArrays ? new ArrayList<>() : new LinkedHashMap<>();
            case DURATION:
                return MiscUtils.parseDuration((String) requireNonNull(definition.defaultValue),
                        requireNonNull(definition.unit));
            default:
                return definition.defaultValue instanceof List ? new ArrayList<>((List<?>) definition.defaultValue) :
                        definition.defaultValue;
        }
    }

    // a number as the settings hold one, an int when it is one
    private static Object number(double d) {
        return d == (int) d ? (Object) (int) d : (Object) d;
    }

    /**
     * {@code configuration.use("<compat>", {[name]: value}, {overwrite: true})} for a setting no source set: a
     * lockfile migration rule. False when a source set it.
     */
    boolean useCompat(String name, Object value) {
        if (sources.containsKey(name) || keySources.containsKey(name)) {
            return false;
        }
        Definition definition = definitions.get(name);
        if (definition != null) {
            values.put(name, parseValue(name, value, definition));
        }
        sources.put(name, "<compat>");
        return true;
    }

    @Nullable
    Object get(String key) {
        if (!values.containsKey(key)) {
            throw new UsageError("Couldn't find a configuration settings named \"" + key + "\"");
        }
        return values.get(key);
    }

    String getString(String key) {
        return (String) get(key);
    }

    boolean getBoolean(String key) {
        return Boolean.TRUE.equals(get(key));
    }

    @SuppressWarnings("unchecked")
    Map<String, @Nullable Object> getMap(String key) {
        return (Map<String, @Nullable Object>) get(key);
    }

    /**
     * {@code getRcFilename}: the project's rc file, {@code YARN_RC_FILENAME}'s name or {@code .yarnrc.yml}.
     */
    String getRcFilename() {
        return getRcFilename(env.known());
    }

    /**
     * The release {@link #restrictTo} named.
     */
    YarnRelease getRelease() {
        return requireNonNull(release, "the release the configuration is restricted to");
    }

    /**
     * {@code getSupportedArchitectures} on a machine, {@code current} its os, cpu and libc, where a null libc (what
     * getLibc detects on an os other than linux) names none.
     */
    Map<String, @Nullable List<String>> getSupportedArchitectures(Host machine) {
        Map<String, @Nullable Object> supported = getMap("supportedArchitectures");
        Map<String, @Nullable List<String>> out = new LinkedHashMap<>();
        out.put("os", current(supported.get("os"), machine.getOs()));
        out.put("cpu", current(supported.get("cpu"), machine.getCpu()));
        out.put("libc", current(supported.get("libc"), machine.getLibc()));
        return out;
    }

    private static @Nullable List<String> current(@Nullable Object values, @Nullable String current) {
        if (!(values instanceof List)) {
            return null;
        }
        List<String> out = new ArrayList<>();
        for (Object value : (List<?>) values) {
            if (!"current".equals(value)) {
                out.add(Js.string(value));
            } else if (current != null) {
                out.add(current);
            }
        }
        return out;
    }

    /**
     * {@code findProjectCwd}, in directories above where Yarn runs: the nearest holding a yarn.lock, else the farthest
     * holding a package.json; -1 for none. {@code holds} answers by path from there ({@code ../yarn.lock}).
     */
    static int findProjectCwd(Predicate<String> holds, int levels) {
        int projectCwd = -1;
        StringBuilder up = new StringBuilder();
        for (int depth = 0; depth <= levels; depth++, up.append("../")) {
            if (holds.test(up + "yarn.lock")) {
                return depth;
            }
            if (holds.test(up + "package.json")) {
                projectCwd = depth;
            }
        }
        return projectCwd;
    }

    String defaultProtocol() {
        return getString("defaultProtocol");
    }

    Locator normalizeLocator(Locator locator) {
        if (YarnpkgSemverUtils.validRange(locator.reference) != null) {
            return StructUtils.makeLocator(locator, defaultProtocol() + locator.reference);
        }
        if (TAG_REGEXP.matcher(locator.reference).find()) {
            return StructUtils.makeLocator(locator, defaultProtocol() + locator.reference);
        }
        return locator;
    }

    Descriptor normalizeDependency(Descriptor dependency) {
        if (YarnpkgSemverUtils.validRange(dependency.range) != null) {
            return StructUtils.makeDescriptor(dependency, defaultProtocol() + dependency.range);
        }
        if (TAG_REGEXP.matcher(dependency.range).find()) {
            return StructUtils.makeDescriptor(dependency, defaultProtocol() + dependency.range);
        }
        return dependency;
    }

    Map<String, Descriptor> normalizeDependencyMap(Map<String, Descriptor> dependencyMap) {
        Map<String, Descriptor> out = new LinkedHashMap<>();
        for (Map.Entry<String, Descriptor> entry : dependencyMap.entrySet()) {
            out.put(entry.getKey(), normalizeDependency(entry.getValue()));
        }
        return out;
    }

    /**
     * {@code getPackageExtensions}: the release's builtin table (the compat plugin's
     * {@code registerPackageExtensions}), then the {@code packageExtensions} setting, computed once.
     */
    Map<String, List<PackageExtensions.RangeExtensions>> getPackageExtensions() {
        if (packageExtensions == null) {
            packageExtensions = PackageExtensions.load(getRelease(), this);
        }
        return packageExtensions;
    }

    /**
     * {@code normalizePackage}: a copy with the matching extensions applied, the implicit peers added and the
     * dependency maps sorted by descriptor.
     */
    Package normalizePackage(Package original) {
        return PackageExtensions.normalizePackage(this, original, getPackageExtensions());
    }

    Resolver makeResolver() {
        List<Resolver> resolvers = new ArrayList<>();
        resolvers.add(new VirtualResolver());
        resolvers.add(new WorkspaceResolver());
        for (String plugin : getRelease().getPlugins()) {
            resolvers.addAll(resolvers(plugin));
        }
        return new MultiResolver(resolvers.toArray(new Resolver[0]));
    }

    /**
     * {@code makeFetcher}, as far as the fetchers that reach a lockfile.
     */
    Fetcher makeFetcher() {
        List<Fetcher> fetchers = new ArrayList<>();
        fetchers.add(new WorkspaceFetcher());
        for (String plugin : getRelease().getPlugins()) {
            fetchers.addAll(fetchers(plugin));
        }
        return new MultiFetcher(fetchers.toArray(new Fetcher[0]));
    }

    private static List<Resolver> resolvers(String plugin) {
        switch (plugin) {
            case "@yarnpkg/plugin-exec":
                return singletonList(new ExecResolver());
            case "@yarnpkg/plugin-file":
                return asList(new TarballFileResolver(), new FileResolver());
            case "@yarnpkg/plugin-git":
                return singletonList(new GitResolver());
            case "@yarnpkg/plugin-http":
                return singletonList(new TarballHttpResolver());
            case "@yarnpkg/plugin-jsr":
                return singletonList(new JsrResolver());
            case "@yarnpkg/plugin-link":
                return asList(new LinkResolver(), new PortalResolver());
            case "@yarnpkg/plugin-npm":
                return asList(new NpmRemapResolver(), new NpmSemverResolver(), new NpmTagResolver());
            case "@yarnpkg/plugin-patch":
                return singletonList(new PatchResolver());
            default:
                return emptyList();
        }
    }

    private static List<Fetcher> fetchers(String plugin) {
        switch (plugin) {
            case "@yarnpkg/plugin-git":
                return singletonList(new GitFetcher());
            case "@yarnpkg/plugin-http":
                return singletonList(new TarballHttpFetcher());
            case "@yarnpkg/plugin-jsr":
                return singletonList(new JsrFetcher());
            case "@yarnpkg/plugin-link":
                return singletonList(new LinkFetcher());
            case "@yarnpkg/plugin-npm":
                return asList(new NpmHttpFetcher(), new NpmSemverFetcher());
            case "@yarnpkg/plugin-patch":
                return singletonList(new PatchFetcher());
            default:
                return emptyList();
        }
    }

    /**
     * {@code reduceHook(hooks => hooks.reduceDependency, ...)} over the core plugin, then the bundled ones in order.
     */
    Descriptor reduceDependency(Descriptor dependency, Project project, Locator locator, Resolver resolver,
                                Resolver.ResolveOptions opts) {
        Descriptor value = Resolutions.reduceDependency(dependency, project, locator, resolver, opts);
        for (String plugin : getRelease().getPlugins()) {
            if ("@yarnpkg/plugin-compat".equals(plugin)) {
                value = CompatPatches.reduceDependency(value, getRelease());
            } else if ("@yarnpkg/plugin-catalog".equals(plugin)) {
                value = Catalogs.reduceDependency(value, project, resolver, opts);
            }
        }
        return value;
    }
}
