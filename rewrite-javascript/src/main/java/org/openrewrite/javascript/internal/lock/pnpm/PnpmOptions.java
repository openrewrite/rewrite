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

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Port of @pnpm/config.reader src/index.ts {@code getConfig} and src/getOptionsFromRootManifest.ts (pnpm 11.2.2),
 * as far as a lock-only install reads the result, with the defaults of @pnpm/installing.deps-installer
 * src/install/extendInstallOptions.ts beneath: the settings by pnpm's own (camelCase) names, as the JavaScript
 * values pnpm-workspace.yaml gives, the catalogs, and the workspace's package patterns.
 * <p>
 * pnpm 11 reads no setting from package.json and only auth, registry and network keys from .npmrc.
 */
@Getter
final class PnpmOptions {

    private static final Pattern CAMEL_CASE = Pattern.compile("^[a-z][a-zA-Z0-9]*$");
    private static final Pattern ENV_EXPR = Pattern.compile("(?<!\\\\)(\\\\*)\\$\\{([^${}]+)}");
    private static final Pattern ENV_VALUE = Pattern.compile("([^:-]+)(:?)-(.+)");

    /**
     * The settings by name, the defaults overlaid by pnpm-workspace.yaml's.
     */
    private final Map<String, @Nullable Object> config;

    private final Set<String> explicitlySetKeys;

    private final Map<String, @Nullable Map<String, @Nullable Object>> catalogs;

    /**
     * The workspace's package globs; null without a pnpm-workspace.yaml (no workspace).
     */
    private final @Nullable List<String> workspacePackagePatterns;

    private PnpmOptions(Map<String, @Nullable Object> config, Set<String> explicitlySetKeys,
                        Map<String, @Nullable Map<String, @Nullable Object>> catalogs,
                        @Nullable List<String> workspacePackagePatterns) {
        this.config = config;
        this.explicitlySetKeys = explicitlySetKeys;
        this.catalogs = catalogs;
        this.workspacePackagePatterns = workspacePackagePatterns;
    }

    /**
     * @param workspaceManifest the text of pnpm-workspace.yaml at the workspace root, null without one
     * @param rootManifest      the root package.json as parsed, for {@code $name} references in overrides
     * @param env               the process environment, null when the host does not know it
     */
    @SuppressWarnings("unchecked")
    static PnpmOptions load(@Nullable String workspaceManifest, @Nullable Map<String, @Nullable Object> rootManifest,
                            @Nullable Map<String, String> env) {
        Map<String, @Nullable Object> config = defaults();
        Set<String> explicitlySetKeys = new LinkedHashSet<>();
        Map<String, @Nullable Map<String, @Nullable Object>> catalogs = new LinkedHashMap<>();
        List<String> patterns = null;
        if (workspaceManifest != null) {
            Map<String, @Nullable Object> manifest = WorkspaceManifest.readWorkspaceManifest(workspaceManifest);
            Object packages = manifest == null ? null : manifest.get("packages");
            patterns = packages instanceof List ? new ArrayList<>((List<String>) packages) :
                    Collections.singletonList(".");
            if (manifest != null) {
                Map<String, @Nullable Object> newSettings = getOptionsFromPnpmSettings(manifest, rootManifest, env);
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
        if (explicitlySetKeys.contains("minimumReleaseAge") && config.get("minimumReleaseAgeStrict") == null) {
            config.put("minimumReleaseAgeStrict", true);
        }
        return new PnpmOptions(config, explicitlySetKeys, catalogs, patterns);
    }

    // the defaults a lock-only install reads, config/reader's over extendInstallOptions'
    private static Map<String, @Nullable Object> defaults() {
        Map<String, @Nullable Object> config = new LinkedHashMap<>();
        config.put("autoInstallPeers", true);
        config.put("autoInstallPeersFromHighestMatch", false);
        config.put("blockExoticSubdeps", true);
        config.put("dedupeDirectDeps", false);
        config.put("dedupeInjectedDeps", true);
        config.put("dedupePeerDependents", true);
        config.put("dedupePeers", false);
        config.put("engineStrict", false);
        config.put("excludeLinksFromLockfile", false);
        config.put("ignoreCompatibilityDb", false);
        config.put("injectWorkspacePackages", false);
        config.put("linkWorkspacePackages", false);
        config.put("lockfileIncludeTarballUrl", false);
        config.put("minimumReleaseAge", 1440.0);
        config.put("minimumReleaseAgeIgnoreMissingTime", true);
        config.put("optional", true);
        config.put("peersSuffixMaxLength", 1000.0);
        config.put("preferFrozenLockfile", true);
        config.put("preferWorkspacePackages", false);
        config.put("registry", "https://registry.npmjs.org/");
        config.put("resolutionMode", "highest");
        config.put("resolvePeersFromWorkspaceRoot", true);
        config.put("saveWorkspaceProtocol", "rolling");
        config.put("sharedWorkspaceLockfile", true);
        config.put("strictPeerDependencies", false);
        return config;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, @Nullable Object> getOptionsFromPnpmSettings(
            Map<String, @Nullable Object> pnpmSettings, @Nullable Map<String, @Nullable Object> manifest,
            @Nullable Map<String, String> env) {
        Map<String, @Nullable Object> settings = new LinkedHashMap<>();
        for (String key : Js.keys(pnpmSettings)) {
            Object value = pnpmSettings.get(key);
            String newKey = envReplace(key, env);
            if (value instanceof String) {
                settings.put(newKey, envReplace((String) value, env));
            } else if (("registries".equals(newKey) || "namedRegistries".equals(newKey)) && value instanceof Map) {
                Map<String, @Nullable Object> out = new LinkedHashMap<>();
                for (String k : Js.keys((Map<String, @Nullable Object>) value)) {
                    Object v = ((Map<String, @Nullable Object>) value).get(k);
                    out.put(k, v instanceof String ? envReplace((String) v, env) : v);
                }
                settings.put(newKey, out);
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
                                                     (overrides instanceof List ? "array" : Js.typeOf(overrides)));
        }
        for (Map.Entry<?, ?> entry : ((Map<?, ?>) overrides).entrySet()) {
            Object spec = entry.getValue();
            if (!(spec instanceof String)) {
                throw new PnpmError("INVALID_OVERRIDES", "The value of overrides." + entry.getKey() +
                                                         " should be a string, but got " +
                                                         (spec == null ? "null" : spec instanceof List ? "array" :
                                                                 Js.typeOf(spec)));
            }
        }
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
                    throw new CannotKnow("the environment (" + orig + " in pnpm-workspace.yaml)");
                }
                String envValue = getEnvValue(env, name);
                if (envValue == null) {
                    throw new JsError("Failed to replace env in config: " + orig);
                }
                replacement = halfEscape + envValue;
            }
            m.appendReplacement(out, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(out);
        return out.toString();
    }

    private static @Nullable String getEnvValue(Map<String, String> env, String name) {
        Matcher matched = ENV_VALUE.matcher(name);
        if (!matched.matches()) {
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
}
