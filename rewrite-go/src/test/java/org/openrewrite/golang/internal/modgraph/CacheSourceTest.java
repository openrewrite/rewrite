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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class CacheSourceTest {

    @Test
    void readsGoModAndPackageFilesFromTheStandardCacheLayout(@TempDir Path gomodcache) throws Exception {
        // Lay out cache/download/<esc>/@v/<ver>.{mod,zip} as `go mod download` would.
        String mod = "github.com/Example/Dep"; // uppercase -> escaped on disk
        String ver = "v1.2.3";
        String esc = "github.com/!example/!dep";
        Path dl = gomodcache.resolve("cache/download").resolve(esc).resolve("@v");
        Files.createDirectories(dl);
        Files.writeString(dl.resolve(ver + ".mod"),
          "module github.com/Example/Dep\n\ngo 1.21\n");

        // A module zip with a package `pkg` importing a unique module.
        byte[] zip = zip(Map.of(
          mod + "@" + ver + "/pkg/util.go", "package util\n\nimport _ \"example.com/other\"\n",
          mod + "@" + ver + "/go.mod", "module github.com/Example/Dep\n"
        ));
        Files.write(dl.resolve(ver + ".zip"), zip);

        CacheSource cache = new CacheSource(gomodcache.toString());

        byte[] goMod = cache.goMod(mod, ver);
        assertThat(goMod).isNotNull();
        assertThat(new String(goMod, StandardCharsets.UTF_8)).contains("module github.com/Example/Dep");

        Map<String, byte[]> files = cache.packageGoFiles(mod, ver, mod + "/pkg");
        assertThat(files).isNotNull().containsKey("util.go");
        assertThat(GoImports.parse(new String(files.get("util.go"), StandardCharsets.UTF_8)))
          .containsExactly("example.com/other");
    }

    @Test
    void missingModuleReturnsNull(@TempDir Path gomodcache) {
        CacheSource cache = new CacheSource(gomodcache.toString());
        assertThat(cache.goMod("github.com/absent/mod", "v1.0.0")).isNull();
        assertThat(cache.packageGoFiles("github.com/absent/mod", "v1.0.0", "github.com/absent/mod")).isNull();
    }

    private static byte[] zip(Map<String, String> entries) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bos)) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return bos.toByteArray();
    }
}
