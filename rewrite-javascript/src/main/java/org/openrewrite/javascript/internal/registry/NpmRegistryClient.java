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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ipc.http.HttpSender;
import org.openrewrite.ipc.http.HttpUrlConnectionSender;
import org.openrewrite.javascript.NodeRegistry;
import org.openrewrite.javascript.internal.registry.NodeRegistryException.Reason;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A two-step npm registry client: the abbreviated packument to select a version over a range, then
 * the single-version manifest for the byte-exact locked entry. Results are cached in memory per run,
 * keyed by (registry, name[, version]). Mirrors the Python {@code SimpleIndexClient}.
 */
public class NpmRegistryClient {
    private static final String PACKUMENT_ACCEPT = "application/vnd.npm.install-v1+json";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final HttpSender httpSender;
    private final Map<String, AbbreviatedPackument> packumentCache = new ConcurrentHashMap<>();
    private final Map<String, VersionManifest> manifestCache = new ConcurrentHashMap<>();

    public NpmRegistryClient(HttpSender httpSender) {
        this.httpSender = httpSender;
    }

    public AbbreviatedPackument getPackument(NodeRegistry registry, String name) {
        String key = registry.getUrl() + "#" + name;
        AbbreviatedPackument cached = packumentCache.get(key);
        if (cached == null) {
            cached = fetchPackument(registry, name);
            packumentCache.put(key, cached);
        }
        return cached;
    }

    public VersionManifest getManifest(NodeRegistry registry, String name, String version) {
        String key = registry.getUrl() + "#" + name + "@" + version;
        VersionManifest cached = manifestCache.get(key);
        if (cached == null) {
            cached = fetchManifest(registry, name, version);
            manifestCache.put(key, cached);
        }
        return cached;
    }

    private AbbreviatedPackument fetchPackument(NodeRegistry registry, String name) {
        guard(registry);
        String url = base(registry) + "/" + Urls.encodeName(name);
        byte[] body = send(registry, name, null, url, PACKUMENT_ACCEPT, true);
        try {
            return parsePackument(name, body);
        } catch (Exception e) {
            throw new NodeRegistryException(Reason.MALFORMED_MANIFEST, registry.getUrl(), name, null,
                    "Malformed packument JSON from " + url, e);
        }
    }

    private VersionManifest fetchManifest(NodeRegistry registry, String name, String version) {
        guard(registry);
        String url = base(registry) + "/" + Urls.encodeName(name) + "/" + version;
        byte[] body = send(registry, name, version, url, HttpSender.Request.Builder.APPLICATION_JSON, false);
        try {
            return parseManifest(body);
        } catch (Exception e) {
            throw new NodeRegistryException(Reason.MALFORMED_MANIFEST, registry.getUrl(), name, version,
                    "Malformed manifest JSON from " + url, e);
        }
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
                throw new NodeRegistryException(Reason.AUTH_FAILED, registry.getUrl(), name, version,
                        "HTTP " + code + " from " + url, null);
            }
            if (code == 404) {
                Reason reason = packument ? Reason.PACKAGE_NOT_FOUND : Reason.VERSION_NOT_FOUND;
                throw new NodeRegistryException(reason, registry.getUrl(), name, version,
                        "HTTP 404 from " + url, null);
            }
            if (!response.isSuccessful()) {
                throw new NodeRegistryException(Reason.UNREACHABLE, registry.getUrl(), name, version,
                        "HTTP " + code + " from " + url, null);
            }
            return response.getBodyAsBytes();
        } catch (NodeRegistryException e) {
            throw e;
        } catch (Exception e) {
            throw new NodeRegistryException(Reason.UNREACHABLE, registry.getUrl(), name, version,
                    "Failed to fetch " + url + ": " + e, e);
        }
    }

    private void guard(NodeRegistry registry) {
        if (registry.isUnresolvedPlaceholders()) {
            throw new NodeRegistryException(Reason.UNREACHABLE, registry.getUrl(),
                    "Registry URL or credentials contain unresolved environment placeholders: " + registry.getUrl());
        }
        // The default sender cannot load a custom CA or disable strict SSL; fail loud rather than
        // ignore the config and later throw an opaque handshake error.
        if ((registry.getCafile() != null || !registry.isStrictSsl()) &&
                httpSender.getClass() == HttpUrlConnectionSender.class) {
            throw new NodeRegistryException(Reason.UNREACHABLE, registry.getUrl(),
                    "Registry " + registry.getUrl() + " requires a custom CA (cafile/strict-ssl) but no " +
                            "TLS-capable HttpSender was injected");
        }
    }

    private static void applyAuth(HttpSender.Request.Builder builder, NodeRegistry registry) {
        if (registry.getAuthToken() != null) {
            builder.withAuthentication("Bearer", registry.getAuthToken());
        } else if (registry.getAuthBase64() != null) {
            builder.withAuthentication("Basic", registry.getAuthBase64());
        } else if (registry.getUsername() != null) {
            builder.withBasicAuthentication(registry.getUsername(), registry.getPassword());
        }
    }

    private static String base(NodeRegistry registry) {
        String url = registry.getUrl();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static AbbreviatedPackument parsePackument(String name, byte[] body) throws Exception {
        JsonNode root = MAPPER.readTree(body);
        Map<String, String> distTags = new LinkedHashMap<>();
        JsonNode tags = root.get("dist-tags");
        if (tags != null && tags.isObject()) {
            for (Iterator<Map.Entry<String, JsonNode>> it = tags.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                distTags.put(entry.getKey(), entry.getValue().asText());
            }
        }
        Set<String> versions = new LinkedHashSet<>();
        JsonNode versionsNode = root.get("versions");
        if (versionsNode != null && versionsNode.isObject()) {
            for (Iterator<String> it = versionsNode.fieldNames(); it.hasNext(); ) {
                versions.add(it.next());
            }
        }
        String resolvedName = root.hasNonNull("name") ? root.get("name").asText() : name;
        return new AbbreviatedPackument(resolvedName, distTags, versions);
    }

    private static VersionManifest parseManifest(byte[] body) throws Exception {
        JsonNode root = MAPPER.readTree(body);
        String name = text(root, "name");
        String version = text(root, "version");

        JsonNode licenseNode = root.get("license");
        String licenseString = licenseString(licenseNode, root.get("licenses"));

        VersionManifest.Dist dist = null;
        JsonNode distNode = root.get("dist");
        if (distNode != null && distNode.isObject()) {
            dist = new VersionManifest.Dist(text(distNode, "tarball"), text(distNode, "shasum"),
                    text(distNode, "integrity"));
        }

        Map<String, VersionManifest.PeerMeta> peerMeta = null;
        JsonNode peerMetaNode = root.get("peerDependenciesMeta");
        if (peerMetaNode != null && peerMetaNode.isObject()) {
            peerMeta = new LinkedHashMap<>();
            for (Iterator<Map.Entry<String, JsonNode>> it = peerMetaNode.fields(); it.hasNext(); ) {
                Map.Entry<String, JsonNode> entry = it.next();
                JsonNode optional = entry.getValue().get("optional");
                peerMeta.put(entry.getKey(),
                        new VersionManifest.PeerMeta(optional == null ? null : optional.asBoolean()));
            }
        }

        Map<String, String> scripts = stringMap(root.get("scripts"));
        Boolean hasInstallScript;
        if (root.has("hasInstallScript")) {
            hasInstallScript = root.get("hasInstallScript").asBoolean();
        } else {
            hasInstallScript = deriveInstallScript(scripts);
        }

        return new VersionManifest(
                name,
                version,
                licenseNode,
                licenseString,
                stringMap(root.get("dependencies")),
                stringMap(root.get("optionalDependencies")),
                stringMap(root.get("peerDependencies")),
                peerMeta,
                root.get("bin"),
                stringMap(root.get("engines")),
                stringList(root.get("os")),
                stringList(root.get("cpu")),
                stringList(root.get("libc")),
                hasInstallScript,
                scripts,
                bundleDependencies(root),
                text(root, "deprecated"),
                root.has("_hasShrinkwrap") ? root.get("_hasShrinkwrap").asBoolean() : null,
                dist);
    }

    private static @Nullable Boolean deriveInstallScript(@Nullable Map<String, String> scripts) {
        if (scripts == null) {
            return null;
        }
        return scripts.containsKey("preinstall") || scripts.containsKey("install") ||
                scripts.containsKey("postinstall") ? Boolean.TRUE : null;
    }

    private static @Nullable List<String> bundleDependencies(JsonNode root) {
        JsonNode node = root.get("bundleDependencies");
        if (node == null) {
            node = root.get("bundledDependencies");
        }
        return stringList(node);
    }

    private static @Nullable String licenseString(@Nullable JsonNode license, @Nullable JsonNode licenses) {
        if (license != null) {
            if (license.isTextual()) {
                return license.asText();
            }
            if (license.isObject() && license.hasNonNull("type")) {
                return license.get("type").asText();
            }
        }
        if (licenses != null && licenses.isArray() && licenses.size() > 0) {
            JsonNode first = licenses.get(0);
            if (first.isTextual()) {
                return first.asText();
            }
            if (first.isObject() && first.hasNonNull("type")) {
                return first.get("type").asText();
            }
        }
        return null;
    }

    private static @Nullable String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private static @Nullable Map<String, String> stringMap(@Nullable JsonNode node) {
        if (node == null || !node.isObject()) {
            return null;
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext(); ) {
            Map.Entry<String, JsonNode> entry = it.next();
            map.put(entry.getKey(), entry.getValue().asText());
        }
        return map;
    }

    private static @Nullable List<String> stringList(@Nullable JsonNode node) {
        if (node == null || !node.isArray()) {
            return null;
        }
        List<String> list = new ArrayList<>(node.size());
        for (JsonNode element : node) {
            list.add(element.asText());
        }
        return list;
    }
}
