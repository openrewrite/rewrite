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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Reads module metadata from the local Go module cache (the value of {@code go
 * env GOMODCACHE}). go.mod files live under {@code cache/download/<esc>/@v/}; a
 * package's sources come from the extracted module tree when present, otherwise
 * from the cached {@code .zip} that the write-through proxy persists — so a clean
 * checkout can serve dependency sources without any {@code go} extraction step.
 */
public final class CacheSource implements ModSource {

    private final Path root;     // GOMODCACHE; extracted module dirs live directly under it
    private final Path download; // GOMODCACHE/cache/download

    public CacheSource(String gomodcache) {
        this.root = Paths.get(gomodcache);
        this.download = root.resolve("cache").resolve("download");
    }

    @Override
    public byte @Nullable [] goMod(String path, String version) {
        Path p = download.resolve(ModulePath.escapePath(path)).resolve("@v")
          .resolve(ModulePath.escapeVersion(version) + ".mod");
        try {
            return Files.readAllBytes(p);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public @Nullable Map<String, byte[]> packageGoFiles(String modPath, String version, String importPath) {
        String ep = ModulePath.escapePath(modPath);
        String ev = ModulePath.escapeVersion(version);

        // Preferred: the extracted module tree ($GOMODCACHE/<esc>@<ver>/<rel>).
        String rel = importPath.startsWith(modPath) ? importPath.substring(modPath.length()) : importPath;
        if (rel.startsWith("/")) {
            rel = rel.substring(1);
        }
        Path dir = root.resolve(ep + "@" + ev);
        for (String seg : rel.split("/")) {
            if (!seg.isEmpty()) {
                dir = dir.resolve(seg);
            }
        }
        if (Files.isDirectory(dir)) {
            Map<String, byte[]> files = new HashMap<>();
            try (Stream<Path> list = Files.list(dir)) {
                list.filter(f -> f.getFileName().toString().endsWith(".go") && Files.isRegularFile(f))
                  .forEach(f -> {
                      try {
                          files.put(f.getFileName().toString(), Files.readAllBytes(f));
                      } catch (IOException ignored) {
                      }
                  });
            } catch (IOException ignored) {
            }
            if (!files.isEmpty()) {
                return files;
            }
        }

        // Fallback: the cached download zip.
        Path zip = download.resolve(ep).resolve("@v").resolve(ev + ".zip");
        try {
            Map<String, byte[]> entries = Zips.goFiles(Files.readAllBytes(zip));
            if (entries != null) {
                return Zips.packageFiles(entries, modPath, version, importPath);
            }
        } catch (IOException ignored) {
        }
        return null;
    }
}
