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
import org.openrewrite.ExecutionContext;
import org.openrewrite.javascript.NodeExecutionContextView;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.NodeRegistryCredentials;
import org.openrewrite.javascript.marker.NodeResolutionResult;
import org.openrewrite.javascript.marker.NodeResolutionResult.Npmrc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the npm registries (and their credentials) a project locks against: host-supplied
 * registries on the {@link NodeExecutionContextView} win outright; otherwise the marker's merged
 * {@code .npmrc} is parsed via {@link NpmConfig}, with credentials filled from the URL, then
 * host-supplied credentials, then {@code .netrc}. Mirrors the Python {@code IndexDiscovery}.
 */
public final class RegistryDiscovery {
    private static final String DEFAULT_REGISTRY = "https://registry.npmjs.org/";

    private RegistryDiscovery() {
    }

    public static NodeRegistries discover(ExecutionContext ctx, @Nullable NodeResolutionResult marker, Environment env) {
        NodeExecutionContextView view = NodeExecutionContextView.view(ctx);
        List<NodeRegistry> explicit = view.getRegistries();
        if (!explicit.isEmpty()) {
            return fromExplicit(explicit);
        }

        NpmConfig config = new NpmConfig(mergeNpmrc(marker), env);
        List<NodeRegistryCredentials> credentials = view.getRegistryCredentials();

        EnvExpansion.Expansion defaultUrl = config.defaultRegistry();
        String resolvedDefaultUrl = defaultUrl.value != null ? defaultUrl.value : DEFAULT_REGISTRY;
        NodeRegistry defaultRegistry = buildRegistry(null, resolvedDefaultUrl,
                defaultUrl.value != null && defaultUrl.unresolvedPlaceholders, config, credentials, env);

        Map<String, NodeRegistry> byScope = new LinkedHashMap<>();
        for (Map.Entry<String, EnvExpansion.Expansion> scoped : config.scopeRegistries().entrySet()) {
            EnvExpansion.Expansion url = scoped.getValue();
            if (url.value != null) {
                byScope.put(scoped.getKey(), buildRegistry(scoped.getKey(), url.value,
                        url.unresolvedPlaceholders, config, credentials, env));
            }
        }

        return new NodeRegistries(defaultRegistry, byScope,
                config.getProxy(), config.getHttpsProxy(), config.getNoProxy());
    }

    private static NodeRegistries fromExplicit(List<NodeRegistry> registries) {
        NodeRegistry defaultRegistry = null;
        Map<String, NodeRegistry> byScope = new LinkedHashMap<>();
        for (NodeRegistry registry : registries) {
            if (registry.getScope() == null) {
                defaultRegistry = registry;
            } else {
                byScope.put(registry.getScope(), registry);
            }
        }
        if (defaultRegistry == null) {
            defaultRegistry = anonymous(null, DEFAULT_REGISTRY, false);
        }
        return new NodeRegistries(defaultRegistry, byScope, null, null, null);
    }

    private static NodeRegistry buildRegistry(@Nullable String scope, String url, boolean urlUnresolved,
                                              NpmConfig config, List<NodeRegistryCredentials> credentials,
                                              Environment env) {
        boolean unresolved = urlUnresolved;
        String authToken = null;
        String username = null;
        String password = null;
        String authBase64 = null;
        boolean alwaysAuth = config.isAlwaysAuth();

        // 1. Credentials embedded in the registry URL's userinfo.
        int[] userinfo = Urls.userinfoRange(url);
        if (userinfo != null) {
            String raw = url.substring(userinfo[0], userinfo[1]);
            int colon = raw.indexOf(':');
            username = EnvExpansion.percentDecode(colon < 0 ? raw : raw.substring(0, colon));
            password = colon < 0 ? null : EnvExpansion.percentDecode(raw.substring(colon + 1));
            url = Urls.stripUserinfo(url);
        } else {
            // 2. npmrc auth keyed by nerf-dart.
            NpmConfig.Auth auth = config.authFor(url);
            if (auth != null) {
                authToken = auth.authToken;
                username = auth.username;
                password = auth.password;
                authBase64 = auth.authBase64;
                alwaysAuth |= auth.alwaysAuth;
                unresolved |= auth.unresolvedPlaceholders;
            } else {
                // 3. Host-supplied credentials, then 4. .netrc.
                String host = Urls.host(url);
                if (host != null) {
                    NodeRegistryCredentials matched = matchByHost(credentials, host);
                    if (matched != null) {
                        authToken = matched.getAuthToken();
                        username = matched.getUsername();
                        password = matched.getPassword();
                    } else {
                        Netrc.Login login = Netrc.find(env, host);
                        if (login != null) {
                            username = login.getLogin();
                            password = login.getPassword();
                        }
                    }
                }
            }
        }

        return new NodeRegistry(scope, url, authToken, username, password, authBase64,
                alwaysAuth, config.getCafile(), config.isStrictSsl(), unresolved);
    }

    private static @Nullable NodeRegistryCredentials matchByHost(List<NodeRegistryCredentials> credentials, String host) {
        for (NodeRegistryCredentials candidate : credentials) {
            if (host.equalsIgnoreCase(candidate.getHost())) {
                return candidate;
            }
        }
        return null;
    }

    private static NodeRegistry anonymous(@Nullable String scope, String url, boolean unresolved) {
        return new NodeRegistry(scope, url, null, null, null, null, false, null, true, unresolved);
    }

    private static Map<String, String> mergeNpmrc(@Nullable NodeResolutionResult marker) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (marker == null || marker.getNpmrcConfigs() == null) {
            return merged;
        }
        List<Npmrc> configs = new ArrayList<>(marker.getNpmrcConfigs());
        // Global -> User -> Project, later scopes overriding earlier per key.
        configs.sort(Comparator.comparingInt(n -> n.getScope().ordinal()));
        for (Npmrc config : configs) {
            merged.putAll(config.getProperties());
        }
        return merged;
    }
}
