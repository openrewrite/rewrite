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
import org.openrewrite.javascript.internal.lock.RecordedRegistry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * The registry documents a scenario recorded for the phase that read a lock, served only for a registry pnpm asked
 * then, with a 404 where pnpm's log reports one and a fixture's empty versions filled from its version documents.
 */
final class ScenarioRegistry extends RecordedRegistry<PnpmScenarios.Scenario> implements RegistryMetadata {

    /**
     * A document the recording does not hold.
     */
    static final class RouteNotRecorded extends RuntimeException {
        RouteNotRecorded(String route) {
            super("not recorded: " + route);
        }
    }

    private final String phase;

    ScenarioRegistry(PnpmScenarios.Scenario scenario, String phase) {
        super(scenario);
        this.phase = phase;
    }

    @Override
    public @Nullable String packument(String registry, String name, boolean fullMetadata) {
        Set<String> hosts = scenario.registryHosts(phase);
        if (!hosts.contains(cacheDirectory(registry))) {
            throw new RouteNotRecorded("GET " + registry + name + " (pnpm asked " + String.join(", ", hosts) + ")");
        }
        String document = scenario.packument(name, fullMetadata, phase);
        if (document == null) {
            if (answered404(registry, name)) {
                return null;
            }
            throw new RouteNotRecorded("GET " + registry + name + (fullMetadata ? " (full)" : ""));
        }
        return scenario.recordedWith() == null ? filled(name, document) : document;
    }

    // the recorder never saves this route; every request pnpm made for it answered 404
    @Override
    public @Nullable String attestations(String registry, String name, String version) {
        return null;
    }

    // a document outside the registry, at http-url/<host><path> (gzipped when it ends .gz)
    @Override
    public @Nullable String document(String url) {
        URI uri = URI.create(url);
        Path file = scenario.getDir().resolve("http-url").resolve(uri.getHost() + uri.getPath());
        Path gz = file.resolveSibling(file.getFileName() + ".gz");
        try {
            if (Files.isRegularFile(file)) {
                return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            }
            if (Files.isRegularFile(gz)) {
                try (InputStream in = new GZIPInputStream(Files.newInputStream(gz))) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) >= 0) {
                        out.write(buf, 0, n);
                    }
                    return new String(out.toByteArray(), StandardCharsets.UTF_8);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        throw new RouteNotRecorded("GET " + url);
    }

    private boolean answered404(String registry, String name) {
        String route = "GET " + registry + (name.startsWith("@") ? name.replace("/", "%2F") : name) +
                       ": Not Found - 404";
        for (String file : scenario.files()) {
            String log = file.startsWith("pnpm-") && file.endsWith(".log") ? scenario.file(file) : null;
            if (log != null && log.contains(route)) {
                return true;
            }
        }
        return false;
    }

    // an empty version entry a lock of the fixture names, and no document of it, is a route not recorded; the shared
    // cache's packument serves it when there is one
    @SuppressWarnings("unchecked")
    private String filled(String name, String document) {
        Object meta = Manifests.parseValue(document, name);
        if (!(meta instanceof Map) || !(((Map<String, Object>) meta).get("versions") instanceof Map)) {
            return document;
        }
        Map<String, Object> versions = (Map<String, Object>) ((Map<String, Object>) meta).get("versions");
        for (Map.Entry<String, Object> version : versions.entrySet()) {
            if (version.getValue() instanceof Map && ((Map<?, ?>) version.getValue()).isEmpty()) {
                String manifest = scenario.http(name + "-" + version.getKey());
                if (manifest != null) {
                    version.setValue(Manifests.parseValue(manifest, name + "-" + version.getKey()));
                } else if (named(name + "@" + version.getKey())) {
                    String cached = cached(name);
                    if (cached != null) {
                        return cached;
                    }
                    throw new RouteNotRecorded("GET " + name + "/" + version.getKey());
                }
            }
        }
        return Js.jsonStringify(meta);
    }

    private boolean named(String pkgId) {
        for (String lock : new String[]{"before", "after"}) {
            String text = scenario.file(lock);
            if (text != null && (text.contains(pkgId + ":") || text.contains("'" + pkgId + "':"))) {
                return true;
            }
        }
        return false;
    }

    private @Nullable String cached(String name) {
        Path cache = scenario.getHttpCache();
        if (cache == null || !Files.isRegularFile(cache.resolve(name))) {
            return null;
        }
        try {
            return new String(Files.readAllBytes(cache.resolve(name)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // the directory pnpm's metadata cache keeps a registry's documents in: its host, a port after a +
    private static String cacheDirectory(String registry) {
        URI uri = URI.create(registry);
        return uri.getPort() == -1 ? uri.getHost() : uri.getHost() + "+" + uri.getPort();
    }
}
