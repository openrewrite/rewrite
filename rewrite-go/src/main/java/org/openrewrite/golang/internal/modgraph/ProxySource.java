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
package org.openrewrite.golang.internal.modgraph;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ipc.http.HttpSender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fetches module metadata from one or more GOPROXY bases over the CLI's
 * {@link HttpSender} — proxy, auth, and TLS are honored, and crucially the HTTP
 * stays entirely inside the host process (no peer-initiated fetch crosses the
 * RPC boundary). When a write-through cache directory is configured, fetched
 * {@code .mod}/{@code .zip} are persisted into the standard
 * {@code $GOMODCACHE/cache/download} layout so later lookups (and the real
 * {@code go} toolchain) serve them offline. Writes are atomic (temp + move).
 */
public final class ProxySource implements ModSource {

    private final List<String> bases;
    private final HttpSender http;
    private final @Nullable Path cacheDir; // GOMODCACHE/cache/download, or null for no write-through
    private final Map<String, @Nullable Map<String, byte[]>> zips = new HashMap<>();

    public ProxySource(String goproxy, HttpSender http) {
        this(goproxy, http, null);
    }

    /** @param gomodcache enables write-through into {@code <gomodcache>/cache/download}; null disables it. */
    public ProxySource(String goproxy, HttpSender http, @Nullable String gomodcache) {
        this.http = http;
        this.cacheDir = gomodcache == null ? null : Paths.get(gomodcache).resolve("cache").resolve("download");
        this.bases = parseProxyList(goproxy);
    }

    private static List<String> parseProxyList(String goproxy) {
        List<String> out = new ArrayList<>();
        if (goproxy != null) {
            for (String p : goproxy.split("[,|]")) {
                p = p.trim();
                if (p.isEmpty() || p.equals("off") || p.equals("direct") || p.equals("none")) {
                    continue;
                }
                out.add(p.replaceAll("/+$", ""));
            }
        }
        if (out.isEmpty()) {
            out.add("https://proxy.golang.org");
        }
        return out;
    }

    @Override
    public byte @Nullable [] goMod(String path, String version) {
        String ep = ModulePath.escapePath(path);
        String ev = ModulePath.escapeVersion(version);
        byte[] body = fetch("/" + ep + "/@v/" + ev + ".mod");
        if (body != null) {
            persist(ep, ev, ".mod", body);
        }
        return body;
    }

    @Override
    public @Nullable Map<String, byte[]> packageGoFiles(String modPath, String version, String importPath) {
        Map<String, byte[]> entries = moduleZip(modPath, version);
        if (entries == null) {
            return null;
        }
        return Zips.packageFiles(entries, modPath, version, importPath);
    }

    private @Nullable Map<String, byte[]> moduleZip(String modPath, String version) {
        String key = modPath + "@" + version;
        if (zips.containsKey(key)) {
            return zips.get(key);
        }
        String ep = ModulePath.escapePath(modPath);
        String ev = ModulePath.escapeVersion(version);
        byte[] raw = fetch("/" + ep + "/@v/" + ev + ".zip");
        Map<String, byte[]> entries = raw == null ? null : Zips.goFiles(raw);
        if (raw != null && entries != null) {
            persist(ep, ev, ".zip", raw); // write through only a well-formed zip
        }
        zips.put(key, entries); // memoize (null records a failed download)
        return entries;
    }

    private byte @Nullable [] fetch(String suffix) {
        for (String base : bases) {
            try (HttpSender.Response resp = http.send(http.get(base + suffix).build())) {
                if (resp.isSuccessful()) {
                    byte[] body = resp.getBodyAsBytes();
                    if (body != null && body.length > 0) {
                        return body;
                    }
                }
            } catch (Exception ignored) {
                // try the next base
            }
        }
        return null;
    }

    private void persist(String ep, String ev, String suffix, byte[] content) {
        if (cacheDir == null) {
            return;
        }
        Path dir = cacheDir.resolve(ep).resolve("@v");
        Path target = dir.resolve(ev + suffix);
        try {
            Files.createDirectories(dir);
            Path tmp = Files.createTempFile(dir, ".tmp-", suffix);
            Files.write(tmp, content);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ignored) {
            // best-effort: a cache write failure never affects resolution
        }
    }
}
