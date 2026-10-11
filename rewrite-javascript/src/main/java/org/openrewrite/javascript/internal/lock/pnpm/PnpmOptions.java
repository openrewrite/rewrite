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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import lombok.With;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.Ini;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.StripBom;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/config.reader src/index.ts {@code getConfig} (pnpm 11.2.2) and the modules it reads the project's
 * .npmrc, pnpm-workspace.yaml and environment with, for {@code pnpm install --lockfile-only}.
 */
@Getter(AccessLevel.PACKAGE)
public final class PnpmOptions {

    private static final Pattern CAMEL_CASE = Pattern.compile("^[a-z][a-zA-Z0-9]*$");
    private static final Pattern ENV_EXPR = Pattern.compile("(?<!\\\\)(\\\\*)\\$\\{([^${}]+)}");
    private static final Pattern ENV_VALUE = Pattern.compile("([^:-]+)(:?)-(.+)");
    private static final Pattern LOWER_SNAKE_SEGMENT = Pattern.compile("^[a-z0-9]+$");
    private static final Pattern UPPER_SNAKE_SEGMENT = Pattern.compile("^[A-Z0-9]+$");
    private static final Pattern HOME_PREFIX = Pattern.compile("^~[/\\\\]");
    private static final String DEFAULT_REGISTRY = "https://registry.npmjs.org/";
    private static final List<String> CLI_OPTIONS = Collections.singletonList("lockfile-only");

    /**
     * The settings by name: the defaults, the .npmrc's, pnpm-workspace.yaml's, then the environment's.
     */
    private final Map<String, @Nullable Object> config;

    private final Set<String> explicitlySetKeys;

    private final Map<String, @Nullable Map<String, @Nullable Object>> catalogs;

    /**
     * The registry of every package ({@code default}) and of each scope that has its own, normalized.
     */
    private final Map<String, String> registries;

    /**
     * The workspace's package globs; null without a pnpm-workspace.yaml (no workspace).
     */
    private final @Nullable List<String> workspacePackagePatterns;

    /**
     * What the root manifest's devEngines.packageManager or packageManager names; null when neither names one.
     */
    private final @Nullable WantedPackageManager wantedPackageManager;

    private PnpmOptions(Map<String, @Nullable Object> config, Set<String> explicitlySetKeys,
                        Map<String, @Nullable Map<String, @Nullable Object>> catalogs, Map<String, String> registries,
                        @Nullable List<String> workspacePackagePatterns,
                        @Nullable WantedPackageManager wantedPackageManager) {
        this.config = config;
        this.explicitlySetKeys = explicitlySetKeys;
        this.catalogs = catalogs;
        this.registries = registries;
        this.workspacePackagePatterns = workspacePackagePatterns;
        this.wantedPackageManager = wantedPackageManager;
    }

    @Value
    @With
    public static class WantedPackageManager {
        Object name;

        @Nullable
        Object version;

        @Nullable
        Object onFail;

        boolean fromDevEngines;
    }

    static PnpmOptions load(@Nullable String workspaceManifest, @Nullable Map<String, @Nullable Object> rootManifest,
                            @Nullable Map<String, String> env) {
        return load(workspaceManifest, null, rootManifest, env);
    }

    static PnpmOptions load(@Nullable String workspaceManifest, @Nullable String npmrc,
                            @Nullable Map<String, @Nullable Object> rootManifest, @Nullable Map<String, String> env) {
        return load(workspaceManifest, npmrc, null, rootManifest, env);
    }

    /**
     * {@code getConfig} over the project's pnpm-workspace.yaml and .npmrc, the user's .npmrc (the host's registries),
     * each null without one, the root manifest and the environment (null when unknown).
     */
    @SuppressWarnings("unchecked")
    static PnpmOptions load(@Nullable String workspaceManifest, @Nullable String npmrc, @Nullable String userNpmrc,
                            @Nullable Map<String, @Nullable Object> rootManifest, @Nullable Map<String, String> env) {
        Map<String, @Nullable Object> defaultOptions = defaultOptions();
        Map<String, @Nullable Object> workspaceNpmrc = npmrc == null ? new LinkedHashMap<>() :
                readAndFilterNpmrc(npmrc, env);
        Map<String, @Nullable Object> userConfig = userNpmrc == null ? new LinkedHashMap<>() :
                readAndFilterNpmrc(userNpmrc, env);
        Map<String, @Nullable Object> builtin = new LinkedHashMap<>();
        builtin.put("registry", DEFAULT_REGISTRY);
        builtin.put("@jsr:registry", "https://npm.jsr.io/");
        Map<String, @Nullable Object> rawConfig = new LinkedHashMap<>();
        Map<String, @Nullable Object> mergedConfig = new LinkedHashMap<>();
        for (Map<String, @Nullable Object> source : Arrays.asList(builtin, defaultOptions, userConfig,
                workspaceNpmrc)) {
            for (String key : Js.keys(source)) {
                rawConfig.put(key, source.get(key));
                if (isNpmrcReadableKey(key)) {
                    mergedConfig.put(key, source.get(key));
                }
            }
        }

        Map<String, @Nullable Object> config = new LinkedHashMap<>();
        for (String key : Js.keys(defaultOptions)) {
            config.put(camelcase(key), defaultOptions.get(key));
        }
        for (String key : Js.keys(mergedConfig)) {
            if (ConfigTypes.TYPES.containsKey(key)) {
                config.put(camelcase(key), mergedConfig.get(key));
            }
        }
        Set<String> explicitlySetKeys = new LinkedHashSet<>();
        for (String option : CLI_OPTIONS) {
            explicitlySetKeys.add(camelcase(option));
        }

        Map<String, @Nullable Object> authConfig = pickIniConfig(rawConfig);
        Map<String, String> networkRegistries = getNetworkConfigs(authConfig);
        Map<String, String> registriesFromNpmrc = new LinkedHashMap<>();
        registriesFromNpmrc.put("default", normalizeRegistryUrl(authConfig.get("registry")));
        registriesFromNpmrc.putAll(networkRegistries);
        getDefaultCreds(authConfig);
        for (String key : Js.keys(authConfig)) {
            if (key.endsWith("tokenHelper") && !(userConfig.containsKey(key) &&
                                                 Js.strictEquals(userConfig.get(key), authConfig.get(key)))) {
                throw new PnpmError("TOKEN_HELPER_IN_PROJECT_CONFIG",
                        "tokenHelper must not be configured in project-level .npmrc",
                        "The key \"" + key + "\" was found in project config. Move it to ~/.npmrc or the global " +
                        "pnpm auth.ini.");
            }
        }
        config.put("registries", new LinkedHashMap<>(registriesFromNpmrc));
        if (rootManifest != null) {
            // the root manifest is read only now, converted as every read converts it
            Manifests.convertManifestAfterRead(rootManifest);
        }
        if (rootManifest != null && config.get("nodeVersion") == null) {
            config.put("nodeVersion", getNodeVersionFromEnginesRuntime(rootManifest));
        }

        Map<String, @Nullable Map<String, @Nullable Object>> catalogs = new LinkedHashMap<>();
        List<String> patterns = null;
        if (workspaceManifest != null) {
            Map<String, @Nullable Object> manifest = WorkspaceManifest.readWorkspaceManifest(workspaceManifest);
            patterns = workspacePackagePatterns(manifest);
            if (manifest != null) {
                Map<String, @Nullable Object> newSettings = getOptionsFromPnpmSettings(manifest, rootManifest, env);
                for (String option : CLI_OPTIONS) {
                    newSettings.put(camelcase(option), true);
                }
                for (Map.Entry<String, @Nullable Object> setting : newSettings.entrySet()) {
                    if (!CAMEL_CASE.matcher(setting.getKey()).matches()) {
                        continue;
                    }
                    config.put(setting.getKey(), setting.getValue());
                    explicitlySetKeys.add(setting.getKey());
                }
                catalogs = Catalogs.getCatalogsFromWorkspaceManifest(manifest);
            }
        }

        Map<String, String> registries = new LinkedHashMap<>(registriesFromNpmrc);
        Object workspaceRegistries = config.get("registries");
        if (workspaceRegistries instanceof Map) {
            for (String scope : Js.keys((Map<String, @Nullable Object>) workspaceRegistries)) {
                Object url = ((Map<String, @Nullable Object>) workspaceRegistries).get(scope);
                if (!(url instanceof String)) {
                    throw new CannotKnow("what pnpm makes of registries." + scope + " (" + Js.typeOf(url) +
                                         ", where pnpm reads a string)");
                }
                registries.put(scope, (String) url);
            }
        } else if (workspaceRegistries != null) {
            throw new CannotKnow("what pnpm makes of registries (" + Js.typeOf(workspaceRegistries) +
                                 ", where pnpm reads an object)");
        }
        if (!Js.truthy(registries.get("default"))) {
            registries.put("default", registriesFromNpmrc.get("default"));
        }
        for (Map.Entry<String, String> registry : registries.entrySet()) {
            registry.setValue(normalizeRegistryUrl(registry.getValue()));
        }
        if (!explicitlySetKeys.contains("registry") &&
            !registries.get("default").equals(registriesFromNpmrc.get("default"))) {
            config.put("registry", registries.get("default"));
        }

        Map<String, String> fromEnv = new LinkedHashMap<>();
        for (ConfigPair pair : parseEnvVars(ENV_TYPES, env)) {
            if (pair.value == Js.UNDEFINED) {
                continue;
            }
            if (CLI_OPTIONS.contains(kebabCase(pair.key))) {
                continue;
            }
            String previous = fromEnv.put(pair.key, pair.envKey);
            if (previous != null && !Js.strictEquals(config.get(pair.key), pair.value)) {
                throw new CannotKnow("which of " + previous + " and " + pair.envKey + " pnpm reads (the order of " +
                                     "the environment)");
            }
            config.put(pair.key, pair.value);
            explicitlySetKeys.add(pair.key);
            if ("registry".equals(pair.key)) {
                if (!(pair.value instanceof String)) {
                    throw new JsError(JsError.ERROR, "Unexpected type of registry, expecting a string but received " +
                                      Js.jsonStringify(pair.value));
                }
                registries.put("default", normalizeRegistryUrl(pair.value));
            }
        }
        config.put("registries", registries);

        if (explicitlySetKeys.contains("minimumReleaseAge") && config.get("minimumReleaseAgeStrict") == null) {
            config.put("minimumReleaseAgeStrict", true);
        }
        Object lockfile = config.get("lockfile");
        Object packageLock = config.get("packageLock");
        config.put("useLockfile", lockfile instanceof Boolean ? lockfile :
                packageLock instanceof Boolean ? packageLock : false);
        Object gitBranchLockfile = config.get("gitBranchLockfile");
        config.put("useGitBranchLockfile", gitBranchLockfile instanceof Boolean ? gitBranchLockfile : false);
        WantedPackageManager wantedPackageManager = rootManifest == null ? null :
                withPmOnFail(getWantedPackageManager(rootManifest), config.get("pmOnFail"));
        return new PnpmOptions(config, explicitlySetKeys, catalogs, registries, patterns, wantedPackageManager);
    }

    /**
     * The release getConfig wants for the root manifest {@code rootManifest}, under the {@code pmOnFail} of the
     * workspace's pnpm-workspace.yaml as pnpm reads it (null without one); null when the manifest names none.
     */
    static @Nullable WantedPackageManager wantedPackageManager(
            String rootManifest, @Nullable Map<String, @Nullable Object> workspace) {
        return withPmOnFail(getWantedPackageManager(Manifests.parse(rootManifest, "package.json")),
                workspace == null ? null : workspace.get("pmOnFail"));
    }

    private static @Nullable WantedPackageManager withPmOnFail(@Nullable WantedPackageManager pm,
                                                               @Nullable Object pmOnFail) {
        if (pm == null) {
            return null;
        }
        if (Js.truthy(pmOnFail)) {
            return pm.withOnFail(pmOnFail);
        }
        return pm.getOnFail() == null ? pm.withOnFail("download") : pm;
    }

    @SuppressWarnings("unchecked")
    static @Nullable String getNodeVersionFromEnginesRuntime(Map<String, @Nullable Object> manifest) {
        for (String enginesFieldName : new String[]{"devEngines", "engines"}) {
            Object engines = manifest.get(enginesFieldName);
            Object enginesRuntime = engines instanceof Map ? ((Map<String, @Nullable Object>) engines).get("runtime") :
                    null;
            if (enginesRuntime == null || enginesRuntime == Js.UNDEFINED) {
                continue;
            }
            List<@Nullable Object> runtimes = enginesRuntime instanceof List ? (List<@Nullable Object>) enginesRuntime :
                    Collections.singletonList(enginesRuntime);
            Object nodeRuntime = null;
            for (Object runtime : runtimes) {
                if (runtime == null || runtime == Js.UNDEFINED) {
                    throw new JsError(JsError.ERROR,
                            "Cannot read properties of " + Js.string(runtime) + " (reading 'name')");
                }
                if (runtime instanceof Map && "node".equals(((Map<String, @Nullable Object>) runtime).get("name"))) {
                    nodeRuntime = runtime;
                    break;
                }
            }
            Object version = nodeRuntime == null ? null : ((Map<String, @Nullable Object>) nodeRuntime).get("version");
            if (version == null || version == Js.UNDEFINED) {
                continue;
            }
            if (!(version instanceof String)) {
                throw new JsError(JsError.ERROR, "range.trim is not a function");
            }
            String minVersion;
            try {
                minVersion = NodeSemver.minVersion((String) version, NodeSemver.Options.DEFAULT);
            } catch (IllegalArgumentException e) {
                throw new JsError(JsError.ERROR, e.getMessage());
            }
            if (minVersion != null) {
                return minVersion;
            }
        }
        return null;
    }

    /**
     * The workspace's package patterns as getConfig takes them from a pnpm-workspace.yaml it read.
     */
    @SuppressWarnings("unchecked")
    static List<String> workspacePackagePatterns(@Nullable Map<String, @Nullable Object> manifest) {
        Object packages = manifest == null ? null : manifest.get("packages");
        return packages instanceof List ? new ArrayList<>((List<String>) packages) : Collections.singletonList(".");
    }

    static @Nullable WantedPackageManager getWantedPackageManager(Map<String, @Nullable Object> manifest) {
        WantedPackageManager pmFromDevEngines = parseDevEnginesPackageManager(manifest.get("devEngines"));
        if (pmFromDevEngines != null) {
            Object version = pmFromDevEngines.getVersion();
            if (version != null && !(version instanceof String &&
                                     NodeSemver.validRange((String) version, NodeSemver.Options.DEFAULT) != null)) {
                pmFromDevEngines = pmFromDevEngines.withVersion(null);
            }
            return pmFromDevEngines.withFromDevEngines(true);
        }
        Object packageManager = manifest.get("packageManager");
        if (!Js.truthy(packageManager)) {
            return null;
        }
        if (!(packageManager instanceof String)) {
            throw new CannotKnow("what pnpm makes of packageManager (" + Js.typeOf(packageManager) +
                                 ", where pnpm reads a string)");
        }
        String[] pm = parsePackageManager((String) packageManager);
        String version = pm[1];
        if (version != null) {
            String cleanVersion = NodeSemver.valid(version, NodeSemver.Options.DEFAULT);
            if (cleanVersion == null || !cleanVersion.equals(version)) {
                version = null;
            }
        }
        return new WantedPackageManager(pm[0], version, null, false);
    }

    /**
     * {@code parsePackageManager}: the name and version of {@code <name>@<version>[+<hash>]}.
     */
    static @Nullable String[] parsePackageManager(String packageManager) {
        if (!packageManager.contains("@")) {
            return new String[]{packageManager, null};
        }
        String[] parts = packageManager.split("@", -1);
        String pmReference = parts[1];
        if (pmReference.contains(":")) {
            return new String[]{parts[0], null};
        }
        return new String[]{parts[0], pmReference.split("\\+", -1)[0]};
    }

    @SuppressWarnings("unchecked")
    private static @Nullable WantedPackageManager parseDevEnginesPackageManager(@Nullable Object devEngines) {
        Object packageManager = devEngines instanceof Map ?
                ((Map<String, @Nullable Object>) devEngines).get("packageManager") : null;
        if (!Js.truthy(packageManager)) {
            return null;
        }
        Object pmEngine;
        Object onFail;
        if (packageManager instanceof List) {
            List<@Nullable Object> engines = (List<@Nullable Object>) packageManager;
            if (engines.isEmpty()) {
                return null;
            }
            int pnpmIndex = -1;
            for (int i = 0; i < engines.size() && pnpmIndex == -1; i++) {
                if ("pnpm".equals(property(engines.get(i), "name"))) {
                    pnpmIndex = i;
                }
            }
            if (pnpmIndex != -1) {
                pmEngine = engines.get(pnpmIndex);
                Object own = property(pmEngine, "onFail");
                onFail = own != null ? own : pnpmIndex == engines.size() - 1 ? "error" : "ignore";
            } else {
                pmEngine = engines.get(0);
                Object own = property(engines.get(engines.size() - 1), "onFail");
                onFail = own != null ? own : "error";
            }
        } else {
            pmEngine = packageManager;
            onFail = property(pmEngine, "onFail");
        }
        Object name = pmEngine instanceof Map ? ((Map<String, @Nullable Object>) pmEngine).get("name") : null;
        if (!Js.truthy(name)) {
            return null;
        }
        return new WantedPackageManager(name, property(pmEngine, "version"), onFail, false);
    }

    // value.key where pnpm reads a property: undefined on a primitive, a TypeError on null or undefined
    @SuppressWarnings("unchecked")
    private static @Nullable Object property(@Nullable Object value, String key) {
        if (value == null || value == Js.UNDEFINED) {
            throw new JsError(JsError.ERROR,
                    "Cannot read properties of " + Js.string(value) + " (reading '" + key + "')");
        }
        Object property = value instanceof Map ? ((Map<String, @Nullable Object>) value).get(key) : null;
        return property == Js.UNDEFINED ? null : property;
    }

    // config/reader's defaultOptions, by pnpm's own (kebab-case) names, as far as a lock-only install reads them
    private static Map<String, @Nullable Object> defaultOptions() {
        Map<String, @Nullable Object> config = new LinkedHashMap<>();
        config.put("auto-install-peers", true);
        config.put("bail", true);
        config.put("dedupe-peer-dependents", true);
        config.put("dedupe-peers", false);
        config.put("dedupe-direct-deps", false);
        config.put("dedupe-injected-deps", true);
        config.put("disallow-workspace-cycles", false);
        config.put("enable-modules-dir", true);
        config.put("exclude-links-from-lockfile", false);
        config.put("git-branch-lockfile", false);
        config.put("optional", true);
        config.put("inject-workspace-packages", false);
        config.put("link-workspace-packages", false);
        config.put("lockfile-include-tarball-url", false);
        config.put("minimum-release-age", 1440.0);
        config.put("minimum-release-age-ignore-missing-time", true);
        config.put("package-lock", true);
        config.put("prefer-workspace-packages", false);
        config.put("registry", DEFAULT_REGISTRY);
        config.put("block-exotic-subdeps", true);
        config.put("resolution-mode", "highest");
        config.put("resolve-peers-from-workspace-root", true);
        config.put("save-workspace-protocol", "rolling");
        config.put("shared-workspace-lockfile", true);
        config.put("strict-peer-dependencies", false);
        config.put("registry-supports-time-field", false);
        config.put("peers-suffix-max-length", 1000.0);
        config.put("virtual-store-only", false);
        // extendInstallOptions' defaults where config/reader sets none
        config.put("auto-install-peers-from-highest-match", false);
        config.put("engine-strict", false);
        config.put("ignore-compatibility-db", false);
        config.put("prefer-frozen-lockfile", true);
        return config;
    }

    // loadNpmrcFiles' readAndFilterNpmrc over read-ini-file 5 (ini 6 after strip-bom)
    private static Map<String, @Nullable Object> readAndFilterNpmrc(String text, @Nullable Map<String, String> env) {
        Map<String, @Nullable Object> result = new LinkedHashMap<>();
        Map<String, @Nullable Object> raw;
        try {
            raw = Ini.decode(StripBom.stripBom(text), Ini.Release.V6_0_0);
        } catch (IllegalArgumentException e) {
            // pnpm warns of the file and reads nothing from it
            return result;
        }
        for (String rawKey : Js.keys(raw)) {
            Object rawValue = raw.get(rawKey);
            String key = envReplaceLossy(rawKey, env);
            Object value = rawValue;
            if (rawValue instanceof String) {
                // a credential or network value reaches no lock, so an environment the host does not know is no matter
                value = env == null && !readByALockOnlyInstall(key) ? rawValue :
                        envReplaceLossy((String) rawValue, env);
            }
            if (isNpmrcReadableKey(key)) {
                result.put(key, value);
            }
        }
        return result;
    }

    private static final Pattern UNREAD_VALUE = Pattern.compile(
            "(?:^|:)(?:_authToken|email|ca|cert|key|https-proxy|proxy|no-proxy|http-proxy|local-address|" +
            "strict-ssl)$");

    private static boolean readByALockOnlyInstall(String key) {
        return !UNREAD_VALUE.matcher(key).find();
    }

    private static final List<String> NPM_AUTH_SETTINGS = Arrays.asList("ca", "cafile", "cert", "key", "registry",
            "_auth", "_authToken", "_password", "email", "keyfile", "username");
    private static final List<String> NETWORK_INI_KEYS = Arrays.asList("https-proxy", "proxy", "no-proxy",
            "http-proxy", "local-address", "strict-ssl");

    static boolean isIniConfigKey(String key) {
        return key.startsWith("@") || key.startsWith("//") || NPM_AUTH_SETTINGS.contains(key);
    }

    static boolean isNpmrcReadableKey(String key) {
        return isIniConfigKey(key) || NETWORK_INI_KEYS.contains(key);
    }

    private static Map<String, @Nullable Object> pickIniConfig(Map<String, @Nullable Object> rawConfig) {
        Map<String, @Nullable Object> result = new LinkedHashMap<>();
        for (String key : Js.keys(rawConfig)) {
            if (isIniConfigKey(key)) {
                result.put(key, rawConfig.get(key));
            }
        }
        return result;
    }

    private static final Pattern AUTH_SUFFIX = Pattern.compile(":(_auth|_authToken|_password|username|tokenHelper)$");
    private static final Pattern SSL_FILE_SUFFIX = Pattern.compile(":(cert|key|ca)file$");
    private static final Map<String, String> AUTH_SUFFIX_KEY_MAP = new LinkedHashMap<>();

    static {
        AUTH_SUFFIX_KEY_MAP.put("_auth", "authPairBase64");
        AUTH_SUFFIX_KEY_MAP.put("_authToken", "authToken");
        AUTH_SUFFIX_KEY_MAP.put("_password", "authPassword");
        AUTH_SUFFIX_KEY_MAP.put("username", "authUsername");
        AUTH_SUFFIX_KEY_MAP.put("tokenHelper", "tokenHelper");
    }

    /**
     * getNetworkConfigs: the scoped registries, after the checks pnpm makes of each registry's credentials; the
     * credentials themselves and the certificates are the host's.
     */
    static Map<String, String> getNetworkConfigs(Map<String, @Nullable Object> rawConfig) {
        Map<String, String> registries = new LinkedHashMap<>();
        Map<String, Map<String, String>> rawCredsMap = new LinkedHashMap<>();
        for (String configKey : Js.keys(rawConfig)) {
            Object value = rawConfig.get(configKey);
            if (!configKey.isEmpty() && configKey.charAt(0) == '@' && configKey.endsWith(":registry")) {
                registries.put(configKey.substring(0, configKey.indexOf(':')), normalizeRegistryUrl(value));
                continue;
            }
            Matcher creds = AUTH_SUFFIX.matcher(configKey);
            if (creds.find()) {
                rawCredsMap.computeIfAbsent(configKey.substring(0, creds.start()), r -> new LinkedHashMap<>())
                        .put(AUTH_SUFFIX_KEY_MAP.get(creds.group(1)), Js.string(value));
                continue;
            }
            if (SSL_FILE_SUFFIX.matcher(configKey).find()) {
                throw new CannotKnow("whether " + Js.string(value) + " exists (pnpm reads the .npmrc's " +
                                     configKey + ")");
            }
        }
        for (Map<String, String> creds : rawCredsMap.values()) {
            parseCreds(creds);
        }
        return registries;
    }

    private static void getDefaultCreds(Map<String, @Nullable Object> rawConfig) {
        Map<String, String> input = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : AUTH_SUFFIX_KEY_MAP.entrySet()) {
            Object value = rawConfig.get(entry.getKey());
            if (value != null) {
                input.put(entry.getValue(), Js.string(value));
            }
        }
        parseCreds(input);
    }

    // parseCreds' checks: what it throws for a token helper, _auth or _password pnpm cannot use
    static void parseCreds(Map<String, String> input) {
        String tokenHelper = input.get("tokenHelper");
        if (tokenHelper != null && !tokenHelper.isEmpty()) {
            for (char c : Js.trim(tokenHelper).toCharArray()) {
                if ("$%`\"'".indexOf(c) >= 0) {
                    String hint = "Try wrapping the current command in a script whose name does not contain " +
                                  "unsupported characters";
                    if (c == '"' || c == '\'') {
                        hint = "pnpm does not support quotations in tokenHelper. " + hint;
                    } else if (c == '$' || c == '%') {
                        hint = "pnpm does not support environment variables. " + hint;
                    }
                    throw new PnpmError("TOKEN_HELPER_UNSUPPORTED_CHARACTER",
                            "Unexpected character " + Js.jsonStringify(String.valueOf(c)), hint);
                }
            }
        }
        String authPairBase64 = input.get("authPairBase64");
        if (authPairBase64 != null && !authPairBase64.isEmpty()) {
            if (decodeBase64Credential(authPairBase64, "_auth").indexOf(':') < 0) {
                throw new PnpmError("AUTH_MISSING_SEPARATOR", "No separator found in the decoded form of _auth",
                        "_auth is a base64 encoded form of <username>:<password> where the colon (:) serves as the " +
                        "separator");
            }
            return;
        }
        String authUsername = input.get("authUsername");
        String authPassword = input.get("authPassword");
        if (authUsername != null && !authUsername.isEmpty() && authPassword != null && !authPassword.isEmpty()) {
            decodeBase64Credential(authPassword, "_password");
        }
    }

    private static String decodeBase64Credential(String value, String key) {
        String decoded = atob(value);
        if (decoded == null) {
            String normalizedValue = normalizeBase64Padding(value);
            if (!normalizedValue.equals(value)) {
                decoded = atob(normalizedValue);
            }
        }
        if (decoded == null) {
            throw new PnpmError("AUTH_INVALID_BASE64", "Failed to decode " + key + " as base64",
                    key + " must contain a base64-encoded " + ("_auth".equals(key) ? "<username>:<password>" :
                            "password") + " value");
        }
        return decoded;
    }

    private static String normalizeBase64Padding(String value) {
        int paddingStart = value.length();
        while (paddingStart > 0 && value.charAt(paddingStart - 1) == '=') {
            paddingStart--;
        }
        String valueWithoutPadding = value.substring(0, paddingStart);
        if (valueWithoutPadding.isEmpty()) {
            return value;
        }
        int remainder = valueWithoutPadding.length() % 4;
        if (remainder == 1) {
            return value;
        }
        StringBuilder padded = new StringBuilder(valueWithoutPadding);
        for (int i = 0; i < (4 - remainder) % 4; i++) {
            padded.append('=');
        }
        return padded.toString();
    }

    private static final String BASE64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    // the HTML standard's forgiving-base64 decode, which atob runs; null where atob throws
    private static @Nullable String atob(String data) {
        StringBuilder s = new StringBuilder();
        for (char c : data.toCharArray()) {
            if (c != '\t' && c != '\n' && c != '\f' && c != '\r' && c != ' ') {
                s.append(c);
            }
        }
        if (s.length() % 4 == 0) {
            if (s.length() >= 2 && s.charAt(s.length() - 1) == '=' && s.charAt(s.length() - 2) == '=') {
                s.setLength(s.length() - 2);
            } else if (s.length() >= 1 && s.charAt(s.length() - 1) == '=') {
                s.setLength(s.length() - 1);
            }
        }
        if (s.length() % 4 == 1) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (int i = 0; i < s.length(); i++) {
            int v = BASE64.indexOf(s.charAt(i));
            if (v < 0) {
                return null;
            }
            buffer = buffer << 6 | v;
            bits += 6;
            if (bits >= 8) {
                bits -= 8;
                out.append((char) (buffer >> bits & 0xFF));
            }
        }
        return out.toString();
    }

    /**
     * normalize-registry-url 2.0.1.
     */
    static String normalizeRegistryUrl(@Nullable Object value) {
        if (!(value instanceof String)) {
            throw new JsError(JsError.ERROR, "`registry` should be a string");
        }
        String registry = (String) value;
        WhatwgUrl url = WhatwgUrl.tryParse(registry);
        if (url != null) {
            registry = url.toString();
        }
        if (registry.endsWith("/") || registry.indexOf('/', registry.indexOf("//") + 2) != -1) {
            return registry;
        }
        return registry + "/";
    }

    static final class ConfigPair {
        final String envKey;
        final String key;
        final @Nullable Object value;

        ConfigPair(String envKey, String key, @Nullable Object value) {
            this.envKey = envKey;
            this.key = key;
            this.value = value;
        }
    }

    // the types config/reader parses the environment with: all but those whose schema its parser cannot read
    private static final Function<String, @Nullable Object> ENV_TYPES = key ->
            Arrays.asList("init-version", "node-version", "umask").contains(key) ? null : ConfigTypes.TYPES.get(key);

    /**
     * src/env.ts {@code parseEnvVars}: the {@code pnpm_config_} variables by camelCase key, {@link Js#UNDEFINED}
     * where the schema does not parse one.
     */
    static List<ConfigPair> parseEnvVars(Function<String, @Nullable Object> getSchema,
                                         @Nullable Map<String, String> env) {
        List<ConfigPair> pairs = new ArrayList<>();
        if (env == null) {
            return pairs;
        }
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String suffix = getEnvKeySuffix(entry.getKey());
            if (suffix == null || entry.getValue() == null) {
                continue;
            }
            String schemaKey = suffix.replace('_', '-');
            Object schema = getSchema.apply(schemaKey);
            if (schema == null) {
                continue;
            }
            pairs.add(new ConfigPair(entry.getKey(), camelcase(schemaKey),
                    parseValueBySchema(schema, entry.getValue(), env)));
        }
        return pairs;
    }

    private static @Nullable Object parseValueBySchema(Object schema, String envVar, Map<String, String> env) {
        if (schema instanceof List) {
            List<?> variants = (List<?>) schema;
            List<Object> sorted = new ArrayList<>();
            for (Object variant : variants) {
                if (variant != ConfigTypes.Type.STRING) {
                    sorted.add(variant);
                }
            }
            if (variants.contains(ConfigTypes.Type.STRING)) {
                sorted.add(ConfigTypes.Type.STRING);
            }
            for (Object variant : sorted) {
                Object value;
                if (variant instanceof String) {
                    value = envVar.equals(variant) ? variant : Js.UNDEFINED;
                } else if (variant instanceof Boolean) {
                    value = variant.toString().equals(envVar) ? variant : Js.UNDEFINED;
                } else if (variant == ConfigTypes.NULL) {
                    value = "null".equals(envVar) ? null : Js.UNDEFINED;
                } else {
                    value = parseValueByType((ConfigTypes.Type) variant, envVar, env);
                }
                if (value != Js.UNDEFINED) {
                    return value;
                }
            }
            return Js.UNDEFINED;
        }
        return parseValueByType((ConfigTypes.Type) schema, envVar, env);
    }

    private static @Nullable Object parseValueByType(ConfigTypes.Type type, String envVar, Map<String, String> env) {
        switch (type) {
            case ARRAY: {
                Object value;
                try {
                    value = Manifests.parseValue(envVar, "the environment");
                } catch (PnpmError e) {
                    return Js.UNDEFINED;
                }
                return value instanceof List ? value : Js.UNDEFINED;
            }
            case BOOLEAN:
                return "true".equals(envVar) ? Boolean.TRUE : "false".equals(envVar) ? Boolean.FALSE : Js.UNDEFINED;
            case NUMBER: {
                double value = Js.toNumber(envVar);
                return Double.isNaN(value) ? Js.UNDEFINED : value;
            }
            case STRING:
                return envVar;
            case PATH: {
                String home = env.get("HOME");
                Matcher m = HOME_PREFIX.matcher(envVar);
                if (home != null && !home.isEmpty() && m.find()) {
                    return PosixPath.join(home, envVar.substring(m.end()));
                }
                return envVar;
            }
            case URL: {
                WhatwgUrl url = WhatwgUrl.tryParse(envVar);
                if (url == null) {
                    throw new JsError(JsError.ERROR, "Invalid URL");
                }
                return url.toString();
            }
            default:
                return Js.UNDEFINED;
        }
    }

    private static @Nullable String getEnvKeySuffix(String envKey) {
        if (envKey.startsWith("pnpm_config_")) {
            String suffix = envKey.substring("pnpm_config_".length());
            return isSnakeCase(suffix, LOWER_SNAKE_SEGMENT) ? suffix : null;
        }
        if (envKey.startsWith("PNPM_CONFIG_")) {
            String suffix = envKey.substring("PNPM_CONFIG_".length());
            return isSnakeCase(suffix, UPPER_SNAKE_SEGMENT) ? suffix.toLowerCase(Locale.ROOT) : null;
        }
        return null;
    }

    private static boolean isSnakeCase(String s, Pattern segment) {
        if (s.isEmpty()) {
            return false;
        }
        for (String part : s.split("_", -1)) {
            if (!segment.matcher(part).matches()) {
                return false;
            }
        }
        return true;
    }

    // camelcase 6 of a kebab-case name of lowercase letters
    private static String camelcase(String kebab) {
        StringBuilder out = new StringBuilder();
        boolean upper = false;
        for (char c : kebab.toCharArray()) {
            if (c == '-') {
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return out.toString();
    }

    private static String kebabCase(String camel) {
        StringBuilder out = new StringBuilder();
        for (char c : camel.toCharArray()) {
            if (Character.isUpperCase(c)) {
                out.append('-').append(Character.toLowerCase(c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> getOptionsFromPnpmSettings(
            Map<String, @Nullable Object> pnpmSettings, @Nullable Map<String, @Nullable Object> manifest,
            @Nullable Map<String, String> env) {
        Map<String, @Nullable Object> settings = new LinkedHashMap<>();
        for (String key : Js.keys(pnpmSettings)) {
            Object value = pnpmSettings.get(key);
            String newKey = envReplace(key, env);
            if (value instanceof String) {
                settings.put(newKey, envReplace((String) value, env));
            } else if ("registries".equals(newKey) || "namedRegistries".equals(newKey)) {
                if (value instanceof Map) {
                    Map<String, @Nullable Object> out = new LinkedHashMap<>();
                    for (String k : Js.keys((Map<String, @Nullable Object>) value)) {
                        Object v = ((Map<String, @Nullable Object>) value).get(k);
                        out.put(k, v instanceof String ? envReplace((String) v, env) : v);
                    }
                    settings.put(newKey, out);
                } else {
                    settings.put(newKey, value);
                }
            } else {
                settings.put(newKey, value);
            }
        }
        if (settings.containsKey("overrides") && Js.truthy(settings.get("overrides"))) {
            Object overrides = settings.get("overrides");
            assertValidOverrides(overrides);
            Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) overrides;
            if (map.isEmpty()) {
                settings.remove("overrides");
            } else if (manifest != null) {
                Map<String, @Nullable Object> replaced = new LinkedHashMap<>();
                for (String selector : Js.keys(map)) {
                    replaced.put(selector, replaceVersionReferences(manifest, (String) map.get(selector)));
                }
                settings.put("overrides", replaced);
            }
        }
        return settings;
    }

    private static void assertValidOverrides(@Nullable Object overrides) {
        if (!(overrides instanceof Map)) {
            throw new PnpmError("INVALID_OVERRIDES", "The overrides field should be an object, but got " +
                                                     renderReceivedType(overrides));
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) overrides).entrySet()) {
            Object spec = entry.getValue();
            if (!(spec instanceof String)) {
                throw new PnpmError("INVALID_OVERRIDES", "The value of overrides." + entry.getKey() +
                                                         " should be a string, but got " + renderReceivedType(spec));
            }
        }
    }

    private static String renderReceivedType(@Nullable Object value) {
        return value == null ? "null" : value instanceof List ? "array" : Js.typeOf(value);
    }

    @SuppressWarnings("unchecked")
    private static String replaceVersionReferences(Map<String, @Nullable Object> manifest, String spec) {
        if (spec.isEmpty() || spec.charAt(0) != '$') {
            return spec;
        }
        Map<String, @Nullable Object> allDeps = new LinkedHashMap<>();
        for (String field : new String[]{"devDependencies", "dependencies", "optionalDependencies"}) {
            Object deps = manifest.get(field);
            if (deps instanceof Map) {
                allDeps.putAll((Map<String, @Nullable Object>) deps);
            }
        }
        String dependencyName = spec.substring(1);
        Object newSpec = allDeps.get(dependencyName);
        if (Js.truthy(newSpec)) {
            return Js.string(newSpec);
        }
        throw new PnpmError("CANNOT_RESOLVE_OVERRIDE_VERSION", "Cannot resolve version " + spec +
                " in overrides. The direct dependencies don't have dependency \"" + dependencyName + "\".");
    }

    /**
     * @pnpm/config.env-replace 4.1.0 {@code envReplace}.
     */
    static String envReplace(String settingValue, @Nullable Map<String, String> env) {
        return replaceWith(settingValue, env, orig -> {
            throw new JsError(JsError.ERROR, "Failed to replace env in config: " + orig);
        });
    }

    /**
     * @pnpm/config.env-replace 4.1.0 {@code envReplaceLossy}: an unresolved placeholder is dropped.
     */
    static String envReplaceLossy(String settingValue, @Nullable Map<String, String> env) {
        return replaceWith(settingValue, env, orig -> "");
    }

    private static String replaceWith(String settingValue, @Nullable Map<String, String> env,
                                      Function<String, String> onUnresolved) {
        Matcher m = ENV_EXPR.matcher(settingValue);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            String orig = m.group();
            String escape = m.group(1);
            String name = m.group(2);
            String replacement;
            if (escape.length() % 2 == 1) {
                replacement = orig.substring((escape.length() + 1) / 2);
            } else {
                String halfEscape = escape.substring(escape.length() / 2);
                if (env == null) {
                    throw new CannotKnow("the environment (" + orig + " in the configuration)");
                }
                String envValue = getEnvValue(env, name);
                replacement = halfEscape + (envValue == null ? onUnresolved.apply(orig) : envValue);
            }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static @Nullable String getEnvValue(Map<String, String> env, String name) {
        Matcher matched = ENV_VALUE.matcher(name);
        if (!matched.find()) {
            return env.get(name);
        }
        String v = env.get(matched.group(1));
        if (v == null) {
            return matched.group(3);
        }
        return v.isEmpty() && !matched.group(2).isEmpty() ? matched.group(3) : v;
    }

    @Nullable
    Object get(String name) {
        return config.get(name);
    }

    boolean isTrue(String name) {
        return Js.truthy(config.get(name));
    }

    /**
     * Whether pnpm-workspace.yaml or the environment sets the setting.
     */
    boolean isExplicitlySet(String name) {
        return explicitlySetKeys.contains(name) && !CLI_OPTIONS.contains(kebabCase(name));
    }

    /**
     * The overrides, their {@code $name} references replaced by the root manifest's specifiers.
     */
    @SuppressWarnings("unchecked")
    Map<String, String> getOverrides() {
        Object overrides = config.get("overrides");
        Map<String, String> out = new LinkedHashMap<>();
        if (overrides instanceof Map) {
            for (String key : Js.keys((Map<String, @Nullable Object>) overrides)) {
                out.put(key, (String) ((Map<String, @Nullable Object>) overrides).get(key));
            }
        }
        return out;
    }

    /**
     * installDeps' {@code linkWorkspacePackagesDepth}: {@code deep} is any depth, true the importers', else -1.
     */
    double getLinkWorkspacePackagesDepth() {
        Object link = config.get("linkWorkspacePackages");
        return "deep".equals(link) ? Double.POSITIVE_INFINITY : Js.truthy(link) ? 0 : -1;
    }

    /**
     * config.lockfileDir, from the directory pnpm runs in: pnpm-workspace.yaml's (here only that directory), else the
     * workspace's when it shares one lock; null when each project installs into its own.
     */
    @Nullable
    String getLockfileDir(String dir) {
        Object lockfileDir = config.get("lockfileDir");
        if (!Js.truthy(lockfileDir)) {
            return isTrue("sharedWorkspaceLockfile") ? "" : null;
        }
        if (!(lockfileDir instanceof String)) {
            throw new CannotKnow("what pnpm makes of lockfileDir (" + Js.typeOf(lockfileDir) +
                                 ", where pnpm reads a path)");
        }
        String path = (String) lockfileDir;
        if (path.startsWith("/") || path.matches("^[A-Za-z]:.*") ||
            !RootRelativePath.join(dir, path).equals(dir)) {
            throw new CannotKnow("the lock pnpm writes in lockfileDir " + path + " (a path from the directory " +
                                 "pnpm runs in, here " + (dir.isEmpty() ? "the root" : dir) + ")");
        }
        return dir;
    }

    /**
     * extendInstallOptions' tag, the dist-tag a bare specifier resolves to.
     */
    String getTag() {
        Object tag = config.get("tag");
        if (tag == null || tag == Js.UNDEFINED) {
            return "latest";
        }
        if (!(tag instanceof String) || ((String) tag).isEmpty()) {
            throw new CannotKnow("what pnpm makes of tag (" + Js.jsonStringify(tag) + ", where pnpm reads a dist-tag)");
        }
        return (String) tag;
    }

    /**
     * The install command's getFetchFullMetadata.ts: {@code supportedArchitectures.libc && true}.
     */
    @Nullable
    Object getFetchFullMetadata() {
        Map<String, @Nullable Object> supportedArchitectures = getSupportedArchitectures();
        Object libc = supportedArchitectures == null ? null : supportedArchitectures.get("libc");
        return Js.truthy(libc) ? Boolean.TRUE : libc;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    Map<String, @Nullable Object> getSupportedArchitectures() {
        Object supportedArchitectures = config.get("supportedArchitectures");
        return supportedArchitectures instanceof Map ? (Map<String, @Nullable Object>) supportedArchitectures : null;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    Map<String, @Nullable Object> getNodeDownloadMirrors() {
        Object mirrors = config.get("nodeDownloadMirrors");
        return mirrors instanceof Map ? (Map<String, @Nullable Object>) mirrors : null;
    }

    /**
     * The nodeVersion setting in whatever type pnpm-workspace.yaml gives it; null for the host's Node.js.
     */
    @Nullable
    Object getNodeVersion() {
        Object nodeVersion = config.get("nodeVersion");
        return nodeVersion == Js.UNDEFINED ? null : nodeVersion;
    }

    @SuppressWarnings("unchecked")
    @Nullable
    Map<String, @Nullable Object> getNamedRegistries() {
        Object value = config.get("namedRegistries");
        if (value == null || value == Js.UNDEFINED) {
            return null;
        }
        if (!(value instanceof Map)) {
            throw new CannotKnow("what pnpm makes of namedRegistries (" + Js.typeOf(value) +
                                 ", where pnpm reads an object)");
        }
        return (Map<String, @Nullable Object>) value;
    }

    List<String> getNamedRegistryPrefixes() {
        List<String> namedRegistryPrefixes = new ArrayList<>();
        namedRegistryPrefixes.add("gh:");
        Object named = config.get("namedRegistries");
        if (named instanceof Map) {
            for (Object key : ((Map<?, ?>) named).keySet()) {
                if (!"gh".equals(key)) {
                    namedRegistryPrefixes.add(key + ":");
                }
            }
        }
        return namedRegistryPrefixes;
    }

    @Nullable
    Double getNumber(String name) {
        Object value = config.get(name);
        if (value == null || value == Js.UNDEFINED) {
            return null;
        }
        if (!(value instanceof Number)) {
            throw new CannotKnow("what pnpm makes of " + name + " (" + Js.typeOf(value) +
                                 ", where pnpm reads a number)");
        }
        return ((Number) value).doubleValue();
    }

    @Nullable
    List<String> getStrings(String name) {
        return strings(config.get(name), name);
    }

    private static @Nullable List<String> strings(@Nullable Object value, String name) {
        if (value == null || value == Js.UNDEFINED) {
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
    @Nullable
    List<@Nullable Object> getIgnoredOptionalDependencies() {
        Object value = config.get("ignoredOptionalDependencies");
        if (value == null || value == Js.UNDEFINED) {
            return null;
        }
        if (!(value instanceof List)) {
            throw new CannotKnow("what pnpm makes of ignoredOptionalDependencies (" + Js.typeOf(value) +
                                 ", where pnpm reads an array)");
        }
        return (List<@Nullable Object>) value;
    }

    @SuppressWarnings("unchecked")
    ReportPeerDependencyIssues.@Nullable PeerDependencyRules getPeerDependencyRules() {
        Object rules = config.get("peerDependencyRules");
        if (rules == null || rules == Js.UNDEFINED) {
            return null;
        }
        if (!(rules instanceof Map)) {
            throw new CannotKnow("what pnpm makes of peerDependencyRules (" + Js.typeOf(rules) +
                                 ", where pnpm reads an object)");
        }
        Map<String, @Nullable Object> map = (Map<String, @Nullable Object>) rules;
        Map<String, String> allowedVersions = null;
        Object allowed = map.get("allowedVersions");
        if (allowed != null && allowed != Js.UNDEFINED) {
            if (!(allowed instanceof Map)) {
                throw new CannotKnow("what pnpm makes of peerDependencyRules.allowedVersions (" + Js.typeOf(allowed) +
                                     ", where pnpm reads an object)");
            }
            allowedVersions = new LinkedHashMap<>();
            for (String key : Js.keys((Map<String, @Nullable Object>) allowed)) {
                Object range = ((Map<String, @Nullable Object>) allowed).get(key);
                if (!(range instanceof String)) {
                    throw new CannotKnow("what pnpm makes of peerDependencyRules.allowedVersions." + key + " (" +
                                         Js.typeOf(range) + ", where pnpm reads a string)");
                }
                allowedVersions.put(key, (String) range);
            }
        }
        return new ReportPeerDependencyIssues.PeerDependencyRules(
                strings(map.get("ignoreMissing"), "peerDependencyRules.ignoreMissing"),
                strings(map.get("allowAny"), "peerDependencyRules.allowAny"), allowedVersions);
    }

    /**
     * The settings object pnpm sets on the lock, in its key order.
     */
    Map<String, @Nullable Object> getLockfileSettings() {
        Map<String, @Nullable Object> settings = new LinkedHashMap<>();
        settings.put("autoInstallPeers", undefinedIfAbsent(config.get("autoInstallPeers")));
        Object dedupePeers = config.get("dedupePeers");
        settings.put("dedupePeers", Js.truthy(dedupePeers) ? dedupePeers : Js.UNDEFINED);
        settings.put("excludeLinksFromLockfile", undefinedIfAbsent(config.get("excludeLinksFromLockfile")));
        settings.put("peersSuffixMaxLength", undefinedIfAbsent(config.get("peersSuffixMaxLength")));
        settings.put("injectWorkspacePackages", undefinedIfAbsent(config.get("injectWorkspacePackages")));
        return settings;
    }

    private static Object undefinedIfAbsent(@Nullable Object value) {
        return value == null ? Js.UNDEFINED : value;
    }
}
