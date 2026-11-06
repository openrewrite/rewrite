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

import org.jspecify.annotations.Nullable;
import org.openrewrite.javascript.internal.lock.CannotKnow;
import org.openrewrite.javascript.internal.lock.WhatwgUrl;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @pnpm/network.auth-header's createGetAuthHeaderByURI over the release's filtered config, not the host's npm config.
 * Values have already been substituted by config.reader and must never be expanded again by the transport.
 */
public final class PnpmAuthHeaders {
    private static final Pattern AUTH_KEY = Pattern.compile("^(.*):(_authToken|_auth|username|_password|tokenHelper)$");
    private final Map<String, String> headers = new LinkedHashMap<>();
    private final Map<String, Map<String, String>> scopedHeaders = new LinkedHashMap<>();

    PnpmAuthHeaders(Map<String, @Nullable Object> config, PnpmRelease release) {
        Map<String, Map<String, String>> groups = new LinkedHashMap<>();
        for (String key : Js.keys(config)) {
            Matcher match = AUTH_KEY.matcher(key);
            if (match.matches()) {
                groups.computeIfAbsent(match.group(1), k -> new LinkedHashMap<>())
                        .put(match.group(2), Js.string(config.get(key)));
            }
        }
        // Before rescoping, unscoped credentials are applied last to the merged default registry.
        Map<String, String> defaults = new LinkedHashMap<>();
        String defaultRegistry = null;
        if (!release.rescopesUnscopedCredentials()) {
            for (String attr : new String[]{"_authToken", "_auth", "username", "_password", "tokenHelper"}) {
                if (config.get(attr) != null) {
                    defaults.put(attr, Js.string(config.get(attr)));
                }
            }
            String registry = WhatwgUrl.parse(Js.string(config.get("registry"))).getHost();
            String path = WhatwgUrl.parse(Js.string(config.get("registry"))).getPathname();
            defaultRegistry = "//" + registry + path;
        }
        Map<String, Map<String, Map<String, String>>> byScope = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, String>> group : groups.entrySet()) {
            String[] registryAndScope = release.groupsCredentialsByRegistryAndScope() ?
                    PnpmOptions.registryAndScope(group.getKey()) : new String[]{group.getKey(), "@"};
            byScope.computeIfAbsent(registryAndScope[0], k -> new LinkedHashMap<>())
                    .computeIfAbsent(registryAndScope[1], k -> new LinkedHashMap<>()).putAll(group.getValue());
        }
        for (Map.Entry<String, Map<String, Map<String, String>>> registry : byScope.entrySet()) {
            for (Map.Entry<String, Map<String, String>> scope : registry.getValue().entrySet()) {
                String header = header(scope.getValue(), release);
                if (header != null) {
                    Map<String, String> target = "@".equals(scope.getKey()) ? headers :
                            scopedHeaders.computeIfAbsent(scope.getKey(), k -> new LinkedHashMap<>());
                    String key = registry.getKey();
                    target.put(key.endsWith("/") ? key : key + "/", header);
                }
            }
        }
        if (defaultRegistry != null) {
            String header = header(defaults, release);
            if (header != null) {
                headers.put(defaultRegistry.endsWith("/") ? defaultRegistry : defaultRegistry + "/", header);
            }
        }
    }

    /** The Authorization value pnpm sends for this URL and package; null means send no credentials. */
    public @Nullable String authorization(String url, String packageName) {
        WhatwgUrl parsed = WhatwgUrl.parse(url);
        if (!parsed.getUsername().isEmpty() || !parsed.getPassword().isEmpty()) {
            return basic(parsed.getUsername() + ":" + parsed.getPassword());
        }
        int slash = packageName.indexOf('/');
        String scope = packageName.startsWith("@") && slash > 1 ? packageName.substring(0, slash) : "@";
        Map<String, String> scoped = scopedHeaders.get(scope);
        String header = scoped == null ? null : lookup(scoped, parsed);
        return header != null ? header : lookup(headers, parsed);
    }

    private static @Nullable String lookup(Map<String, String> headers, WhatwgUrl url) {
        String path = url.getPathname();
        String nerfed = "//" + url.getHost() + (path.endsWith("/") ? path : path + "/");
        String found = longest(headers, nerfed);
        if (found == null && !url.getPort().isEmpty()) {
            found = longest(headers, "//" + url.getHostname() + (path.endsWith("/") ? path : path + "/"));
        }
        return found;
    }

    private static @Nullable String longest(Map<String, String> headers, String nerfed) {
        String found = null;
        int length = 0;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (nerfed.startsWith(entry.getKey()) && entry.getKey().length() > length) {
                found = entry.getValue();
                length = entry.getKey().length();
            }
        }
        return found;
    }

    private static @Nullable String header(Map<String, String> attrs, PnpmRelease release) {
        if (present(attrs.get("tokenHelper"))) {
            throw new CannotKnow("the output of pnpm's configured token helper");
        }
        if (present(attrs.get("_authToken"))) {
            if (PnpmOptions.hasEnvPlaceholder(attrs.get("_authToken"))) {
                throw new CannotKnow("the unresolved placeholder in pnpm's registry credential");
            }
            return "Bearer " + attrs.get("_authToken");
        }
        if (present(attrs.get("_auth"))) {
            return basic(PnpmOptions.decodeBase64Credential(attrs.get("_auth"), "_auth", release));
        }
        if (present(attrs.get("username")) && present(attrs.get("_password"))) {
            return basic(attrs.get("username") + ":" +
                    PnpmOptions.decodeBase64Credential(attrs.get("_password"), "_password", release));
        }
        return null;
    }

    private static boolean present(@Nullable String value) {
        return value != null && !value.isEmpty();
    }

    private static String basic(String value) {
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
