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
package org.openrewrite.golang.internal;

import lombok.experimental.UtilityClass;
import org.jspecify.annotations.Nullable;
import org.openrewrite.golang.marker.GoResolutionResult.Require;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Where a module's extracted sources live, and the {@code go mod download} that puts them there.
 * The Go RPC server's {@code ProjectImporter} type-checks a third-party import against those
 * sources, so real types for a {@code require} start with getting the module into this cache.
 */
@UtilityClass
public class ModuleCache {

    /**
     * go.mod contents a download has already been attempted for, so a run whose modules are
     * unpublished or whose proxy is unreachable pays that cost once.
     */
    private final Set<String> attempted = ConcurrentHashMap.newKeySet();

    /**
     * Mirrors {@code parser.GoModCache()} on the Go side — {@code $GOMODCACHE}, else the first
     * {@code $GOPATH} entry's {@code pkg/mod}, else {@code ~/go/pkg/mod} — so both ends of the
     * RPC agree on which directory holds a module's sources.
     */
    public Path location() {
        String goModCache = System.getenv("GOMODCACHE");
        if (goModCache != null && !goModCache.trim().isEmpty()) {
            return Paths.get(goModCache.trim());
        }
        String goPath = System.getenv("GOPATH");
        if (goPath == null || goPath.trim().isEmpty()) {
            goPath = System.getProperty("user.home") + File.separator + "go";
        } else {
            int sep = goPath.indexOf(File.pathSeparatorChar);
            goPath = sep >= 0 ? goPath.substring(0, sep) : goPath;
        }
        return Paths.get(goPath.trim(), "pkg", "mod");
    }

    /**
     * Whether {@code modulePath@version} has its sources extracted in the cache.
     */
    public boolean contains(String modulePath, String version) {
        return Files.isDirectory(location().resolve(directoryName(modulePath, version)));
    }

    /**
     * Extract the sources of every {@code require} the cache is missing, so a go.mod it already
     * satisfies needs neither network nor toolchain. A missing {@code go}, or a download that
     * fails, leaves the module absent and the parse that follows attributes it to an empty stub.
     */
    public void download(String goModContent, Collection<Require> requires) {
        List<String> missing = new ArrayList<>();
        for (Require require : requires) {
            if (!contains(require.getModulePath(), require.getVersion())) {
                missing.add(require.getModulePath() + "@" + require.getVersion());
            }
        }
        if (missing.isEmpty() || !attempted.add(goModContent)) {
            return;
        }
        String go = GoExecutor.GO.find();
        if (go == null) {
            return;
        }
        Path workDir = null;
        try {
            workDir = Files.createTempDirectory("openrewrite-go-download");
            Files.write(workDir.resolve("go.mod"), goModContent.getBytes(StandardCharsets.UTF_8));
            List<String> args = new ArrayList<>();
            args.add("mod");
            args.add("download");
            args.addAll(missing);
            // The server reads the cache location from its environment, which the `go env` config
            // file would override for this download alone; pinning it keeps the two in agreement.
            Map<String, String> env = new HashMap<>();
            env.put("GOMODCACHE", location().toString());
            GoExecutor.GO.run(workDir, go, env, args.toArray(new String[0]));
        } catch (IOException ignored) {
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            deleteRecursively(workDir);
        }
    }

    /**
     * Go stores {@code github.com/BurntSushi/toml@v1.0.0-RC1} under
     * {@code github.com/!burnt!sushi/toml@v1.0.0-!r!c1} so that coordinates differing only in case
     * stay distinct on a case-insensitive filesystem. Path and version take the same escaping.
     */
    String directoryName(String modulePath, String version) {
        return escape(modulePath) + "@" + escape(version);
    }

    private String escape(String pathOrVersion) {
        StringBuilder escaped = new StringBuilder(pathOrVersion.length());
        for (int i = 0; i < pathOrVersion.length(); i++) {
            char c = pathOrVersion.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                escaped.append('!').append((char) (c - 'A' + 'a'));
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }

    private void deleteRecursively(@Nullable Path dir) {
        if (dir == null || !Files.exists(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException ignored) {
                }
            });
        } catch (IOException ignored) {
        }
    }
}
