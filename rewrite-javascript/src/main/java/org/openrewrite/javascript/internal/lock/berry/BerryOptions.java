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

import org.openrewrite.javascript.internal.lock.HostEnvironment;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.math.BigInteger;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @yarnpkg/core sources/Configuration.ts {@code find} for the settings a lock reads (Yarn 4.10.3).
 */
@Getter
public final class BerryOptions {

    public static final String DEFAULT_RC_FILENAME = ".yarnrc.yml";
    private static final String ENVIRONMENT_PREFIX = "yarn_";
    private static final Pattern YARN_RELEASE = Pattern.compile("(?:^|/)yarn-(\\d+\\.\\d+\\.\\d+[^/]*?)\\.c?js$");
    private static final Pattern PACKAGE_MANAGER = Pattern.compile("^yarn@(\\d+\\.\\d+\\.\\d+(?:-[^+]*)?)(?:\\+.*)?$");
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

    private static final Set<String> LEGACY_PLUGINS = new LinkedHashSet<>(Arrays.asList(
            "@yarnpkg/plugin-constraints", "@yarnpkg/plugin-exec", "@yarnpkg/plugin-interactive-tools",
            "@yarnpkg/plugin-stage", "@yarnpkg/plugin-typescript", "@yarnpkg/plugin-version",
            "@yarnpkg/plugin-workspace-tools"));

    private enum Type {
        ANY, BOOLEAN, ABSOLUTE_PATH, NUMBER, STRING, SECRET, SHAPE, MAP
    }

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

        Definition(Type type, @Nullable Object defaultValue, @Nullable List<Object> values, boolean isArray,
                   boolean isNullable, Map<String, Definition> properties, @Nullable Definition valueDefinition) {
            this.type = type;
            this.defaultValue = defaultValue;
            this.values = values;
            this.isArray = isArray;
            this.isNullable = isNullable;
            this.properties = properties;
            this.valueDefinition = valueDefinition;
        }

        static Definition of(Type type, @Nullable Object defaultValue) {
            return new Definition(type, defaultValue, null, false, false, Collections.emptyMap(), null);
        }

        static Definition withoutDefault(Type type) {
            return of(type, Syml.UNDEFINED);
        }

        static Definition map(Definition valueDefinition) {
            return new Definition(Type.MAP, null, null, false, false, Collections.emptyMap(), valueDefinition);
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
        DEFINITIONS.put("npmRegistries", Definition.map(Definition.shape(registry)));

        Map<String, Definition> architectures = new LinkedHashMap<>();
        for (String property : new String[]{"os", "cpu", "libc"}) {
            architectures.put(property, new Definition(Type.STRING, Collections.singletonList("current"), null, true,
                    true, Collections.emptyMap(), null));
        }
        DEFINITIONS.put("supportedArchitectures", Definition.shape(architectures));

        DEFINITIONS.put("catalog", Definition.map(Definition.withoutDefault(Type.STRING)));
        DEFINITIONS.put("catalogs", Definition.map(Definition.map(Definition.withoutDefault(Type.STRING))));
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

    private final Map<String, @Nullable Object> values = new LinkedHashMap<>();
    private final Map<String, String> sources = new LinkedHashMap<>();

    /**
     * Every key the environment and the rc files set, with its first source; the release decides whether Yarn
     * knows it.
     */
    private final Map<String, String> keySources = new LinkedHashMap<>();

    private final HostEnvironment env;

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

    private BerryOptions(HostEnvironment env) {
        this.env = env;
        for (Map.Entry<String, Definition> definition : DEFINITIONS.entrySet()) {
            values.put(definition.getKey(), getDefaultValue(definition.getValue(), false));
        }
    }

    /**
     * The configuration as the sources set it, for one install to restrict and to apply migration rules to.
     */
    BerryOptions copy() {
        BerryOptions copy = new BerryOptions(env);
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
    public static BerryOptions defaults() {
        return new BerryOptions(HostEnvironment.of(Collections.emptyMap()));
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
     * Reads {@code configFiles}, the rc files by path from the project's directory, from where Yarn runs up (another
     * name only as {@code YARN_RC_FILENAME} names it), under {@code env}; throws UsageError where Yarn rejects one.
     */
    public static BerryOptions load(Map<String, String> configFiles, HostEnvironment env) {
        BerryOptions options = new BerryOptions(env);
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
            if (slash >= 0 && up == 0) {
                up = -rest.substring(0, slash).split("/").length;
                rest = rest.substring(slash + 1);
            }
            if (!rest.equals(rcFilename)) {
                continue;
            }
            Object data;
            try {
                data = Syml.parseSyml(file.getValue());
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
        for (boolean primary : new boolean[]{true, false}) {
            options.use("<environment>", fields(environmentSettings, primary));
            if (resolvedRcFile != null) {
                options.use(resolvedRcFile.getSource(), fields(asMap(resolvedRcFile.getValue()), primary));
            }
        }
        options.thirdPartyPlugin = thirdPartyPlugin(environmentSettings, rcFiles);
        return options;
    }

    // Configuration.find's pickPrimaryCoreFields, read first: the release yarnPath picks reads the others
    private static Map<String, @Nullable Object> fields(Map<String, @Nullable Object> data, boolean primary) {
        Map<String, @Nullable Object> out = new LinkedHashMap<>();
        if (primary) {
            for (String key : PRIMARY_FIELDS) {
                if (data.containsKey(key)) {
                    out.put(key, data.get(key));
                }
            }
        } else {
            out.putAll(data);
            out.keySet().removeAll(PRIMARY_FIELDS);
        }
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
     * The version a root manifest's {@code packageManager} names ({@code yarn@4.10.3}, with or without a hash).
     */
    static @Nullable String packageManagerVersion(@Nullable String packageManager) {
        if (packageManager == null) {
            return null;
        }
        Matcher m = PACKAGE_MANAGER.matcher(packageManager);
        return m.find() ? m.group(1) : null;
    }

    /**
     * The version in the file name of the release {@code yarnPath} names ({@code .yarn/releases/yarn-4.10.3.cjs}).
     */
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
            if ("<environment>".equals(source) && IGNORED_ENV_VARIABLES.contains(key)) {
                continue;
            }
            if ("rcFilename".equals(key)) {
                throw new UsageError("The rcFilename settings can only be set via YARN_RC_FILENAME, not via a rc file");
            }
            if (!"enableStrictSettings".equals(key)) {
                keySources.putIfAbsent(key, source);
            }
            Definition definition = DEFINITIONS.get(key);
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

    /**
     * {@code use}'s check of a key against the settings the release defines: under {@code enableStrictSettings}
     * an unknown key is Yarn's usage error, otherwise it is set aside and its default stays.
     */
    void restrictTo(YarnRelease release) {
        checkShapeKeys(release);
        // an unknown key is read in the plugin pass, under the stored value, which only the environment sets
        boolean strict = getBoolean("enableStrictSettings");
        for (Map.Entry<String, String> key : keySources.entrySet()) {
            if (release.getSettings().contains(key.getKey())) {
                continue;
            }
            if (strict) {
                throw new UsageError("Unrecognized or legacy configuration settings found: " + key.getKey() +
                                     " - run \"" + release.getUnknownSettingHint() + "\" to see the list of " +
                                     "settings supported in Yarn (in " + key.getValue() + ")");
            }
            Definition definition = DEFINITIONS.get(key.getKey());
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
                                 release.getUnknownSettingHint() + "\" to see the list of settings supported in " +
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
                                                 "\" to be a string, got " + typeOf(value));
            }
            String replaced = MiscUtils.replaceEnvVariables((String) value, env);
            switch (definition.type) {
                case NUMBER:
                    interpreted = parseInt(replaced);
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
            result.put(entry.getKey(), parseValue(path + "['" + entry.getKey() + "']", entry.getValue(),
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
            default:
                return definition.defaultValue instanceof List ? new ArrayList<>((List<?>) definition.defaultValue) :
                        definition.defaultValue;
        }
    }

    private static String typeOf(@Nullable Object value) {
        if (value == null || value instanceof Map || value instanceof List) {
            return "object";
        }
        if (value instanceof Boolean) {
            return "boolean";
        }
        return value instanceof Number ? "number" : "undefined";
    }

    // parseInt: the leading integer of the string, hexadecimal after 0x, NaN when there is none
    static Object parseInt(String s) {
        int i = 0;
        while (i < s.length() && Js.isWhiteSpace(s.charAt(i))) {
            i++;
        }
        boolean negative = false;
        if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
            negative = s.charAt(i) == '-';
            i++;
        }
        int radix = 10;
        if (i + 1 < s.length() && s.charAt(i) == '0' && (s.charAt(i + 1) == 'x' || s.charAt(i + 1) == 'X')) {
            radix = 16;
            i += 2;
        }
        int start = i;
        while (i < s.length() && s.charAt(i) < 128 && Character.digit(s.charAt(i), radix) >= 0) {
            i++;
        }
        if (i == start) {
            return Double.NaN;
        }
        BigInteger magnitude = new BigInteger(s.substring(start, i), radix);
        BigInteger value = negative ? magnitude.negate() : magnitude;
        return value.bitLength() < 32 ? (Object) value.intValue() : (Object) value.doubleValue();
    }

    /**
     * {@code configuration.use("<compat>", {[name]: value}, {overwrite: true})} for a setting no source set: a
     * lockfile migration rule. False when a source set it.
     */
    boolean useCompat(String name, Object value) {
        if (sources.containsKey(name) || keySources.containsKey(name)) {
            return false;
        }
        if (DEFINITIONS.containsKey(name)) {
            values.put(name, value);
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
}
