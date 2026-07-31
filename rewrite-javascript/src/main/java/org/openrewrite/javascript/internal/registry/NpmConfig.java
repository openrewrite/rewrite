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
package org.openrewrite.javascript.internal.registry;

import org.jspecify.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A typed view over the merged {@code .npmrc} key/value map: the default registry, per-scope
 * registries, and an auth table keyed by npm's nerf-dart ({@code //host/path/}). Values are
 * {@code ${VAR}}-expanded and {@code _password}/{@code _auth} are base64-decoded on read.
 */
final class NpmConfig {
    private static final String REGISTRY_SUFFIX = ":registry";

    private final Environment env;
    private final @Nullable String defaultRegistryRaw;
    private final Map<String, String> scopeRegistriesRaw = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> nerfAuthRaw = new LinkedHashMap<>();
    private final @Nullable String globalAuthBase64Raw;
    private final @Nullable String alwaysAuthRaw;
    private final @Nullable String cafileRaw;
    private final @Nullable String strictSslRaw;
    private final @Nullable String proxyRaw;
    private final @Nullable String httpsProxyRaw;
    private final @Nullable String noProxyRaw;

    NpmConfig(Map<String, String> properties, Environment env) {
        this.env = env;
        String defaultRegistry = null;
        String globalAuth = null;
        String alwaysAuth = null;
        String cafile = null;
        String strictSsl = null;
        String proxy = null;
        String httpsProxy = null;
        String noProxy = null;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey().trim();
            String value = entry.getValue();
            if (key.startsWith("//")) {
                int slashColon = key.lastIndexOf("/:");
                if (slashColon >= 0) {
                    String nerf = key.substring(0, slashColon + 1);
                    String attr = key.substring(slashColon + 2);
                    nerfAuthRaw.computeIfAbsent(nerf, k -> new LinkedHashMap<>()).put(attr, value);
                }
                continue;
            }
            if ("registry".equals(key)) {
                defaultRegistry = value;
            } else if (key.endsWith(REGISTRY_SUFFIX) && key.startsWith("@")) {
                scopeRegistriesRaw.put(key.substring(0, key.length() - REGISTRY_SUFFIX.length()), value);
            } else if ("_auth".equals(key)) {
                globalAuth = value;
            } else if ("always-auth".equals(key)) {
                alwaysAuth = value;
            } else if ("cafile".equals(key)) {
                cafile = value;
            } else if ("strict-ssl".equals(key)) {
                strictSsl = value;
            } else if ("proxy".equals(key)) {
                proxy = value;
            } else if ("https-proxy".equals(key)) {
                httpsProxy = value;
            } else if ("noproxy".equals(key) || "no-proxy".equals(key)) {
                noProxy = value;
            }
        }
        this.defaultRegistryRaw = defaultRegistry;
        this.globalAuthBase64Raw = globalAuth;
        this.alwaysAuthRaw = alwaysAuth;
        this.cafileRaw = cafile;
        this.strictSslRaw = strictSsl;
        this.proxyRaw = proxy;
        this.httpsProxyRaw = httpsProxy;
        this.noProxyRaw = noProxy;
    }

    EnvExpansion.Expansion defaultRegistry() {
        return EnvExpansion.expand(defaultRegistryRaw, env);
    }

    /** Per-scope registry URLs keyed by {@code @scope}, in configuration order. */
    Map<String, EnvExpansion.Expansion> scopeRegistries() {
        Map<String, EnvExpansion.Expansion> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : scopeRegistriesRaw.entrySet()) {
            resolved.put(entry.getKey(), EnvExpansion.expand(entry.getValue(), env));
        }
        return resolved;
    }

    boolean isAlwaysAuth() {
        return parseBool(EnvExpansion.expand(alwaysAuthRaw, env).value);
    }

    @Nullable
    String getCafile() {
        return EnvExpansion.expand(cafileRaw, env).value;
    }

    boolean isStrictSsl() {
        String v = EnvExpansion.expand(strictSslRaw, env).value;
        return v == null || parseBool(v);
    }

    @Nullable
    String getProxy() {
        return EnvExpansion.expand(proxyRaw, env).value;
    }

    @Nullable
    String getHttpsProxy() {
        return EnvExpansion.expand(httpsProxyRaw, env).value;
    }

    @Nullable
    String getNoProxy() {
        return EnvExpansion.expand(noProxyRaw, env).value;
    }

    /**
     * The auth selected for a registry URL, walking npm's nerf-dart path most-specific-first
     * ({@code //host/a/b/} → {@code //host/a/} → {@code //host/}), falling back to a global
     * {@code _auth}. Null when no credentials are configured for the URL.
     */
    @Nullable
    Auth authFor(String registryUrl) {
        String nerf = Urls.nerfDart(registryUrl);
        while (nerf.length() > 2) {
            Map<String, String> attrs = nerfAuthRaw.get(nerf);
            if (attrs != null) {
                Auth auth = toAuth(attrs);
                if (auth.hasAny() || auth.alwaysAuth) {
                    return auth;
                }
            }
            int slash = nerf.lastIndexOf('/', nerf.length() - 2);
            if (slash <= 1) {
                break;
            }
            nerf = nerf.substring(0, slash + 1);
        }
        if (globalAuthBase64Raw != null) {
            Auth auth = new Auth();
            EnvExpansion.Expansion e = EnvExpansion.expand(globalAuthBase64Raw, env);
            auth.authBase64 = e.value;
            auth.unresolvedPlaceholders = e.unresolvedPlaceholders;
            return auth;
        }
        return null;
    }

    private Auth toAuth(Map<String, String> attrs) {
        Auth auth = new Auth();
        EnvExpansion.Expansion token = EnvExpansion.expand(attrs.get("_authToken"), env);
        auth.authToken = token.value;
        auth.unresolvedPlaceholders |= token.unresolvedPlaceholders;

        EnvExpansion.Expansion authBase64 = EnvExpansion.expand(attrs.get("_auth"), env);
        auth.authBase64 = authBase64.value;
        auth.unresolvedPlaceholders |= authBase64.unresolvedPlaceholders;

        EnvExpansion.Expansion user = EnvExpansion.expand(attrs.get("username"), env);
        auth.username = user.value;
        auth.unresolvedPlaceholders |= user.unresolvedPlaceholders;

        EnvExpansion.Expansion password = EnvExpansion.expand(attrs.get("_password"), env);
        auth.password = password.value == null ? null : decodeBase64(password.value);
        auth.unresolvedPlaceholders |= password.unresolvedPlaceholders;

        auth.alwaysAuth = parseBool(EnvExpansion.expand(attrs.get("always-auth"), env).value);
        return auth;
    }

    private static boolean parseBool(@Nullable String value) {
        return "true".equalsIgnoreCase(value);
    }

    private static String decodeBase64(String value) {
        try {
            return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    /**
     * Resolved credentials for one registry: at most one of a bearer token, a pre-encoded
     * {@code _auth}, or a username/password pair is populated.
     */
    static final class Auth {
        @Nullable String authToken;
        @Nullable String authBase64;
        @Nullable String username;
        @Nullable String password;
        boolean alwaysAuth;
        boolean unresolvedPlaceholders;

        boolean hasAny() {
            return authToken != null || authBase64 != null || username != null || password != null;
        }
    }
}
