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
import org.openrewrite.javascript.internal.lock.EcmaScript;
import org.openrewrite.javascript.internal.lock.Ini;
import org.openrewrite.javascript.internal.lock.JsError;
import org.openrewrite.javascript.internal.lock.PosixPath;
import org.openrewrite.javascript.internal.lock.RootRelativePath;
import org.openrewrite.javascript.internal.lock.StripBom;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;
import org.openrewrite.javascript.semver.NodeSemver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
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
     * 11.5.3's {@code packageManagerRegistries}: the registries of the user's .npmrc and the environment alone, which
     * pnpm resolves itself from; null where a release before 11.5.3 could not read them.
     */
    private final @Nullable Map<String, String> packageManagerRegistries;

    /**
     * The workspace's package globs; null without a pnpm-workspace.yaml (no workspace).
     */
    private final @Nullable List<String> workspacePackagePatterns;

    /**
     * What the root manifest's devEngines.packageManager or packageManager names; null when neither names one.
     */
    private final @Nullable WantedPackageManager wantedPackageManager;

    /**
     * {@code packageManager.version}, which the install gives its resolver as {@code pnpmVersion}: the release whose
     * config this is, the one ported until pnpm hands the install to another ({@link #withPackageManagerVersion}).
     */
    private final String packageManagerVersion;

    /**
     * The release whose config this is, which the ported modules ask what it does ({@link PnpmRelease}).
     */
    private final PnpmRelease release;

    private PnpmOptions(Map<String, @Nullable Object> config, Set<String> explicitlySetKeys,
                        Map<String, @Nullable Map<String, @Nullable Object>> catalogs, Map<String, String> registries,
                        @Nullable Map<String, String> packageManagerRegistries,
                        @Nullable List<String> workspacePackagePatterns,
                        @Nullable WantedPackageManager wantedPackageManager, String packageManagerVersion,
                        PnpmRelease release) {
        this.config = config;
        this.explicitlySetKeys = explicitlySetKeys;
        this.catalogs = catalogs;
        this.registries = registries;
        this.packageManagerRegistries = packageManagerRegistries;
        this.workspacePackagePatterns = workspacePackagePatterns;
        this.wantedPackageManager = wantedPackageManager;
        this.packageManagerVersion = packageManagerVersion;
        this.release = release;
    }

    /**
     * The config the release that installs reads from the same files, {@code version} its
     * {@code packageManager.version}: {@link Main#checkWantedPackageManager}'s, a release or a range of them.
     */
    PnpmOptions withPackageManagerVersion(String version) {
        return new PnpmOptions(config, explicitlySetKeys, catalogs, registries, packageManagerRegistries,
                workspacePackagePatterns, wantedPackageManager, version, release);
    }

    /**
     * The config of {@code release}, the one that installs ({@link Main#installs}).
     */
    PnpmOptions withRelease(PnpmRelease release) {
        return new PnpmOptions(config, explicitlySetKeys, catalogs, registries, packageManagerRegistries,
                workspacePackagePatterns, wantedPackageManager, packageManagerVersion, release);
    }

    /**
     * The config pnpm resolves and installs itself with, {@code running} the release that runs: from 11.5.3 its
     * registries are those of the user's .npmrc and the environment alone (getPackageManagerBootstrapConfig), the
     * project's .npmrc and pnpm-workspace.yaml left out (PnpmRelease#resolvesThePackageManagerFromTrustedConfig),
     * asked where the two differ.
     */
    PnpmOptions packageManagerBootstrap(PnpmRelease running) {
        if (packageManagerRegistries == null || packageManagerRegistries.equals(registries) ||
            !running.resolvesThePackageManagerFromTrustedConfig()) {
            return this;
        }
        Map<String, @Nullable Object> bootstrapConfig = new LinkedHashMap<>(config);
        bootstrapConfig.put("registries", packageManagerRegistries);
        return new PnpmOptions(bootstrapConfig, explicitlySetKeys, catalogs, packageManagerRegistries,
                packageManagerRegistries, workspacePackagePatterns, wantedPackageManager, packageManagerVersion,
                release);
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
     * {@code getConfig} as 11.2.2 reads it.
     */
    static PnpmOptions load(@Nullable String workspaceManifest, @Nullable String npmrc, @Nullable String userNpmrc,
                            @Nullable Map<String, @Nullable Object> rootManifest, @Nullable Map<String, String> env) {
        return load(workspaceManifest, npmrc, userNpmrc, rootManifest, env, PnpmRelease.PORTED);
    }

    /**
     * {@code getConfig} of {@code release} over the project's pnpm-workspace.yaml and .npmrc, the user's .npmrc (the
     * host's registries), each null without one, the root manifest and the environment (null when unknown).
     */
    @SuppressWarnings("unchecked")
    static PnpmOptions load(@Nullable String workspaceManifest, @Nullable String npmrc, @Nullable String userNpmrc,
                            @Nullable Map<String, @Nullable Object> rootManifest, @Nullable Map<String, String> env,
                            PnpmRelease release) {
        Map<String, @Nullable Object> defaultOptions = defaultOptions();
        Map<String, @Nullable Object> workspaceNpmrc = npmrc == null ? new LinkedHashMap<>() :
                readAndFilterNpmrc(npmrc, env, release, true);
        requireTheUserNpmrcAtHome(env, release);
        Map<String, @Nullable Object> userConfig = userNpmrc == null ? new LinkedHashMap<>() :
                readAndFilterNpmrc(userNpmrc, env, release, false);
        Map<String, @Nullable Object> builtin = new LinkedHashMap<>();
        builtin.put("registry", DEFAULT_REGISTRY);
        builtin.put("@jsr:registry", "https://npm.jsr.io/");
        // from 11.6.0 the environment's npm_config_//… and pnpm_config_//… variables follow the project's .npmrc
        // (PnpmRelease#readsUrlScopedConfigFromTheEnvironment)
        Map<String, @Nullable Object> envScopedConfig = readUrlScopedEnvConfig(env);
        if (!envScopedConfig.isEmpty() && !release.readsUrlScopedConfigFromTheEnvironment()) {
            envScopedConfig = new LinkedHashMap<>();
        }
        // from 11.10.0 pnpm_config__auth's tokens follow them (PnpmRelease#readsTheAuthSetting)
        JsonAuth jsonAuth = readJsonAuthEnv(env, release);
        Map<String, @Nullable Object> rawConfig = new LinkedHashMap<>();
        Map<String, @Nullable Object> mergedConfig = new LinkedHashMap<>();
        for (Map<String, @Nullable Object> source : Arrays.asList(builtin, defaultOptions, userConfig,
                workspaceNpmrc, envScopedConfig, jsonAuth.auth)) {
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
        Map<String, String> networkRegistries = getNetworkConfigs(authConfig, release);
        Map<String, String> registriesFromNpmrc = new LinkedHashMap<>();
        registriesFromNpmrc.put("default", normalizeRegistryUrl(authConfig.get("registry")));
        registriesFromNpmrc.putAll(networkRegistries);
        Map<String, String> packageManagerRegistries = trustedRegistries(builtin, defaultOptions, userConfig,
                envScopedConfig, jsonAuth, release);
        if (holdsUnscopedCreds(authConfig) && !release.rescopesUnscopedCredentials()) {
            getDefaultCreds(authConfig, release);
        }
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
            Manifests.convertManifestAfterRead(rootManifest, release);
        }
        if (rootManifest != null && config.get("nodeVersion") == null) {
            config.put("nodeVersion", getNodeVersionFromEnginesRuntime(rootManifest, release));
        }

        Map<String, @Nullable Map<String, @Nullable Object>> catalogs = new LinkedHashMap<>();
        List<String> patterns = null;
        if (workspaceManifest != null) {
            Map<String, @Nullable Object> manifest = WorkspaceManifest.readWorkspaceManifest(workspaceManifest,
                    release);
            patterns = workspacePackagePatterns(manifest);
            if (manifest != null) {
                Map<String, @Nullable Object> newSettings = getOptionsFromPnpmSettings(manifest, rootManifest, env,
                        release);
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
        // from 11.10.0 pnpm_config__auth's routes win over the workspace's (PnpmRelease#readsTheAuthSetting)
        registries.putAll(jsonAuth.registries);
        // from 11.10.0 the registry setting (pnpm-workspace.yaml's) is the default registry, which before it is not
        // (PnpmRelease#takesTheRegistrySettingAsTheDefault), asked where it is another
        Object registrySetting = config.get("registry");
        if (explicitlySetKeys.contains("registry") && registrySetting instanceof String) {
            String setting = normalizeRegistryUrl(registrySetting);
            String current = Js.truthy(registries.get("default")) ? registries.get("default") :
                    registriesFromNpmrc.get("default");
            if (!setting.equals(normalizeRegistryUrl(current)) && release.takesTheRegistrySettingAsTheDefault()) {
                registries.put("default", setting);
            }
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
        for (ConfigPair pair : parseEnvVars(ENV_TYPES, env, release)) {
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
                    throw new JsError(JsError.TYPE_ERROR, "Unexpected type of registry, expecting a string but " +
                                      "received " + Js.jsonStringify(pair.value));
                }
                registries.put("default", normalizeRegistryUrl(pair.value));
                if (packageManagerRegistries != null) {
                    packageManagerRegistries.put("default", normalizeRegistryUrl(pair.value));
                }
            }
        }
        config.put("registries", registries);

        if (explicitlySetKeys.contains("minimumReleaseAge") && config.get("minimumReleaseAgeStrict") == null &&
            release.defaultsMinimumReleaseAgeStrict()) {
            config.put("minimumReleaseAgeStrict", true);
        }
        Object lockfile = config.get("lockfile");
        Object packageLock = config.get("packageLock");
        config.put("useLockfile", lockfile instanceof Boolean ? lockfile :
                packageLock instanceof Boolean ? packageLock : false);
        Object gitBranchLockfile = config.get("gitBranchLockfile");
        config.put("useGitBranchLockfile", gitBranchLockfile instanceof Boolean ? gitBranchLockfile : false);
        WantedPackageManager wantedPackageManager = rootManifest == null ? null :
                withPmOnFail(getWantedPackageManager(rootManifest, release), config.get("pmOnFail"));
        return new PnpmOptions(config, explicitlySetKeys, catalogs, registries, packageManagerRegistries, patterns,
                wantedPackageManager, release.toString(), release);
    }

    /**
     * getConfig's {@code packageManagerRegistries} of 11.5.3, from its {@code trustedConfig}: pnpm's built-in config,
     * the defaults and the user's .npmrc (the port reads no auth.ini, and no command line names a registry), with
     * the checks getNetworkConfigs makes of their credentials; null where a release before 11.5.3, which reads none,
     * would fail those checks.
     */
    private static @Nullable Map<String, String> trustedRegistries(Map<String, @Nullable Object> builtin,
                                                                   Map<String, @Nullable Object> defaultOptions,
                                                                   Map<String, @Nullable Object> userConfig,
                                                                   Map<String, @Nullable Object> envScopedConfig,
                                                                   JsonAuth jsonAuth, PnpmRelease release) {
        Map<String, @Nullable Object> trustedConfig = new LinkedHashMap<>();
        for (Map<String, @Nullable Object> source : Arrays.asList(builtin, defaultOptions, userConfig,
                envScopedConfig, jsonAuth.auth)) {
            for (String key : Js.keys(source)) {
                if (isNpmrcReadableKey(key)) {
                    trustedConfig.put(key, source.get(key));
                }
            }
        }
        Map<String, @Nullable Object> trustedAuthConfig = pickIniConfig(trustedConfig);
        Map<String, String> trustedNetworkRegistries;
        try {
            trustedNetworkRegistries = getNetworkConfigs(trustedAuthConfig, release);
        } catch (PnpmError e) {
            if (release.resolvesThePackageManagerFromTrustedConfig()) {
                throw e;
            }
            return null;
        }
        Map<String, String> packageManagerRegistries = new LinkedHashMap<>();
        packageManagerRegistries.put("default", normalizeRegistryUrl(trustedAuthConfig.get("registry")));
        packageManagerRegistries.putAll(trustedNetworkRegistries);
        // from 11.10.0 pnpm_config__auth's routes too (PnpmRelease#readsTheAuthSetting)
        packageManagerRegistries.putAll(jsonAuth.registries);
        return packageManagerRegistries;
    }

    /**
     * loadNpmrcFiles' {@code jsonAuth} of 11.10.0: the {@code //…:_authToken} keys the {@code _auth} setting gives,
     * after the environment's URL-scoped config, and the registry it routes each scope to.
     */
    private static final class JsonAuth {
        static final JsonAuth NONE = new JsonAuth(Collections.emptyMap(), Collections.emptyMap());

        final Map<String, @Nullable Object> auth;
        final Map<String, String> registries;

        JsonAuth(Map<String, @Nullable Object> auth, Map<String, String> registries) {
            this.auth = auth;
            this.registries = registries;
        }
    }

    /**
     * loadNpmrcFiles' {@code readJsonAuthEnv} of 11.10.0: {@code pnpm_config__auth}, else {@code PNPM_CONFIG__AUTH},
     * where set and not empty, a JSON object of http(s) registry URLs, each an object of scopes ({@code @} the
     * default, {@code @org}) with an {@code authToken} alone, else {@code ERR_PNPM_INVALID_AUTH_SETTING}; before,
     * pnpm reads no such setting ({@link PnpmRelease#readsTheAuthSetting}). The global config.yaml's {@code _auth},
     * which the port reads no global config of, is none.
     */
    @SuppressWarnings("unchecked")
    private static JsonAuth readJsonAuthEnv(@Nullable Map<String, String> env, PnpmRelease release) {
        String value = env == null ? null : env.get("pnpm_config__auth") != null &&
                                            !env.get("pnpm_config__auth").isEmpty() ? env.get("pnpm_config__auth") :
                env.get("PNPM_CONFIG__AUTH") != null && !env.get("PNPM_CONFIG__AUTH").isEmpty() ?
                        env.get("PNPM_CONFIG__AUTH") : null;
        if (value == null || !release.readsTheAuthSetting()) {
            return JsonAuth.NONE;
        }
        String source = "pnpm_config__auth";
        Object parsed;
        try {
            parsed = EcmaScript.jsonParse(value);
        } catch (EcmaScript.JsonSyntaxError e) {
            throw new PnpmError("INVALID_AUTH_SETTING", "Failed to parse " + source + " as JSON: " + e.getMessage());
        }
        if (!(parsed instanceof Map)) {
            throw new PnpmError("INVALID_AUTH_SETTING", source + " must be a JSON object");
        }
        Map<String, @Nullable Object> auth = new LinkedHashMap<>();
        Map<String, String> registries = new LinkedHashMap<>();
        int entryNumber = 0;
        for (String url : Js.keys((Map<String, @Nullable Object>) parsed)) {
            Object scopes = ((Map<String, @Nullable Object>) parsed).get(url);
            String label = jsonAuthRegistryLabel(url, ++entryNumber);
            String[] registry = parseJsonAuthRegistry(url, label, source);
            if (!(scopes instanceof Map)) {
                throw new PnpmError("INVALID_AUTH_SETTING", source + "[" + label + "] must be an object keyed by " +
                                                            "scope");
            }
            for (String scope : Js.keys((Map<String, @Nullable Object>) scopes)) {
                Object creds = ((Map<String, @Nullable Object>) scopes).get(scope);
                String at = source + "[" + label + "][" + Js.jsonStringify(scope) + "]";
                if (!("@".equals(scope) || scope.startsWith("@") && scope.length() > 1 && !scope.contains("/") &&
                                           !scope.contains(":"))) {
                    throw new PnpmError("INVALID_AUTH_SETTING", at + ": scope must be \"@\" or a package scope like " +
                                                                "\"@org\"");
                }
                if (!(creds instanceof Map)) {
                    throw new PnpmError("INVALID_AUTH_SETTING", at + " must be an auth object");
                }
                for (String field : Js.keys((Map<String, @Nullable Object>) creds)) {
                    if (!"authToken".equals(field)) {
                        throw new PnpmError("INVALID_AUTH_SETTING", at + "[" + Js.jsonStringify(field) + "]: " +
                                                                    "unsupported auth field (only \"authToken\" is " +
                                                                    "supported)");
                    }
                }
                Object token = ((Map<String, @Nullable Object>) creds).get("authToken");
                if (!(token instanceof String)) {
                    throw new PnpmError("INVALID_AUTH_SETTING", at + ": \"authToken\" must be a string");
                }
                auth.put(registry[1] + ":" + ("@".equals(scope) ? "" : scope + ":") + "_authToken", token);
                registries.put("@".equals(scope) ? "default" : scope, registry[0]);
            }
        }
        return new JsonAuth(auth, registries);
    }

    // jsonAuthRegistryLabel: the entry's number, and its http(s) URL's origin where it has one
    private static String jsonAuthRegistryLabel(String url, int entryNumber) {
        String entryLabel = "entry " + entryNumber;
        WhatwgUrl parsed = WhatwgUrl.tryParse(url);
        if (parsed != null && ("https:".equals(parsed.getProtocol()) || "http:".equals(parsed.getProtocol())) &&
            !parsed.getHostname().isEmpty()) {
            return entryLabel + " (" + parsed.getProtocol() + "//" + parsed.getHost() + ")";
        }
        return entryLabel;
    }

    // parseJsonAuthRegistry: the registry URL normalized, and its nerf dart
    private static String[] parseJsonAuthRegistry(String url, String label, String source) {
        WhatwgUrl parsed = WhatwgUrl.tryParse(url);
        String notARegistry = source + "[" + label + "]: key must be an http(s) registry URL";
        if (parsed == null || !"https:".equals(parsed.getProtocol()) && !"http:".equals(parsed.getProtocol()) ||
            parsed.getHostname().isEmpty()) {
            throw new PnpmError("INVALID_AUTH_SETTING", notARegistry);
        }
        if (!parsed.getUsername().isEmpty() || !parsed.getPassword().isEmpty() || !parsed.getSearch().isEmpty() ||
            !parsed.getHash().isEmpty()) {
            throw new PnpmError("INVALID_AUTH_SETTING", source + "[" + label + "]: registry URL must not include " +
                                                        "credentials, query, or fragment");
        }
        String normalized = normalizeRegistryUrl(parsed.getHref());
        String nerfed = nerfDart(normalized);
        if (nerfed == null || nerfed.isEmpty()) {
            throw new PnpmError("INVALID_AUTH_SETTING", notARegistry);
        }
        return new String[]{normalized, nerfed};
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

    static @Nullable String getNodeVersionFromEnginesRuntime(Map<String, @Nullable Object> manifest) {
        return getNodeVersionFromEnginesRuntime(manifest, PnpmRelease.PORTED);
    }

    /**
     * {@code getNodeVersionFromEnginesRuntime} as {@code release} reads it: from 11.4.0 a version that is no valid
     * range is skipped ({@link PnpmRelease#skipsAnInvalidNodeRuntimeRange}).
     */
    @SuppressWarnings("unchecked")
    static @Nullable String getNodeVersionFromEnginesRuntime(Map<String, @Nullable Object> manifest,
                                                             PnpmRelease release) {
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
                // from 11.4.0 semver.validRange swallows the TypeError and the runtime is skipped
                if (release.skipsAnInvalidNodeRuntimeRange()) {
                    continue;
                }
                throw new JsError(JsError.ERROR, "range.trim is not a function");
            }
            String range = (String) version;
            if (release.underSemver(semver -> NodeSemver.validRange(range,
                    NodeSemver.Options.DEFAULT.release(semver))) == null && release.skipsAnInvalidNodeRuntimeRange()) {
                continue;
            }
            String minVersion;
            try {
                minVersion = release.underSemver(semver -> NodeSemver.minVersion(range,
                        NodeSemver.Options.DEFAULT.release(semver)));
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
        return getWantedPackageManager(manifest, PnpmRelease.PORTED);
    }

    static @Nullable WantedPackageManager getWantedPackageManager(Map<String, @Nullable Object> manifest,
                                                                  PnpmRelease release) {
        WantedPackageManager pmFromDevEngines = parseDevEnginesPackageManager(manifest.get("devEngines"), release);
        if (pmFromDevEngines != null) {
            Object version = pmFromDevEngines.getVersion();
            if (version != null && !(version instanceof String &&
                                     release.underSemver(semver -> NodeSemver.validRange((String) version,
                                             NodeSemver.Options.DEFAULT.release(semver))) != null)) {
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
        // from 11.8.0 split at the first @ after a scope's, everything after it the reference
        // (PnpmRelease#splitsThePackageManagerAtItsVersionsAt); before, at each @
        String[] old = parsePackageManager((String) packageManager);
        String[] split = splitPackageManager((String) packageManager);
        String[] pm = Arrays.equals(old, split) || !release.splitsThePackageManagerAtItsVersionsAt() ? old : split;
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
     * 11.8.0's {@code parsePackageManager}: the name before the first {@code @} after a scope's, the version after it
     * up to a {@code +}.
     */
    static @Nullable String[] splitPackageManager(String packageManager) {
        int separatorIndex = packageManager.startsWith("@") ? packageManager.indexOf('@', 1) :
                packageManager.indexOf('@');
        if (separatorIndex == -1) {
            return new String[]{packageManager, null};
        }
        String name = packageManager.substring(0, separatorIndex);
        String pmReference = packageManager.substring(separatorIndex + 1);
        if (pmReference.contains(":")) {
            return new String[]{name, null};
        }
        int hashIndex = pmReference.indexOf('+');
        return new String[]{name, hashIndex == -1 ? pmReference : pmReference.substring(0, hashIndex)};
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

    // devEngines.packageManager as one object takes onFail from pmOnFail from 11.1.3, "error" before
    // (PnpmRelease#takesPmOnFailForDevEnginesWithoutOnFail)
    @SuppressWarnings("unchecked")
    private static @Nullable WantedPackageManager parseDevEnginesPackageManager(@Nullable Object devEngines,
                                                                                PnpmRelease release) {
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
            if (onFail == null && !release.takesPmOnFailForDevEnginesWithoutOnFail()) {
                onFail = "error";
            }
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

    /**
     * getConfig's {@code npmrcAuthFile}, the user .npmrc loadNpmrcConfig reads in place of ~/.npmrc where no command
     * line names one: from 11.0.6 the file {@code pnpm_config_npmrc_auth_file}, else {@code pnpm_config_userconfig},
     * names ({@link PnpmRelease#readsTheUserNpmrcTheEnvironmentNames}), and after them and the global config.yaml's
     * npmrcAuthFile (the port reads no global config.yaml) from 11.0.9 the one {@code npm_config_userconfig} names
     * ({@link PnpmRelease#readsTheUserNpmrcNpmConfigUserconfigNames}), each read in its upper-case form too. The host
     * gives ~/.npmrc alone; an environment it does not know names none.
     *
     * @throws CannotKnow where the environment names the file pnpm reads
     */
    private static void requireTheUserNpmrcAtHome(@Nullable Map<String, String> env, PnpmRelease release) {
        if (env == null) {
            return;
        }
        String variable = envVarOf(env, "pnpm_config_", "npmrc_auth_file");
        if (variable == null) {
            variable = envVarOf(env, "pnpm_config_", "userconfig");
        }
        if (variable == null || !release.readsTheUserNpmrcTheEnvironmentNames()) {
            variable = envVarOf(env, "npm_config_", "userconfig");
            if (variable != null && !release.readsTheUserNpmrcNpmConfigUserconfigNames()) {
                variable = null;
            }
        }
        if (variable != null) {
            throw new CannotKnow("the .npmrc " + env.get(variable) + " pnpm reads as the user's, which " + variable +
                                 " names in place of ~/.npmrc");
        }
    }

    // the variable readEnvVar (pnpm_config_) or readNpmEnvVar (npm_config_) reads key from: <prefix><key>, else its
    // upper-case form where that one is unset; none where the value read is empty
    private static @Nullable String envVarOf(Map<String, String> env, String prefix, String key) {
        String variable = prefix + key;
        if (env.get(variable) == null) {
            variable = variable.toUpperCase(Locale.ROOT);
        }
        String value = env.get(variable);
        return value == null || value.isEmpty() ? null : variable;
    }

    // loadNpmrcFiles' readAndFilterNpmrc over read-ini-file 5 (ini 6 after strip-bom); from 11.4.0 it pins the
    // file's unscoped credentials to the file's registry (PnpmRelease#rescopesUnscopedCredentials); from 11.5.3 the
    // project's own file (repositoryControlled) expands no environment variable in a request destination or a
    // credential, and leaves such an entry out (PnpmRelease#ignoresTheEnvironmentInRepositoryRequestDestinations)
    private static Map<String, @Nullable Object> readAndFilterNpmrc(String text, @Nullable Map<String, String> env,
                                                                   PnpmRelease release,
                                                                   boolean repositoryControlled) {
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
            boolean keyFromEnv = repositoryControlled && hasEnvPlaceholder(rawKey);
            if (keyFromEnv && (isRequestDestinationKey(rawKey) || isAuthValueKey(rawKey)) &&
                leavesOutAnEntryFromTheEnvironment(rawKey, release)) {
                continue;
            }
            String key = substituteEnv(rawKey, env, release);
            if (keyFromEnv && (isRequestDestinationKey(key) || isAuthValueKey(key)) &&
                leavesOutAnEntryFromTheEnvironment(key, release)) {
                continue;
            }
            Object value = rawValue;
            if (rawValue instanceof String) {
                if (repositoryControlled && hasEnvPlaceholder((String) rawValue) &&
                    (isRequestDestinationValueKey(key) || isAuthValueKey(key)) &&
                    leavesOutAnEntryFromTheEnvironment(key, release)) {
                    continue;
                }
                // a credential or network value reaches no lock, so an environment the host does not know is no matter
                value = env == null && !readByALockOnlyInstall(key) ? rawValue :
                        substituteEnv((String) rawValue, env, release);
            }
            if (isNpmrcReadableKey(key)) {
                result.put(key, value);
            }
        }
        return holdsUnscopedCreds(result) && release.rescopesUnscopedCredentials() ? rescopeUnscopedCreds(result) :
                result;
    }

    // loadNpmrcFiles' substituteEnv: from 11.1.3 envReplaceLossy, which drops a placeholder the environment does not
    // set; before, envReplace, whose failure keeps the value as written, every placeholder in it
    // (PnpmRelease#dropsAnUnresolvedNpmrcPlaceholder)
    private static String substituteEnv(String value, @Nullable Map<String, String> env, PnpmRelease release) {
        String lossy = envReplaceLossy(value, env);
        String kept;
        try {
            kept = envReplace(value, env);
        } catch (JsError e) {
            kept = value;
        }
        return lossy.equals(kept) || release.dropsAnUnresolvedNpmrcPlaceholder() ? lossy : kept;
    }

    private static final Pattern URL_SCOPED_ENV = Pattern.compile("^p?npm_config_(//.+)$", Pattern.CASE_INSENSITIVE);

    /**
     * loadNpmrcFiles' {@code readUrlScopedEnvConfig} of 11.6.0: each set {@code npm_config_//…} and
     * {@code pnpm_config_//…} variable as its {@code //…} key (a tokenHelper none), the pnpm one winning; nothing
     * from an environment the host does not know.
     */
    static Map<String, @Nullable Object> readUrlScopedEnvConfig(@Nullable Map<String, String> env) {
        Map<String, @Nullable Object> npmScoped = new LinkedHashMap<>();
        Map<String, @Nullable Object> pnpmScoped = new LinkedHashMap<>();
        if (env == null) {
            return npmScoped;
        }
        Map<String, String> envKeys = new LinkedHashMap<>();
        for (Map.Entry<String, String> variable : env.entrySet()) {
            String value = variable.getValue();
            if (value == null || value.isEmpty()) {
                continue;
            }
            Matcher match = URL_SCOPED_ENV.matcher(variable.getKey());
            if (!match.matches() || match.group(1).endsWith(":tokenHelper")) {
                continue;
            }
            boolean pnpm = variable.getKey().substring(0, 5).toLowerCase(Locale.ROOT).equals("pnpm_");
            Map<String, @Nullable Object> target = pnpm ? pnpmScoped : npmScoped;
            String key = match.group(1);
            String previous = envKeys.put((pnpm ? "pnpm " : "npm ") + key, variable.getKey());
            if (previous != null && !value.equals(target.get(key))) {
                throw new CannotKnow("which of " + previous + " and " + variable.getKey() + " pnpm reads (the " +
                                     "order of the environment)");
            }
            target.put(key, value);
        }
        Map<String, @Nullable Object> scoped = new LinkedHashMap<>(npmScoped);
        scoped.putAll(pnpmScoped);
        return scoped;
    }

    // an entry a lock-only install does not read ends the same kept or left out, so the release is asked of the others
    private static boolean leavesOutAnEntryFromTheEnvironment(String key, PnpmRelease release) {
        return !readByALockOnlyInstall(key) || release.ignoresTheEnvironmentInRepositoryRequestDestinations();
    }

    private static final Pattern ENV_PLACEHOLDER = Pattern.compile("\\$\\{[^}]+}");

    private static final List<String> AUTH_VALUE_KEYS = Arrays.asList("_authToken", "_auth", "_password",
            "username", "tokenHelper", "cert", "key");

    static boolean hasEnvPlaceholder(String value) {
        return ENV_PLACEHOLDER.matcher(value).find();
    }

    private static boolean isRequestDestinationKey(String key) {
        return isRegistryKey(key) || key.startsWith("//");
    }

    private static boolean isRequestDestinationValueKey(String key) {
        return isRegistryKey(key) || "https-proxy".equals(key) || "http-proxy".equals(key) || "proxy".equals(key);
    }

    private static boolean isRegistryKey(String key) {
        return "registry".equals(key) || key.startsWith("@") && key.endsWith(":registry");
    }

    private static boolean isAuthValueKey(String key) {
        if (AUTH_VALUE_KEYS.contains(key)) {
            return true;
        }
        for (String authKey : AUTH_VALUE_KEYS) {
            if (key.endsWith(":" + authKey)) {
                return true;
            }
        }
        return false;
    }

    private static boolean holdsUnscopedCreds(Map<String, @Nullable Object> source) {
        for (String key : UNSCOPED_RESCOPABLE_KEYS) {
            if (source.containsKey(key)) {
                return true;
            }
        }
        return false;
    }

    private static final List<String> UNSCOPED_RESCOPABLE_KEYS = Arrays.asList("_authToken", "_auth", "username",
            "_password", "tokenHelper", "cert", "key");

    /**
     * loadNpmrcFiles.ts' {@code rescopeUnscopedCreds} (11.4.0): each unscoped credential of a source moves to the
     * nerf-dart of the source's registry, else npm's, unless the source scopes one there already; with a registry that
     * is no URL, pnpm drops them.
     */
    static Map<String, @Nullable Object> rescopeUnscopedCreds(Map<String, @Nullable Object> source) {
        if (!holdsUnscopedCreds(source)) {
            return source;
        }
        Object rawRegistry = source.get("registry");
        String fallbackRegistry = rawRegistry instanceof String && !((String) rawRegistry).isEmpty() ?
                (String) rawRegistry : DEFAULT_REGISTRY;
        String nerfedRegistry = nerfDart(normalizeRegistryUrl(fallbackRegistry));
        for (String key : UNSCOPED_RESCOPABLE_KEYS) {
            if (!source.containsKey(key)) {
                continue;
            }
            if (nerfedRegistry != null) {
                String scopedKey = nerfedRegistry + ":" + key;
                if (!source.containsKey(scopedKey)) {
                    source.put(scopedKey, source.get(key));
                }
            }
            source.remove(key);
        }
        return source;
    }

    // @pnpm/config.nerf-dart 1.0.1; null where new URL() throws
    private static @Nullable String nerfDart(String url) {
        WhatwgUrl parsed = WhatwgUrl.tryParse(url);
        WhatwgUrl rel = parsed == null ? null :
                WhatwgUrl.tryParse(".", parsed.getProtocol() + "//" + parsed.getHost() + parsed.getPathname());
        return rel == null ? null : "//" + rel.getHost() + rel.getPathname();
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
    static Map<String, String> getNetworkConfigs(Map<String, @Nullable Object> rawConfig, PnpmRelease release) {
        Map<String, String> registries = new LinkedHashMap<>();
        // the credentials of each registry key as written, and from 11.7.0 of each registry and scope, whether the
        // scope follows the registry's URL ({@code //h/@s/:}) or a colon ({@code //h/:@s:})
        Map<String, Map<String, String>> rawCredsMap = new LinkedHashMap<>();
        Map<String, Map<String, Map<String, String>>> rawCredsByScope = new LinkedHashMap<>();
        for (String configKey : Js.keys(rawConfig)) {
            Object value = rawConfig.get(configKey);
            if (!configKey.isEmpty() && configKey.charAt(0) == '@' && configKey.endsWith(":registry")) {
                registries.put(configKey.substring(0, configKey.indexOf(':')), normalizeRegistryUrl(value));
                continue;
            }
            Matcher creds = AUTH_SUFFIX.matcher(configKey);
            if (creds.find()) {
                String registry = configKey.substring(0, creds.start());
                rawCredsMap.computeIfAbsent(registry, r -> new LinkedHashMap<>())
                        .put(AUTH_SUFFIX_KEY_MAP.get(creds.group(1)), Js.string(value));
                String[] registryAndScope = registryAndScope(registry);
                rawCredsByScope.computeIfAbsent(registryAndScope[0], r -> new LinkedHashMap<>())
                        .computeIfAbsent(registryAndScope[1], s -> new LinkedHashMap<>())
                        .put(AUTH_SUFFIX_KEY_MAP.get(creds.group(1)), Js.string(value));
                continue;
            }
            if (SSL_FILE_SUFFIX.matcher(configKey).find()) {
                throw new CannotKnow("whether " + Js.string(value) + " exists (pnpm reads the .npmrc's " +
                                     configKey + ")");
            }
        }
        RuntimeException byKey = firstCredsFailure(rawCredsMap.values(), release);
        List<Map<String, String>> scopedCreds = new ArrayList<>();
        for (Map<String, Map<String, String>> byRegistry : rawCredsByScope.values()) {
            scopedCreds.addAll(byRegistry.values());
        }
        RuntimeException byScope = firstCredsFailure(scopedCreds, release);
        RuntimeException failure = sameFailure(byKey, byScope) ||
                                   !release.groupsCredentialsByRegistryAndScope() ? byKey : byScope;
        if (failure != null) {
            throw failure;
        }
        return registries;
    }

    // parseCreds of each group in turn: the first failure, null where none fails
    private static @Nullable RuntimeException firstCredsFailure(Collection<Map<String, String>> groups,
                                                                PnpmRelease release) {
        try {
            for (Map<String, String> creds : groups) {
                parseCreds(creds, release);
            }
            return null;
        } catch (PnpmError | JsError e) {
            return e;
        }
    }

    private static boolean sameFailure(@Nullable RuntimeException a, @Nullable RuntimeException b) {
        if (a == null || b == null) {
            return a == b;
        }
        return a.getClass() == b.getClass() && Objects.equals(a.getMessage(), b.getMessage()) &&
               (!(a instanceof PnpmError) || ((PnpmError) a).getCode().equals(((PnpmError) b).getCode()));
    }

    /**
     * getNetworkConfigs.ts' splitScopeFromRegistry of 11.7.0, which a credential's registry is grouped by: the
     * registry and its scope, {@code @} for none.
     */
    static String[] registryAndScope(String registry) {
        if (registry.startsWith("//")) {
            int scopeSeparatorIndex = registry.lastIndexOf(":@");
            if (scopeSeparatorIndex != -1 && isPackageScope(registry.substring(scopeSeparatorIndex + 1))) {
                String host = registry.substring(0, scopeSeparatorIndex);
                return new String[]{host.endsWith("/") ? host : host + "/",
                        registry.substring(scopeSeparatorIndex + 1)};
            }
            String trimmed = registry.endsWith("/") ? registry.substring(0, registry.length() - 1) : registry;
            int lastSlashIndex = trimmed.lastIndexOf('/');
            if (lastSlashIndex != -1 && isPackageScope(trimmed.substring(lastSlashIndex + 1))) {
                return new String[]{trimmed.substring(0, lastSlashIndex + 1), trimmed.substring(lastSlashIndex + 1)};
            }
        }
        return new String[]{registry, "@"};
    }

    private static boolean isPackageScope(String scope) {
        return scope.startsWith("@") && scope.length() > 1 && !scope.contains("/") && !scope.contains(":");
    }

    private static void getDefaultCreds(Map<String, @Nullable Object> rawConfig, PnpmRelease release) {
        Map<String, String> input = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : AUTH_SUFFIX_KEY_MAP.entrySet()) {
            Object value = rawConfig.get(entry.getKey());
            if (value != null) {
                input.put(entry.getValue(), Js.string(value));
            }
        }
        parseCreds(input, release);
    }

    // parseCreds' checks: what it throws for a token helper, _auth or _password pnpm cannot use
    static void parseCreds(Map<String, String> input) {
        parseCreds(input, PnpmRelease.PORTED);
    }

    static void parseCreds(Map<String, String> input, PnpmRelease release) {
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
            if (decodeBase64Credential(authPairBase64, "_auth", release).indexOf(':') < 0) {
                throw new PnpmError("AUTH_MISSING_SEPARATOR", "No separator found in the decoded form of _auth",
                        "_auth is a base64 encoded form of <username>:<password> where the colon (:) serves as the " +
                        "separator");
            }
            return;
        }
        String authUsername = input.get("authUsername");
        String authPassword = input.get("authPassword");
        if (authUsername != null && !authUsername.isEmpty() && authPassword != null && !authPassword.isEmpty()) {
            decodeBase64Credential(authPassword, "_password", release);
        }
    }

    // parseCreds' decodeBase64Credential of 11.1.3, its padding mended before it fails; before, atob alone, whose
    // InvalidCharacterError pnpm does not catch (PnpmRelease#decodesACredentialForgivingItsPadding)
    private static String decodeBase64Credential(String value, String key, PnpmRelease release) {
        String decoded = atob(value);
        if (decoded == null && !release.decodesACredentialForgivingItsPadding()) {
            throw new JsError("InvalidCharacterError", atobError(value));
        }
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
    /**
     * The message of the InvalidCharacterError Node's {@code atob} throws for {@code input}, which it does not decode.
     */
    static String atobError(String input) {
        int nonAsciiWhitespaceCharCount = 0;
        int equalCharCount = 0;
        for (char c : input.toCharArray()) {
            if (c == '\t' || c == '\n' || c == '\f' || c == '\r' || c == ' ') {
                continue;
            }
            if (c != '=' && BASE64.indexOf(c) < 0) {
                return "Invalid character";
            }
            nonAsciiWhitespaceCharCount++;
            if (c == '=') {
                equalCharCount++;
            } else if (equalCharCount > 0) {
                return "Invalid character";
            }
            if (equalCharCount > 2) {
                return "Invalid character";
            }
        }
        int reminder = nonAsciiWhitespaceCharCount % 4;
        if (reminder == 0) {
            reminder = (nonAsciiWhitespaceCharCount - equalCharCount) % 4;
        } else if (equalCharCount > 0) {
            return "Invalid character";
        }
        return reminder == 1 ? "The string to be decoded is not correctly encoded." : "Invalid character";
    }

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
        return parseEnvVars(getSchema, env, PnpmRelease.PORTED);
    }

    /**
     * {@link #parseEnvVars(Function, Map)} as {@code release} reads the environment: the {@code PNPM_CONFIG_} variables
     * from 11.0.6 on ({@link PnpmRelease#readsUpperCaseConfigVariables}).
     */
    static List<ConfigPair> parseEnvVars(Function<String, @Nullable Object> getSchema,
                                         @Nullable Map<String, String> env, PnpmRelease release) {
        List<ConfigPair> pairs = new ArrayList<>();
        if (env == null) {
            return pairs;
        }
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String suffix = getEnvKeySuffix(entry.getKey());
            if (suffix != null && entry.getKey().startsWith("PNPM_CONFIG_") &&
                !release.readsUpperCaseConfigVariables()) {
                continue;
            }
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

    static Map<String, @Nullable Object> getOptionsFromPnpmSettings(
            Map<String, @Nullable Object> pnpmSettings, @Nullable Map<String, @Nullable Object> manifest,
            @Nullable Map<String, String> env) {
        return getOptionsFromPnpmSettings(pnpmSettings, manifest, env, PnpmRelease.PORTED);
    }

    /**
     * getOptionsFromRootManifest.ts' {@code getOptionsFromPnpmSettings} as {@code release} reads the settings: the
     * URLs of {@code registries} and {@code namedRegistries} take the environment from 11.1.0
     * ({@link PnpmRelease#readsNamedRegistries}).
     */
    @SuppressWarnings("unchecked")
    static Map<String, @Nullable Object> getOptionsFromPnpmSettings(
            Map<String, @Nullable Object> pnpmSettings, @Nullable Map<String, @Nullable Object> manifest,
            @Nullable Map<String, String> env, PnpmRelease release) {
        Map<String, @Nullable Object> settings = new LinkedHashMap<>();
        for (String key : Js.keys(pnpmSettings)) {
            Object value = pnpmSettings.get(key);
            String newKey = envReplace(key, env);
            if (value instanceof String) {
                // from 11.5.3 the project's registry and pnprServer take nothing from the environment: such a value
                // is left out (PnpmRelease#ignoresTheEnvironmentInRepositoryRequestDestinations)
                if (("registry".equals(newKey) || "pnprServer".equals(newKey)) && hasEnvPlaceholder((String) value) &&
                    release.ignoresTheEnvironmentInRepositoryRequestDestinations()) {
                    continue;
                }
                settings.put(newKey, envReplace((String) value, env));
            } else if (("registries".equals(newKey) || "namedRegistries".equals(newKey)) &&
                       holdsAnEnvReference(value) && release.readsNamedRegistries()) {
                if (value instanceof Map) {
                    // and leave out an entry that would
                    boolean leavesOut = holdsAnEnvPlaceholder((Map<String, @Nullable Object>) value) &&
                                        release.ignoresTheEnvironmentInRepositoryRequestDestinations();
                    Map<String, @Nullable Object> out = new LinkedHashMap<>();
                    for (String k : Js.keys((Map<String, @Nullable Object>) value)) {
                        Object v = ((Map<String, @Nullable Object>) value).get(k);
                        if (leavesOut && v instanceof String && hasEnvPlaceholder((String) v)) {
                            continue;
                        }
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
            try {
                assertValidOverrides(overrides);
            } catch (PnpmError e) {
                // the check is 11.0.6's (PnpmRelease#rejectsOverridesThatAreNoStrings); what a release before makes
                // of such overrides the port does not follow
                if (!release.rejectsOverridesThatAreNoStrings()) {
                    throw new CannotKnow("what pnpm " + release + " makes of overrides that are no object of " +
                                         "strings (" + e.getMessage() + ")");
                }
                throw e;
            }
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
        if ((pnpmSettings.containsKey("update") || pnpmSettings.containsKey("audit")) &&
            release.translatesTheUpdateAndAuditSettings()) {
            translateUpdateSettings(pnpmSettings, settings);
            translateAuditSettings(pnpmSettings, settings);
        }
        return settings;
    }

    private static final List<String> AUDIT_LEVELS = Arrays.asList("info", "low", "moderate", "high", "critical");

    // translateUpdateSettings (11.16.0): the update setting is an object, read into updateConfig, never update itself
    @SuppressWarnings("unchecked")
    private static void translateUpdateSettings(Map<String, @Nullable Object> pnpmSettings,
                                                Map<String, @Nullable Object> settings) {
        settings.remove("update");
        Object update = pnpmSettings.get("update");
        if (update == null || update == Js.UNDEFINED) {
            return;
        }
        assertObjectSetting(update, "update");
        Map<String, @Nullable Object> updateSetting = (Map<String, @Nullable Object>) update;
        Map<String, @Nullable Object> updateConfig = new LinkedHashMap<>();
        if (updateSetting.get("ignoreDeps") != null) {
            assertStringArray(updateSetting.get("ignoreDeps"), "update.ignoreDeps");
            updateConfig.put("ignoreDependencies", updateSetting.get("ignoreDeps"));
        }
        if (updateSetting.get("changeset") != null) {
            assertBoolean(updateSetting.get("changeset"), "update.changeset");
            updateConfig.put("changeset", updateSetting.get("changeset"));
        }
        if (updateSetting.get("githubActions") != null) {
            assertBoolean(updateSetting.get("githubActions"), "update.githubActions");
            updateConfig.put("githubActions", updateSetting.get("githubActions"));
        }
        settings.put("updateConfig", updateConfig);
    }

    // translateAuditSettings (11.16.0): the audit setting is an object, read into auditConfig and auditLevel
    @SuppressWarnings("unchecked")
    private static void translateAuditSettings(Map<String, @Nullable Object> pnpmSettings,
                                               Map<String, @Nullable Object> settings) {
        settings.remove("audit");
        Object audit = pnpmSettings.get("audit");
        if (audit == null || audit == Js.UNDEFINED) {
            return;
        }
        assertObjectSetting(audit, "audit");
        Map<String, @Nullable Object> auditSetting = (Map<String, @Nullable Object>) audit;
        if (auditSetting.get("ignore") != null) {
            assertStringArray(auditSetting.get("ignore"), "audit.ignore");
            Map<String, @Nullable Object> auditConfig = settings.get("auditConfig") instanceof Map ?
                    new LinkedHashMap<>((Map<String, @Nullable Object>) settings.get("auditConfig")) :
                    new LinkedHashMap<>();
            auditConfig.put("ignoreGhsas", auditSetting.get("ignore"));
            settings.put("auditConfig", auditConfig);
        }
        Object level = auditSetting.get("level");
        if (level != null) {
            if (!AUDIT_LEVELS.contains(level)) {
                throw new PnpmError("INVALID_SETTING", "The \"audit.level\" setting should be one of " +
                                                       String.join(", ", AUDIT_LEVELS) + ", but got " +
                                                       Js.jsonStringify(level));
            }
            settings.put("auditLevel", level);
        }
    }

    private static void assertStringArray(@Nullable Object value, String settingName) {
        boolean strings = value instanceof List;
        if (strings) {
            for (Object item : (List<?>) value) {
                strings &= item instanceof String;
            }
        }
        if (!strings) {
            throw new PnpmError("INVALID_SETTING", "The \"" + settingName + "\" setting should be an array of " +
                                                   "strings, but got " + renderReceivedType(value));
        }
    }

    private static void assertBoolean(@Nullable Object value, String settingName) {
        if (!(value instanceof Boolean)) {
            throw new PnpmError("INVALID_SETTING", "The \"" + settingName + "\" setting should be a boolean, but " +
                                                   "got " + renderReceivedType(value));
        }
    }

    private static void assertObjectSetting(@Nullable Object value, String settingName) {
        if (!(value instanceof Map)) {
            throw new PnpmError("INVALID_SETTING", "The \"" + settingName + "\" setting should be an object, but " +
                                                   "got " + renderReceivedType(value));
        }
    }

    // whether a map setting holds a string env-replace would read a ${VAR} of; one that holds none reads the same
    // with the environment or without it
    @SuppressWarnings("unchecked")
    private static boolean holdsAnEnvPlaceholder(Map<String, @Nullable Object> value) {
        for (Object v : value.values()) {
            if (v instanceof String && hasEnvPlaceholder((String) v)) {
                return true;
            }
        }
        return false;
    }

    private static boolean holdsAnEnvReference(@Nullable Object value) {
        if (value instanceof Map) {
            for (Object v : ((Map<String, @Nullable Object>) value).values()) {
                if (v instanceof String && ((String) v).contains("${")) {
                    return true;
                }
            }
        }
        return false;
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
     * The install command's getFetchFullMetadata.ts: {@code supportedArchitectures.libc && true}, which 11.13.0
     * removed ({@link CreateNewStoreController#createInstallStoreController}).
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

    /**
     * resolveDependencyTree's {@code namedRegistryPrefixes} from 11.1.0 on; a release before has none
     * ({@link PnpmRelease#readsNamedRegistries}), which the resolution asks where a spec starts with one.
     */
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
