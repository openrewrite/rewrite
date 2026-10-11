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
package org.openrewrite.javascript.internal.lock.bun;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.HostEnvironment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of bun's src/install/PackageManager/PackageManagerOptions.zig {@code load} (bun 1.3.10) over bunfig.toml, the
 * user's .npmrc, the project's and the environment, in that order: the options that change what an install writes.
 */
@Getter
public final class BunOptions {

    static final String DEFAULT_REGISTRY = "https://registry.npmjs.org/";

    /**
     * The default registry's url ({@code options.scope.url.href}).
     */
    private String registry = DEFAULT_REGISTRY;

    /**
     * Scoped registries by scope name without the {@code @}.
     */
    private final Map<String, String> scopes = new LinkedHashMap<>();

    private boolean linkWorkspacePackages = true;
    private boolean frozenLockfile;
    private boolean loadLockfile = true;
    private @Nullable Double minimumReleaseAgeMs;
    private List<String> minimumReleaseAgeExcludes = Collections.emptyList();

    private boolean localDevDependencies = true;
    private boolean localOptionalDependencies = true;
    private boolean localPeerDependencies = true;
    private boolean remoteOptionalDependencies = true;
    private boolean remotePeerDependencies = true;

    private BunOptions() {
    }

    /**
     * The options from the project's bunfig.toml, the user's .npmrc (the host's registries), the project's .npmrc
     * and the environment, any of which may be absent.
     */
    public static BunOptions load(@Nullable String bunfigToml, @Nullable String userNpmrc, @Nullable String npmrc,
                                  HostEnvironment env) {
        Install install = new Install();
        if (bunfigToml != null) {
            Json.Expr bunfig = Toml.parse(bunfigToml);
            Json.Expr installObj = bunfig.getObject("install");
            if (installObj != null) {
                install.readBunfig(installObj);
            }
        }
        if (userNpmrc != null) {
            install.readNpmrc(userNpmrc, env);
        }
        if (npmrc != null) {
            install.readNpmrc(npmrc, env);
        }
        BunOptions options = new BunOptions();
        options.apply(install, env);
        return options;
    }

    /**
     * {@code scopeForPackageName}: a scoped package's registry, else the default.
     */
    String scopeForPackageName(String name) {
        if (name.isEmpty() || name.charAt(0) != '@') {
            return registry;
        }
        int slash = name.indexOf('/');
        String scope = slash >= 0 ? name.substring(1, slash) : name.substring(1);
        String url = scopes.get(scope);
        return url != null ? url : registry;
    }

    private void apply(Install install, HostEnvironment env) {
        String base = install.defaultRegistry != null && !install.defaultRegistry.isEmpty() ?
                install.defaultRegistry : DEFAULT_REGISTRY;
        registry = expandEnv(base, env);
        if (install.linkWorkspacePackages != null) {
            linkWorkspacePackages = install.linkWorkspacePackages;
        }
        for (Map.Entry<String, String> scope : install.scoped.entrySet()) {
            String url = scope.getValue().isEmpty() ? base : scope.getValue();
            scopes.put(scope.getKey(), expandEnv(url, env));
        }
        if (install.saveDev != null) {
            localDevDependencies = install.saveDev;
        }
        if (install.saveOptional != null) {
            remoteOptionalDependencies = install.saveOptional;
            localOptionalDependencies = install.saveOptional;
        }
        if (install.savePeer != null) {
            remotePeerDependencies = install.savePeer;
            localPeerDependencies = install.savePeer;
        }
        if (Boolean.TRUE.equals(install.production)) {
            localDevDependencies = false;
            frozenLockfile = true;
        }
        if (Boolean.TRUE.equals(install.frozenLockfile)) {
            frozenLockfile = true;
        }
        minimumReleaseAgeMs = install.minimumReleaseAgeMs;
        minimumReleaseAgeExcludes = install.minimumReleaseAgeExcludes;
        for (String key : new String[]{"BUN_CONFIG_REGISTRY", "NPM_CONFIG_REGISTRY", "npm_config_registry"}) {
            String value = env.setting(key);
            if (value != null && !value.isEmpty() && (value.startsWith("https://") || value.startsWith("http://"))) {
                registry = value;
                break;
            }
        }
        String skipLoad = env.setting("BUN_CONFIG_SKIP_LOAD_LOCKFILE");
        if (skipLoad != null) {
            loadLockfile = "0".equals(skipLoad);
        }
    }

    // Npm.Registry.Scope.fromAPI: a url of $NAME is the environment variable's value
    private static String expandEnv(String url, HostEnvironment env) {
        if (url.startsWith("$")) {
            String replaced = env.reference(Strings.trim(url.substring(1), "/"));
            if (replaced != null && replaced.length() > 1) {
                return replaced;
            }
        }
        return url;
    }

    /**
     * {@code Api.BunInstall}: what bunfig.toml and .npmrc set, before {@code Options.load} applies it.
     */
    private static final class Install {
        @Nullable String defaultRegistry;
        final Map<String, String> scoped = new LinkedHashMap<>();
        @Nullable Boolean linkWorkspacePackages;
        @Nullable Boolean saveDev;
        @Nullable Boolean saveOptional;
        @Nullable Boolean savePeer;
        @Nullable Boolean production;
        @Nullable Boolean frozenLockfile;
        @Nullable Double minimumReleaseAgeMs;
        List<String> minimumReleaseAgeExcludes = Collections.emptyList();

        void readBunfig(Json.Expr installObj) {
            Json.Expr registry = installObj.get("registry");
            if (registry != null) {
                defaultRegistry = parseRegistry(registry);
            }
            Json.Expr scopes = installObj.get("scopes");
            if (scopes != null && scopes.isObject()) {
                for (Json.Property prop : scopes.properties()) {
                    String name = prop.key.asString();
                    if (name == null || name.isEmpty()) {
                        continue;
                    }
                    scoped.put(name.charAt(0) == '@' ? name.substring(1) : name, parseRegistry(prop.value));
                }
            }
            production = bool(installObj, "production", production);
            frozenLockfile = bool(installObj, "frozenLockfile", frozenLockfile);
            saveOptional = bool(installObj, "optional", saveOptional);
            savePeer = bool(installObj, "peer", savePeer);
            saveDev = bool(installObj, "dev", saveDev);
            linkWorkspacePackages = bool(installObj, "linkWorkspacePackages", linkWorkspacePackages);
            Json.Expr minAge = installObj.get("minimumReleaseAge");
            if (minAge != null && minAge.kind == Json.Kind.NUMBER && minAge.asNumber() >= 0) {
                minimumReleaseAgeMs = minAge.asNumber() * 1000;
            }
            Json.Expr exclusions = installObj.get("minimumReleaseAgeExcludes");
            if (exclusions != null && exclusions.isArray() && !exclusions.items().isEmpty()) {
                List<String> list = new ArrayList<>();
                for (Json.Expr item : exclusions.items()) {
                    if (item.isString()) {
                        list.add(item.asString());
                    }
                }
                minimumReleaseAgeExcludes = list;
            }
        }

        private static String parseRegistry(Json.Expr expr) {
            if (expr.isString()) {
                return expr.asString();
            }
            if (expr.isObject()) {
                Json.Expr url = expr.get("url");
                return url != null && url.isString() ? url.asString() : "";
            }
            return "";
        }

        private static @Nullable Boolean bool(Json.Expr obj, String key, @Nullable Boolean current) {
            Json.Expr value = obj.get(key);
            Boolean b = value == null ? null : value.asBool();
            return b != null ? b : current;
        }

        // ini.zig loadNpmrc: the top-level keys a lock-only install uses, each of the type it takes
        void readNpmrc(String npmrc, HostEnvironment env) {
            Map<String, @Nullable Object> out = Ini.parse(npmrc, env);
            Object registry = out.get("registry");
            if (registry instanceof String) {
                defaultRegistry = (String) registry;
            }
            for (String key : new String[]{"omit", "include"}) {
                boolean include = "include".equals(key);
                Object value = out.get(key);
                List<?> values = value instanceof List ? (List<?>) value : Collections.singletonList(value);
                for (Object item : values) {
                    if ("dev".equals(item)) {
                        saveDev = include;
                    } else if ("peer".equals(item)) {
                        savePeer = include;
                    } else if ("optional".equals(item)) {
                        saveOptional = include;
                    }
                }
            }
            Object link = out.get("link-workspace-packages");
            if (link instanceof Boolean) {
                linkWorkspacePackages = (Boolean) link;
            }
            for (Map.Entry<String, @Nullable Object> entry : out.entrySet()) {
                String key = entry.getKey();
                if (key.startsWith("@") && key.endsWith(":registry") && entry.getValue() instanceof String) {
                    scoped.put(key.substring(1, key.length() - ":registry".length()), (String) entry.getValue());
                }
            }
        }
    }
}
