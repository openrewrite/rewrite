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
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.registry.NodeRegistryException.Reason;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The registry documents and tarballs the lock regeneration ports read, as the registry serves them, cached in
 * memory per run.
 */
public class NpmRegistryClient {
    // https://github.com/npm/registry/blob/main/docs/responses/package-metadata.md
    private static final String PACKUMENT_ACCEPT =
            "application/vnd.npm.install-v1+json; q=1.0, application/json; q=0.8, */*";

    private static final String FULL_ACCEPT = "application/json";

    private final HttpSender httpSender;
    private final Map<String, byte[]> tarballCache = new ConcurrentHashMap<>();
    private final Map<String, String> documentCache = new ConcurrentHashMap<>();

    public NpmRegistryClient(HttpSender httpSender) {
        this.httpSender = httpSender;
    }

    /**
     * The packument as the registry serves it: abbreviated ({@code application/vnd.npm.install-v1+json}) or,
     * with {@code fullMetadata}, full ({@code application/json}, with publish times). Cached per run.
     */
    public String getPackumentText(NodeRegistry registry, String name, boolean fullMetadata) {
        String key = (fullMetadata ? "full:" : "corgi:") + registry.getUrl() + "#" + name;
        String cached = documentCache.get(key);
        if (cached == null) {
            guard(registry);
            String url = base(registry) + "/" + Urls.encodeName(name);
            cached = new String(send(registry, name, null, url, fullMetadata ? FULL_ACCEPT : PACKUMENT_ACCEPT, true),
                    StandardCharsets.UTF_8);
            documentCache.put(key, cached);
        }
        return cached;
    }

    /**
     * The single-version manifest as the registry serves it. Cached per run.
     */
    public String getManifestText(NodeRegistry registry, String name, String version) {
        String key = "manifest:" + registry.getUrl() + "#" + name + "@" + version;
        String cached = documentCache.get(key);
        if (cached == null) {
            guard(registry);
            String url = base(registry) + "/" + Urls.encodeName(name) + "/" + version;
            cached = new String(send(registry, name, version, url, FULL_ACCEPT, false), StandardCharsets.UTF_8);
            documentCache.put(key, cached);
        }
        return cached;
    }

    /**
     * Another document the registry serves for a package version, at {@code url} (an attestations bundle). Cached
     * per run.
     */
    public String getDocumentText(NodeRegistry registry, String name, String version, String url) {
        String key = "document:" + url;
        String cached = documentCache.get(key);
        if (cached == null) {
            guard(registry);
            cached = new String(send(registry, name, version, url, FULL_ACCEPT, false), StandardCharsets.UTF_8);
            documentCache.put(key, cached);
        }
        return cached;
    }

    /**
     * The raw gzipped tarball bytes from {@code dist.tarball}, cached per url. Needed to reproduce
     * a Yarn Berry checksum, which hashes the tarball's repacked contents.
     */
    public byte[] getTarball(NodeRegistry registry, String name, @Nullable String version, String tarballUrl) {
        byte[] cached = tarballCache.get(tarballUrl);
        if (cached == null) {
            cached = send(registry, name, version, tarballUrl, HttpSender.Request.Builder.APPLICATION_JSON, false);
            tarballCache.put(tarballUrl, cached);
        }
        return cached;
    }

    private byte[] send(NodeRegistry registry, String name, @Nullable String version, String url,
                        String accept, boolean packument) {
        HttpSender.Request request;
        try {
            HttpSender.Request.Builder builder = httpSender.get(url).accept(accept);
            applyAuth(builder, registry);
            request = builder.build();
        } catch (Exception e) {
            throw new NodeRegistryException(Reason.UNREACHABLE, registry.getUrl(), name, version,
                    "Invalid registry URL: " + url, e);
        }
        try (HttpSender.Response response = httpSender.send(request)) {
            int code = response.getCode();
            if (code == 401 || code == 403) {
                throw new NodeRegistryException(Reason.AUTH_FAILED, safeUrl(registry), name, version,
                        "HTTP " + code + " from " + url + unresolvedCredentialsHint(registry), null);
            }
            if (code == 404) {
                Reason reason = packument ? Reason.PACKAGE_NOT_FOUND : Reason.VERSION_NOT_FOUND;
                throw new NodeRegistryException(reason, safeUrl(registry), name, version,
                        "HTTP 404 from " + url, null);
            }
            if (!response.isSuccessful()) {
                throw new NodeRegistryException(Reason.UNREACHABLE, safeUrl(registry), name, version,
                        "HTTP " + code + " from " + url, null);
            }
            return response.getBodyAsBytes();
        } catch (NodeRegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new NodeRegistryException(Reason.UNREACHABLE, safeUrl(registry), name, version,
                    "Failed to fetch " + url + ": " + e, e);
        }
    }

    private void guard(NodeRegistry registry) {
        if (registry.isUnresolvedPlaceholders()) {
            throw new NodeRegistryException(Reason.UNREACHABLE, safeUrl(registry),
                    "Registry URL contains unresolved environment placeholders: " + safeUrl(registry));
        }
        // The default sender cannot load a custom CA or disable strict SSL; fail loud rather than
        // ignore the config and later throw an opaque handshake error.
        if ((registry.getCafile() != null || !registry.isStrictSsl()) &&
                httpSender.getClass() == HttpUrlConnectionSender.class) {
            throw new NodeRegistryException(Reason.UNREACHABLE, safeUrl(registry),
                    "Registry " + safeUrl(registry) + " requires a custom CA (cafile/strict-ssl) but no " +
                            "TLS-capable HttpSender was injected");
        }
    }

    /**
     * Why a rejected request may not have authenticated: its {@code .npmrc} credentials reference variables
     * that were unset where the recipe ran. Empty when every placeholder resolved.
     */
    private static String unresolvedCredentialsHint(NodeRegistry registry) {
        List<String> placeholders = registry.getUnresolvedCredentialPlaceholders();
        if (placeholders.isEmpty()) {
            return "";
        }
        return "; .npmrc credentials for this registry reference " + String.join(", ", placeholders) + ", which " +
                (placeholders.size() == 1 ? "is" : "are") + " not set in the environment the recipe runs in";
    }

    private static void applyAuth(HttpSender.Request.Builder builder, NodeRegistry registry) {
        // Never send credentials over a non-TLS scheme — a cleartext (or unknown-scheme) registry could
        // exfiltrate the token. npm's own default is to refuse auth over http.
        if (!isHttps(registry.getUrl())) {
            return;
        }
        if (registry.getAuthToken() != null) {
            builder.withAuthentication("Bearer", registry.getAuthToken());
        } else if (registry.getAuthBase64() != null) {
            builder.withAuthentication("Basic", registry.getAuthBase64());
        } else if (registry.getUsername() != null) {
            builder.withBasicAuthentication(registry.getUsername(), registry.getPassword());
        }
    }

    private static boolean isHttps(String url) {
        return url.regionMatches(true, 0, "https://", 0, "https://".length());
    }

    private static String base(NodeRegistry registry) {
        String url = safeUrl(registry);
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /**
     * The registry URL with any userinfo stripped, so a {@code user:token@host} credential never reaches a failure
     * detail.
     */
    private static String safeUrl(NodeRegistry registry) {
        return Urls.stripUserinfo(registry.getUrl());
    }
}
